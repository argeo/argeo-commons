qx.Class.define("org.argeo.security.ria.components.PasswordCredentialImpl", {
	extend : qx.ui.container.Composite,
	implement : [org.argeo.security.ria.components.ICredentialPane],
	events : {
		"modified" : "qx.event.type.Event"		
	},
	properties : {
		valid : {
			init : false
		}
	},
	
	construct : function(){
		this.base(arguments);
		this.setLayout(new qx.ui.layout.HBox(5, "center"));
		this.add(new qx.ui.basic.Label("Password"), {flex:1});
		this.pass1 = new qx.ui.form.PasswordField();
		this.add(this.pass1, {flex:2});
		this.add(new qx.ui.basic.Label("Confirm Password"), {flex:1});
		this.pass2 = new qx.ui.form.PasswordField();
		this.add(this.pass2, {flex:2});
		this.pass1.addListener("changeValue", function(){this.fireEvent("modified");}, this);
		this.pass2.addListener("changeValue", function(){this.fireEvent("modified");}, this);
		this.pass2.addListener("changeValue", this.validate, this);
	},
	
	members : {
		getContainer  : function(){
			return this;
		},
		getData    : function(format){return true;},
		validate : function(){
			if(this.pass1.getValue() == this.pass2.getValue()){
				this.setValid(true);
			}else{
				// TODO WHEN TESTING 0.8.3
				//this.pass1.setValid(false);
				//this.pass2.setValid(false); 
				this.setValid(false);
			}
			return this.getValid();
		},
		setEditMode : function(editMode){return true;}		
	}
});