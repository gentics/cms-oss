/*!
 * Aloha Editor
 * Author & Copyright (c) 2012-2013 Gentics Software GmbH
 * aloha-sales@gentics.com
 * Licensed under the terms of http://www.aloha-editor.com/license.html
 */
define('gcn/gcn-util', [
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
	 * Annotates the given block DOM element with the mandatory GCN block data
	 * attributes and classes, as well as any other attributes specified in the
	 * `attributes' argument.
	 *
	 * Each user-specified attribute will be prefixed with "data-gcn-"
	 *
	 * If any such user-specified attributes are given, a
	 * "data-gcn-custom-annotations" attribute will be added to the object.
	 * This attribute contains the a comma seperated list of the custom
	 * attributes keys.  This attribute will be used in deannotateBlock() to
	 * identify custom attributes.
	 *
	 * @param {jQuery.<HTMLElement>} $block jQuery unit set containing a DOM
	 *                                      element.
	 * @param {object<string, string>} attributes A key-value pair map of
	 *                                            attributes with which to add
	 *                                            to the given DOM element.
	 * @return {jQuery.<HTMLElement>} The given block, annotated.
	 */
	function addBlockAnnotation($block, attributes) {
		$block.contentEditable(false)
			.attr('data-aloha-block-type', 'GCNBlock')
			.addClass('GENTICS_block')
			.addClass('aloha-block');

		var customAnnotation = [];
		var attr;
		for (attr in attributes) {
			if (attributes.hasOwnProperty(attr)) {
				$block.attr('data-gcn-' + attr, attributes[attr]);
				customAnnotation.push(attr);
			}
		}

		if (0 < customAnnotation.length) {
			$block.attr('data-gcn-custom-annotation',
				customAnnotation.join(','));
		}

		return $block;
	}

	/**
	 * Remove annotations that were added to the block's root element.
	 *
	 * @param {jQuery.<HTMLElement>} $block The block element to have
	 *                                      annotations removed from it.
	 * @return {jQuery.<HTMLElement>} The given block, de-annotated.
	 */
	function removeBlockAnnotation($block) {
		$block.removeAttr('contenteditable')
			.attr('data-aloha-block-type', null)
			.removeClass('GENTICS_block')
			.removeClass('aloha-block')
			.removeClass('aloha-block-GCNBlock');

		var customAnnotation = $block.attr('data-gcn-custom-annotation');
		if (!customAnnotation) {
			return $block;
		}

		var attributes = customAnnotation.split(',');
		var i;
		for (i = 0; i < attributes.length; i++) {
			$block.attr('data-gcn-' + attributes[i], null);
		}

		$block.attr('data-gcn-custom-annotation', null);

		return $block;
	}

	/**
	 * Replace the given (copied) tag block with a placeholder, that contains a
	 * throbber.
	 *
	 * @param {jQuery.<HTMLElement>} $block jQuery object of the tag block.
	 * @return {jQuery.<HTMLElement>} jQuery container of the replacement DOM
	 *                                object.
	 */
	function setPlaceholder($block, pageid, tagname) {
		if (0 === $block.length) {
			return;
		}

		var nodeName = $block[0].nodeName;
		// check whether the root tag of the "block" is actually allowed
		if ($.inArray(nodeName.toLowerCase(), BlockPlugin.settings.rootTags) === -1) {
			// root tag is not allowed, we replace it with <div> or <span>
			if (Dom.isBlockLevelElement($block[0])) {
				nodeName = 'div';
			} else {
				nodeName = 'span';
			}
		}

		var $placeholder = $('<' + nodeName + '>');
		$placeholder = addBlockAnnotation($placeholder, {
			copy: 'true',
			'copy-pageid': pageid,
			'copy-tagname': tagname
		});
		var $img = $('<img/>').attr(
			'src',
			Aloha.getPluginUrl('gcn') + '/images/dark_rounded/loader.gif'
		);
		$block.after($placeholder.append($img)).remove();
		return $placeholder;
	}

	/**
	 * Checks whether the given editable contains tags that need to be copied.
	 * If not, we publish an event.
	 *
	 * @param {editable} editable The editable to check.
	 */
	function finishedCopyingBlock(editable) {
		if (editable &&
			editable.obj.attr('data-gcn-copy') &&
			0 === editable.obj.find('[data-gcn-copy]').length) {
			editable.obj.attr('data-gcn-copy', null);
			PubSub.pub('gcn.tagcopy.finished', { editable: editable });
		}
	}

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

	function withinCMS(callback) {
		if (window.GCMSUI != null) {
			callback();
			return;
		}

		var called = false;
		Aloha.bind('gcmsui.ready', function () {
			if (called) {
				return;
			}
			called = true;
			callback();
		});
	}

	let constructIdCache = null;
	let constructKeywordCache = null;

	function buildConstructCache(constructs) {
		if (constructIdCache == null) {
			constructIdCache = {};
			for (const elem of Object.values(constructs || {})) {
				constructIdCache[elem.id] = structuredClone(elem);
			}
		}
		if (constructKeywordCache == null) {
			constructKeywordCache = structuredClone(constructs);
		}
	}

	function getConstructFromId(id) {
		return new Promise((function(resolve, reject) {
			if (constructIdCache != null) {
				resolve(constructIdCache[id]);
				return;
			}
	
			withinCMS(function() {
				GCMSUI.getConstructs().then(constructs => {
					buildConstructCache(constructs);
					resolve(constructIdCache[id]);
				}).catch(reject);
			});
		}));
	}

	function getConstructFromKeyword(keyword) {
		return new Promise((function(resolve, reject) {
			if (constructKeywordCache != null) {
				resolve(constructKeywordCache[keyword]);
				return;
			}

			withinCMS(function() {
				GCMSUI.getConstructs().then(constructs => {
					buildConstructCache(constructs);
					resolve(constructKeywordCache[keyword]);
				}).catch(reject);
			});
		}));
	}

	return {
		addBlockAnnotation: addBlockAnnotation,
		removeBlockAnnotation: removeBlockAnnotation,
		finishedCopyingBlock: finishedCopyingBlock,
		setPlaceholder: setPlaceholder,
		createUrl: createUrl,
		withinCMS: withinCMS,
		getConstructFromId: getConstructFromId,
		getConstructFromKeyword: getConstructFromKeyword,
	};
});
