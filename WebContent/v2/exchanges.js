function exchangesCtrl($scope, $route, $rootScope, $templateCache, $parse, $uibModal) {
	$templateCache.remove($route.current.templateUrl);
	console.info("exchangesCtrl!");
	/*
	 * autoDelete : false durable : true enabled : true exchangeName : null id :
	 * 566 serverCfg : null type : "direct" version :
	 */
	var xhr = RS.ctx.ExchangeCfgRes.getAllJson.call();
	
	var gridCfgs = {
		paginationPageSizes: [20, 50, 75],
		paginationPageSize: 20,					
		onRegisterApi : function(gridApi) {
			$scope.gridApi = gridApi;
			$scope.gridApi.grid.registerRowsProcessor($scope.singleFilter, 200);
		},
		rowIdentity: exchangeCfgKey,
		columnDefs : [ {field : "id", maxWidth:40},
		               {field : "exchangeName", minWidth: 100},
		               {field : "serverCfg.host", displayName:"Host", minWidth: 100}, 
		               {field : "type", maxWidth: 100}, 
		               {field : "durable", maxWidth: 100}, 
		               {field : "autoDelete", maxWidth: 100}, 
		               {field : "enable", maxWidth: 100}, 
		               {field : "id", maxWidth: 120, displayName:"", cellTemplate:"cellCtrl", enableSorting:false, enableColumnMenu: false}]
	};
	gridCfgs.data = xhr.responseJSON;
	gridCfgs.enableFiltering = false;

	$scope.gridCfgs = gridCfgs;

	$scope.filter = function() {
		$scope.gridApi.grid.refresh();
	};

	$scope.singleFilter = buildFilterWithParse($scope, [ "exchangeName", "type", "serverCfg.host" ], $parse);
	
	function exchangeModalCtrl($scope, $uibModalInstance, ec) {
		$scope.ec = ec;
		$scope.serverCfgs = loadServerCfgOpts();
		$scope.save = function() {
			var saved = saveExchangeCfg(this.ec);
			if (saved) {
				this.ec = saved;
				$uibModalInstance.close(this.ec);
				return;
			}
		};
		$scope.cancel = function() {
			$uibModalInstance.dismiss("cancel");
		};
	}

	$scope.update = function(ec) {
		this.gridApi.grid.refresh(updateExchangeCfgs(ec, gridCfgs.data));
	}
	
	$scope.open = function(ec) {
		var modal = $uibModal.open({
			templateUrl: "Exchange_modal",
			controller: ["$scope", "$uibModalInstance", "ec", exchangeModalCtrl],
//			size: "sm",
			resolve: {
				"ec": function() {
					var _ec=angular.copy(ec);
					delete _ec.label;
					return _ec;
				}
			}
		}).result.then(this.update.bind(this));
	}
	
	$scope.create = function(_ctx) {
		this.open(newExchangeCfg());
	}
	$scope.edit = function(ec) {
		this.open(ec);
	}
	$scope.copy = function(ec) {
		this.open(newExchangeCfg(ec));
	}
}

function exchangeToString(ec) {
	var sc = ec.serverCfg;
	return ec.id +": " + sc.host + ":" + sc.port + sc.virtualHost + "/" + ec.exchangeName;
}


function exchangeCfgKey(ec) {
	if (!$.isEmptyObject(ec.id) || ec.id > 0) return ec.id;
	var sc = ec.serverCfg;
	return serverCfgToString(sc) + "/" + ec.exchangeName;
}

function updateExchangeCfgs(ec, ecs) {
	return updateArray(ec, ecs, exchangeCfgKey);
}

function newExchangeCfg(original) {
	var ExchangeCfg = null;
	if (original) {
		ExchangeCfg = $.extend({}, original);
		ExchangeCfg.id = -1;
	} else {
		ExchangeCfg = {"version":0,"id":-1,"exchangeName":null,"durable":true,"autoDelete":false,"type":"direct","serverCfg":null};
	}
	return ExchangeCfg;
}

function getExchangeCfg(idx) {
	if (!idx || idx < 0) {
		return newExchangeCfg();
	}

	var ExchangeCfg = null;
	var xhr = RS.ctx.ExchangeCfgRes.getJson.with_id(idx).call();
	if (xhr.statusCode().status != 200) {
		console.log(xhr);
		return;
	}
	ExchangeCfg = xhr.responseJSON;
	return ExchangeCfg;
}

function saveExchangeCfg(ExchangeCfg) {
	var xhr = null; 
	var oper = (ExchangeCfg.id < 0) ? RS.ctx.ExchangeCfgRes.create : RS.ctx.ExchangeCfgRes.update; 
	xhr = oper.with_entity(JSON.stringify(ExchangeCfg)).call({async:false});

	if (xhr.statusCode().status != 200) {
		console.log(xhr);
		alert(xhr.responseText);
		return;
	}
	
	var newExchangeCfg = xhr.responseJSON;
	return newExchangeCfg;
}

function loadExchangeCfgOpts(scId) {
	var xhr = RS.ctx.ExchangeCfgRes.getExchangesJsonByServer.with_server_id(scId).call({async:false});
	var data = xhr.responseJSON;
	for (var i = 0, j = data.length; i < j; i++) {
		data[i].label = exchangeToString(data[i]);
	}
	return data;
}