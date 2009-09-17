qx.Class.define("org.argeo.security.ria.components.CoworkerNatureImpl", {
	extend : org.argeo.security.ria.components.SimpleUserNatureImpl,
	properties : {
		natureType : {
			refine : true,
			init : "Coworker"
		}
	},
	
	construct : function(){
		this.base(arguments);  		
	},
		
	members : {
		_createGui : function(){
	  		var grid = new qx.ui.layout.Grid(5,5);
	  		this.setLayout(grid);
	  		grid.setColumnFlex(0,1);
	  		grid.setColumnAlign(0,"right", "middle");
	  		grid.setColumnFlex(1,3);
			  		
	  		this.descField = new qx.ui.form.TextField();
	  		this.mobileField = new qx.ui.form.TextField();
	  		this.phoneField = new qx.ui.form.TextField();
	  		
	  		var labels = ["Description", "Mobile Phone", "Home Phone"]; 
	  		this.fields = {
	  			"description" : this.descField, 
	  			"mobile": this.mobileField, 
	  			"telephoneNumber" : this.phoneField
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
		
		getNatureLabel : function(){
			return "Co-Worker";
		}
	}
});