<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:template match="/">
<html>
<head>
</head>
<body>
		<table id="mqueue_cfg_tbl" class="table table-striped">
			<thead>
				<tr>
					<th>id</th>
					<th>server user name</th>
					<th>queue name</th>
					<th>url</th>
					<th>http method</th>
					<th>timeout</th>
					<th>host header</th>
					<th>retry limit</th>
					<th>processed</th>
					<th>failed</th>
					
					<th></th>
				</tr>
			</thead>
			<xsl:for-each select="queueCfgs/queueCfg">
				<tr data_id="{id}">
					<td><xsl:value-of select="id" /></td>
					<td><xsl:value-of select="serverCfg/userName" /></td>
					<td style="word-break:break-all"> 
						<a href="queue_ctrl.html?qc_id={id}">
						<xsl:value-of select="queueName" />	
						</a>
					</td>
					<td style="word-break:break-all"><xsl:value-of select="destCfg/url" /></td>
					<td><xsl:value-of select="destCfg/httpMethod" /></td>
					<td><xsl:value-of select="destCfg/timeout" /></td>
					<td style="word-break:break-all"><xsl:value-of select="destCfg/hostHead" /></td>
					<td><xsl:value-of select="retryLimit" /></td>
					<td><span class="badge"><xsl:value-of select="status/processed" /></span></td>
					<td><span class="badge"><xsl:value-of select="status/failed" /></span></td>
					<td>
						<div class="btn-group">
						  <button type="button" id="editBtn" class="btn btn-default btn-sm" onclick="editQueueCfg({id})">edit</button>
						  <button type="button" id="copyBtn" class="btn btn-default btn-sm" onclick="copyQueueCfg({id})">copy</button>
						  <button type="button" id="deleteBtn" class="btn btn-default btn-sm">
						  <xsl:if test="enabled = 'true'">
						  stop
						  </xsl:if>
						  <xsl:if test="enabled = 'false'">
						  start
						  </xsl:if>
						  </button>
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