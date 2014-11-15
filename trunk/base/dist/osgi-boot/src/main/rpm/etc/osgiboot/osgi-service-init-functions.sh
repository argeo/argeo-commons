#!/bin/bash

# Source function library.
. /etc/rc.d/init.d/functions

RETVAL=0

osgi_service_start() {
	APP=$1
	# create log an run directories writable by the application user
	USER=$APP
	GROUP=$APP
	RUN_DIR=/var/run/$APP
	LOG_DIR=/var/log/$APP
	if [ ! -d $LOG_DIR ];then
		mkdir -m 0750 $LOG_DIR
		chown -R $USER.$GROUP $LOG_DIR
	fi
	if [ ! -d $RUN_DIR ];then
		mkdir -m 0750 $RUN_DIR
		chown -R $USER.$GROUP $RUN_DIR
	fi
	
	# call Argeo Commons OSGi utilities as the application user
	daemon --user $USER /usr/sbin/osgi-service $APP start
	
	RETVAL=$?
	#action $"Start $APP" /bin/true
	if [ $RETVAL -eq 0 ];then
		PID=`cat $RUN_DIR/$APP.pid`
		action $"Started $APP with pid $PID" /bin/true
	else
		action $"Could not start $APP" /bin/false
	fi
	return $RETVAL
}

osgi_service_stop() {
	APP=$1
	USER=$APP
	# call Argeo Commons OSGi utilities as the application user
	runuser -s /bin/bash $USER -c "/usr/sbin/osgi-service $APP stop"
	RETVAL=$?
	if [ $RETVAL -eq 0 ];then
		action $"Stopped $APP" /bin/true
	else
		action $"Could not stop $APP" /bin/false
	fi
	return $RETVAL
}

osgi_service_status() {
	APP=$1
	USER=$APP
	# call Argeo Commons OSGi utilities as the application user
	runuser -s /bin/bash $USER -c "/usr/sbin/osgi-service $APP status"
	RETVAL=$?
	return $RETVAL
}
