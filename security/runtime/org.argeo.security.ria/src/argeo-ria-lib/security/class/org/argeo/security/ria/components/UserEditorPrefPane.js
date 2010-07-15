qx.Class.define("org.argeo.security.ria.components.UserEditorPrefPane", {

	extend : org.argeo.security.ria.components.UserEditor,
	implement : [org.argeo.ria.components.IPreferencePane],
	  
	construct : function(){
		this.base(arguments);
		this.setSelfEdition(true);
		this.initGUI();		
		this.basicGB.setVisibility("excluded");
		var saveButton = new qx.ui.form.Button("Save", "org/argeo/security/ria/document-save.png");
		saveButton.addListener("execute", this.saveUser, this);
		this.buttonGB.add(saveButton);
		var authService = org.argeo.ria.session.AuthService.getInstance();
		this.loadUserData(authService.getUser());
		authService.addListener("changeUser", function(){
			if(authService.getUser() == "anonymous"){
				this.clearUserData();
			}else{
				this.loadUserData(authService.getUser());
			}
		}, this);
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
			return "org/argeo/security/ria/preferences-users.png";
		}  			
	}
});