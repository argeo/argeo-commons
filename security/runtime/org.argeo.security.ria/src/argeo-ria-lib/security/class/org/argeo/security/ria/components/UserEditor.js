/**
 * A simple Hello World applet for documentation purpose. 
 * The only associated command is the "Close" command.
 */
/* *************************************************
#asset(resource/org.argeo.security.ria/*)
****************************************************/
qx.Class.define("org.argeo.security.ria.components.UserEditor",
{
  extend : qx.ui.container.Composite,
  
  construct : function(){
  	this.base(arguments);
	this.setLayout(new qx.ui.layout.VBox(5));
  },
  
  events : {
  	"savedUser" : "qx.event.type.Data"
  },
  
  properties : 
  {
  	selfEdition : {
  		init : false
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
  	initGUI : function(ROLES_LIST){

  		this.naturesManager = new org.argeo.security.ria.components.NaturesManager();
  		var detectedNatures = this.naturesManager.getDetectedNatures();
  		this.setAvailableNatures(detectedNatures);
  		
  		// TOOLBAR
  		this.buttonGB = new qx.ui.container.Composite(new qx.ui.layout.HBox(5, "right"));
  		this.add(this.buttonGB);
  		
  		this.setCurrentNatureTabs([]);
  		this.naturesTab = new qx.ui.tabview.TabView("top");
  		this.naturesTab.addListener("changeSelection", function(e){
  			this.setSelectedNatureTab(e.getData()[0] || null);
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
  		if(ROLES_LIST){
	  		this.rolesField.setChoiceValues(ROLES_LIST);
  		}
  		this.basicGB.add(new qx.ui.basic.Label("Roles"), {row:1,column:0});  		
  		this.basicGB.add(this.rolesField, {row:1,column:1});
  		
  		this.passPane = new org.argeo.security.ria.components.PasswordCredentialImpl(this.getSelfEdition());
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
  		if(this.basicGB.getVisibility()!= "excluded"){
	  		user.setName(this.usernameField.getValue());
	  		var roles = this.rolesField.getValue();
	  		if(roles && roles != ""){
		  		user.setRoles(roles.split(","));
	  		}else{
	  			user.setRoles([]);
	  		}
  		}
  		
  		// GO TO AND RETURN FROM SERVER
  		if(user.isCreate()){
  			if(!this.passPane.validate()){
  				this.error("Warning, passwords differ!");
  				return;
  			}
  			user.setPassword(this.passPane.getData());
  			var create = true;
  			var userExists = false;
  			var req = org.argeo.security.ria.SecurityAPI.getUserExistsService(user.getName());
  			req.addListener("completed", function(response){
  				userExists = response.getContent().value;
  			}, this);
  			req.setAsynchronous(false);
  			req.send();
  			if(userExists){
  				this.error("User already exists, choose another name!");
  				return;
  			}
  		}else{
  			var pass = this.passPane.getData();
  			if(pass != null && !this.passPane.validate()){
  				this.error("Warning, passwords differ!");
  				return;  				
  			}  			
  		}
  		this.passPane.clear();
  		var saveCompletedCallback = qx.lang.Function.bind(function(){
			this._setGuiInCreateMode(false);
  			this.partialRefreshUser(user, ["details","natures"]);
			this.setModified(false);
			this.fireDataEvent("savedUser", user);  			
  		}, this);
  		var userService = user.getSaveService(this.getSelfEdition());
  		userService.addListener("completed", function(response){
  			if(response.getContent().status && response.getContent().status == "ERROR"){
  				return;
  			}
  			user.load(response.getContent(), "json");
  			if(pass!=null){
  				var passService;
  				if(!this.getSelfEdition()){
  					passService = org.argeo.security.ria.SecurityAPI.getUpdateUserPassService(user.getName(), pass);
  				}else{
  					passService = org.argeo.security.ria.SecurityAPI.getUpdatePassService(pass.oldPass, pass.newPass);
  				}
  				passService.addListener("completed", function(response){
  					if(response.getContent().status != "ERROR"){
  						this.info(response.getContent().message);
  					}
  					saveCompletedCallback();
  				}, this);
  				passService.send();
  			}else{
	  			saveCompletedCallback();
  			}
  		}, this);  		
  		userService.send();
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
  		page.add(new qx.ui.container.Scroll(newClass.getContainer()), {edge:"center"});
  		
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
  		newClass.addListener("modified", function(){
  			this.setNaturesModified(true);
  		}, this);  
  		if(select){
  			this.naturesTab.setSelection([page]);
  		}
  		return page;
  	},
  	
  	_removeNatureTab : function(natureClass){
  		this.naturesTab.getChildren().forEach(function(el){
  			if(el.getUserData("NATURE_CLASS") == natureClass){
  				this.naturesTab.remove(el);
  				qx.lang.Array.remove(this.getCurrentNatureTabs(), natureClass.NATURE_TYPE);
  			}
  		}, this);
  		if(this.naturesTab.getChildren().length == 0){
  			this.naturesTab.setVisibility("excluded");
  			this.fakePane.setVisibility("visible");
  		}
  	},
  	
  	removeSelectedTab : function(){
  		if(this.naturesTab.isSelectionEmpty()) return;
  		var selected = this.naturesTab.getSelection()[0];
  		var tabClass = selected.getUserData("NATURE_CLASS");
  		var user = this.getCurrentUser();
  		user.removeNature(tabClass.NATURE_TYPE);
  		this.saveUser();
  		this._removeNatureTab(tabClass);
  	},
  	
  	removeAllTabs : function(){
  		while(!this.naturesTab.isSelectionEmpty()){
  			this._removeNatureTab(this.naturesTab.getSelection()[0].getUserData("NATURE_CLASS"));
  		}
  	},
  	  
  	_setGuiInCreateMode : function(bool){
  		if(bool){
  			if(!this.natureButtonGB.isVisible()) return;
  			this.natureButtonGB.hide();
  			this.fakePane.setVisibility("excluded");
  		}else{  			
  			if(this.natureButtonGB.isVisible()) return;
  			this.natureButtonGB.show();
  			this.fakePane.setVisibility("visible");
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
  	
  	loadUserData : function(userName){
  		var userDataService = org.argeo.security.ria.SecurityAPI.getUserDetailsService(userName);
  		userDataService.addListener("completed", function(response){
  			var user = new org.argeo.security.ria.model.User();
  			user.load(response.getContent(), "json");  			
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
			this._attachListeners();			
  		}, this);
  		userDataService.send();
  	},
  	
  	clearUserData : function(){
  		this.usernameField.setValue("");
  		this.rolesField.setValue([]);
  		this.removeAllTabs();
  	},
  	
  	partialRefreshUser : function(user, target){
  		if(!qx.lang.Type.isArray(target)) target = [target];
  		
  		if(qx.lang.Array.contains(target,"natures")){
  			if(this.getSelectedNatureTab()){
  				var selectedTab = this.getSelectedNatureTab().getUserData("NATURE_CLASS");
  			}
  			this.removeAllTabs();
			var userNatureTabs = this.naturesManager.detectNaturesInData(user.getNatures());
			if(userNatureTabs.length){
				userNatureTabs.forEach(function(el){
					this._addNatureTab(el.NATURE_CLASS, el.NATURE_DATA, (selectedTab && selectedTab == el.NATURE_CLASS));
				}, this);
			}			
  		}
  		if(qx.lang.Array.contains(target,"details")){
	  		this.usernameField.setValue(user.getName());
	  		this.rolesField.setValue(user.getRoles());  
	  		this.usernameField.setReadOnly(true);
	  		this.fireEvent("saveUser");
  		}
  	},
  	
  	_applyDetailsModified : function(value){},
  	
  	_applyNaturesModified : function(value){}  	
  	  	
  }
});