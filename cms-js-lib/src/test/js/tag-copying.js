(function () {
	'use strict';
	Aloha.ready(function () {
		Aloha.require(['jquery'], function ($) {
			GCN.login('node', 'node', function (success, data) {
				$('#content').html(success
					? '<p>Hello, ' + data.user.firstName + '!</p>'
					: '<p>Login failed</p>');

				GCN.page(116).tag('content').edit('#content', function () {
					GCN.page(116).tag('teasers').edit('#teasers', function () {
						var $teasers = $('#teasers');
						var range = Aloha.createRange();
						range.setStart($teasers[0], 0);
						range.setEnd($teasers[0], 0);
						Aloha.getSelection().removeAllRanges();
						Aloha.getSelection().addRange(range);
						setTimeout(function () {
							var content = $('#content').html();
							Aloha.execCommand('insertHTML', false, content, range);
						}, 3000);
					});
				});
			});
		});
	});
}());
