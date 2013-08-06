AJS.$(document).ready(function() {
	var pid = AJS.$("#create-issue ul.operations li:first a.create-issue-type").attr("data-pid");
	if (!pid) {
		var href = AJS.$("#create-issue ul.operations li:first a.lnk").attr('href');
		if (href) {
			var st = href.indexOf("?pid="), e = href.indexOf("&", st + 1);
			if (st != -1 && e > st) {
				pid = href.substring(st + 5, e);
			}
		}
	}

	if (pid != undefined) {
		AJS.$("#create-issue ul.operations").append("<li><a href='"
				+ contextPath +"/secure/BulkCreateIssues!default.jspa?pid=" + pid + "' class='bulk-create-issues lnk' title='"
				+ AJS.I18n.getText("jbcp.bulk.create.tooltip") + "'>" + AJS.I18n.getText("jbcp.bulk.create") + "</a></li>");
	}

	/*
	AJS.$(".bulk-create-issues").each(function() {
		var dialog = new JIRA.FormDialog({
			trigger: this,
			id: this.id + "-dialog",
			ajaxOptions: {
				url: this.href,
				data: {
					decorator: "dialog",
					inline: "true"
				}
			}
		});
	});*/

});