qx.Class.define("org.argeo.security.ria.components.NaturesManager",{
	extend : qx.core.Object,
	properties : {
		detectedNatures : {
			check : "Map"			
		},
		nonAssignedNatures : {
			check : "Map", 
			event : "changeNonAssignedNatures"
		}
	},
	construct : function(){
		this.base(arguments);
		this.detectNatures();
	},
	members : {
		detectNatures : function(){			
			var natures = {};
			var sortedNatures = {};
			var ranks = {};
			for (var key in qx.Bootstrap.$$registry) {
				if (qx.Class.hasInterface(qx.Bootstrap.$$registry[key],	org.argeo.security.ria.components.INaturePane)) {
					// FILTER BY ROLE HERE!
					var klass = qx.Bootstrap.$$registry[key];
					natures[klass.NATURE_TYPE] = klass;
					ranks[klass.NATURE_TYPE] = klass.NATURE_RANK;
				}
			}
			org.argeo.ria.util.Utils.asort(ranks);
			for(var key in ranks){
				sortedNatures[key] = natures[key];
			}
			this.setDetectedNatures(sortedNatures);
			this.setNonAssignedNatures(sortedNatures);
		},
		detectNaturesInData : function(userNaturesList){
			var detected = this.getDetectedNatures();
			var userDetected = [];
			var nonAssigned = qx.lang.Object.clone(detected);
			for(var i = 0;i<userNaturesList.length;i++){
				var type = userNaturesList[i].type;
				if(detected[type]){
					userDetected.push({
						NATURE_CLASS : detected[type],
						NATURE_DATA  : userNaturesList[i]
					});
					delete(nonAssigned[type]);
				}					
			}
			this.setNonAssignedNatures(nonAssigned);
			return userDetected;
		}
	}
});