/**
 * A simple Hello World applet for documentation purpose. 
 * The only associated command is the "Close" command.
 */
/* *************************************************
#asset(org/argeo/ria/sample/window-close.png)
****************************************************/
qx.Class.define("org.argeo.security.ria.RolesApplet",
{
  extend : qx.ui.container.Composite,
  implement : [org.argeo.ria.components.IView], 

  construct : function(){
  	this.base(arguments);
	this.setLayout(new qx.ui.layout.VBox());
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
  			"reload" : {
  				label 		: "Reload Data",
  				icon 		: "org/argeo/security/ria/view-refresh.png",
  				shortcut	: "Control+h",
  				enabled 	: true,
  				menu		: "Roles",
  				toolbar		: "roles",
  				callback	: function(e){
  					this.loadRolesList();
  				},
  				command		: null
  			},  			
  			"new_role" : {
  				label	 	: "Create Role", 
  				icon 		: "org/argeo/security/ria/list-add.png",
  				shortcut 	: null,
  				enabled  	: true,
  				menu	   	: "Roles",
  				toolbar  	: null,
  				callback	: function(e){
  					// Prompt for new name
  					var modal = new org.argeo.ria.components.Modal();
  					modal.makePromptForm("Please enter a role name", function(roleName){
  						var service = org.argeo.security.ria.SecurityAPI.getCreateRoleService(roleName);
  						service.addListener("completed", function(response){
  							this.loadRolesList();
  						}, this);
  						service.send();
  					}, this);
  					modal.attachAndShow();
  				},
  				command 	: null
  			},   	
  			"delete_role" : {
  				label	 	: "Delete Role", 
  				icon 		: "org/argeo/security/ria/list-remove.png",
  				shortcut 	: null,
  				enabled  	: true,
  				menu	   	: "Roles",
  				toolbar  	: null,
  				callback	: function(e){
  					// Call service to delete
					var roles = this.getViewSelection().getNodes();
					var modal = new org.argeo.ria.components.Modal("Delete");					
					modal.addConfirm("Are you sure you want to delete the selected roles?");
					modal.addListener("ok", function(e){
						for(var i=0;i<roles.length;i++){
	  						var service = org.argeo.security.ria.SecurityAPI.getDeleteRoleService(roles[i]);
	  						service.addListener("completed", function(response){
	  							this.loadRolesList();
	  						}, this);
	  						service.send();
						}
					}, this);
					modal.attachAndShow();					
  				},
  				selectionChange : function(viewName, data){
  					if(viewName != "roles") return;
  					this.setEnabled(!(data == null || !data.length));  					
  				},
  				command 	: null
  			},  			  			
  			"edit_role" : {
  				label	 	: "Edit Role", 
  				icon 		: "org/argeo/security/ria/document-properties.png",
  				shortcut 	: "Control+r",
  				enabled  	: true,
  				menu	   	: "Roles",
  				toolbar  	: null,
  				callback	: function(e){
  					// Call service to delete
					this.setGuiMode("edit");
  				},
  				selectionChange : function(viewName, data){
  					if(viewName != "roles") return;
  					this.setEnabled(!(data == null || !data.length || data.length > 1));  					
  				},
  				command 	: null
  			}  			  			
  		}
  	},
  	viewSelection : {
  		nullable:false, 
  		check:"org.argeo.ria.components.ViewSelection"
  	},
  	guiMode : {
  		apply : "_applyGuiMode"
  	},
  	rolesList : {
  		check : "Array",
  		event : "changeRolesList"
  	},  	
  	chooserOriginalSelection : {},
  	chooserSelectionModified : {
  		init:false,
  		event : "chooserSelectionWasModified"
  	},
  	instanceId : {init:""},
  	instanceLabel : {init:""}
  },

  members :
  {
  	/**
  	 * Called at applet creation. Just registers viewPane.
  	 * @param viewPane {org.argeo.ria.components.ViewPane} The viewPane.
  	 */
  	init : function(viewPane){
  		this.setView(viewPane);
  		this.setViewSelection(new org.argeo.ria.components.ViewSelection(viewPane.getViewId()));
  		
  		this.toolBar = new qx.ui.toolbar.ToolBar();
  		this.toolBarPart = new qx.ui.toolbar.Part();
  		this.toolBar.add(this.toolBarPart);  		
  		viewPane.add(this.toolBar);
  		
  		this.tableModel = new qx.ui.table.model.Simple();
  		this.tableModel.setColumns(["Role Name"]);
  		this.table = new qx.ui.table.Table(this.tableModel, {
			  	tableColumnModel: function(obj){
					return new qx.ui.table.columnmodel.Resize(obj)
				}
			});
  		this.table.setStatusBarVisible(false);
  		this.table.setShowCellFocusIndicator(false);
  		this.table.setColumnVisibilityButtonVisible(false);
  		this.table.highlightFocusedRow(false);  		
  		viewPane.add(this.table, {height:"100%"});
  		this.table.getSelectionModel().addListener("changeSelection", function(){
  			this._selectionToValues(this.table.getSelectionModel(), this.getViewSelection());
  		}, this);
  		
  		this.rolesUsersStub = {"ROLE_ADMIN":["gandalf"],"ROLE_USER":["demo","frodo","gandalf"]};
  		  		  	
  		this.toggleButton = new qx.ui.form.ToggleButton("Filter", "org/argeo/security/ria/go-next.png");
  		this.toggleButton.set({
  			show:"icon",
  			margin:2,
  			toolTip :new qx.ui.tooltip.ToolTip("Apply automatic filtering on Users list")  			
  		});
  		  		
  		// TOGGLE THE GUI MODES  		
  		this.toggleButton.addListener("changeValue", function(event){
  			this.setGuiMode(event.getData()?"filter":"clear");
  		}, this);
		
		this.saveButton = new qx.ui.form.Button("Save", "org/argeo/security/ria/document-save.png");
  		this.saveButton.set({
  			show:"icon",
  			margin:2,
  			toolTip :new qx.ui.tooltip.ToolTip("Save changes"),
  			visibility : "excluded"
  		});
		
		this.cancelButton = new qx.ui.form.Button("Cancel", "org/argeo/security/ria/window-close.png");
  		this.cancelButton.set({
  			show:"icon",
  			margin:2,
  			toolTip :new qx.ui.tooltip.ToolTip("Cancel changes"),
  			visibility : "excluded"  			
  		});
		
  		this.saveButton.addListener("execute", function(){
  			if(!this.usersAppletReference){
  				this.setGuiMode(this.initialState);
  				return;
  			}
  			var newSelection = this.usersAppletReference.getViewSelection().getNodes();
  			var diff = this._selectionDiff(this.getChooserOriginalSelection(), newSelection);
  			this.saveRoleModifications(diff.deltaPlus, diff.deltaMinus);
			this.setGuiMode(this.initialState);
  		}, this);
  		this.cancelButton.addListener("execute", function(){
  			if(!this.getChooserSelectionModified()){
  				this.setGuiMode(this.initialState);
  				return;
  			}
			var modal = new org.argeo.ria.components.Modal("Warning");
			modal.addConfirm("There are unsaved changes!\n Are you sure you want to close?");
			modal.addListener("ok", function(){
				this.setGuiMode(this.initialState);
			}, this);
			modal.attachAndShow();  		
  		}, this);
  		
		this.table.addListener("cellDblclick", function(cellEvent){
			this.setGuiMode("edit");
		}, this);
		this.addListener("changeRolesList", function(event){
  			var data = [];
  			event.getData().forEach(function(el){data.push([el]);});
  			this.tableModel.setData(data);  						
		}, this);
		
		this.setGuiMode("clear");
  	},
  	
  	_applyGuiMode : function(guiMode, previousMode){
		var selectionModel = this.table.getSelectionModel();
		if(!this.usersAppletReference){
			var vManager = org.argeo.ria.components.ViewsManager.getInstance();
			this.usersAppletReference = vManager.getViewPaneById("users").getContent();
		}
		
		this.saveButton.setVisibility((guiMode=="edit"?"visible":"excluded"));
		this.cancelButton.setVisibility((guiMode=="edit"?"visible":"excluded"));
		this.table.setEnabled((guiMode=="edit"?false:true));
		this.toggleButton.setVisibility((guiMode=="edit"?"excluded":"visible"));			
		
  		if(guiMode == "filter"){
			if(this.usersAppletReference){
				this.usersAppletReference.setGuiMode(("filter"));
				var viewSel = this.usersAppletReference.getViewSelection();
				viewSel.removeListener("changeSelection", this.monitorChooserSelectionChanges, this);			
			}
			selectionModel.addListener("changeSelection", this.selectionToFilter, this);
			if(selectionModel.getSelectedCount()){
				var orig = selectionModel.getSelectedRanges()[0].minIndex;
			}
			selectionModel.setSelectionMode(qx.ui.table.selection.Model.MULTIPLE_INTERVAL_SELECTION_TOGGLE);
			if(orig){
				selectionModel.addSelectionInterval(orig, orig);
			}
			this.selectionToFilter();  			
  		}else if(guiMode == "edit"){
			if(!this.usersAppletReference) return;
			this.initialState = previousMode;
			if(previousMode == "filter"){
				this.usersAppletReference.setGuiMode(("clear"));
				selectionModel.removeListener("changeSelection", this.selectionToFilter, this);
			}
			this.usersAppletReference.setGuiMode(("chooser"));
			this.selectionToChooser(); // Warning, to be called before calling listener!
			var viewSel = this.usersAppletReference.getViewSelection();
			viewSel.addListener("changeSelection", this.monitorChooserSelectionChanges, this);
  		}else if(guiMode == "clear"){
			if(this.usersAppletReference){
				this.usersAppletReference.setGuiMode(("clear"));
				var viewSel = this.usersAppletReference.getViewSelection();
				viewSel.removeListener("changeSelection", this.monitorChooserSelectionChanges, this);			
			}
			this.table.setEnabled(true);
			selectionModel.removeListener("changeSelection", this.selectionToFilter, this);
			if(selectionModel.getSelectedCount()){
				var orig = selectionModel.getSelectedRanges()[0].minIndex;
			}
			selectionModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);  			
			if(orig){
				selectionModel.addSelectionInterval(orig, orig);
			}
  		}
  	},
  	
  	saveRoleModifications : function(deltaPlus, deltaMinus){
  		// LOAD CONCERNED USERS
  		var selectionModel = this.table.getSelectionModel();
		if(!selectionModel.getSelectedCount()){
			return;
		}
		var roleValue = this._selectionToValues(selectionModel)[0];
  		
  		var users = deltaPlus.concat(deltaMinus);
  		var modal = new org.argeo.ria.components.Modal("Batch Update", "", "Please wait, updating roles for selected users");
  		modal.attachAndShow();
  		for(var i=0;i<users.length;i++){
  			var user = users[i];
  			var userDetailService = org.argeo.security.ria.SecurityAPI.getUserDetailsService(users[i]);
  			userDetailService.addListener("completed", function(response){
  				var userRoles = response.getContent().roles;
  				if(qx.lang.Array.contains(deltaPlus, user)){
  					userRoles.push(roleValue);
  				}else if(qx.lang.Array.contains(deltaMinus, user)){
  					qx.lang.Array.remove(userRoles, roleValue);
  				}
  				var userSaveService = org.argeo.security.ria.SecurityAPI.getUpdateUserService(response.getContent());
  				userSaveService.setAsynchronous(false);
  				userSaveService.send();
  			}, this);
  			userDetailService.setAsynchronous(false);
  			userDetailService.send();
  		}
  		this.fireDataEvent("changeRolesList", this.getRolesList());
  		modal.hide();
  		modal.destroy();
  	},
  	
  	monitorChooserSelectionChanges : function(event){
  		if(!this.usersAppletReference || this.getChooserSelectionModified()) return;
  		var initialSelection = this.getChooserOriginalSelection();
  		var crtSelection = event.getTarget().getNodes();
  		if(!qx.lang.Array.equals(initialSelection.sort(), crtSelection.sort())){
  			this.setChooserSelectionModified(true);  			
  			this.saveButton.setEnabled(true);
  		}
  	},
  	
  	selectionToFilter : function(){
  		if(!this.usersAppletReference) return;
  		var selectionModel = this.table.getSelectionModel();
		if(!selectionModel.getSelectedCount()){
			this.usersAppletReference.resetHiddenRows();
			return;
		}
		this.usersAppletReference.applyFilter(this._selectionToValues(selectionModel), "roles", true);  		
  	},
  	
  	selectionToChooser : function(){
  		if(!this.usersAppletReference) return;
  		var selectionModel = this.table.getSelectionModel();
		if(!selectionModel.getSelectedCount()){
			this.usersAppletReference.resetHiddenRows();
			return;
		}
		var uniqueValue = this._selectionToValues(selectionModel)[0];
		//var initSelection = this.rolesUsersStub[uniqueValue];
		this.usersAppletReference.applySelection(uniqueValue, "roles");
		var initSelection = this.usersAppletReference.getViewSelection().getNodes(); 
		this.setChooserOriginalSelection(initSelection);
		this.setChooserSelectionModified(false);
		this.saveButton.setEnabled(false);
  	},
  	
  	_selectionToValues : function(selectionModel, viewSelection){  		
  		if(viewSelection){
  			viewSelection.setBatchMode(true);
  			viewSelection.clear();
  		}
  		if(!selectionModel.getSelectedCount()) return [];
		var ranges = selectionModel.getSelectedRanges();
		var values = [];
		for(var i=0;i<ranges.length;i++){
			for(var j=ranges[i].minIndex;j<=ranges[i].maxIndex;j++){  					
				values.push(this.tableModel.getData()[j][0]);
				if(viewSelection){
					viewSelection.addNode(this.tableModel.getData()[j][0]);
				}
			}
		}
  		if(viewSelection){
  			viewSelection.setBatchMode(false);
  		}
  		return values;
  	},
  	
  	_selectionDiff : function(initialSelection, modifiedSelection){
  		var deltaMinus = qx.lang.Array.clone(initialSelection);
  		var deltaPlus = qx.lang.Array.clone(modifiedSelection);
  		qx.lang.Array.exclude(deltaPlus, initialSelection);
  		qx.lang.Array.exclude(deltaMinus, modifiedSelection);
  		return {deltaPlus : deltaPlus, deltaMinus : deltaMinus};
  	},
  	
  	/**
  	 * Load a given row : the data passed must be a simple data array.
  	 * @param data {Element} The text xml description. 
  	 */
  	load : function(){
  		
  		var commands = this.getCommands();
  		this.toolBarPart.add(commands["new_role"].command.getToolbarButton());
  		this.toolBarPart.add(commands["delete_role"].command.getToolbarButton());
  		this.toolBarPart.add(commands["edit_role"].command.getToolbarButton());  		
  		this.toolBar.addSpacer();
  		this.toolBar.add(this.toggleButton);
  		this.toolBar.add(this.saveButton);		
  		this.toolBar.add(this.cancelButton);		  		
  		this.toolBar.setShow("icon");
  		
  		this.loadRolesList();
  		
  	},
  	
  	loadRolesList : function(){
  		this.setRolesList([]);
  		var service = org.argeo.security.ria.SecurityAPI.getListRolesService();
  		service.addListener("completed", function(response){
  			this.setRolesList(response.getContent());
  		}, this);
  		service.send();  		
  	},
  	  	 
	addScroll : function(){
		return false;
	},
	
	close : function(){
		return false;
	}
  	
  }
});