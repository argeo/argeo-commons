var AppUi = Java.type('org.argeo.cms.script.AppUi')
var CmsE4EntryPointFactory = Java.type('org.argeo.cms.e4.rap.CmsE4EntryPointFactory')

APP.webPath = 'cms'
APP.pageTitle = 'Argeo CMS'
	
APP.ui['devops'] = new AppUi(APP, new CmsE4EntryPointFactory('org.argeo.cms.e4/e4xmi/cms-devops.e4xmi'))
APP.ui['devops'].pageTitle = 'Argeo CMS DevOps'
