#!/usr/bin/a2jjs
load("share/argeo/cms.js");
osgi.httpPort = 8080;
//osgi.conf("argeo.node.useradmin.uris", "os:///");
//osgi.clean = true;
osgi.launch();
