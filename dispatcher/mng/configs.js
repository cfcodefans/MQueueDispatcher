function buildFilterWithParse($scope, checkFields, $parse) {
	var funcs = [];
	for (var i = 0, j = checkFields.length; i < j; i++) {
		var field = checkFields[i].trim();
		funcs.push($parse(field));
	}
		
	return function(renderableRows) {
		if ($scope.filterValue == "") {
			return renderableRows;
		}
		var matcher = new RegExp($scope.filterValue);
		for (var i = 0, j = renderableRows.length; i < j; i++) {
			var row = renderableRows[i];
			var match = false;
			for (var m = 0, n = funcs.length; m < n; m++) {
				var func = funcs[m];
				var val=func(row.entity);
				if ($.isEmptyObject(val))
					continue;
				if (val.match(matcher)) {
					match = true;
					break;
				}
			}
			row.visible = row.visible && match;
		}
		return renderableRows;
	}
}

function buildFilter($scope, checkFields) {
	return function(renderableRows) {
		if ($scope.filterValue == "") {
			return renderableRows;
		}
		var matcher = new RegExp($scope.filterValue);
		for (var i = 0, j = renderableRows.length; i < j; i++) {
			var row = renderableRows[i];
			var match = false;
			for (var m = 0, n = checkFields.length; m < n; m++) {
				var val = row.entity[checkFields[m]];
				if ($.isEmptyObject(val))
					continue;
				if (val.match(matcher)) {
					match = true;
					break;
				}
			}
			row.visible = row.visible && match;
		}
		return renderableRows;
	}
}


function overviewCtrl($scope, $route, $rootScope, $templateCache) {
	$templateCache.remove($route.current.templateUrl);
	$scope.ov = window.ov;
	/*$rootScope.$on("$routeUpdate", function() {
		console.info($route.current.templateUrl + " $routeUpldate");
		$route.reload();
	});*/
}

function serversCtrl($scope, $route, $rootScope, $templateCache) {
	$templateCache.remove($route.current.templateUrl);
	console.info("serversCtrl!");
	var xhr = RS.ctx.ServerCfgRes.getAllJson.call();

	var gridCfgs = {
		flatEntityAccess:true,
		paginationPageSizes: [20, 50, 75],
		paginationPageSize: 20,
		rowIdentity: function(row) {
			var sc = row;
			if (!$.isEmptyObject(sc.id) || sc.id > 0) return sc.id;
			return sc.host+"/"+virtualHost;
		},
		onRegisterApi : function(gridApi) {
			$scope.gridApi = gridApi;
			$scope.gridApi.grid.registerRowsProcessor($scope.singleFilter, 200);
		},
		columnDefs : [ {field : 'id', maxWidth:70},
		               {field : 'host', maxWidth: 100},
		               {field : 'port', maxWidth: 100}, 
		               {field : 'virtualHost', maxWidth: 100}, 
		               {field : 'userName', maxWidth: 100}, 
		               {field : 'password', maxWidth: 100}, 
		               {field : 'logFilePath'}, 
		               {field : 'maxFileSize', maxWidth: 100},
		               {field : 'id', maxWidth: 100, displayName:"", cellTemplate:"cellCtrl", enableSorting:false, enableColumnMenu: false}]
	};
	gridCfgs.data = xhr.responseJSON;
	gridCfgs.enableFiltering = false;

	$scope.gridCfgs = gridCfgs;

	$scope.filter = function() {
		$scope.gridApi.grid.refresh();
	};

	$scope.singleFilter = buildFilter($scope, [ "virtualHost", "userName", "password", "logFilePath" ]);
}

function exchangesCtrl($scope, $route, $rootScope, $templateCache, $parse) {
	$templateCache.remove($route.current.templateUrl);
	console.info("exchangesCtrl!");
	/*
	autoDelete : false
	durable			:			true
	enabled			:			true
	exchangeName			:			null
	id			:			566
	serverCfg			:			null
	type			:			"direct"
	version			:			*/
	var xhr = RS.ctx.ExchangeCfgRes.getAllJson.call();
	
	var gridCfgs = {
			paginationPageSizes: [20, 50, 75],
			paginationPageSize: 20,					
			onRegisterApi : function(gridApi) {
				$scope.gridApi = gridApi;
				$scope.gridApi.grid.registerRowsProcessor($scope.singleFilter, 200);
			},
			rowIdentity: function(row) {
				var ec = row;
				if (!$.isEmptyObject(ec.id) || ec.id > 0) return ec.id;
				var sc = ec.serverCfg;
				return sc.host+"/"+virtualHost+"/"+ec.exchangeName;
			},
			columnDefs : [ {field : 'id', maxWidth:70},
			               {field : 'exchangeName', minWidth: 100},
			               {field : "serverCfg.host", displayName:"Host", minWidth: 100}, 
			               {field : 'type', maxWidth: 100}, 
			               {field : 'durable', maxWidth: 100}, 
			               {field : 'autoDelete', maxWidth: 100}, 
			               {field : 'enable', maxWidth: 100}, 
			               {field : 'id', maxWidth: 100, displayName:"", cellTemplate:"cellCtrl", enableSorting:false, enableColumnMenu: false}]
		};
		gridCfgs.data = xhr.responseJSON;
		gridCfgs.enableFiltering = false;

		$scope.gridCfgs = gridCfgs;

		$scope.filter = function() {
			$scope.gridApi.grid.refresh();
		};

		$scope.singleFilter = buildFilterWithParse($scope, [ "exchangeName", "type", "serverCfg.host" ], $parse);
}

function queuesCtrl($scope, $route, $routeParams, $rootScope, $templateCache, $parse) {
	$templateCache.remove($route.current.templateUrl);
	var xhr = RS.ctx.MQueueCfgRes.getAllJson.call();
	
	var gridCfgs = {
			paginationPageSizes: [20, 50, 75],
			paginationPageSize: 20,					
			onRegisterApi : function(gridApi) {
				$scope.gridApi = gridApi;
				$scope.gridApi.grid.registerRowsProcessor($scope.singleFilter, 200);
			},
			rowIdentity: function(row) {
				var qc = row;
				if (!$.isEmptyObject(qc.id) || qc.id > 0) return qc.id;
				var sc = qc.serverCfg;
				return sc.host+"/"+virtualHost+"/"+qc.queueName;
			},
			columnDefs : [ {field : 'id', maxWidth:40},
			               {field : "serverCfg.host", displayName:"Host", maxWidth: 100}, 
			               {field : "serverCfg.virtualHost", displayName:"vHost", maxWidth: 80},
			               {field : 'queueName', maxWidth: 100}, 
			               {field : 'destCfg.url', displayName:"Url", minWidth: 100}, 
			               {field : 'destCfg.httpMethod', displayName:"Method", maxWidth: 70},
			               {field : 'destCfg.timeout', displayName:"Timeout", maxWidth: 70},
			               {field : 'retryLimit', displayName:"Retry", maxWidth: 40},
			               {field : 'enable', maxWidth: 50}, 
			               {field : 'id', maxWidth: 120, displayName:"", cellTemplate:"queueCtrl", enableSorting:false, enableColumnMenu: false}]
		};
		gridCfgs.data = xhr.responseJSON;
		gridCfgs.enableFiltering = false;

		$scope.gridCfgs = gridCfgs;

		$scope.filter = function() {
			$scope.gridApi.grid.refresh();
		};

		$scope.singleFilter = buildFilterWithParse($scope, [ "serverCfg.host",
		                                                     "serverCfg.virtualHost",
		                                                     "queueName",
		                                                     "destCfg.url"], $parse);
}