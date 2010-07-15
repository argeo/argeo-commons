/**
 * A simple Hello World applet for documentation purpose. 
 * The only associated command is the "Close" command.
 */
/* *************************************************
#asset(org/argeo/security/ria/*)
****************************************************/
qx.Class.define("org.argeo.security.ria.UserEditorApplet",
{
  extend : org.argeo.security.ria.components.UserEditor,
  implement : [org.argeo.ria.components.IView], 
  
  construct : function(){
  	this.base(arguments);
  },
  
  properties : 
  {
  	/**
  	 * The viewPane inside which this applet is added. 
  	 */
  	view : {
  		init : null
  	},  	
  	/**
  	 * Commands definition, see {@link org.argeo.ria.event.CommandsManager#definitions} 
  	 */
  	commands : {
  		init : {
  			"save_user" : {
  				label	 	: "Save", 
  				icon 		: "org/argeo/security/ria/document-save.png",
  				shortcut 	: "Control+s",
  				enabled  	: true,
  				menu	   	: "Users",
  				toolbar  	: "user",
  				callback	: function(e){
  					// CALL SERVICE AND GET UP TO DATE USER
  					this.saveUser();
  				},
				selectionChange : function(viewName, data){
  					if(viewName != "editor") return;
  					var iApplet = org.argeo.ria.components.ViewsManager.getInstance().getViewPaneById("editor").getContent();
  					this.setEnabled(iApplet.getModified());
				},
				command 	: null
  			},
  			"add_nature" : {
  				label	 	: "Add Nature ...", 
  				icon 		: "org/argeo/security/ria/list-add.png",
  				shortcut 	: null,
  				enabled  	: true,
  				menu	   	: "Natures",
  				toolbar  	: null,
  				callback	: function(e){
  				},
  				submenuCallback : function(commandClass){
  					this._addNatureTab(commandClass, null, true);
  				},
  				submenu		: [
  					{"label" : "Totot", "icon":"","commandId" : "toto"},
  					{"label" : "Totot", "icon":"","commandId" : "toto"}
  				],
  				selectionChange : function(viewName, data){
  					if(viewName != "editor") return;
  					var iApplet = org.argeo.ria.components.ViewsManager.getInstance().getViewPaneById("editor").getContent();
  					if(iApplet && iApplet.getCurrentNatureTabs() && iApplet.getNaturesManager().getDetectedNatures()
  						&& iApplet.getCurrentNatureTabs().length < qx.lang.Object.getLength(iApplet.getNaturesManager().getDetectedNatures())){
  						this.setEnabled(true);
  					}else{
  						this.setEnabled(false);
  					}
  				},
  				command 	: null  				
  			},
  			"remove_nature" : {
  				label	 	: "Remove Nature", 
  				icon 		: "org/argeo/security/ria/list-remove.png",
  				shortcut 	: null,
  				enabled  	: true,
  				menu	   	: "Natures",
  				toolbar  	: null,
  				callback	: function(e){
  					this.removeSelectedTab();
  				},
  				selectionChange : function(viewName, data){
  					if(viewName != "editor") return;
  					var iApplet = org.argeo.ria.components.ViewsManager.getInstance().getViewPaneById("editor").getContent();
  					if(iApplet && iApplet.getSelectedNatureTab() && iApplet.getSelectedNatureTab().getUserData("NATURE_CLASS")){
  						this.setEnabled(true);
  					}else{
  						this.setEnabled(false);
  					}
  				},
  				command 	: null
  			},
  			"close" : {
  				label	 	: "Close", 
  				icon 		: "org/argeo/security/ria/window-close.png",
  				shortcut 	: "Control+w",
  				enabled  	: true,
  				menu	   	: "Users",
  				toolbar  	: "user",
  				callback	: function(e){
  					// Call service to delete
					var iApplet = org.argeo.ria.components.ViewsManager.getInstance().getViewPaneById("editor").getContent();
					if(!iApplet.getModified() && !iApplet.getNaturesModified()){
						this.getView().closeCurrent();
						return;
					}
  					var modal = new org.argeo.ria.components.Modal("Warning");
  					modal.addConfirm("There are unsaved changes!\n Are you sure you want to close?");
  					modal.addListener("ok", function(){
  						this.getView().closeCurrent();
  					}, this);
  					modal.attachAndShow();
  				},
  				command 	: null
  			}  			  			
  		}
  	},
  	viewSelection : {
  		nullable:false, 
  		check:"org.argeo.ria.components.ViewSelection"
  	},
  	instanceId : {
  		init:"",
  		event : "changeInstanceId"
  	},
  	instanceLabel : {
  		init:"Editor",
  		event : "changeInstanceLabel"
  	}
  },

  members :
  {
  	/**
  	 * Called at applet creation. Just registers viewPane.
  	 * @param viewPane {org.argeo.ria.components.ViewPane} The viewPane.
  	 */
  	init : function(viewPane, data){
  		if(!data.USER){
  			var now = new Date();
	  		this.setInstanceId(now.getTime());
	  		this.setInstanceLabel("New User");  			
  		}else{
	  		this.setInstanceId(data.USER);
	  		this.setInstanceLabel("User " + data.USER);
  		}
  		this.setView(viewPane);
  		this.setViewSelection(new org.argeo.ria.components.ViewSelection(viewPane.getViewId()));

  		this.initGUI(data.ROLES_LIST);
  		
  		this.addListener("savedUser", function(e){  
  			if(this.getCurrentUser()){
				this.setInstanceLabel("User " + this.getCurrentUser().getName());
				this.setInstanceId(this.getCurrentUser().getName());
  			}
  		}, this);
  		  		  	  		
  	},
  	
  	/**
  	 * Load a given row : the data passed must be a simple data array.
  	 * @param data {Element} The text xml description. 
  	 */
  	load : function(userName){
  		if(this.getLoaded()){
  			return;
  		}  		
  		// MUST BE DONE AFTER COMMANDS ARE INITIALIZED! 
  		var commands = this.getCommands();
  		var saveButton = commands["save_user"].command.getFormButton(); 
  		var closeButton = commands["close"].command.getFormButton(); 
  		var removeButton = commands["remove_nature"].command.getFormButton();
  		var natureButton = commands["add_nature"].command.getFormButton();
  		
  		this.getNaturesManager().addListener("changeNonAssignedNatures", function(event){
  			var natures = event.getData();
	  		var newMenu = [];
	  		for(var key in natures){
	  			newMenu.push({
	  				"label" : natures[key].NATURE_LABEL, 
	  				"icon":"", 
	  				"commandId" : natures[key]
	  				});
	  		}
	  		commands["add_nature"].command.setMenuDef(newMenu);  		
  		}, this);
  		
  		natureButton.setShow("icon");
  		removeButton.setShow("icon");
  		saveButton.setShow("icon");
  		closeButton.setShow("icon");
  		
  		this.buttonGB.add(saveButton);
  		this.buttonGB.add(closeButton);  		
  		this.natureButtonGB.add(natureButton);
  		this.natureButtonGB.add(removeButton);
  		
  		if(userName){
  			this.loadUserData(userName);
  			this._setGuiInCreateMode(false);	  		
  		}else{
  			this.setCurrentUser(new org.argeo.security.ria.model.User());
  			this._setGuiInCreateMode(true);
	  		this._attachListeners();
  			this.setModified(true);
  		}
  		
  		
  		this.setLoaded(true);
  		
  	},
  	  	 
  	_applyDetailsModified : function(value){  		
  		if(value) this.getViewSelection().triggerEvent();
  	},
  	
  	_applyNaturesModified : function(value){
  		if(value) this.getViewSelection().triggerEvent();
  	},  	
  	
	addScroll : function(){
		return false;
	},
	
	close : function(){
		return false;
	}
  	
  }
});