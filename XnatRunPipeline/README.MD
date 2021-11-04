# ICM - XNAT Export Data Plugin

This project adds a plugin which
1. Adds an "Export Data (ICM)" item to the menu of a XNAT project screen
2. This item opens a custom webpage for exporting data
3. The users selects the subject(s) / type of files (DICOM / NIFTI + JSON / BOTH) / folder structure format (XNAT / CENIR / BIDS  [BETA])
4. The webpage returns a zip file which the user can download

# Prerequisites

To build this plugin, you need to install gradle

## Build and use the plugin with XNAT

1. Clone the repository:
```
$ git clone git@gitlab.icm-institute.org:iconics/ICM-XNAT.git
$ cd ICM-XNAT/plugins/xnat-plugin-export-data
```

2. Build using gradle:
```
$ ./gradlew build
```

3. This plugin needs then to be copied to the XNAT plugins folder (see the configuration of XNAT, usually in /data/xnat/home/plugins)

4. Copy the "org.json.simple" JAR to the Tomcat "lib" folder. 
You can obtain the file from [this link](http://central.maven.org/maven2/org/apache/clerezza/ext/org.json.simple/0.4/org.json.simple-0.4.jar).

5. Copy the "org.apache.commons" CSV JAR to the Tomcat "lib" folder.
You can obtain the file from [this link](http://central.maven.org/maven2/org/apache/commons/commons-csv/1.6/commons-csv-1.6.jar).

6. You need then to restart Tomcat

6. The ["dcm2niix"](https://github.com/rordenlab/dcm2niix) tool must be installed
```
$ Go to the link above
$ Follow the install instructions under the "Build command line version with cmake (Linux, MacOS, Windows)" section
$ For the last step, use "make install" instead of "make"
```

## TODO
```
$ Add logging
$ Add a waiting screen while the zip file is generated
$ What to do in case the zip file is very large
```