/**
 * A simple Hello World applet for documentation purpose. 
 * The only associated command is the "Close" command.
 */
/* *************************************************
#asset(resource/org.argeo.security.ria/*)
****************************************************/
qx.Class.define("org.argeo.security.ria.UserEditorApplet",
{
  extend : qx.ui.container.Composite,
  implement : [org.argeo.ria.components.IView], 

  construct : function(){
  	this.base(arguments);
	this.setLayout(new qx.ui.layout.VBox(5));
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
  				icon 		: "org.argeo.security.ria/document-save.png",
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
  				icon 		: "org.argeo.security.ria/list-add.png",
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
  					if(iApplet && iApplet.getCurrentNatureTabs() && iApplet.getAvailableNatures()
  						&& iApplet.getCurrentNatureTabs().length < qx.lang.Object.getLength(iApplet.getAvailableNatures())){
  						this.setEnabled(true);
  					}else{
  						this.setEnabled(false);
  					}
  				},
  				command 	: null  				
  			},
  			"remove_nature" : {
  				label	 	: "Remove Nature", 
  				icon 		: "org.argeo.security.ria/list-remove.png",
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
  				icon 		: "org.argeo.security.ria/window-close.png",
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
  	modified : {
  		init : false,
  		apply : "_applyDetailsModified"
  	},
  	naturesModified : {
  		init : false,
  		apply : "_applyNaturesModified"
  	},
  	rolesList : {
  		
  	},
  	instanceId : {init:""},
  	instanceLabel : {init:"Editor"},
  	loaded : {
  		init : false
  	},
  	currentNatureTabs : {  		
  	},
  	availableNatures : {  		
  	},
  	selectedNatureTab : {  	
  		nullable : true
  	},
  	currentUser : {
  		
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

  		this.naturesManager = new org.argeo.security.ria.components.NaturesManager();
  		var detectedNatures = this.naturesManager.getDetectedNatures();
  		this.setAvailableNatures(detectedNatures);
  		
  		// TOOLBAR
  		this.buttonGB = new qx.ui.container.Composite(new qx.ui.layout.HBox(5, "right"));
  		this.add(this.buttonGB);
  		
  		this.setCurrentNatureTabs([]);
  		this.naturesTab = new qx.ui.tabview.TabView("top");
  		this.naturesTab.addListener("changeSelected", function(e){
  			this.setSelectedNatureTab(e.getData());
  			this.getViewSelection().triggerEvent();
  		}, this);
  		
  		this.basicPage = new qx.ui.tabview.Page("Basic Information");
  		this.basicPage.setLayout(new qx.ui.layout.VBox(5));
  		
  		// GROUPBOXES
  		
  		this.basicGB = new qx.ui.groupbox.GroupBox("Base Informations");
  		var grid = new qx.ui.layout.Grid(5,5);
  		this.basicGB.setLayout(grid);
  		grid.setColumnFlex(0,1);
  		grid.setColumnAlign(0,"right", "middle");
  		grid.setColumnFlex(1,2);
  		this._initializeGroupBox(this.basicGB);
  		  		
  		this.passGB = new qx.ui.groupbox.GroupBox("Set/Modify Password");
  		this.passGB.setLayout(new qx.ui.layout.VBox());
  		this._initializeGroupBox(this.passGB);

  		this.add(this.basicGB);
  		this.add(this.passGB);
  		
  		// FIELDS
  		this.usernameField = new qx.ui.form.TextField();
  		this.basicGB.add(new qx.ui.basic.Label("Username"), {row:0,column:0});  		
  		this.basicGB.add(this.usernameField, {row:0,column:1});
  		
  		this.rolesField = new org.argeo.ria.components.ui.MultipleComboBox();
  		this.rolesField.setChoiceValues(data.ROLES_LIST);
  		this.basicGB.add(new qx.ui.basic.Label("Roles"), {row:1,column:0});  		
  		this.basicGB.add(this.rolesField, {row:1,column:1});
  		
  		this.passPane = new org.argeo.security.ria.components.PasswordCredentialImpl();
  		this.passGB.add(this.passPane.getContainer());
  		
  		//this.naturesTab.add(this.basicPage);
  		this.natureButtonGB = new qx.ui.container.Composite(new qx.ui.layout.HBox(5, "right"));
  		this.natureButtonGB.setMarginTop(15);
  		this.add(this.natureButtonGB);
  		
	
  		this.add(this.naturesTab, {flex:1});
  		
		this.naturesTab.setVisibility("excluded");
		this.fakePane = new qx.ui.container.Composite(new qx.ui.layout.Canvas());
		this.fakePane.setVisibility("visible");
		this.fakePane.setDecorator("tabview-pane");
		this.fakePane.setMarginTop(30);
		this.add(this.fakePane, {flex:1});
  		
  		title = new qx.ui.basic.Atom("User Details", "org.argeo.security.ria/preferences-users.png");
  		title.setFont(qx.bom.Font.fromString("16px sans-serif bold"));  		
  		this.buttonGB.add(title);
  		this.buttonGB.add(new qx.ui.core.Spacer(), {flex:1});

  		var title2 = new qx.ui.basic.Atom("User Natures", "org.argeo.security.ria/identity.png");
  		title2.setFont(qx.bom.Font.fromString("16px sans-serif bold"));  		
  		this.natureButtonGB.add(title2);
  		this.natureButtonGB.add(new qx.ui.core.Spacer(), {flex:1});  		
		
  		  	  		
  	},
  	
  	saveUser : function(){
  		var user = this.getCurrentUser();
  		user.setName(this.usernameField.getValue());
  		user.setRoles((this.rolesField.getValue()||"").split(","));
  		// GO TO AND RETURN FROM SERVER
  		var userService = user.getSaveService();
  		userService.send();
  		userService.addListener("completed", function(e){
  			this.partialRefreshUser(user, ["details","natures"]);
			this.setModified(false);
			this.getViewSelection().triggerEvent();
  		}, this);
  	},
  	
  	_addNatureTab : function(natureClass, natureData, select){
  		var crtTabs = this.getCurrentNatureTabs();
  		if(qx.lang.Array.contains(crtTabs, natureClass.NATURE_TYPE)){
  			this.error("There can only be one instance of a given nature type!");
  			return null;
  		}
  		if(!this.naturesTab.isVisible()){
  			if(this.fakePane) this.fakePane.setVisibility("excluded");
  			this.naturesTab.setVisibility("visible");
  		}
  		var page = new qx.ui.tabview.Page("Nature : " + natureClass.NATURE_LABEL);
  		page.setLayout(new qx.ui.layout.Dock());
  		page.setUserData("NATURE_CLASS", natureClass);
		var newClass = new natureClass();
  		page.add(newClass.getContainer(), {edge:"center"});
  		
  		buttons = new qx.ui.container.Composite(new qx.ui.layout.HBox(5, "center"));
  		var editB = new qx.ui.form.Button("Edit this Nature", "org.argeo.security.ria/document-properties-22.png");
  		var saveB = new qx.ui.form.Button("Save", "org.argeo.security.ria/dialog-apply.png");
  		var cancelB = new qx.ui.form.Button("Cancel", "org.argeo.security.ria/dialog-cancel.png");
  		buttons.add(editB);
  		buttons.add(saveB);
  		buttons.add(cancelB);
  		page.add(buttons, {edge:"south"});
  		editB.addListener("execute", function(){
  			newClass.setEditMode(true);
  			editB.setVisibility("excluded");
  			saveB.setVisibility("visible");
  			cancelB.setVisibility("visible");
  		});
  		cancelB.addListener("execute", function(){
  			if(newClass.getIsNew()){
  				this._removeNatureTab(natureClass);
  			}
  			newClass.setEditMode(false);
  			editB.setVisibility("visible");
  			saveB.setVisibility("excluded");
  			cancelB.setVisibility("excluded");
  		}, this);
  		saveB.addListener("execute", function(){
  			// SAVE CURRENT NATURE
  			var data = newClass.getData();
  			if(newClass.getIsNew()){
  				this.getCurrentUser().addNature(data);
  			}else{
  				this.getCurrentUser().updateNature(data);
  			}
  			this.saveUser();
  			this.setNaturesModified(false);
  			newClass.setEditMode(false);
  			editB.setVisibility("visible");
  			saveB.setVisibility("excluded");
  			cancelB.setVisibility("excluded");
  		}, this);
  		if(natureData){
  			newClass.setData(natureData);  			
  			cancelB.execute();
  		}else{
  			newClass.setIsNew(true);
  			editB.execute();
  		}
  		this.naturesTab.add(page);
  		crtTabs.push(natureClass.NATURE_TYPE);
  		this.getViewSelection().triggerEvent();
  		newClass.addListener("modified", function(){
  			this.setNaturesModified(true);
  		}, this);  
  		if(select){
  			this.naturesTab.setSelected(page);
  		}
  		return page;
  	},
  	
  	_removeNatureTab : function(natureClass){
  		this.naturesTab.getChildren().forEach(function(el){
  			if(el.getUserData("NATURE_CLASS") == natureClass){
  				this.naturesTab.remove(el);
  				qx.lang.Array.remove(this.getCurrentNatureTabs(), natureClass.NATURE_TYPE);
  				this.getViewSelection().triggerEvent();
  			}
  		}, this);
  		if(this.naturesTab.getChildren().length == 0){
  			this.naturesTab.setVisibility("excluded");
  			this.fakePane.setVisibility("visible");
  		}
  	},
  	
  	removeSelectedTab : function(){
  		var selected = this.naturesTab.getSelected();
  		var tabClass = selected.getUserData("NATURE_CLASS");
  		var user = this.getCurrentUser();
  		user.removeNature(tabClass.NATURE_TYPE);
  		this.saveUser();
  		this._removeNatureTab(tabClass);
  	},
  	
  	removeAllTabs : function(){
  		while(this.naturesTab.getSelected()){
  			this._removeNatureTab(this.naturesTab.getSelected().getUserData("NATURE_CLASS"));
  		}
  	},
  	  	
  	_attachListeners : function(){
  		this.usernameField.addListener("changeValue", function(){
  			this.setModified(true);
  		}, this);
  		this.rolesField.addListener("changeValue", function(){
  			this.setModified(true);
  		}, this);
  		this.passPane.addListener("modified", function(){
  			this.setModified(true);
  		}, this);
  	},
  	
  	_initializeGroupBox: function(groupBox){
  		groupBox.setPadding(0);
  		groupBox.getChildrenContainer().setPadding(8);  		
  	},
  	
  	_applyDetailsModified : function(value){  		
  		if(value) this.getViewSelection().triggerEvent();
  	},
  	
  	_applyNaturesModified : function(value){
  		if(value) this.getViewSelection().triggerEvent();
  	},
  	
  	loadUserData : function(user){
  		this.setCurrentUser(user);
  		this.usernameField.setValue(user.getName());
  		this.usernameField.setReadOnly(true);
  		this.rolesField.setValue(user.getRoles());
		var userNatureTabs = this.naturesManager.detectNaturesInData(user.getNatures());
		if(userNatureTabs.length){
			userNatureTabs.forEach(function(el){
				this._addNatureTab(el.NATURE_CLASS, el.NATURE_DATA);
			}, this);
		}  		
  	},
  	
  	partialRefreshUser : function(user, target){
  		if(!qx.lang.Array.isArray(target)) target = [target];
  		
  		if(qx.lang.Array.contains(target,"natures")){
  			this.removeAllTabs();
			var userNatureTabs = this.naturesManager.detectNaturesInData(user.getNatures());
			if(userNatureTabs.length){
				userNatureTabs.forEach(function(el){
					this._addNatureTab(el.NATURE_CLASS, el.NATURE_DATA);
				}, this);
			}  		  			
  		}
  		if(qx.lang.Array.contains(target,"details")){
  			this.setInstanceLabel("User "+user.getName());
	  		this.usernameField.setValue(user.getName());
	  		this.rolesField.setValue(user.getRoles());  
	  		this.usernameField.setReadOnly(true);
  		}
  	},
  	
  	/**
  	 * Load a given row : the data passed must be a simple data array.
  	 * @param data {Element} The text xml description. 
  	 */
  	load : function(user){
  		if(this.getLoaded()){
  			return;
  		}  		
  		// MUST BE DONE AFTER COMMANDS ARE INITIALIZED! 
  		var commands = this.getCommands();
  		var saveButton = commands["save_user"].command.getFormButton(); 
  		var closeButton = commands["close"].command.getFormButton(); 
  		var removeButton = commands["remove_nature"].command.getFormButton();
  		var natureButton = commands["add_nature"].command.getFormButton();
  		
  		var detectedNatures = this.getAvailableNatures();
  		var newMenu = [];
  		for(var key in detectedNatures){
  			newMenu.push({"label" : detectedNatures[key].NATURE_LABEL, "icon":"", "commandId" : detectedNatures[key]});
  		}
  		commands["add_nature"].command.setMenu(newMenu);
  		
  		natureButton.setShow("icon");
  		removeButton.setShow("icon");
  		saveButton.setShow("icon");
  		closeButton.setShow("icon");
  		
  		this.buttonGB.add(saveButton);
  		this.buttonGB.add(closeButton);  		
  		this.natureButtonGB.add(natureButton);
  		this.natureButtonGB.add(removeButton);
  		
  		if(user){
  			this.loadUserData(user);
	  		this._attachListeners();
  		}else{
  			this.setCurrentUser(new org.argeo.security.ria.model.User());
	  		this._attachListeners();
  			this.setModified(true);
  		}
  		
  		
  		this.setLoaded(true);
  		
  	},
  	  	 
	addScroll : function(){
		return false;
	},
	
	close : function(){
		return false;
	}
  	
  }
});