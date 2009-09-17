qx.Class.define("org.argeo.security.ria.model.Nature", {
	extend : qx.core.Object,
	properties : {
		uuid : {
			check : "String"
		},
		type : {
			check : "String"
		},
		data : {
			check : "Map"
		}
	},
	construct : function(){
		this.base(arguments);
	},
	members : {
		load : function(data, format){
			
		}
	}	
});