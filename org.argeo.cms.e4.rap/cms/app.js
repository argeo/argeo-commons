var AppUi = Java.type('org.argeo.cms.script.AppUi')
var CmsE4EntryPointFactory = Java
		.type('org.argeo.cms.e4.rap.CmsE4EntryPointFactory')

APP.setWebPath('cms')
APP.setPageTitle('Argeo CMS')

APP.getUi().put(
		'devops',
		new AppUi(APP, new CmsE4EntryPointFactory(
				'org.argeo.cms.e4/e4xmi/cms-devops.e4xmi')))
APP.getUi().get('devops').setPageTitle('Argeo CMS DevOps')
