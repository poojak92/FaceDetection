#!/bin/bash

./build.sh design
./build.sh core
./build.sh facialcaptureworkflow
./build.sh misnapworkflow
./build.sh mitekplugin
./build.sh demoplugin
./build.sh app
./gradlew --refresh-dependencies :libtest:clean :libtest:build
./gradlew --refresh-dependencies :a1test:clean :a1test:build
