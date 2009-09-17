qx.Class.define("org.argeo.security.ria.components.SimpleUserNatureImpl", {
	extend : qx.ui.container.Composite,
	implement : [org.argeo.security.ria.components.INaturePane],
	events : {
		"modified" : "qx.event.type.Event"		
	},
	properties : {
		valid : {
			init : false
		},
		natureUuid : {
			init : ""
		},
		natureType : {
			init : "SimpleUser"
		},
		dataMap : {
			
		}
	},
	
	construct : function(){
		this.base(arguments);		
  		var grid = new qx.ui.layout.Grid(5,5);
  		this.setLayout(grid);
  		grid.setColumnFlex(0,1);
  		grid.setColumnAlign(0,"right", "middle");
  		grid.setColumnFlex(1,3);
		  		
  		this.mailField = new qx.ui.form.TextField();
  		this.fNameField = new qx.ui.form.TextField();
  		this.nameField = new qx.ui.form.TextField();
  		
  		var labels = ["Email", "First Name", "Last Name"]; 
  		this.fields = {
  			"email" : this.mailField, 
  			"firstName": this.fNameField, 
  			"lastName" : this.nameField
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
		
	members : {
		getContainer  : function(){
			return this;
		},
		getNatureLabel : function(){
			return "Simple User";
		},
		setData    : function(dataMap, format){
			this.setNatureUuid(dataMap["uuid"]);
			this.setNatureType(dataMap["type"]);
			for(var key in this.fields){
				if(dataMap[key]){
					this.fields[key].setValue(dataMap[key]);
				}
			}
			this.setDataMap(dataMap);
		},
		getData    : function(format){
			var dataMap = this.getDataMap();
			for(var key in dataMap){
				if(this.fields[key]){
					dataMap[key] = this.fields[key].getValue();
				}
			}
			this.setDataMap(dataMap);
			return dataMap;
		},
		validate : function(){return true;}
	}
});