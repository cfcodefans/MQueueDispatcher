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
	} else {
		QueueCfg = {"version":0,"autoDelete":false,
					"destCfg":{"version":0,"hostHead":null,"httpMethod":"post","id":-1,"timeout":30000,"url":null},
					"durable":true,"enabled":true,
					"exchanges":[],
					"exclusive":false,"id":-1,"priority":0,"queueName":null,"retryLimit":0,"routeKey":"default_route_key",
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
	this.value = qc.enabled;
}

function sendMsgToQueue(qc_id, msgStr) {
	if (!(qc_id && msgStr)){return;}
	
	var xhr = RS.ctx.MQueueCfgRes.sendMessage.with_message(msgStr).with_qc_id(qc_id).call({async:false});
}