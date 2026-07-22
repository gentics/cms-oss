define('gcn/gcnjs-util', [
	'/gcnjsapi/' + window.Aloha.settings.plugins.gcn.buildRootTimestamp + '/' + (
		window.Aloha.settings.plugins.gcn.gcnLibVersion || 'bin'
	) + '/gcnjsapi.js',
	'jquery',
	'aloha',
	'PubSub',
	'block/block-plugin',
	'util/dom',
], function (
	_GCN_,
	$,
	Aloha,
	PubSub,
	BlockPlugin,
	Dom
) {
	'use strict';

	var GCN = window.GCN;

	/**
	 * Creates a URL for GCN.
	 *
	 * Will automatically add the sid as request parameters, additional
	 * parameters may be given.
	 *
	 * The data may contain the following properties:
	 * - url: part of the URL for the specific request after /rest,
	 *        must start with / and must not contain request parameters
	 * - params: additional request parameters
	 * - noCache: forces the browser to re-fetch the URL,
	 *            irrespective of any caching headers sent with an earlier
	 *            response for the same URL.  This is achived by appending a
	 *            timestamp. Note: a timestamp has millisecond granularty which
	 *            may not be enough.  This parameter is a hack and should not be
	 *            used. Instead, the headers of the response should indicate
	 *            whether a resource can be cached or not.
	 * @param {object} data The data describing the GCN URL
	 * @return {string} A GCN url
	 */
	function createUrl(data) {
		var url = data.url + '?sid=' + GCN.sid;
		if (data.noCache) {
			url += '&time=' + (new Date()).getTime();
		}
		var name;
		for (name in data.params) {
			if (data.params.hasOwnProperty(name)) {
				url += '&' + name
					+ '=' + encodeURI(data.params[name]);
			}
		}
		return url;
	}

	return {
		createUrl: createUrl,
	};
});