#!/bin/sh
rm *runnable.jar
cp ../calibration-standalone/target/*runnable.jar .
docker build . -t cct:latest