qx.Class.define("org.argeo.security.ria.components.SimpleUserNatureImpl", {
	extend : qx.ui.container.Composite,
	implement : [org.argeo.security.ria.components.INaturePane],
	events : {
		"modified" : "qx.event.type.Event"		
	},
	
	construct : function(){
		this.base(arguments);
		this.setLayout(new qx.ui.layout.Grid());
	},
		
	members : {
		getContainer  : function(){
			return this;
		},
		getNatureLabel : function(){
			return "Basic User";
		},
		setData    : function(dataMap, format){return true;},
		getData    : function(format){return true;},
		validate : function(){return true;}
	}
});