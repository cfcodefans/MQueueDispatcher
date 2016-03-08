function queuesCtrl($scope, $route, $rootScope, $templateCache, $parse, $uibModal) {
	$templateCache.remove($route.current.templateUrl);
	var xhr = RS.ctx.MQueueCfgRes.getAllJson.call();
	
	var gridCfgs = {
		paginationPageSizes: [20, 50, 75],
		paginationPageSize: 20,					
		onRegisterApi : function(gridApi) {
			$scope.gridApi = gridApi;
			$scope.gridApi.grid.registerRowsProcessor($scope.singleFilter, 200);
		},
		rowIdentity: queueCfgKey,
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
	
	function queueModalCtrl($scope, $uibModalInstance, qc) {
		$scope.qc = qc;
		$scope.serverCfgs = loadServerCfgOpts();
		
		$scope.loadExchangeOpts = function() {
			var scId = 0;
			if (this.qc != null && this.qc.serverCfg != null) {
				scId = this.qc.serverCfg.id;
			}
			this.exchangeCfgs = loadExchangeCfgOpts(scId);
		}
		
		$scope.loadExchangeOpts();
		
		$scope.onServerCfgSelected = function(sc) {
			if (sc)
				this.exchangeCfgs = loadExchangeCfgOpts(sc.id);
			else
				this.exchangeCfgs = [];
		}
		
		$scope.save = function() {
			var saved = saveQueueCfg(this.qc);
			if (saved) {
				this.qc = saved;
				$uibModalInstance.close(this.qc);
				return;
			}
		};
		$scope.cancel = function() {
			$uibModalInstance.dismiss('cancel');
		};
	}
	
	$scope.update = function(qc) {
		this.gridApi.grid.refresh(updateExchangeCfgs(ec, gridCfgs.data));
	}
	
	$scope.open = function(qc) {
		var modal = $uibModal.open({
			templateUrl: "queue_modal",
			controller: ["$scope", "$uibModalInstance", "qc", queueModalCtrl],
//			size: "sm",
			resolve: {
				"qc": function() {
					return angular.copy(qc);
				}
			}
		}).result.then(this.update.bind(this));
	}
	
	$scope.create = function(_ctx) {
		this.open(newQueueCfg());
	}
	$scope.edit = function(qc) {
		this.open(qc);
	}
	$scope.copy = function(qc) {
		this.open(newQueueCfg(qc));
	}
}

function queueToString(qc) {
	return queueCfgKey(qc);
}

function queueCfgKey(qc) {
	if (!$.isEmptyObject(qc.id) || qc.id > 0) return qc.id;
	var sc = qc.serverCfg;
	return serverCfgToString(sc) + "/" + qc.queueName;
}

function updateQueueCfgs(qc, qcs) {
	return updateArray(qc, qcs, queueCfgKey);
}

function newQueueCfg(original) {
	var QueueCfg = null;
	if (original) {
		QueueCfg = $.extend({}, original);
		QueueCfg.id = -1;
		QueueCfg.exchanges = [];
	} else {
		QueueCfg = {"version":0,"autoDelete":false,
					"destCfg":{"version":0,"hostHead":null,"httpMethod":"post","id":-1,"timeout":30000,"url":null},
					"durable":true,"enabled":true,
					"exchanges":[],
					"exclusive":false,
					"id":-1,
					"priority":0,
					"queueName":null,
					"retryLimit":2,
					"routeKey":"",
					"serverCfg":null};
	}
	return QueueCfg;
}

function getQueueCfg(idx) {
	if (!idx || idx < 0) {
		return newQueueCfg();
	}

	var QueueCfg = null;
	var xhr = RS.ctx.QueueCfgRes.getJson.with_id(idx).call();
	if (xhr.statusCode().status != 200) {
		console.error(xhr);
		return;
	}
	QueueCfg = xhr.responseJSON;
	return QueueCfg;
}

function saveQueueCfg(QueueCfg) {
	var xhr = null; 
	var oper = (QueueCfg.id < 0) ? RS.ctx.QueueCfgRes.create : RS.ctx.QueueCfgRes.update; 
	xhr = oper.with_entity(JSON.stringify(QueueCfg)).call({async:false});

	if (xhr.statusCode().status != 200) {
		console.log(xhr);
		alert(xhr.responseText);
		return;
	}
	
	var newQueueCfg = xhr.responseJSON;
	return newQueueCfg;
}