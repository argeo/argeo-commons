qx.Class.define("org.argeo.security.ria.SecurityAPI", {
	extend : qx.core.Object,
	statics : {
		
		DEFAULT_CONTEXT : "/org.argeo.security.webapp",
		
		CREDENTIALS_SERVICE : "getCredentials.security",
		
		USERS_LIST_SERVICE : "getUsersList.security",
		USER_EXISTS_SERVICE : "userExists.security",		
		DELETE_USER_SERVICE : "deleteUser.security",
		UPDATE_USER_SERVICE : "updateUser.security",
		GET_USER_DETAILS_SERVICE : "getUserDetails.security",
		CREATE_USER_SERVICE : "createUser.security",
		UPDATE_USER_PASS_SERVICE : "updateUserPassword.security",
		UPDATE_PASS_SERVICE : "updatePassword.security",

		ROLES_LIST_SERVICE : "getRolesList.security",
		CREATE_ROLE_SERVICE : "createRole.security",
		DELETE_ROLE_SERVICE : "deleteRole.security",		

	  	/**
		 * Standard Request getter
		 * @param serviceName {String} The name of the service to call (without base context)
		 * @param fireReloadEventType {String} Whether query should trigger a ReloadEvent
		 * @param iLoadStatusables {org.argeo.ria.components.ILoadStatusables[]} Gui parts to update
		 * @return {qx.io.remote.Request}
		 */
		getServiceRequest : function(serviceName) {
			var serviceManager = org.argeo.ria.remote.RequestManager.getInstance();
			return serviceManager.getRequest(
				org.argeo.security.ria.SecurityAPI.DEFAULT_CONTEXT + "/" + serviceName, 
				"GET",
				"application/json");
		},
		
		/**
		 * 
		 * @param {qx.io.remote.Request} request
		 * @param {Array} argumentsArray
		 * @param {Integer} startIndex
		 */
		parseOptionalArguments : function(request, argumentsArray, startIndex){
			// Attach Error listener
			request.addListener("completed", function(response){
				var jSonContent = response.getContent();  
				if(typeof jSonContent == "object" && jSonContent.status && jSonContent.status == "ERROR"){
					org.argeo.ria.components.Logger.getInstance().error(jSonContent.message);
				}
				request.setState("failed");
			});

			// Attach ILoadStatusables & reloadEvents
			if(argumentsArray.length <= startIndex) return;
			var serviceManager = org.argeo.ria.remote.RequestManager.getInstance();
			for(var i=startIndex;i<argumentsArray.length;i++){
				var argument = argumentsArray[i];
				if(qx.lang.Array.isArray(argument)){
					serviceManager.attachILoadStatusables(request, argument);
				}else if(typeof argument == "string"){
					serviceManager.attachReloadEventType(request, argument);
				}
			}
		},
		
		getCredentialsService : function(){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.CREDENTIALS_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 0);
			return req;			
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getListUsersService : function(getNatures){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.USERS_LIST_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.setParameter("getNatures", (getNatures || false));
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUserExistsService : function(userName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.USER_EXISTS_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.setParameter("username", userName);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getDeleteUserService : function(userName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.DELETE_USER_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.setParameter("username", userName);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUpdateUserService : function(userObject){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.UPDATE_USER_SERVICE);
			req.setMethod("POST");
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			var jsonString = qx.util.Json.stringify(userObject);
			req.setData(jsonString);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUserDetailsService : function(userName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.GET_USER_DETAILS_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.setParameter("username", userName);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getCreateUserService : function(userName, password){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.CREATE_USER_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 2);
			req.setParameter("username", userName);
			req.setParameter("password", password);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUpdatePassService : function(oldPassword, newPassword){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.UPDATE_USER_PASS_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 2);
			req.setParameter("password", newPassword);
			req.setParameter("oldpassword", oldPassword);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUpdateUserPassService : function(userName, userPassword){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.UPDATE_USER_PASS_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 2);
			req.setParameter("username", userName);
			req.setParameter("password", userPassword);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getListRolesService : function(){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.ROLES_LIST_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 0);
			return req;
		},
				
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getCreateRoleService : function(roleName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.CREATE_ROLE_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.setParameter("role", roleName);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getDeleteRoleService : function(roleName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.DELETE_ROLE_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.setParameter("role", roleName);
			return req;
		}				
	}
});