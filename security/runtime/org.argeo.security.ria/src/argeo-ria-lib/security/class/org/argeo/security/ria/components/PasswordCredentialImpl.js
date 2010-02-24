qx.Class.define("org.argeo.security.ria.components.PasswordCredentialImpl", {
	extend : qx.ui.container.Composite,
	implement : [org.argeo.security.ria.components.ICredentialPane],
	events : {
		"modified" : "qx.event.type.Event"		
	},
	properties : {
		valid : {
			init : false
		},
		selfEdition : {
			init : false
		},
		encoderCallback : {
			init : function(string){
				var encoderShort = org.argeo.ria.util.Encoder;
				return "{SHA}"+encoderShort.base64Encode(encoderShort.hexDecode(encoderShort.hash(string, "sha1")));
			},
			check : "Function"
		}
	},
	
	construct : function(selfEdition){
		this.base(arguments);
		if(selfEdition){
			this.setSelfEdition(selfEdition);
		}
		this.setLayout(new qx.ui.layout.HBox(5, "center"));
		this.getLayout().setAlignY("middle");
		if(this.getSelfEdition()){
			this.oldPass = new qx.ui.form.PasswordField();
			this.add(new qx.ui.basic.Label("Old"), {flex:1});
			this.add(this.oldPass, {flex:2});
			this.oldPass.addListener("changeValue", function(){this.fireEvent("modified");}, this);
		}
		this.add(new qx.ui.basic.Label((selfEdition?"New":"Password")), {flex:1});
		this.pass1 = new qx.ui.form.PasswordField();
		this.add(this.pass1, {flex:2});
		this.add(new qx.ui.basic.Label((selfEdition?"Confirm":"Confirm Password")), {flex:1});
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
		getData : function(format){
			var encoder = this.getEncoderCallback();
			var encoded = null;			
			if(this.pass1.getValue() != null && this.pass1.getValue() != ""){
				encoded = encoder(this.pass1.getValue()); 
			}
			if(this.getSelfEdition() && encoded && this.oldPass.getValue()!=""){
				return {oldPass:encoder(this.oldPass.getValue()), newPass:encoded};
			}
			return encoded;
		},
		clear : function(){
			this.pass1.setValue("");
			this.pass2.setValue("");		
			if(this.getSelfEdition()){
				this.oldPass.setValue("");
			}
		},
		validate : function(){
			if(this.pass1.getValue() != this.pass2.getValue()){
				this.pass1.setValid(false);
				this.pass2.setValid(false);
				this.setValid(false);				
			}else{
				this.pass1.setValid(true);
				this.pass2.setValid(true); 
				this.setValid(true);
			}
			return this.getValid();
		},
		setEditMode : function(editMode){return true;}		
	}
});