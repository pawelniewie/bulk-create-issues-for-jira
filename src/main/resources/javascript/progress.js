AJS.$(document).ready(function() {
	AJS.$("#import-progress").progressBar(0);

	var intervalId = setInterval(function (){
		var eid = AJS.$("#eid").val();

		AJS.$.ajax({
			url: contextPath + "/rest/bulk-create-issues-plugin/1.0/progress/" + eid,
			cache: false,
			data: { "atl_token": atl_token },
			success: function(data, textStatus, jqXHR) {
				if (data.running) {
					var progress = Math.round(data.currentRow * 100 / data.rowsToImport);
					AJS.$("#import-progress").progressBar(isNaN(progress) ? 0 : progress);
				} else {
					AJS.$("#import-progress").progressBar(100);
					AJS.$("#finished").val("true");
					clearInterval(intervalId);
				}
				var messages = AJS.$("#messages").empty();
				if (data.messages && data.messages.length > 0) {
					AJS.$(data.messages).each(function(idx, elem) {
						messages.append(AJS.$("<div/>").text(elem));
					});
				}
				var issues = AJS.$("#issues").empty();
				if (data.issueKeys && data.issueKeys.length > 0) {
					AJS.$(data.issueKeys).each(function(idx, elem) {
						issues.append(AJS.$("<span/>").append(AJS.$("<a/>").attr("href", contextPath + "/browse/" + elem).attr("target", "_blank").text(elem)));
					});
				}
			},
			error: function(jqXHR, textStatus, errorThrown) {
				clearInterval(intervalId);
			}
		});
	}, 1000);
});