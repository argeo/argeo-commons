#!/home/mbaudier/dev/git/apache2/argeo-commons/dist/osgi-boot/src/main/rpm/usr/bin/a2jjs
// demo specific
var app = "node";
var demoHome = $ENV.HOME + "/dev/git/apache2/argeo-commons/demo";
var appHome = demoHome + "/exec/argeo_node.js";
var appConf = demoHome;
var policyFile = "all.policy";

// CMS config
load("../dist/argeo-node/rpm/usr/share/node/jjs/cms.js");
// Provisioning
osgi.baseUrl = "http://forge.argeo.org/data/java/argeo-2.1/";
osgi.install("org.argeo.commons:org.argeo.dep.cms.platform:2.1.70");
// HTTP
osgi.conf("org.osgi.service.http.port", 0);

// osgi.conf("osgi.clean", true);
osgi.launch();
openUi();
