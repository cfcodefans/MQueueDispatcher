<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:template match="/">
		<html>
			<head>
			</head>
			<body>
				<table id="exchange_cfg_tbl" class="table table-striped">
					<thead>
						<tr>
							<th>id</th>
							<th>server user name</th>
							<th>exchange name</th>
							<th>type</th>
							<th>durable</th>
							<th>autoDelete</th>
							<th></th>
						</tr>
					</thead>
					<xsl:for-each select="exchangeCfgs/exchangeCfg">
						<tr data_id="{id}">
							<td>
								<xsl:value-of select="id" />
							</td>
							<td>
								<xsl:value-of select="serverCfg/userName" />
							</td>
							<td>
								<xsl:value-of select="exchangeName" />
							</td>
							<td>
								<xsl:value-of select="type" />
							</td>
							<td>
								<xsl:value-of select="durable" />
							</td>
							<td>
								<xsl:value-of select="autoDelete" />
							</td>
							<td class="last_col">
								<div class="btn-group">
									<button type="button" id="editBtn" class="btn btn-default btn-sm" onclick="editExchangeCfg({id})">edit</button>
									<button type="button" id="copyBtn" class="btn btn-default btn-sm" onclick="copyExchangeCfg({id})">copy</button>
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