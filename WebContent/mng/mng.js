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