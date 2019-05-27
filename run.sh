#!/bin/bash

cd target

java -Xmx400g -javaagent:"./libs/hjlib-cooperative-0.1.13-SNAPSHOT.jar" -cp ./libs/*:ForwardSimulatorHjLib-1.0-SNAPSHOT-shaded.jar com.intrepiditee.Main "$@"

