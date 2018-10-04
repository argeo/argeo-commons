// CMS
var CmsScriptApp = Java.type('org.argeo.cms.script.CmsScriptApp');
var AppUi = Java.type('org.argeo.cms.script.AppUi');
var Theme = Java.type('org.argeo.cms.script.Theme');
var ScriptUi = Java.type('org.argeo.cms.script.ScriptUi');
var CmsUtils = Java.type('org.argeo.cms.util.CmsUtils');
var SimpleCmsHeader = Java.type('org.argeo.cms.util.SimpleCmsHeader');
var CmsLink = Java.type('org.argeo.cms.util.CmsLink');
var MenuLink = Java.type('org.argeo.cms.util.MenuLink');
var UserMenuLink = Java.type('org.argeo.cms.util.UserMenuLink');

// SWT
var SWT = Java.type('org.eclipse.swt.SWT');
var Composite = Java.type('org.eclipse.swt.widgets.Composite');
var Label = Java.type('org.eclipse.swt.widgets.Label');
var Button = Java.type('org.eclipse.swt.widgets.Button');
var Text = Java.type('org.eclipse.swt.widgets.Text');

var FillLayout = Java.type('org.eclipse.swt.layout.FillLayout');
var GridLayout = Java.type('org.eclipse.swt.layout.GridLayout');
var GridData = Java.type('org.eclipse.swt.layout.GridData');

function loadNode(node) {
	var json = CmsScriptApp.toJson(node)
	var fromJson = JSON.parse(json)
	return fromJson
}

function newArea(parent, style, layout) {
	var control = new Composite(parent, SWT.NONE)
	control.setLayout(layout)
	CmsUtils.style(control, style)
	return control
}

function newLabel(parent, style, text) {
	var control = new Label(parent, SWT.NONE)
	control.setText(text)
	CmsUtils.style(control, style)
	CmsUtils.markup(control)
	return control
}

function newButton(parent, style, text) {
	var control = new Button(parent, SWT.FLAT)
	control.setText(text)
	CmsUtils.style(control, style)
	CmsUtils.markup(control)
	return control
}

function newFormLabel(parent, style, text) {
	return newLabel(parent, style, '<b>' + text + '</b>')
}

function newText(parent, style, msg) {
	var control = new Text(parent, SWT.NONE)
	control.setMessage(msg)
	CmsUtils.style(control, style)
	return control
}

// print(__FILE__, __LINE__, __DIR__)
