qx.Class.define("org.argeo.security.ria.model.User", {
	extend : qx.core.Object,
	properties : {
		name : {
			init : "",
			check : "String"
		},
		roles : {
			check : "Array"
		},
		natures :{
			check : "Array"
		},
		rawData : {
			
		}
	},
	construct : function(){
		this.base(arguments);
		this.setRoles([]);
		this.setNatures([]);
	},
	members : {
		load : function(data, format){
			this.setName(data.username);
			this.setRoles(data.roles);
			this.setNatures(data.userNatures);
			this.setRawData(data);
		},
		toJSON : function(){
			var rawData = this.getRawData();
			rawData.username = this.getName();
			rawData.roles = this.getRoles();
			rawData.userNatures = this.getNatures();
		}
	}
	
});