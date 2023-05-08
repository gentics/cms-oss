(function () {
	'use strict';
	Aloha.ready(function () {
		GCN.login('node', 'node', function (success, data) {
			Aloha.jQuery('#content')
			     .html(success ? '<p>Hello, ' + data.user.firstName + '!</p>'
				               : '<p>Login failed</p>');
		});
	});
}());
