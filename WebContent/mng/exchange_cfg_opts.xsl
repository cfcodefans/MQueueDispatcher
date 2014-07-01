<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<html>
			<head>
			</head>
			<body>
				<select class="form-control" id="exchangeCfgs" multiple="multiple" size="7">
					<xsl:for-each select="exchangeCfgs/exchangeCfg">
						<option value="{id}">
							<xsl:value-of select="exchangeName" /> &#160;
							<xsl:value-of select="serverCfg/userName" /> &#160;
							<xsl:value-of select="type" /> &#160;
						</option>
					</xsl:for-each>
				</select>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>