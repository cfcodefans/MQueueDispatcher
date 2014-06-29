
function MethodBuilder(_method, _metadata) {
	var invocable = {
		metadata: _metadata,
		method: _method,
		paramAndValues: []
	};

	for (var i = 0, j = invocable.method.params.length; i < j; i++)	{
		var param = invocable.method.params[i];
		invocable["with_" + param.sourceName] = function(value) {
			invocable.paramAndValues[param.sourceName] = value;
			return this;
		};
	}

	invocable.call = function(settings) {
		if (!settings) {
			settings = {async:false};
		}

		settings["type"] = invocable.method.httpMethod;
		var url = this.metadata.url;
		for (var i = 0, j = this.method.params.length; i < j; i++)	{
			var p = this.method.params[i];
			if (p.source == "PATH") {
				url = url.replace("{" + p.sourceName + "}", this.paramAndValues[p.sourceName]);
			} else if (p.source == "QUERY") {
				url = url + (url.indexOf("?") > 0 ? "&" : "?");
				url = url + p.sourceName + "=" + this.paramAndValues[p.sourceName];
			} else if (p.source == "MATRIX") {
				url = url + (url.indexOf("?") > 0 ? ";" : "?");
				url = url + p.sourceName + "=" + this.paramAndValues[p.sourceName];
			} else {
				console.log("unsupported parameter source: " + JSON.stringify(p));
			}
		}

		var headerParams = [];
		
		for (var i = 0, j = this.method.params.length; i < j; i++)	{
			var p = this.method.params[i];
			if (p.source == "HEADER") {
				headerParams[p.sourceName] = this.paramAndValues[p.sourceName];
			}
		}
		settings.beforeSend = function(xhr) {			
			for (var hp in headerParams) {
				xhr.setRequestHeader(hp, headerParams[hp]);	
			}			
		};

		var data = [];
		for (var i = 0, j = this.method.params.length; i < j; i++)	{
			var p = this.method.params[i];
			if (p.source == "FORM" || p.source == "ENTITY") {
				data.push({name:p.sourceName, value: this.paramAndValues[p.sourceName]});
			}
		}

		settings.data = data;
		
		return $.ajax(url, settings);
	};

	return invocable;
}

function ProxyBuilder(){
	this._proxy = {};
};

ProxyBuilder.prototype = {
	// _proxy:{},

	build: function() {return this._proxy;},

	withName: function(name) {
		this._proxy.name = name;
		return this;
	},

	withMetadata: function(metadata) {
		for (var i = 0, j = metadata.children.length; i < j; i++)	{
			var subMetadata = metadata.children[i];
			this.withMetadata(subMetadata);
		}

		for (var i = 0, j = metadata.methods.length; i < j; i++)	{
			var method = metadata.methods[i];
			this._proxy[method.name] = MethodBuilder(method, metadata);
		}
		return this;
	}
};

RS = {
	ctx:[],
	init: function(ajaxMetaDatas) {
		for (var i = 0, j = ajaxMetaDatas.length; i < j; i++)	{
			var metadata = ajaxMetaDatas[i];
			var _proxy = new ProxyBuilder().withName(metadata.name).withMetadata(metadata).build();
			this.ctx[metadata.name] = _proxy;
		}
	}
};

var xhr = $.ajax("/dispatcher-0.0.1/rest/v1/ajax", {
	async : false,
	dataType : "json",
	complete : function(xhr) {
		console.log("ajax loaded: " + xhr.responseJSON);
	}
});

RS.init(xhr.responseJSON);

