#!/bin/bash

# TODO source files and allow to override
A2_CATEGORY=org.argeo.commons

# Works on Fedora 34
JVM=/usr/lib/jvm/jre-11/bin/java
ECJ_JAR=/usr/share/java/ecj/ecj.jar
OSGI_JAR=/usr/share/java/eclipse/osgi.jar

SDK_DIR="$(cd "$(dirname "$0")"; pwd -P)"
echo SDK: $SDK_DIR
BUNDLES_BASEDIR="$(cd "$SDK_DIR/.."; pwd -P)"
A2_UPSTREAM="$(cd "$SDK_DIR/a2/upstream"; pwd -P)"
A2_BUILD="$(cd "$SDK_DIR/a2/build"; pwd -P)"

echo PREPARING
SOURCE_PATH=
for bundle in $BUNDLES_BASEDIR/*.*.*/ ; do
echo $bundle
# clean
rm -rf $bundle/generated/*
rm -rf $bundle/bin/*
# copy resources
rsync -r --exclude "*.java" $bundle/src/ $bundle/bin
SOURCE_PATH="$SOURCE_PATH $bundle/src[-d $bundle/bin]"
done

echo COMPILING
$JVM -jar $ECJ_JAR @$SDK_DIR/ecj.args -time -cp $OSGI_JAR:"$(printf %s: $A2_UPSTREAM/*/*.jar)" $SOURCE_PATH 

echo PACKAGING
bnd -b $SDK_DIR build

mkdir -p $A2_BUILD/$A2_CATEGORY
mv $BUNDLES_BASEDIR/*/generated/*.jar $A2_BUILD/$A2_CATEGORY
bnd index -d $A2_BUILD/ */*.jar

echo DONE
