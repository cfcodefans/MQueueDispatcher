<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<html>
			<head>
			</head>
			<body>
				<select class="form-control" id="serverCfg">
					<xsl:for-each select="serverCfgs/serverCfg">
						<option value="{id}">
							<xsl:value-of select="host" /> &#160;
							<xsl:value-of select="port" /> &#160;
							<xsl:value-of select="userName" /> &#160;
							<xsl:value-of select="virtualHost" /> &#160;
						</option>
					</xsl:for-each>
				</select>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>