#!/bin/sh

SDK_DIR="$(cd "$(dirname "$0")"; pwd -P)"
A2_UPSTREAM="$(cd "$SDK_DIR/a2/upstream"; pwd -P)"


mvn -f $SDK_DIR clean assembly:single -Pa2-provided
rsync -rv $SDK_DIR/target/sdk-*-a2-provided/ $A2_UPSTREAM
bnd index -d $A2_UPSTREAM/ */*.jar

