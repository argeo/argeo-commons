// Standard CMS App
APP.webPath = 'cms'

// Common
APP.pageTitle = 'Argeo CMS';

APP.ui['devops'] = new org.argeo.cms.script.AppUi(APP, new org.argeo.cms.e4.rap.CmsE4EntryPointFactory('org.argeo.cms.e4/e4xmi/cms-devops.e4xmi'));
APP.ui['devops'].pageTitle = 'Argeo CMS DevOps';
