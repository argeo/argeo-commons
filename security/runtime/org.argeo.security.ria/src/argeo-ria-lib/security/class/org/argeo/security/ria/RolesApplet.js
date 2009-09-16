/**
 * A simple Hello World applet for documentation purpose. 
 * The only associated command is the "Close" command.
 */
/* *************************************************
#asset(resource/org.argeo.ria.sample/window-close.png)
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
  			"edit_role" : {
  				label	 	: "Edit Role", 
  				icon 		: "ria/window-close.png",
  				shortcut 	: "Control+r",
  				enabled  	: true,
  				menu	   	: "Roles",
  				toolbar  	: "role",
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
  		
  		
  		this.rolesUsersStub = {"ROLE_ADMIN":["root","mbaudier"],"ROLE_USER":["mbaudier","cdujeu"]};
  		  		
  		var buttonPane = new qx.ui.container.Composite(new qx.ui.layout.HBox(2, "right"));
  		this.getView().header.setPadding(0);
  		if(this.getView().headerLabel) this.getView().headerLabel.setMargin(8);
  		this.getView().header.add(buttonPane, {edge:"east"});
  	
  		this.toggleButton = new qx.ui.form.ToggleButton("Filter", "ria/go-right.png");
  		this.toggleButton.set({
  			show:"icon",
  			margin:2,
  			toolTip :new qx.ui.tooltip.ToolTip("Apply automatic filtering on Users list")  			
  		});
  		buttonPane.add(this.toggleButton);
  		  		
  		// TOGGLE THE GUI MODES  		
  		this.toggleButton.addListener("changeChecked", function(event){
  			this.setGuiMode(event.getData()?"filter":"clear");
  		}, this);
		
		this.saveButton = new qx.ui.form.Button("Save");
  		this.saveButton.set({
  			show:"label",
  			margin:2,
  			toolTip :new qx.ui.tooltip.ToolTip("Save changes"),
  			visibility : "excluded"
  		});
  		buttonPane.add(this.saveButton);		
		
		this.cancelButton = new qx.ui.form.Button("Cancel");
  		this.cancelButton.set({
  			show:"label",
  			margin:2,
  			toolTip :new qx.ui.tooltip.ToolTip("Cancel changes"),
  			visibility : "excluded"  			
  		});
  		buttonPane.add(this.cancelButton);		
		
  		var listener = function(){
  			this.setGuiMode(this.initialState);
  		};
  		this.saveButton.addListener("execute", listener, this);
  		this.cancelButton.addListener("execute", listener, this);
  		
		this.table.addListener("cellDblclick", function(cellEvent){
			this.setGuiMode("edit");
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
			selectionModel.setSelectionMode(qx.ui.table.selection.Model.MULTIPLE_INTERVAL_SELECTION_TOGGLE);
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
			selectionModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);  			
  		}
  	},
  	
  	monitorChooserSelectionChanges : function(event){
  		if(!this.usersAppletReference || this.getChooserSelectionModified()) return;
  		var initialSelection = this.getChooserOriginalSelection();
  		var crtSelection = event.getTarget().getNodes();
  		if(!qx.lang.Array.equals(initialSelection.sort(), crtSelection.sort())){
  			this.setChooserSelectionModified(true);
  			console.log("Changed!");
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
		var initSelection = this.rolesUsersStub[uniqueValue];
		this.setChooserOriginalSelection(initSelection);
		this.setChooserSelectionModified(false);
		this.usersAppletReference.applySelection(initSelection, "username");
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
  	
  	/**
  	 * Load a given row : the data passed must be a simple data array.
  	 * @param data {Element} The text xml description. 
  	 */
  	load : function(){  		
  		var data = [["ROLE_ADMIN"],["ROLE_USER"]];
  		this.tableModel.setData(data);
  	},
  	  	 
	addScroll : function(){
		return false;
	},
	
	close : function(){
		return false;
	}
  	
  }
});