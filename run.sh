#!/bin/bash

cd target

mkdir -p out
mkdir -p ukb
mkdir -p final
mkdir -p ibd
mkdir -p degree
mkdir -p meiosis
mkdir -p map

java -Xmx100g -javaagent:"./libs/hjlib-cooperative-0.1.13-SNAPSHOT.jar" -cp ./libs/*:ForwardSimulatorHjLib-1.0-SNAPSHOT-shaded.jar com.intrepiditee.Main "$@"

