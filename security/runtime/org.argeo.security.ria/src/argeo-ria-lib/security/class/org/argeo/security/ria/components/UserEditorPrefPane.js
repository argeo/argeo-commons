qx.Class.define("org.argeo.security.ria.components.UserEditorPrefPane", {

	extend : org.argeo.security.ria.components.UserEditor,
	implement : [org.argeo.ria.components.IPreferencePane],
	  
	construct : function(){
		this.base(arguments);
		this.initGUI();
		this.basicGB.setVisibility("excluded");
		var saveButton = new qx.ui.form.Button("Save", "org.argeo.security.ria/document-save.png");
		saveButton.addListener("execute", this.saveUser, this);
		this.buttonGB.add(saveButton);
		this.loadUserData(org.argeo.ria.session.AuthService.getInstance().getUser());
	},
	
	members : {
	  	// IPrefPane Implementation
		getPrefPane : function(){			
			return new qx.ui.container.Scroll(this);
		},
		
		getPrefLabel : function(){
			return "User data";
		},
		
		getPrefIcon : function(){
			return "org.argeo.security.ria/preferences-users.png";
		}  			
	}
});