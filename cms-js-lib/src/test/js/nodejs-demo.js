var jsdom = require('jsdom');
var GCN = require('../../../target/debug/gcnjsapi.js');

jsdom.env({
	html: '<html><body></body></html>',
	done: function (errors, window) {
		// Overload the AJAX function
		GCN.settings.BACKEND_PATH = 'http://gcn-testing.office/CNPortletapp';
		GCN.sub('error-encountered', function (error) {
			console.log(error.toString());
		});
		GCN.login('node', 'node', function (success) {
			GCN.page(200).folder().createFile('./nodejs-demo.js', function (file) {
				console.log(file);
			});
		});
	}
});
