#!/bin/bash

# Works on Fedora 34
JVM=/usr/lib/jvm/jre-11/bin/java
ECJ_JAR=/usr/share/java/ecj/ecj.jar
OSGI_JAR=/usr/share/java/eclipse/osgi.jar

echo PREPARING
SOURCE_PATH=
for bundle in ../*.*.*/ ; do
echo $bundle
# clean
rm -rf $bundle/generated/*
rm -rf $bundle/bin/*
# copy resources
rsync -r --exclude "*.java" $bundle/src/ $bundle/bin
SOURCE_PATH="$SOURCE_PATH $bundle/src[-d $bundle/bin]"
done

echo COMPILING
$JVM -jar $ECJ_JAR @ecj.args -time -cp $OSGI_JAR:"$(printf %s: target/sdk-*-a2-target/*/*.jar)" $SOURCE_PATH 

echo PACKAGING
bnd build

mkdir -p target/a2/org.argeo.commons
cp ../*/generated/*.jar target/a2/org.argeo.commons

echo DONE