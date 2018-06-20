#!/bin/bash

mkdir -p ~/.sbt/0.13/plugins/
mkdir -p ~/.sbt/1.0/plugins/

cp ./semanticdb-config-0.13-v3.scala ~/.sbt/0.13/plugins/
cp ./semanticdb-config-1.0-v3.scala ~/.sbt/1.0/plugins/