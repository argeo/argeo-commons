include sdk.mk
.PHONY: clean all build-base build-rcp

all: osgi jni

BASE_BUNDLES := \
$(SDK_BUILD_BASE)/org.argeo.init.$(MAJOR).$(MINOR).jar \
$(SDK_BUILD_BASE)/org.argeo.util.$(MAJOR).$(MINOR).jar \
$(SDK_BUILD_BASE)/org.argeo.api.uuid.$(MAJOR).$(MINOR).jar \
$(SDK_BUILD_BASE)/org.argeo.api.acr.$(MAJOR).$(MINOR).jar \
$(SDK_BUILD_BASE)/org.argeo.api.cms.$(MAJOR).$(MINOR).jar \
$(SDK_BUILD_BASE)/org.argeo.cms.tp.$(MAJOR).$(MINOR).jar \
$(SDK_BUILD_BASE)/org.argeo.cms.$(MAJOR).$(MINOR).jar \
 

NATIVE_PROJECTS = org.argeo.api.uuid/jni

jni:
	$(foreach dir, $(NATIVE_PROJECTS), $(MAKE) -C $(dir);)
	
clean:
	$(foreach dir, $(NATIVE_PROJECTS), $(MAKE) -C $(dir) clean;)
	rm -rf $(SDK_BUILD_BASE)/*



JVM := /usr/lib/jvm/jre-11/bin/java
ECJ_JAR := /usr/share/java/ecj/ecj.jar
BND_TOOL := /usr/bin/bnd

osgi : $(BASE_BUNDLES)

$(SDK_BUILD_BASE)/%.$(MAJOR).$(MINOR).jar : $(SDK_SRC_BASE)/*/generated/%.jar
	$(BND_TOOL) build $<
	cp $< $@

$(SDK_SRC_BASE)/*/generated/*.jar : build-base

#base-java-sources : $(shell find org.argeo.* -name '*.java') 

#base-java-classes : $(shell find org.argeo.* -name '*.class') 

# each dir depends on its package directories
#$(SDK_SRC_BASE)/% : $(shell  find */src -type f  | sed "s%/[^/]*$%%" | sort -u)

# non empty package dirs
#$(SDK_SRC_BASE)/*/src/% : $(shell  grep --no-filename ^import '%/*.java' | sed 's/import //g' | sed 's/static //g' | sed 's/\.[A-Za-z0-9_]*;//' | sed 's/\.[A-Z].*//'  | sort |  uniq)

# convert dir to package
#$(shell find %/src -mindepth 1 -type d -printf '%P\n' | sed "s/\//\./g")

# all packages
# grep --no-filename ^import src/org/argeo/api/uuid/*.java | sed "s/import //g" | sed "s/static //g" | sed "s/\.[A-Za-z0-9_]*;//" | sed "s/\.[A-Z].*//"  | sort |  uniq

BASE_CLASSPATH=\
/usr/share/java/osgi-core/osgi.core.jar:$\
/usr/share/java/osgi-compendium/osgi.cmpn.jar:$\
/usr/share/java/ecj/ecj.jar:$\
/usr/share/java/aqute-bnd/biz.aQute.bndlib.jar:$\
/usr/share/java/slf4j/api.jar:$\
/usr/share/java/commons-io.jar:$\
/usr/share/java/commons-cli.jar:$\
/usr/share/java/bcprov.jar:$\
/usr/share/java/bcpkix.jar:$\
/usr/share/java/commons-httpclient3.jar:$\
/usr/share/java/postgresql-jdbc.jar

build-base:
	$(JVM) -jar $(ECJ_JAR) -11 -nowarn -time -cp $(BASE_CLASSPATH) \
	$(SDK_SRC_BASE)/org.argeo.api.uuid/src[-d $(SDK_BUILD_BASE)/org.argeo.api.uuid/bin] \
	$(SDK_SRC_BASE)/org.argeo.api.acr/src[-d $(SDK_BUILD_BASE)/org.argeo.api.acr/bin] \
	$(SDK_SRC_BASE)/org.argeo.api.cms/src[-d $(SDK_BUILD_BASE)/org.argeo.api.cms/bin] \
	$(SDK_SRC_BASE)/org.argeo.init/src[-d $(SDK_BUILD_BASE)/org.argeo.init/bin] \
	$(SDK_SRC_BASE)/org.argeo.util/src[-d $(SDK_BUILD_BASE)/org.argeo.util/bin] \
	$(SDK_SRC_BASE)/org.argeo.cms.tp/src[-d $(SDK_BUILD_BASE)/org.argeo.cms.tp/bin] \
	$(SDK_SRC_BASE)/org.argeo.cms/src[-d $(SDK_BUILD_BASE)/org.argeo.cms/bin] \
	$(SDK_SRC_BASE)/org.argeo.cms.pgsql/src[-d $(SDK_BUILD_BASE)/org.argeo.cms.pgsql/bin] \
	
	#$(BND_TOOL) build

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


