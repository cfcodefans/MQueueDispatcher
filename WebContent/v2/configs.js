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

function to(ctrlName, viewUrl) {
	return {
		controller : ctrlName,
		templateUrl : viewUrl
	};
}

function redirect(key) {
	return {redirectTo : key};
}
var app = angular.module("Configs", [ "ngRoute", 
                                      "ui.grid", 
                                      "ui.grid.autoResize", 
                                      "ui.grid.pagination",
                                      "ui.bootstrap"]);

app.config([ "$routeProvider", function($routeProvider) {
	$routeProvider
		.when("/overview", 	to("overviewCtrl", 	"../mvc/overview.html"))
		.when("/servers", 	to("serversCtrl", 	"servers.html"))
		.when("/exchanges", to("exchangesCtrl", "exchanges.html"))
		.when("/queues", 	to("queuesCtrl", 	"queues.html"))
		.otherwise(redirect("/overview"));
} ]);

app.controller("overviewCtrl", [ "$scope", 
                                 "$route", 
                                 "$rootScope", 
                                 "$templateCache", 
                                 overviewCtrl ]);

app.controller("serversCtrl", [ "$scope", 
                                "$route", 
                                "$rootScope", 
                                "$templateCache", 
                                "$uibModal", 
                                serversCtrl ]);

app.controller("exchangesCtrl", [ "$scope", 
                                  "$route", 
                                  "$rootScope", 
                                  "$templateCache", 
                                  "$parse", 
                                  "$uibModal", 
                                  exchangesCtrl ]);

app.controller("queuesCtrl", [ "$scope", 
                               "$route", 
                               "$rootScope", 
                               "$templateCache", 
                               "$parse", 
                               "$uibModal", 
                               queuesCtrl ]);