load("nashorn:mozilla_compat.js");

importClass(Packages.java.util.Map);

importClass(Packages.org.jsoup.nodes.Element);
importClass(Packages.org.jsoup.parser.Tag);
importClass(Packages.org.jsoup.parser.Parser);
importClass(Packages.com.thenetcircle.services.commons.Jsons);


function isJavaObject(obj_on_server_side) {
	return obj_on_server_side != null && obj_on_server_side["class"] != undefined;
}

function javaObjToJSString(javaObj) {
	return isJavaObject(javaObj) ? JSON.stringify(JSON.parse(Jsons.toString(javaObj))) : javaObj;
}

function javaObjToJSObj(javaObj) {
	return isJavaObject(javaObj) ? JSON.parse(Jsons.toString(javaObj)) : javaObj;
}

function getValueLiteral(obj_on_server_side) {
	if (isJavaObject(obj_on_server_side)) {
		return JSON.stringify(JSON.parse(Jsons.toString(obj_on_server_side)));	
	} else if (obj_on_server_side instanceof Function) {
		return obj_on_server_side.toString();	
	} else if (obj_on_server_side instanceof Object) {
		return JSON.stringify(obj_on_server_side);
	}

	return obj_on_server_side;
}

function createScriptElement() {
	var script = new Element(Tag.valueOf("script"), "");
	script.attr("type", "text/javascript");
	script.appendText("\n");
	return script;
}

function injectVarToScript(script, var_name_on_client, obj_on_server_side) {
	script.appendText("\n" + var_name_on_client + " = " + getValueLiteral(obj_on_server_side) + ";\n");
	return script;
}

function fillFormByReq(formElement, params) {
	if (formElement == null || params == null) {
		return;
	}
	
	//final Map<String, String[]> params = req.getParameterMap();
	//var params = req.getParameterMap();
	var _params = javaObjToJSObj(params);
	for (var fieldName in _params) {
		var fields = formElement.select("*[name=" + fieldName + "]");

		if (fields == null) {
			continue;
		}
		
		var value = _params[fieldName];
		if (value == null || value.length == 0) {
			continue;
		}
		for (var i = 0, j = fields.size(); i < j; i++) {
			var field = fields.get(i);
			if ("select" == field.tagName()) {
				var opts = field.select("option[value=" + value + "]");
				print("opts" + opts);
				if (opts.size() > 0) {
					opts.get(0).attr("selected", true);
				}
				continue;
			}

			fields.get(i).val(value);
		}
	}
}