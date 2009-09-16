/**
 * A simple Hello World applet for documentation purpose. 
 * The only associated command is the "Close" command.
 */
/* *************************************************
#asset(resource/org.argeo.ria.sample/window-close.png)
****************************************************/
qx.Class.define("org.argeo.security.ria.UserEditorApplet",
{
  extend : qx.ui.container.Composite,
  implement : [org.argeo.ria.components.IView], 

  construct : function(){
  	this.base(arguments);
	this.setLayout(new qx.ui.layout.VBox());
	//this.setDecorator("tabview-pane");
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
  				icon 		: "ria/window-close.png",
  				shortcut 	: "Control+s",
  				enabled  	: true,
  				menu	   	: "Users",
  				toolbar  	: "user",
  				callback	: function(e){
  					
  				},
  				selectionChange : function(viewName, data){
  					if(viewName != "editor") return;
  					var iApplet = org.argeo.ria.components.ViewsManager.getInstance().getViewPaneById("editor").getContent();
  					if(iApplet == null) this.setEnabled(true);
  					this.setEnabled(iApplet.getModified());
  					//this.setEnabled(!(data == null || !data.length || data.length > 1));  					
  				},
  				command 	: null
  			},
  			"close" : {
  				label	 	: "Close", 
  				icon 		: "org.argeo.ria.sample/window-close.png",
  				shortcut 	: "Control+w",
  				enabled  	: true,
  				menu	   	: "Users",
  				toolbar  	: "user",
  				callback	: function(e){
  					// Call service to delete
  					this.getView().closeCurrent();  					
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
  		init : false
  	},
  	rolesList : {
  		
  	},
  	instanceId : {init:""},
  	instanceLabel : {init:"Editor"}  	
  },

  members :
  {
  	/**
  	 * Called at applet creation. Just registers viewPane.
  	 * @param viewPane {org.argeo.ria.components.ViewPane} The viewPane.
  	 */
  	init : function(viewPane, data){
  		if(!data){
  			var now = new Date();
	  		this.setInstanceId(now.getTime());
	  		this.setInstanceLabel("New User");  			
  		}else{
	  		this.setInstanceId(data);
	  		this.setInstanceLabel("User " + data);
  		}
  		this.setView(viewPane);
  		this.setViewSelection(new org.argeo.ria.components.ViewSelection(viewPane.getViewId()));
  		
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

  		this.naturesGB = new qx.ui.groupbox.GroupBox("User Natures");
  		this.naturesGB.setLayout(new qx.ui.layout.Dock());
  		this._initializeGroupBox(this.naturesGB);
  		
  		this.add(this.basicGB);
  		this.add(this.passGB);
  		this.add(this.naturesGB, {flex:1});  		
  		
  		// FIELDS
  		this.usernameField = new qx.ui.form.TextField();
  		this.usernameField.addListener("changeValue", function(){
  			this.setModified(true);
  			this.getViewSelection().triggerEvent();
  		}, this);
  		this.basicGB.add(new qx.ui.basic.Label("Username"), {row:0,column:0});  		
  		this.basicGB.add(this.usernameField, {row:0,column:1});
  		
  		this.rolesField = new org.argeo.ria.components.ui.MultipleComboBox();
  		this.rolesField.setChoiceValues(["ROLE_ADMIN", "ROLE_USER", "ROLE_USER1"]);
  		this.basicGB.add(new qx.ui.basic.Label("Roles"), {row:1,column:0});  		
  		this.basicGB.add(this.rolesField, {row:1,column:1});
  		
  		this.passPane = new org.argeo.security.ria.components.PasswordCredentialImpl();
  		this.passGB.add(this.passPane.getContainer());
  		
  		this.naturesTab = new qx.ui.tabview.TabView("top");
  		this.simpleNature = new org.argeo.security.ria.components.SimpleUserNatureImpl();
  		var page1 = new qx.ui.tabview.Page(this.simpleNature.getNatureLabel());
  		page1.setLayout(new qx.ui.layout.Dock());
  		page1.add(this.simpleNature.getContainer(), {edge:"center"});
  		this.naturesTab.add(page1);
  		this.naturesGB.add(this.naturesTab, {edge:"center"});
  		  		
  	},
  	  	
  	_initializeGroupBox: function(groupBox){
  		groupBox.setPadding(0);
  		groupBox.getChildrenContainer().setPadding(8);  		
  	},
  	
  	/**
  	 * Load a given row : the data passed must be a simple data array.
  	 * @param data {Element} The text xml description. 
  	 */
  	load : function(userData){
  		if(userData){
	  		this.usernameField.setValue(userData);
  		}
  		this.setRolesList(["ROLE_ADMIN", "ROLE_USER"]);
  	},
  	  	 
	addScroll : function(){
		return false;
	},
	
	close : function(){
		return false;
	}
  	
  }
});