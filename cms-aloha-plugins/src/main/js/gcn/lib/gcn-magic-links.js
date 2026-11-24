define('gcn/gcn-magic-links', [
	'/gcnjsapi/' + window.Aloha.settings.plugins.gcn.buildRootTimestamp + '/' + (
		window.Aloha.settings.plugins.gcn.gcnLibVersion || 'bin'
	) + '/gcnjsapi.js',
	'jquery',
	'aloha/pluginmanager',
	'util/dom-to-xhtml',
], function(
	GCN,
	/** @type {JQueryStatic} */
	$,
	PluginManager,
	DomToXhtml,
) {
    function getMagiclinkTags(page, report) {
		page.tags(tags => {
			report(tags.filter(tag => {
				return tag.constructId === GCNMagicLinks.magicLink.id;
			}));
		});
	}

	function cleanMagiclinkTags(tags) {
		var i;
		var $content;
		for (i = 0; i < tags.length; i++) {
			$content = $('<div>' + tags[i].part('text') + '</div>');
			PluginManager.makeClean($content);
			tags[i].part('text', DomToXhtml.contentsToXhtml($content[0]));
		}
	}

	GCN.sub('page.before-saved', function (page) {
		getMagiclinkTags(page, cleanMagiclinkTags);
	});

    const GCNMagicLinks = {
        magicLink: null,
    }

    return GCNMagicLinks;
});