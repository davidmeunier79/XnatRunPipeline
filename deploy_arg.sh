#!/bin/bash
set -e
set +x

echo "delete previous build"
rm -rf build

echo "Building plugin..."
./gradlew build
echo "Delete old jar..."
ssh -o PreferredAuthentications=password xnat@xnat  "mv /data/xnat/home/plugins/xnat-plugin-run-pipeline-cluster-$1.jar /data/xnat/home/old_plugins/xnat-plugin-run-pipeline-cluster-$1.ja| exit   "

cd build/libs
echo "Copying new plugin..."
scp -o PreferredAuthentications=password xnat-plugin-run-pipeline-cluster-$1.jar xnat@xnat:/data/xnat/home/plugins

echo "Restarting tomcat8..."
ssh -o PreferredAuthentications=password -t xnat@xnat "sudo systemctl restart tomcat8 | exit   "

