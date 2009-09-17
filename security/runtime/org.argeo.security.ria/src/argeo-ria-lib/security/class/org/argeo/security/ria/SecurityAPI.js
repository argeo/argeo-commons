qx.Class.define("org.argeo.security.ria.SecurityAPI", {
	extend : qx.core.Object,
	statics : {
		
		DEFAULT_CONTEXT : "org.argeo.security.ria",
		
		USERS_LIST_SERVICE : "getUsersList.security",
		USER_EXISTS_SERVICE : "userExists.security",		
		DELETE_USER_SERVICE : "deleteUser.security",
		GET_USER_DETAILS_SERVICE : "getUserDetails.security",
		CREATE_USER_SERVICE : "createUser.security",
		UPDATE_USER_PASS_SERVICE : "updateUserPassword.security",

		ROLES_LIST_SERVICE : "getRolesList.security",
		GET_USERS_ROLE_SERVICE : "getUsersForRole.security",
		CREATE_ROLE_SERVICE : "createRole.security",
		DELETE_ROLE_SERVICE : "deleteRole.security",
		
		UPDATE_USER_ROLE_LNK_SERVICE : "updateUserRoleLink.security",
		CREATE_NATURE_SERVICE : "createUserNature.security",
		DELETE_NATURE_SERVICE : "deleteUserNature.security",
		UPDATE_NATURE_SERVICE : "updateUserNature.security",

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
				org.argeo.slc.ria.SlcApi.DEFAULT_CONTEXT + "/" + serviceName, 
				"GET",
				"application/json");
		},
		
		parseOptionalArguments : function(request, argumentsArray, startIndex){
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
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getListUsersService : function(){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.USERS_LIST_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUserExistsService : function(userName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.USER_EXISTS_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.addParameter("userName", userName);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getDeleteUserService : function(userName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.DELETE_USER_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.addParameter("userName", userName);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUserDetailsService : function(userName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.GET_USER_DETAILS_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.addParameter("userName", userName);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getCreateUserService : function(userName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.CREATE_USER_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.addParameter("userName", userName);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUpdateUserPassService : function(userName, userPassword){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.UPDATE_USER_PASS_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.addParameter("userName", userName);
			req.addParameter("userPassword", userPassword);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getListRolesService : function(){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.ROLES_LIST_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUsersForRolesService : function(){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.GET_USERS_ROLE_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getCreateRoleService : function(roleName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.CREATE_ROLE_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getDeleteRoleService : function(roleName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.DELETE_ROLE_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUpdateUserRoleLinkService : function(roleName, userName){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.UPDATE_USER_ROLE_LNK_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.addParameter("roleName", roleName);
			req.addParameter("userName", userName);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getCreateNatureService : function(natureData){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.CREATE_NATURE_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.addParameter("natureData", natureData);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getDeleteNatureService : function(natureUuid){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.DELETE_NATURE_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.addParamter("natureUuid", natureUuid);
			return req;
		},
		
		/**
		 * @return  {qx.io.remote.Request}
		 */
		getUpdateNatureService : function(natureUuid, natureData){
			var req = org.argeo.security.ria.SecurityAPI.getServiceRequest(org.argeo.security.ria.SecurityAPI.UPDATE_NATURE_SERVICE);
			org.argeo.security.ria.SecurityAPI.parseOptionalArguments(req, arguments, 1);
			req.addParamter("natureUuid", natureUuid);
			req.addParameter("natureData", natureData);
			return req;
		}		
	}
});