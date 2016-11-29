#!/bin/sh

for i in "$@"
do
    case $i in
        --tsj=*)
            TILE_SERVER_JAR="${i#*=}"
            shift;;
        --site=*)
            SITE_TGZ="${i#*=}"
            shift;;
        --s3u=*)
            S3U="${i#*=}"
            shift;;
        --backend=*)
            BACKEND="${i#*=}"
            shift;;
    esac
done

# This scripts bootstraps each node in the the EMR cluster to install PDAL.

# Ensure that Spark knows where to find things.
sudo aws s3 cp s3://geotrellis-test/pdal-test/environment /etc/environment

# Install minimal explicit dependencies.
sudo yum -y install git geos-devel libcurl-devel cmake libtiff-devel

# laz-perf
cd ~
git clone https://github.com/verma/laz-perf.git laz-perf
cd laz-perf
cmake .
make
sudo make install

# laszip
cd ~
git clone https://github.com/LASzip/LASzip.git laszip
cd laszip
git checkout e7065cbc5bdbbe0c6e50c9d93d1cd346e9be6778  # Yes this is necessary. See https://github.com/PDAL/PDAL/issues/1205
cmake .
make
sudo make install

# proj4
cd ~
wget https://github.com/OSGeo/proj.4/archive/4.9.3.zip
unzip 4.9.3.zip
cd proj.4-4.9.3
cmake .
make
sudo make install

# libgeotiff
cd ~
wget http://download.osgeo.org/geotiff/libgeotiff/libgeotiff-1.4.2.zip
unzip libgeotiff-1.4.2.zip
cd libgeotiff-1.4.2
cmake .
make
sudo make install

# jsoncpp
cd ~
wget https://github.com/open-source-parsers/jsoncpp/archive/1.7.7.zip
unzip 1.7.7.zip
cd jsoncpp-1.7.7
cmake . -DBUILD_SHARED_LIBS=ON  # Need BUILD_SHARED_LIBS or pdal fails.
make
sudo make install

# Compile/install GDAL
cd ~
git clone https://github.com/OSGeo/gdal.git
cd gdal/gdal
./configure
make
sudo make install

# Compile/install PDAL
cd ~
git clone https://github.com/pomadchin/PDAL.git pdal
cd pdal
git checkout feature/pdal-jni
cmake . -DWITH_LAZPERF=ON -DWITH_GEOTIFF=ON -DWITH_LASZIP=ON
make
sudo make install

# Compile the JNI bindings ourselves.
cd /home/hadoop/pdal/java
./sbt native/nativeCompile
sudo cp /home/hadoop/pdal/java/native/target/native/x86_64-linux/bin/libpdaljni.1.4.so /usr/local/lib/

# Copy prebuilt JNI bindings from S3.
#cd ~
#aws s3 cp s3://geotrellis-test/pdal-test/geotrellis-pdal-assembly-0.1.0-SNAPSHOT.jar /tmp/geotrellis-pdal-assembly-0.1.0-SNAPSHOT.jar
#sudo aws s3 cp s3://geotrellis-test/pdal-test/libpdaljni.1.4.so /usr/local/lib/libpdaljni.1.4.so

# spark-submit --conf spark.driver.extraJavaOptions="-Djava.library.path=/usr/local/lib/" --conf spark.executor.extraJavaOptions="-Djava.library.path=/usr/local/lib/" --class com.azavea.PackedPointCount /tmp/geotrellis-pdal-assembly-0.1.0-SNAPSHOT.jar whitestare/test/lidar/Classified_LAS/