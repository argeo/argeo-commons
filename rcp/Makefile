RCP_CLASSPATH=$(BASE_CLASSPATH):$\
$(SDK_BUILD_BASE)/org.argeo.api.uuid/bin:$\
$(SDK_BUILD_BASE)/org.argeo.api.acr/bin:$\
$(SDK_BUILD_BASE)/org.argeo.api.cms/bin:$\
$(SDK_BUILD_BASE)/org.argeo.util/bin:$\
$(SDK_BUILD_BASE)/org.argeo.cms/bin:$\
/usr/share/java/tomcat-servlet-api.jar:$\
/usr/share/java/eclipse/equinox.http.jetty.jar:$\
/usr/lib/java/swt.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.swt.gtk.linux.x86_64_3.116.0.v20210304-1735:$\
/usr/lib/eclipse/plugins/org.eclipse.e4.ui.css.core_0.13.0.v20210304-1735.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.e4.ui.css.swt_0.14.100.v20210304-1735.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.e4.ui.css.swt.theme_0.13.0.v20210304-1735.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.jface_3.22.100.v20210304-1735.jar:$\
/usr/lib/eclipse/plugins/org.eclipse.core.commands_3.9.800.v20210304-1735.jar:$\
/usr/share/java/eclipse/equinox.common.jar:$\
/usr/share/java/sac.jar


build-rcp: build-base
	$(JVM) -jar $(ECJ_JAR) -11 -nowarn -time -cp $(RCP_CLASSPATH) \
	$(SDK_SRC_BASE)/eclipse/org.argeo.cms.servlet/src[-d $(SDK_BUILD_BASE)/eclipse/org.argeo.cms.servlet/bin] \
	$(SDK_SRC_BASE)/rcp/org.argeo.swt.minidesktop/src[-d $(SDK_BUILD_BASE)/rcp/org.argeo.swt.minidesktop/bin] \
	$(SDK_SRC_BASE)/rcp/org.argeo.swt.specific.rcp/src[-d $(SDK_BUILD_BASE)/rcp/org.argeo.swt.specific.rcp/bin] \
	$(SDK_SRC_BASE)/eclipse/org.argeo.cms.swt/src[-d $(SDK_BUILD_BASE)/eclipse/org.argeo.cms.swt/bin] \
	$(SDK_SRC_BASE)/rcp/org.argeo.cms.ui.rcp/src[-d $(SDK_BUILD_BASE)/rcp/org.argeo.cms.ui.rcp/bin] \
