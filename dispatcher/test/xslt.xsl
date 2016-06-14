<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:template match="/">
<html>
<head>
</head>
<body>
		<table id="server_cfg_tbl">
			<thead>
				<tr>
					<th>host</th>
					<th>port</th>
					<th>user name</th>
					<th>password</th>
					<th>virtual host</th>
					<th>log file path</th>
					<th>max file size</th>
				</tr>
			</thead>
			<xsl:for-each select="serverCfgs/serverCfg">
				<tr>
					<td>
						<xsl:value-of select="host" />
					</td>
					<td></td>
					<td></td>
					<td></td>
					<td></td>
					<td></td>
					<td></td>
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