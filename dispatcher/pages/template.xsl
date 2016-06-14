<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">

<html lang="en">
<head>
<meta charset="utf-8"/>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<meta name="description" content=""/>
<meta name="author" content=""/>
<link rel="icon" href="../../favicon.ico"/>

<title>MQueue Dispatcher 1.0</title>

<!-- Bootstrap core CSS -->
<link href="/dispatcher/res/bootstrap/css/bootstrap.min.css" rel="stylesheet" />
<link href="/dispatcher/res/font-awesome-4.2.0/css/font-awesome.css" rel="stylesheet" />
<link href="/dispatcher/res/bootstrap/plugins/metisMenu/metisMenu.css" rel="stylesheet" />
<link href="/dispatcher/res/sb-admin-2.css" rel="stylesheet" />
<link href="/dispatcher/res/style.css" rel="stylesheet" />
</head>

<body>
	
	<div  class="wrap" id="warpper">
		<nav class="navbar navbar-default navbar-static-top" role="navigation" style="margin-bottom: 0px">
			<div class="navbar-header">
				<button class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
					<span class="icon-bar"></span>
					<span class="icon-bar"></span>
					<span class="icon-bar"></span>
				</button>
<!-- 				<img class="nav navbar-nav navbar-left" src="/dispatcher/res/images/payment.png" height="50px"/> -->
				<a class="navbar-brand" href="/dispatcher/mvc/mgr/dashboard.xml">MQueue Dispatcher 1.0</a>
			</div>
			<ui class="nav navbar-nav navbar-right">
				<li role="presentation" class="active">
				<a href="#" class="">Logout</a>
				</li>	
			</ui>
			<div class="nav navbar-right" style=" display:table; height: 50px; padding-right: 15px">
				<div style="vertical-align:middle; display:table-cell;">
<!-- 					<label class="checkbox-inline"> -->
<!-- 						<input type="checkbox" value="poppen" /> -->
<!-- 						poppen -->
<!-- 					</label> -->
				</div>
			</div>
			 
			<div class="navbar-default sidebar" role="navigation">
				<div class="sidebar-nav navbar-collapse">
					<ul class="nav" id="side-menu">
						<li id="dashboard_entry"><a href="/dispatcher/mvc/dashboard.xml">Dashboard</a></li>
						<!--  
						<li>
							<a href="#">
								<i class="fa fa-bar-chart fa-fw"></i>
								Statistic
								<span class="fa arrow"></span>
							</a>
                            <ul class="nav nav-second-level">
                                <li id="stat_membership_entry">
                                    <a href="/dispatcher/mvc/mgr/stat/current_memberships.xml">Current Memberships</a>
                                </li>
                                <li id="stat_item_entry">
                                    <a href="/dispatcher/mvc/mgr/stat/count_articles.xml">Counts for Articles</a>
                                </li>
                                <li id="month_track_entry">
                                    <a href="/dispatcher/mvc/mgr/stat/month_track.xml">Month Tracks</a>
                                </li>
                                <li>
                                    <a href="/dispatcher/mvc/mgr/stat/daily_report.xml">Daliy Reports</a>
                                </li>
                                <li>
                                    <a href="/dispatcher/mvc/mgr/stat/turnover.xml">Turnover</a>
                                </li>
                                <li>
                                    <a href="/dispatcher/mvc/mgr/stat/page_rank.xml">Page Ranks</a>
                                </li>
                            </ul>
						</li>
						-->
					</ul>
				</div>
				<script type="text/javascript">
					function highlightEntry(entry) {
						$("#side-menu").find(".active").each(function(idx){$(this).removeClass("active")});
						
						var activeEntry = $("#side-menu").find(entry);
						
						activeEntry.addClass("active");
						activeEntry.find("a").addClass("active");
						activeEntry.parentsUntil("#side-menu", "li").addClass("active");
					}
				</script>
			</div>
		</nav>
		
		<script type="text/javascript" src="/dispatcher/res/jquery-2.1.1.js"></script>
		<script type="text/javascript" src="/dispatcher/res/jquery.xslt.js"></script>
		<script type="text/javascript" src="/dispatcher/res/bootstrap/js/bootstrap.min.js"></script>
		<script type="text/javascript" src="/dispatcher/res/bootstrap/plugins/metisMenu/metisMenu.js"></script>
		<div id="page-wrapper" class="container-fluid">
		 	<xsl:copy-of select="content"/>
		</div>
		<div id="push"></div>
	</div>

	<div id="footer">
		<div class="container">
			<p class="muted credit">
				Created by <a target="_blank" href="http://intranet.ideawise.de:8001/projects/serviceteam/wiki">service-team@thenetcircle.com</a>.
			</p>
		</div>
	</div>
	
	<script type="text/javascript">
		//$(function() {
 			$('#side-menu').metisMenu();
		//});
	</script>
</body>
</html>

</xsl:template>
</xsl:stylesheet>