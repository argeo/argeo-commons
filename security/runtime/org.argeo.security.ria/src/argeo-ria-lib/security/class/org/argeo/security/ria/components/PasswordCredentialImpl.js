qx.Class.define("org.argeo.security.ria.components.PasswordCredentialImpl", {
	extend : qx.ui.container.Composite,
	implement : [org.argeo.security.ria.components.ICredentialPane],
	events : {
		"modified" : "qx.event.type.Event"		
	},
	
	construct : function(){
		this.base(arguments);
		this.setLayout(new qx.ui.layout.HBox(5, "center"));
		this.add(new qx.ui.basic.Label("Password"), {flex:1});
		this.add(new qx.ui.form.TextField(), {flex:2});
		this.add(new qx.ui.basic.Label("Confirm Password"), {flex:1});
		this.add(new qx.ui.form.TextField(), {flex:2});
	},
	
	members : {
		getContainer  : function(){
			return this;
		},
		getData    : function(format){return true;},
		validate : function(){return true;},
		setEditMode : function(editMode){return true;}		
	}
});