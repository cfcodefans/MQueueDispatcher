<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:template match="/">
<html>
<head>
</head>
<body>
	<table id="server_cfg_tbl" class="table table-striped">
		<thead>
			<tr>
				<th>id</th>
				<th>host</th>
				<th>port</th>
				<th>user name</th>
				<th>password</th>
				<th>virtual host</th>
				<th>log path</th>
				<th>log size</th>
				<th></th>
			</tr>
		</thead>
		<xsl:for-each select="serverCfgs/serverCfg">
		<tr data_id="{id}">
			<td><xsl:value-of select="id" /></td>
			<td><xsl:value-of select="host" /></td>
			<td><xsl:value-of select="port" /></td>
			<td><xsl:value-of select="userName" /></td>
			<td><xsl:value-of select="password" /></td>
			<td><xsl:value-of select="virtualHost" /></td>
			<td><xsl:value-of select="logFilePath" /></td>
			<td><xsl:value-of select="maxFileSize" /></td>
			<td class="last_col">
				<div class="btn-group">
					<button type="button" id="editBtn" class="btn btn-default btn-sm" onclick="editServerCfg({id})">edit</button>
					<button type="button" id="copyBtn" class="btn btn-default btn-sm" onclick="copyServerCfg({id})">copy</button>
					<button type="button" id="deleteBtn" class="btn btn-default btn-sm">delete</button>
				</div>
			</td>
		</tr>
	</xsl:for-each>
	<tfoot>
		<tr>

		</tr>
	</tfoot>
</table>
</body>
</html>
</xsl:template>
</xsl:stylesheet>