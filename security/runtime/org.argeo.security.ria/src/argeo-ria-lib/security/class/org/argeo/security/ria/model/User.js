qx.Class.define("org.argeo.security.ria.model.User", {
	extend : qx.core.Object,
	properties : {
		name : {
			check : "String"
		},
		roles : {
			check : "Array"
		},
		natures :{
			check : "Array"
		}
	},
	construct : function(){
		this.base(arguments);
	},
	members : {
		load : function(data, format){
			this.setName(data.username);
			this.setRoles(data.roles);
			this.setNatures(data.userNatures);
		}
	}
	
});