include config-aws.mk			# Vars related to AWS credentials and services used
include config-emr.mk	    # Vars related to type and size of EMR cluster
include config-ingest.mk  # Vars related to ingest step and spark parameters

POINTCLOUD_ASSEMBLY := target/scala-2.11/geotrellis-pdal-assembly-0.1.0-SNAPSHOT.jar
SCRIPT_RUNNER := s3://elasticmapreduce/libs/script-runner/script-runner.jar

ifeq ($(USE_SPOT),true)
MASTER_BID_PRICE:=BidPrice=${MASTER_PRICE},
WORKER_BID_PRICE:=BidPrice=${WORKER_PRICE},
BACKEND=accumulo
endif

ifdef COLOR
COLOR_TAG=--tags Color=${COLOR}
endif

ifndef CLUSTER_ID
CLUSTER_ID=$(shell if [ -e "cluster-id.txt" ]; then cat cluster-id.txt; fi)
endif

rwildcard=$(foreach d,$(wildcard $1*),$(call rwildcard,$d/,$2) $(filter $(subst *,%,$2),$d))

${POINTCLOUD_ASSEMBLY}: $(call rwildcard, src, *.scala) build.sbt
	./sbt assembly -no-colors
	@touch -m ${POINTCLOUD_ASSEMBLY}

upload-code: ${POINTCLOUD_ASSEMBLY} scripts/emr/*
	@aws s3 cp scripts/emr/bootstrap-pdal.sh ${S3_URI}/
	@aws s3 cp ${POINTCLOUD_ASSEMBLY} ${S3_URI}/

create-cluster:
	aws emr create-cluster --name "${NAME}" ${COLOR_TAG} \
--release-label emr-5.0.0 \
--output text \
--use-default-roles \
--configurations "file://$(CURDIR)/scripts/configurations.json" \
--log-uri ${S3_URI}/logs \
--ec2-attributes KeyName=${EC2_KEY},SubnetId=${SUBNET_ID} \
--applications Name=Ganglia Name=Hadoop Name=Hue Name=Spark Name=Zeppelin \
--instance-groups \
'Name=Master,${MASTER_BID_PRICE}InstanceCount=1,InstanceGroupType=MASTER,InstanceType=${MASTER_INSTANCE}' \
'Name=Workers,${WORKER_BID_PRICE}InstanceCount=${WORKER_COUNT},InstanceGroupType=CORE,InstanceType=${WORKER_INSTANCE}' \
--bootstrap-actions \
Name=BootstrapPDAL,Path=${S3_URI}/bootstrap-pdal.sh \
| tee cluster-id.txt

run:
	aws emr add-steps --output text --cluster-id ${CLUSTER_ID} \
--steps Type=CUSTOM_JAR,Name="PointCloud benchmark",Jar=command-runner.jar,Args=[\
spark-submit,--master,yarn-cluster,\
--class,demo.LandsatIngestMain,\
--driver-memory,${DRIVER_MEMORY},\
--driver-cores,${DRIVER_CORES},\
--executor-memory,${EXECUTOR_MEMORY},\
--executor-cores,${EXECUTOR_CORES},\
--conf,spark.dynamicAllocation.enabled=true,\
--conf,spark.yarn.executor.memoryOverhead=${YARN_OVERHEAD},\
--conf,spark.yarn.driver.memoryOverhead=${YARN_OVERHEAD},\
${S3_URI}/geotrellis-pdal-assembly-0.1.0-SNAPSHOT.jar,\
${POINTCLOUD_PATH}\
] | cut -f2 | tee last-step-id.txt

wait: INTERVAL:=60
wait: STEP_ID=$(shell cat last-step-id.txt)
wait:
	@while (true); do \
	OUT=$$(aws emr describe-step --cluster-id ${CLUSTER_ID} --step-id ${STEP_ID}); \
	[[ $$OUT =~ (\"State\": \"([A-Z]+)\") ]]; \
	echo $${BASH_REMATCH[2]}; \
	case $${BASH_REMATCH[2]} in \
			PENDING | RUNNING) sleep ${INTERVAL};; \
			COMPLETED) exit 0;; \
			*) exit 1;; \
	esac; \
	done

proxy:
	aws emr socks --cluster-id ${CLUSTER_ID} --key-pair-file "${HOME}/${EC2_KEY}.pem"

ssh:
	aws emr ssh --cluster-id ${CLUSTER_ID} --key-pair-file "${HOME}/${EC2_KEY}.pem"

local-run: ${POINTCLOUD_ASSEMBLY}
	spark-submit --name "PointCloud benchmark ${NAME}" --master "local[4]" --driver-memory 4G \
${POINTCLOUD_ASSEMBLY} \
file://${PWD}/data

define UPSERT_BODY
{
  "Changes": [{
    "Action": "UPSERT",
    "ResourceRecordSet": {
      "Name": "${1}",
      "Type": "CNAME",
      "TTL": 300,
      "ResourceRecords": [{
        "Value": "${2}"
      }]
    }
  }]
}
endef

update-route53: VALUE=$(shell aws emr describe-cluster --output text --cluster-id $(CLUSTER_ID) | egrep "^CLUSTER" | cut -f5)
update-route53: export UPSERT=$(call UPSERT_BODY,${ROUTE53_RECORD},${VALUE})
update-route53:
	@tee scripts/upsert.json <<< "$$UPSERT"
	aws route53 change-resource-record-sets \
--hosted-zone-id ${HOSTED_ZONE} \
--change-batch "file://$(CURDIR)/scripts/upsert.json"

get-logs:
	@aws emr ssh --cluster-id $(CLUSTER_ID) --key-pair-file "${HOME}/${EC2_KEY}.pem" \
		--command "rm -rf /tmp/spark-logs && hdfs dfs -copyToLocal /var/log/spark/apps /tmp/spark-logs"
	@mkdir -p  logs/$(CLUSTER_ID)
	@aws emr get --cluster-id $(CLUSTER_ID) --key-pair-file "${HOME}/${EC2_KEY}.pem" --src "/tmp/spark-logs/" --dest logs/$(CLUSTER_ID)

.PHONY: local-ingest ingest local-tile-server update-route53 get-logs
