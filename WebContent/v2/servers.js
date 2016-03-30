function serversCtrl($scope, $route, $rootScope, $templateCache, $uibModal) {
	$templateCache.remove($route.current.templateUrl);
	console.info("serversCtrl!");
	var xhr = RS.ctx.ServerCfgRes.getAllJson.call();

	var gridCfgs = {
		flatEntityAccess:true,
		paginationPageSizes: [20, 50, 75],
		paginationPageSize: 20,
		rowIdentity: serverCfgKey,
		rowEquality: rowKeyEq,
		onRegisterApi : function(gridApi) {
			$scope.gridApi = gridApi;
			$scope.gridApi.grid.registerRowsProcessor($scope.singleFilter, 200);
		},
		columnDefs : [ {field : "id", maxWidth:40},
		               {field : "host", maxWidth: 100},
		               {field : "port", maxWidth: 100}, 
		               {field : "virtualHost", maxWidth: 100}, 
		               {field : "userName", maxWidth: 100}, 
		               {field : "password", maxWidth: 100}, 
		               {field : "logFilePath"}, 
		               {field : "maxFileSize", maxWidth: 100},
		               {field : "id", maxWidth: 120, displayName:"", cellTemplate:"cellCtrl", enableSorting:false, enableColumnMenu: false}]
	};
	gridCfgs.data = xhr.responseJSON;
	gridCfgs.enableFiltering = false;

	$scope.gridCfgs = gridCfgs;

	$scope.filter = function() {
		$scope.gridApi.grid.refresh();
	};

	$scope.singleFilter = buildFilter($scope, [ "virtualHost", "userName", "password", "logFilePath" ]);
	
	function serverModalCtrl($scope, $uibModalInstance, sc) {
		$scope.sc = sc;
		$scope.save = function() {
			var saved = saveServerCfg(this.sc);
			if (saved) {
				this.sc = saved;
				$uibModalInstance.close(this.sc);
				return;
			}
		};
		$scope.cancel = function() {
			$uibModalInstance.dismiss("cancel");
		};
	}

	$scope.update = function(sc) {
		var g = this.gridApi.grid;
		var rows = g.getRow(sc);
		if (rows) {
			updateServerCfgs(sc, gridCfgs.data);
			rows.entity = sc;
			g.modifyRows([sc]);
		} else {
			gridCfgs.data.push(sc);
			
		}
//		g.notifyDataChange("all");
	}
	
	$scope.open = function(sc) {
		var modal = $uibModal.open({
			templateUrl: "server_modal",
			controller: ["$scope", "$uibModalInstance", "sc", serverModalCtrl],
//			size: "sm",
			resolve: {
				"sc": function() {
					var _sc=angular.copy(sc);
					delete _sc.label;
					return _sc;
				}
			}
		}).result.then(this.update.bind(this));
	}
	
	$scope.create = function(_ctx) {
		this.open(newServerCfg());
	}
	$scope.edit = function(sc) {
		this.open(sc);
	}
	$scope.copy = function(sc) {
		this.open(newServerCfg(sc));
	}
}

function newServerCfg(original) {
	var serverCfg = null;
	if (original) {
		serverCfg = $.extend({}, original);
		serverCfg.id = -1;
	} else {
		serverCfg = {
			version : 0,
			host : "localhost",
			id : -1,
			logFilePath: "",
			maxFileSize : "2GB",
			password : "guest",
			port : 5672,
			userName : "guest",
			virtualHost : "/"
		};
	}
	return serverCfg;
}

function serverCfgToString(sc) {
	return sc.id + ": " + sc.host + ":" + sc.port + "/" + sc.virtualHost;
}

function serverCfgKey(sc) {
	if (!$.isEmptyObject(sc.id) || sc.id > 0) return sc.id;
	return sc.host + ":" + sc.port + "/" + sc.virtualHost;
}

function updateServerCfgs(sc, scs) {
	return updateArray(sc, scs, serverCfgKey);
}

function getServerCfg(idx) {
	if (!idx || idx < 0) {
		return newServerCfg();
	}

	var serverCfg = null;
	var xhr = RS.ctx.ServerCfgRes.getJson.with_id(idx).call();
	if (xhr.statusCode().status != 200) {
		console.log(xhr);
		window.alert(xhr.responseText);
		return;
	}
	serverCfg = xhr.responseJSON;
	return serverCfg;
}

function saveServerCfg(serverCfg) {
	var xhr = null;

	var scStr=JSON.stringify(serverCfg);
	var oper = serverCfg.id < 0 ? RS.ctx.ServerCfgRes.create : RS.ctx.ServerCfgRes.update;   
	xhr = oper.with_entity(scStr).call({async : false});

	if (xhr.statusCode().status != 200) {
		console.log(xhr);
		window.alert(xhr.responseText);
		return null;
	}

	var newServerCfg = xhr.responseJSON;
	return newServerCfg;
}

function loadServerCfgOpts() {
	var xhr = RS.ctx.ServerCfgRes.getAllJson.call({async:false});
	var data = xhr.responseJSON;
	for (var i = 0, j = data.length; i < j; i++) {
		data[i].label = serverCfgToString(data[i]);
	}
	return data;
}