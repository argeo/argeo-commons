export LANG=en_US.utf8
JAVA_OPTS="-ea -agentlib:jdwp=transport=dt_socket,server=y,address=*:8000,suspend=n -showversion -Xmx512m -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7084 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
