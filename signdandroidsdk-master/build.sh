#!/bin/bash

./gradlew --info --stacktrace :$1:clean :$1:build :$1:install
