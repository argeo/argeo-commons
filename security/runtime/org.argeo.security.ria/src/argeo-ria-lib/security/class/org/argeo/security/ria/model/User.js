qx.Class.define("org.argeo.security.ria.model.User", {
	extend : qx.core.Object,
	properties : {
		name : {
			init : "",
			check : "String"
		},
		password : {
			nullable : true,
			check : "String"
		},
		roles : {
			check : "Array"
		},
		natures :{
			check : "Array"
		},
		rawData : {
			
		},
		create : {
			check : "Boolean",
			init : true
		}
	},
	construct : function(){
		this.base(arguments);
		this.setRoles([]);
		this.setNatures([]);
		this.setRawData({password:null});
	},
	members : {
		load : function(data, format){
			this.setCreate(false);
			this.setName(data.username);
			this.setRoles(data.roles);
			this.setNatures(data.userNatures);
			this.setRawData(data);
		},
		getSaveService : function(self){
			if(this.isCreate()){
		  		var userService = org.argeo.security.ria.SecurityAPI.getCreateUserService(this.toJSON(true));			
			}else{
		  		var userService = org.argeo.security.ria.SecurityAPI.getUpdateUserService(this.toJSON(), self);				
			}
	  		userService.addListener("completed", function(response){
	  			if(!response || !response.username) return;
	  			this.load(response.getContent(), "json");
	  		}, this);
	  		return userService;	  		
		},
		toJSON : function(create){
			var rawData = this.getRawData();
			rawData.username = this.getName();
			rawData.roles = this.getRoles();
			rawData.userNatures = this.getNatures();
			if(create) rawData.password = this.getPassword();
			return rawData;
		},
		_getNatureByType : function(natureType){
			var found = false;
			this.getNatures().forEach(function(el){
				if(el.type == natureType){
					found = el;
				}
			});		
			return found;
		},
		addNature : function(nature){
			if(this._getNatureByType(nature.type)){
				return;
			}
			this.getNatures().push(nature);
		},
		removeNature : function(natureType){
			var foundNature = this._getNatureByType(natureType)
			if(foundNature){
				qx.lang.Array.remove(this.getNatures(), foundNature);
			}
		},
		updateNature : function(nature){
			var natures = this.getNatures();
			for(var i=0;i<natures;i++){
				if(natures[i].type == nature.type){
					natures[i] = nature;
				}
			}
		}
	}
	
});