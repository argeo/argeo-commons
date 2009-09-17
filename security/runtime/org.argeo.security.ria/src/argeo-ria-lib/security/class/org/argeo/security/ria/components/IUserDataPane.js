qx.Interface.define("org.argeo.security.ria.components.IUserDataPane", {
	members : {
		getContainer  : function(){return true;},
		getData    : function(format){return true;},
		validate : function(){return true;}
	},
	properties : {
		valid : {}
	},
	events : {
		"modified" : "qx.event.type.Event"			
	}
});