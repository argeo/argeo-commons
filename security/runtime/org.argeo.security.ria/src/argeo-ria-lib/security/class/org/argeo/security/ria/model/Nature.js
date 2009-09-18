qx.Class.define("org.argeo.security.ria.model.Nature", {
	extend : qx.core.Object,
	properties : {
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
			this.setData(data);
		}
	}	
});