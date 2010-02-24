qx.Class.define("org.argeo.security.ria.components.SimpleUserNatureImpl", {
	extend : qx.ui.container.Composite,
	implement : [org.argeo.security.ria.components.INaturePane],
	events : {
		"modified" : "qx.event.type.Event"		
	},
	statics : {
		NATURE_TYPE : "org.argeo.security.nature.SimpleUserNature",
		NATURE_LABEL : "Simple User",
		NATURE_ICON : ""
	},
	properties : {
		valid : {
			init : false
		},
		dataMap : {
			
		},
		editMode : {
			init : true,
			apply : "_applyEditMode",
			event : "changeEditMode"
		},
		isNew	: {
			init : false,
			check : "Boolean"
		}		
	},
	
	construct : function(){
		this.base(arguments);  	
		this.setDataMap({
			type:"org.argeo.security.nature.SimpleUserNature"
		});
		this._createGui();
 		this.setEditMode(false);		
	},
		
	members : {		
		
		_createGui : function(){
	  		var grid = new qx.ui.layout.Grid(5,5);
	  		this.setLayout(grid);
	  		grid.setColumnFlex(0,1);
	  		grid.setColumnAlign(0,"right", "middle");
	  		grid.setColumnFlex(1,3);
			  		
	  		this.mailField = new qx.ui.form.TextField();
	  		this.fNameField = new qx.ui.form.TextField();
	  		this.nameField = new qx.ui.form.TextField();
	  		this.descriptionField = new qx.ui.form.TextArea();
	  		
	  		var labels = ["Email", "First Name", "Last Name", "Description"]; 
	  		this.fields = {
	  			"email" : this.mailField, 
	  			"firstName": this.fNameField, 
	  			"lastName" : this.nameField,
	  			"description" : this.descriptionField
	  		};
	  		
	  		for(var i=0;i<labels.length;i++){
	  			this.add(new qx.ui.basic.Label(labels[i]), {row:i,column:0});
	  		}
	  		var j=0;
	  		for(var key in this.fields){  			
	  			this.fields[key].addListener("changeValue", function(e){this.fireEvent("modified");}, this);
	  			this.add(this.fields[key], {row:j,column:1});
	  			j++;
	  		}	
		},
		
		_applyEditMode : function(value){
			for(var key in this.fields){
				this.fields[key].setEnabled(value);
			}
		},
		
		getContainer  : function(){
			return this;
		},
		setData    : function(dataMap, format){
			for(var key in this.fields){
				if(dataMap[key]){
					this.fields[key].setValue(dataMap[key]);
				}
			}
			this.setDataMap(dataMap);
		},
		getData    : function(format){
			var dataMap = this.getDataMap();
			for(var key in this.fields){
				dataMap[key] = this.fields[key].getValue();
			}
			this.setDataMap(dataMap);
			return dataMap;
		},
		validate : function(){return true;}
	}
});