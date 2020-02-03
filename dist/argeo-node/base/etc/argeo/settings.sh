export LANG=en_US.utf8
JAVA_OPTS="-showversion -Xmx128m"

# JMX
#JAVA_OPTS="-showversion -Xmx512m -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7084 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

# Development
#JAVA_OPTS="-ea -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n -showversion -Xmx512m -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7084 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

# JMX over server tunnel
#JAVA_OPTS="-showversion -Xmx2048m -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.rmi.port=7084 -Dcom.sun.management.jmxremote.port=7084 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
# and then: ssh root@remote-host -L 7084:127.0.0.1:7084 -N