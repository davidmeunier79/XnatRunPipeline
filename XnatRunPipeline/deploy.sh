#!/bin/bash
set -e
set +x

echo "Building plugin..."
./gradlew build
echo "Delete old jar..."
ssh xnat@10.164.4.26  "rm /data/xnat/home/plugins/xnat-plugin-icm-export-data-1.0.1.jar | exit   "

cd build/libs
echo "Copying new plugin..."
scp xnat-plugin-icm-export-data-1.0.1.jar xnat@10.164.4.26:/data/xnat/home/plugins

echo "Restarting tomcat8..."
ssh -t xnat@10.164.4.26 "sudo systemctl restart tomcat8 | exit   "


