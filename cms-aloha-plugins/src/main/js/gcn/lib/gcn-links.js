/*!
 * Aloha Editor
 * Author & Copyright (c) 2013 Gentics Software GmbH
 * aloha-sales@gentics.com
 * Licensed under the terms of http://www.aloha-editor.com/license.html
 *
 * @overfiew Implements link handeling for Content.Node's specific needs.
 */
define('gcn/gcn-links', [
	'/gcnjsapi/' + window.Aloha.settings.plugins.gcn.buildRootTimestamp + '/' + (
		window.Aloha.settings.plugins.gcn.gcnLibVersion || 'bin'
	) + '/gcnjsapi.js',
	'jquery',
	'aloha',
	'PubSub',
	'aloha/pluginmanager',
	'util/dom-to-xhtml',
	'i18n!gcn/nls/i18n',
], function (
	GCN,
	$,
	Aloha,
	PubSub,
	PluginManager,
	DomToXhtml,
	i18n
) {
	'use strict';

	/**
	 * Matches an absolute URI.
	 *
	 * Will match:
	 *     http://www.example.com/
	 *     file://example/resource.txt
	 *     //example.com/path/to/resources
	 *     file:example.txt
	 *
	 * Will not match:
	 *     ./path/to/resource
	 *     /example/path/to/resource
	 *
	 * @type {RegExp}
	 */
	var ABSOLUTE_URI = new RegExp('^'
		+ '('
		+ '(([a-z]){3,10}:)?\/\/' // "scheme://" or "//"
		+ ')|('                   // or
		+ '([a-z]){3,10}:'        // "scheme:"
		+ ').+',
		'i');

	/**
	 * Marks the given field appropriately depending on the validity of the
	 * given link information.
	 *
	 * @param {jQuery<HTMLElement>} $field The href input field.
	 * @param {string} href The href value of the given input field.
	 * @param {boolean} internal True if the link being edited is an internal link.
	 */
	function mark($field, href, internal) {
		// The link must either be internal, just an anchor or absolute.
		if (internal || href.indexOf('#') == 0 || ABSOLUTE_URI.test(href)) {
			$field.parent().removeClass('gcn-link-uri-warning');
		} else {
			$field.parent().addClass('gcn-link-uri-warning');
		}
	}

	/**
	 * Determines whether the given link is marked as an internal link or not.
	 *
	 * @param {jQuery<HTMLElement>} $link The link to be checked.
	 */
	function isInternal($link) {
		return undefined !== $link.attr('data-gentics-aloha-repository');
	}

	/**
	 * Updates the data attributes of the specified link element according to
	 * the given href value.
	 *
	 * @param {jQuery<HTMLElement>} $link The link to be upated.
	 * @param {string} href The link's new href value
	 */
	function update($link, href) {
		var anchor;
		var anchorIdx = href.indexOf('#');
		var val = Aloha.settings.plugins.link.anchorLinks;
		var anchorLinks = val === true || (typeof val == 'string' && val.toLowerCase() != 'false');

		if (anchorIdx >= 0) {
			anchor = href.substring(anchorIdx + 1);
			href = href.substring(0, anchorIdx);
		} else {
			anchor = '';
		}

		if (anchorLinks) {
			$link.attr('data-gentics-gcn-anchor', anchor);
		} else {
			//For saving the page we need to leave the 'data-gentics-gcn-anchor' attribute
			//empty and add the anchor to the href, if the anchorLinks setting is deactivated
			$link.attr('data-gentics-gcn-anchor','');
			if (anchor) {
				href = href + '#' + anchor;
			}
		}

		if (!isInternal($link)) {
			$link.attr('data-gentics-gcn-url', href);
		} else {
			// Internal links would still work without this, because
			// they ignore the href attribute, but the attribute can
			// end up looking quite strange without some cleanup.
			$link.attr('href', '#' + anchor);
		}
	}

	/**
	 * Retreives the assosciated warning icon for the given input field.
	 * Will create one if not is found.
	 *
	 * @param {jQuery<HTMLElement>} $input
	 * @param {string} title optional title (defaults to translation of error.invalid-uri)
	 * @return {jQuery<HTMLElement>} Icon DOM Element.
	 */
	function getIcon($input, title) {
		var $container = $input.parent();
		var $icon = $container.find('>span.gcn-link-warning-icon');
		title = title || i18n.t('error.invalid-uri');
		if (0 === $icon.length) {
			$icon = $('<span class="gcn-link-warning-icon"'
				+ ' title="' + title
				+ '">!</span>');
			$container.append($icon);
		}
		$icon.attr('title', title);
		return $icon;
	}

	/**
	 * Handle link interaction messages.
	 *
	 * @param {object} msg PubSub message with the following properties:
	 *                     {String} href - The href value of the input field
	 *                     {HTMLElement} input - The input field element
	 *                     {HTMLElement} element - The link in focus.
	 */
	function handle(msg) {
		var $input = $(msg.input);
		var $element = $(msg.element);

		getIcon($input);
		mark($input, $.trim(msg.href), isInternal($element));
	}

	function getMagiclinkTags(page, report) {
		var fork = page._fork();

		var releasePageFork = function () {
			fork._merge();
		};

		fork.node().constructs(function (constructs) {
			var magiclink = constructs[GCN.settings.MAGIC_LINK]
			             && constructs[GCN.settings.MAGIC_LINK].constructId;
			if (!magiclink) {
				return releasePageFork();
			}
			var links = [];
			fork.tags(function (tags) {
				var i;
				for (i = 0; i < tags.length; i++) {
					if (magiclink === tags[i].prop('constructId')) {
						links.push(tags[i]);
					}
				}
				report(links);
				releasePageFork();
			}, releasePageFork);
		}, releasePageFork);
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

	PubSub.sub('aloha.link.selected', handle);

	PubSub.sub('aloha.link.changed', function (msg) {
		update($(msg.element), $.trim(msg.href));
		handle(msg);
	});
	
	PubSub.sub('aloha.link.pasted', function (msg) {
		update($(msg.element), $.trim(msg.href));
		// Default target is _blank
		$(msg.element).attr('target', '_blank');
	});

	return {
		isInternal : isInternal,
		getIcon	   : getIcon
	};
});
