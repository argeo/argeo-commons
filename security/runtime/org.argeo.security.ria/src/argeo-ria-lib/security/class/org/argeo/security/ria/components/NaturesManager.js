qx.Class.define("org.argeo.security.ria.components.NaturesManager",{
	extend : qx.core.Object,
	properties : {
		detectedNatures : {
			check : "Map"
		}
	},
	construct : function(){
		this.base(arguments);
		this.detectNatures();
	},
	members : {
		detectNatures : function(){			
			var natures = {};
			for (var key in qx.Bootstrap.$$registry) {
				if (qx.Class.hasInterface(qx.Bootstrap.$$registry[key],	org.argeo.security.ria.components.INaturePane)) {
					// FILTER BY ROLE HERE!
					var klass = qx.Bootstrap.$$registry[key];
					natures[klass.NATURE_TYPE] = klass;
				}
			}
			this.setDetectedNatures(natures);
		},
		detectNaturesInData : function(userNaturesList){
			var detected = this.getDetectedNatures();
			var userDetected = [];
			for(var i = 0;i<userNaturesList.length;i++){
				var type = userNaturesList[i].type;
				if(detected[type]){
					userDetected.push({
						NATURE_CLASS : detected[type],
						NATURE_DATA  : userNaturesList[i]
					});
				}
			}
			return userDetected;
		}
	}
});