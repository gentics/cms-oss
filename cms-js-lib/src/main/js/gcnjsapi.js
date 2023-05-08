/*global Aloha: true */

GCN = (function () {

	'use strict';

	var prefix = "../../main/js/";

	if (typeof Aloha !== 'undefined') {
		window.$ = window.jQuery = window.Aloha.jQuery;
		var settings = Aloha.settings.plugins.gcn;
		prefix = settings.webappPrefix + settings.buildRootTimestamp + '/gcnjsapi/dev/';
	}

	jQuery('head').append(
		'<script type="text/javascript" src="' + prefix + 'core.js"></script>'      +
		'<script type="text/javascript" src="' + prefix + 'util.js"></script>'      +
		'<script type="text/javascript" src="' + prefix + 'chainback.js"></script>' +
		'<script type="text/javascript" src="' + prefix + 'session.js"></script>'   +
		'<script type="text/javascript" src="' + prefix + 'abstract-content-object.js"></script>' +
		'<script type="text/javascript" src="' + prefix + 'abstract-tag-container.js"></script>' +
		'<script type="text/javascript" src="' + prefix + 'node.js"></script>'      +
		'<script type="text/javascript" src="' + prefix + 'folder.js"></script>'    +
		'<script type="text/javascript" src="' + prefix + 'page.js"></script>'      +
		'<script type="text/javascript" src="' + prefix + 'template.js"></script>'  +
		'<script type="text/javascript" src="' + prefix + 'file.js"></script>'      +
		'<script type="text/javascript" src="' + prefix + 'image.js"></script>'     +
		'<script type="text/javascript" src="' + prefix + 'message.js"></script>'   +
		'<script type="text/javascript" src="' + prefix + 'multichannelling.js"></script>'   +
		'<script type="text/javascript" src="' + prefix + 'tag.js"></script>'
	);

	return GCN;
}());

return GCN;
