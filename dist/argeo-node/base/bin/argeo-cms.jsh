// Run from base directory with:
// ./bin/a2sh --startup ./share/argeo/cms.jsh ./bin/argeo-cms.jsh

osgi.setHttpPort(7080);
osgi.conf("argeo.node.useradmin.uris", "os:///");
osgi.setClean(true);

// LAUNCH
osgi.launch();