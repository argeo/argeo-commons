qx.Interface.define("org.argeo.security.ria.components.INaturePane", {
	extend  : org.argeo.security.ria.components.IUserDataPane,
	properties : {
		natureUuid : {},
		natureType : {}
	},
	members : {
		getNatureLabel : function(){return true},
		setData : function(dataMap, format){return true;}
	}
});