/**
 * IPerspective Implementation : Horizontal split pane defining two panes,
 * "list" and "applet".
 */
/* ************************************************************************

#asset(resource/org.argeo.security.ria/*)

************************************************************************ */

qx.Class.define("org.argeo.security.ria.Perspective", {
	extend : qx.core.Object,
	implement : [org.argeo.ria.components.IPerspective],

	construct : function() {
		this.base(arguments);
	},

	statics : {
	  	LABEL : "RIA Security",
	  	ICON : "org.argeo.security.ria/preferences-security.png"
	},
	

	members : {

		initViewPanes : function(viewsManager) {

			this._firstSplit = new qx.ui.splitpane.Pane("horizontal");
			this._secondSplit = new qx.ui.splitpane.Pane("horizontal");
			this._secondSplit.setDecorator(null);
			
			var rolesPane = new org.argeo.ria.components.ViewPane("roles", "Roles");
			viewsManager.registerViewPane(rolesPane);
			var usersPane = new org.argeo.ria.components.ViewPane("users", "Users");
			viewsManager.registerViewPane(usersPane);
			var editorPane = new org.argeo.ria.components.ViewPane("editor", "Editor");
			viewsManager.registerViewPane(editorPane);
			
			this._firstSplit.add(rolesPane, 1);
			this._firstSplit.add(this._secondSplit, 4);
			
			this._secondSplit.add(usersPane, 4);
			this._secondSplit.add(editorPane, 3);
			
			viewsManager.getViewPanesContainer().add(this._firstSplit, {flex : 1});
		},

		initViews : function(viewsManager) {

			var usersView = viewsManager.initIViewClass(org.argeo.security.ria.UsersApplet, "users");
			usersView.load();
			
			var rolesView = viewsManager.initIViewClass(org.argeo.security.ria.RolesApplet, "roles");
			rolesView.load();
		},

		remove : function(viewsManager) {
			viewsManager.getViewPaneById("applet").empty();
			viewsManager.getViewPanesContainer().remove(this.splitPane);
		}

	}

});