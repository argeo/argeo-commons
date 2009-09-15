/**
 * A simple Hello World applet for documentation purpose. 
 * The only associated command is the "Close" command.
 */
/* *************************************************
#asset(resource/org.argeo.ria.sample/window-close.png)
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
  			"close" : {
  				label	 	: "Close", 
  				icon 		: "ria/window-close.png",
  				shortcut 	: "Control+w",
  				enabled  	: true,
  				menu	   	: "Applet",
  				toolbar  	: "result",
  				callback	: function(e){
  					// Call service to delete
  					this.getView().empty();  					
  				},
  				command 	: null
  			}  			
  		}
  	},
  	viewSelection : {
  		nullable:false, 
  		check:"org.argeo.ria.components.ViewSelection"
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
  		this.tableModel = new qx.ui.table.model.Filtered();
  		this.tableModel.setColumns(["username", "roles"]);
  		this.table = new qx.ui.table.Table(this.tableModel, {
			  	tableColumnModel: function(obj){
					return new qx.ui.table.columnmodel.Resize(obj)
				}
			});
  		this.table.setStatusBarVisible(false);  		
  		viewPane.add(this.table, {height:"100%"});
  	},
  	
  	/**
  	 * Load a given row : the data passed must be a simple data array.
  	 * @param data {Element} The text xml description. 
  	 */
  	load : function(){  		
  		var data = [["mbaudier", "ROLE_ADMIN,ROLE_USER"], ["cdujeu","ROLE_USER"]];
  		this.tableModel.setData(data);
  		this.applyFilter("ROLE_ADMIN", "roles", true);
  	},
  	
  	applyFilter : function(filterValue, target, ignoreCase){
  		this.tableModel.addRegex("^((?!"+filterValue+").)*$", target, ignoreCase);
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