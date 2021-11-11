#!/bin/bash

# Works on Fedora 34
JVM=/usr/lib/jvm/jre-11/bin/java
ECJ_JAR=/usr/share/java/ecj/ecj.jar
OSGI_JAR=/usr/share/java/eclipse/osgi.jar


SOURCE_PATH=
for bundle in ../*.*.*/ ; do
SOURCE_PATH="$SOURCE_PATH $bundle/src[-d $bundle/bin]"
done

echo ### COMPILATION ###
time $JVM -jar $ECJ_JAR @ecj.args -time -cp $OSGI_JAR:"$(printf %s: target/sdk-*-a2-target/*/*.jar)" $SOURCE_PATH 

echo ### PACKAGING ###
