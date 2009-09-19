/**
 * A simple Hello World applet for documentation purpose. 
 * The only associated command is the "Close" command.
 */
/* *************************************************
#asset(resource/org.argeo.security.ria/*)
****************************************************/
qx.Class.define("org.argeo.security.ria.UsersApplet",
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
  			"load_users" : {
  				label 		: "Reload Users",
  				icon 		: "org.argeo.security.ria/view-refresh.png",
  				shortcut	: "Control+h",
  				enabled 	: true,
  				menu		: "Users",
  				toolbar		: "users",
  				callback	: function(e){
  					this.loadUsersList();
  				},
  				command		: null
  			},
  			"new_user" : {
  				label	 	: "New User", 
  				icon 		: "org.argeo.security.ria/list-add.png",
  				shortcut 	: "Control+n",
  				enabled  	: true,
  				menu	   	: "Users",
  				toolbar  	: null,
  				callback	: function(e){
  					// Call service to delete
  					var classObj = org.argeo.security.ria.UserEditorApplet;
					var iView = org.argeo.ria.components.ViewsManager.getInstance().initIViewClass(classObj, "editor");
					iView.load();
  				},
  				command 	: null
  			},   	
  			"delete_user" : {
  				label	 	: "Delete User", 
  				icon 		: "org.argeo.security.ria/list-remove.png",
  				shortcut 	: "Control+s",
  				enabled  	: true,
  				menu	   	: "Users",
  				toolbar  	: null,
  				callback	: function(e){
  					// Call service to delete
					var crtUsers = this.getViewSelection().getNodes();
					for(var i=0;i<crtUsers.length;i++){
						alert("Delete " + crtUsers[i]);
					}
  				},
  				selectionChange : function(viewName, data){
  					if(viewName != "users") return;
  					this.setEnabled(!(data == null || !data.length));  					
  				},
  				command 	: null
  			},  			
  			"edit_user" : {
  				label	 	: "Edit User", 
  				icon 		: "org.argeo.security.ria/document-properties.png",
  				shortcut 	: "Control+u",
  				enabled  	: true,
  				menu	   	: "Users",
  				toolbar  	: null,
  				callback	: function(e){
  					// Call service to delete
					var crtUser = this.getViewSelection().getNodes()[0];
					var userObject = this.getUsersList()[crtUser];
  					var classObj = org.argeo.security.ria.UserEditorApplet;
					var iView = org.argeo.ria.components.ViewsManager.getInstance().initIViewClass(classObj, "editor", crtUser);
					iView.load(userObject);					
  				},
  				selectionChange : function(viewName, data){
  					if(viewName != "users") return;
  					this.setEnabled(!(data == null || !data.length || data.length > 1));  					
  				},
  				command 	: null
  			}
  		}
  	},
  	
  	guiMode : {
  		init : "filter",
  		apply : "_applyGuiMode"
  	},
  	
  	viewSelection : {
  		nullable:false, 
  		check:"org.argeo.ria.components.ViewSelection"
  	},
  	usersList : {
  		check : "Map",
  		apply : "_applyUsersList",
  		event : "changeUsersList"
  	},
  	rolesList : {
  		check : "Array",
  		event : "changeRolesList"
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
  		  		
  		this.tableModel = new qx.ui.table.model.Filtered();
  		this.tableModel.setColumns(["username", "roles"]);
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
		this.table.addListener("cellDblclick", function(cellEvent){
			this.getCommands()["edit_user"].command.execute();
		}, this);
		
  		this.setUsersList({});		
  		this.setGuiMode("clear");
  	},
  	
  	_applyGuiMode : function(newMode, oldMode){
  		this.table.getSelectionModel().clearSelection();
		this.resetHiddenRows();
  		if(newMode == "filter"){
  			this.table.getSelectionModel().setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
  		}else if(newMode == "chooser"){
  			this.table.getSelectionModel().setSelectionMode(qx.ui.table.selection.Model.MULTIPLE_INTERVAL_SELECTION_TOGGLE);
  		}else if(newMode == "clear"){
  			this.table.getSelectionModel().setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
  		}
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
  	
  	/**
  	 * Load a given row : the data passed must be a simple data array.
  	 * @param data {Element} The text xml description. 
  	 */
  	load : function(){  		
  		var commands = this.getCommands();
  		this.toolBarPart.add(commands["new_user"].command.getToolbarButton());
  		this.toolBarPart.add(commands["delete_user"].command.getToolbarButton());
  		this.toolBarPart.add(commands["edit_user"].command.getToolbarButton());  		
  		this.toolBar.setShow("icon");
		this.loadUsersList();
  	},
  	
  	loadUsersList : function(){
  		var request = org.argeo.security.ria.SecurityAPI.getListUsersService();
  		request.addListener("completed", function(response){
  			var jSon = response.getContent();
  			var usMap = {};
  			for(var i=0;i<jSon.length;i++){
  				var user = new org.argeo.security.ria.model.User();
  				user.load(jSon[i], "json");
  				usMap[user.getName()] = user;
  			}
  			this.setUsersList(usMap);  			
  		}, this);
  		request.send();  		
  	},
  	
  	_applyUsersList : function(usList){
  		var data = [];
  		var rolesList = [];
  		qx.lang.Object.getValues(usList).forEach(function(usObject){
  			var roles = usObject.getRoles();
  			rolesList = qx.lang.Array.unique(rolesList.concat(roles));
  			data.push([usObject.getName(), usObject.getRoles().join(",")]);
  		});
  		this.setRolesList(rolesList);
  		this.tableModel.setData(data);  		  		
  	},
  	  	
  	applySelection : function(selectionValue, target, ignoreCase){
  		var selectionModel = this.table.getSelectionModel();  		
  		selectionModel.clearSelection();
  		if(!selectionValue){
  			return;
  		}
  		selectionModel.setBatchMode(true);
  		var data = this.tableModel.getData();
  		for(var i=0;i<this.tableModel.getRowCount();i++){
  			var value = this.tableModel.getRowDataAsMap(i)[target];
  			var pattern = new RegExp(selectionValue, "g"+(ignoreCase?"i":""));
  			if(pattern.test(value)){
  				selectionModel.addSelectionInterval(i, i);
  			}
  		}
  		selectionModel.setBatchMode(false);
  	},
  	
  	applyFilter : function(filterValues, target, ignoreCase){
  		this.table.clearSelection();
  		this.resetHiddenRows();  		
  		for(var i=0;i<filterValues.length;i++){
	  		this.tableModel.addRegex("^((?!"+filterValues[i]+").)*$", target, ignoreCase);
  		}
  		this.tableModel.applyFilters();
  	},
  	
  	resetHiddenRows : function(){
  		this.tableModel.resetHiddenRows();
  	},
  	 
	addScroll : function(){
		return false;
	},
	
	close : function(){
		return false;
	}
  	
  }
});