function loadIntoDom(dom_sel, xml, xslt_src) {
	var xslt_xhr = $.ajax({url:xslt_src, async:false});
	var xslt = xslt_xhr.responseText;
	
	var dom = $(dom_sel);
	dom.xslt(xml, xslt);
	
	return dom;
}

function loadServerCfgOpts() {
	var xhr = RS.ctx.ServerCfgRes.getAll.call({async:false});
	var data = xhr.responseText;
	loadIntoDom("#serverCfg_select_div", data, "server_cfg_opts.xsl");
}

function loadExchangeCfgOpts(serverCfgId) {
	var ecs = null;
	var xhr = null;
	if (serverCfgId && serverCfgId > 0) {
		xhr = RS.ctx.ExchangeCfgRes.getExchangesByServer.with_server_id(serverCfgId).call({async:false});
	} else {
		xhr = RS.ctx.ExchangeCfgRes.getAll.call({async:false});
	}

	ecs = xhr.responseText;
	loadIntoDom("#exchangeCfg_select_div", ecs, "exchange_cfg_opts.xsl");
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
					"exclusive":false,"id":-1,"priority":0,"queueName":null,"retryLimit":10,"routeKey":"",
					"serverCfg":null};
	}
	return QueueCfg;
}

function getQueueCfg(idx) {
	if (!idx || idx < 0) {
		return newQueueCfg();
	}

	var QueueCfg = null;
	var xhr = RS.ctx.MQueueCfgRes.getJson.with_qc_id(idx).call();
	if (xhr.statusCode().status != 200) {
		console.log(xhr);
		return;
	}
	QueueCfg = xhr.responseJSON;
	return QueueCfg;
}

function switchQueue(qc_id) {
	var qc = getQueueCfg(qc_id);
	if (!qc) {
		return;
	}
	
	var xhr = RS.ctx.MQueueCfgRes.switchQueue.with_qc_id(qc_id).with_on(!qc.enabled).call({async:false});
	qc = xhr.responseJSON;
	event.target.innerText = qc.enabled ? "Stop" : "Start";
}

function sendMsgToQueue(qc_id, msgStr) {
	if (!(qc_id && msgStr)){return;}
	
	var xhr = RS.ctx.MQueueCfgRes.sendMessage.with_message(msgStr).with_qc_id(qc_id).call({async:false});
}

function resendFailedMsg(qc_id, msg_id) {
	var xhr = RS.ctx.FailedJobRes.resendFailedMsg.with_qc_id(qc_id).with_mc_id(msg_id).call({async:false});
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

function serverCfgKey(sc) {
	var sc = row;
	if (!$.isEmptyObject(sc.id) || sc.id > 0) return sc.id;
	return sc.host + ":" + sc.port + "/" + sc.virtualHost;
}

function updateServerCfgs(sc, scs) {
	if (!scs) scs = [];
	
	for (var i = 0; i < scs.length; i++) {
		var _sc = scs[i];
		if (_sc.id == sc.id) {
			scs[i] = sc;
			return;
		}
	}
	scs.push(sc);
}

function getServerCfg(idx) {
	if (!idx || idx < 0) {
		return newServerCfg();
	}

	var serverCfg = null;
	var xhr = RS.ctx.ServerCfgRes.getJson.with_id(idx).call();
	if (xhr.statusCode().status != 200) {
		console.log(xhr);
		return;
	}
	serverCfg = xhr.responseJSON;
	return serverCfg;
}

function save(serverCfg) {
	serverCfg = toEntity(serverCfg);
	var xhr = null;

	var scStr=JSON.stringify(serverCfg);
	var oper = serverCfg.id < 0 ? RS.ctx.ServerCfgRes.create : RS.ctx.ServerCfgRes.update;   
	xhr = oper.with_entity(scStr).call({async : false});

	if (xhr.statusCode().status != 200) {
		console.log(xhr);
		return;
	}

	var newServerCfg = xhr.responseJSON;
	return newServerCfg;
}
