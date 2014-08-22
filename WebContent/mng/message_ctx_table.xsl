<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:template match="/">
		<html>
			<head>
			</head>
			<body>
				<table id="message_ctx_tbl" class="table table-striped" style="table-layout:fixed">
					<thead>
						<tr>
							<th width="120px">id</th>
							<th width="150px">time</th>
							<th>message</th>
							<th>response</th>
							<th width="120px">fail times</th>
							<th width="120px"></th>

						</tr>
					</thead>

					<xsl:for-each select="messageContexts/messageContext">
						<tr data_id="{id}">
							<td>
								<xsl:value-of select="id" />
							</td>
							<td>
								<xsl:value-of select="timestampStr" />
							</td>
							<td style="word-wrap:break-word;" width="600px">
								<xsl:value-of select="messageContent" />
							</td>
							<td style="word-wrap:break-word;" width="200px">
								<p>
									<xsl:value-of select="response" />
								</p>
							</td>
							<td>
								<xsl:value-of select="failTimes" />
							</td>
							<td class="last_col">
								<div class="btn-group">
									<button type="button" id="editBtn" class="btn btn-default btn-sm" onclick="resendFailedMsg({id})">retry</button>
									<button type="button" id="copyBtn" class="btn btn-default btn-sm" onclick="delMsg({id})">delete</button>
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