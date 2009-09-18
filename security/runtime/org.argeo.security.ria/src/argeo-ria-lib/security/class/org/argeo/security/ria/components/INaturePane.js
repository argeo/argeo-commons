qx.Interface.define("org.argeo.security.ria.components.INaturePane", {
	extend  : org.argeo.security.ria.components.IUserDataPane,
	statics : {
		NATURE_TYPE : "",
		NATURE_LABEL : "",
		NATURE_ICON : ""
	},
	properties : {
		editMode : {
			init : true,
			apply : "_applyEditMode",			
			event : "changeEditMode"
		}
	},
	members : {
		setData : function(dataMap, format){return true;}
	}
});