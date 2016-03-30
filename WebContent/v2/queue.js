function queueCtrl($scope, $route, $routeParams, $rootScope, $templateCache, $parse) {
	console.info("loading queue: " + $routeParams.qc_id);
	var qc = getQueueCfg($routeParams.qc_id);
	
	loadQueueCfgIntoScope($scope, qc)
	
	$scope.filter = function() {
		$scope.gridApi.grid.refresh();
	};

	var gridCfgs = {
			paginationPageSizes: [20, 50, 75],
			paginationPageSize: 20,					
			onRegisterApi : function(gridApi) {
				$scope.gridApi = gridApi;
				$scope.gridApi.grid.registerRowsProcessor($scope.singleFilter, 200);
			},
			rowHeight: 80,
			rowEquality: rowKeyEq,
			rowIdentity: queueCfgKey,
			columnDefs : [ {field : "id", maxWidth:80},
			               {field : "messageContent",cellTemplate: "msg_content", displayName:"Content", minWidth: 150}, 
			               {field : "response", cellTemplate: "resp_content", displayName:"Resp", minWidth: 100}, 
			               {field : "timestampStr", displayName:"Time", maxWidth: 90}] 
		};
	
	var xhr = RS.ctx.FailedJobRes.getFailedJobs.with_qc_id(qc.id).call();
	gridCfgs.data = xhr.responseJSON;
	gridCfgs.enableFiltering = false;

	$scope.gridCfgs = gridCfgs;

	$scope.filter = function() {
		$scope.gridApi.grid.refresh();
	};
	
	$scope.singleFilter = buildFilterWithParse($scope, [ "messageContent",
	                                                     "response.responseStr"], $parse);
	
}