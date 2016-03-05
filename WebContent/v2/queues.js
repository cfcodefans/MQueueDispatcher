function queuesCtrl($scope, $route, $rootScope, $templateCache, $parse) {
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
			               {field : 'queueName', maxWidth: 150}, 
			               {field : 'destCfg.url', displayName:"Url", minWidth: 100}, 
			               {field : 'destCfg.httpMethod', displayName:"Method", maxWidth: 70},
			               {field : 'destCfg.timeout', displayName:"Timeout", maxWidth: 70},
			               {field : 'retryLimit', displayName:"Retry", maxWidth: 40},
			               {field : 'enable', maxWidth: 50}, 
			               {field : 'id', maxWidth: 150, displayName:"", cellTemplate:"queueCtrl", enableSorting:false, enableColumnMenu: false}]
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