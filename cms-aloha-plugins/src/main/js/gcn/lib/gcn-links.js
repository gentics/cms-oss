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
	'aloha/ephemera',
	'util/dom-to-xhtml',
	'i18n!gcn/nls/i18n',
], function (
	GCN,
	$,
	Aloha,
	PubSub,
	PluginManager,
	Ephemera,
	DomToXhtml,
	i18n
) {
	'use strict';

	// Link block attributes
	var ATTR_TAG_ID = 'data-gcn-tagid';
	var ATTR_TAG_NAME = 'data-gcn-tagname';

	// General link attributes
	var ATTR_REPOSITORY = 'data-gentics-aloha-repository';
	var ATTR_OBJECT_ID = 'data-gentics-aloha-object-id';
	var ATTR_HREF = 'href';
	var ATTR_HREF_LANG = 'hreflang';
	var ATTR_TITLE = 'title';
	
	// Internal Link attributes
	var ATTR_ANCHOR = 'data-gentics-gcn-anchor';
	var ATTR_URL = 'data-gentics-gcn-url';
	var ATTR_FILE_URL = 'data-gentics-gcn-fileurl';
	var ATTR_CHANNEL_ID = 'data-gcn-channelid';
	var ATTR_TARGET_LABEL = 'data-gcn-target-label';
	var ATTR_OBJECT_ONLINE = 'data-gentics-aloha-object-online';
	
	// Misc constants
	var REPO_INTERNAL_LINK = 'com.gentics.aloha.GCN.Page';
	var TYPE_PAGE = '10007';
	var TYPE_FILE = '10008';
	var TYPE_IMAGE = '10011';

	Ephemera.attributes(ATTR_TAG_NAME, ATTR_OBJECT_ONLINE);

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
	var ABSOLUTE_URI = new RegExp('([a-z][a-z0-9-+.]+):(?:\/\/)?(.+)', 'i');

	var originalLinkContextProviderFn;
	var originalLinkUpsertFn;

	/**
	 * Determines whether the given link is marked as an internal link or not.
	 *
	 * @param {jQuery<HTMLElement>} $link The link to be checked.
	 */
	function isInternal($link) {
		return null != $link.attr(ATTR_REPOSITORY);
	}

	function interjectLinkPlugin(plugin) {
		originalLinkContextProviderFn = plugin.createInsertLinkContext;
		originalLinkUpsertFn = plugin.upsertLink;

		plugin.createInsertLinkContext = function(existingLink) {
			var context = originalLinkContextProviderFn(existingLink);

			if (context.controls.url.options == null) {
				context.controls.url.options = {};
			}
			context.controls.url.options.showPicker = true;

			var originalValidateFn = context.controls.url.validate;
			context.controls.url.validate = function(value) {
				// If it's an internal link, we do the validation here
				if (value.isInternal) {
					if (value.internalTargetId != null && value.internalTargetId > 0) {
						return null;
					}

					return { required: true };
				}

				if (typeof originalValidateFn === 'function') {
					return originalValidateFn(value);
				}
				return null;
			};

			if (existingLink && $(existingLink)) {
				context.initialValue = extractLinkTargetFromElement(context.initialValue, existingLink);
			}

			return context;
		};

		plugin.upsertLink = function(linkElement, formData) {
			var link = originalLinkUpsertFn(linkElement, formData, true);

			if (!formData.url.isInternal) {
				stripInternalLinkAttributesFromElement(link);
			} else {
				applyLinkTargetToElement(formData, link);
			}

			plugin.hrefChange(link);

			return link;
		};
	}

	function stripInternalLinkAttributesFromElement(link) {
		link.removeAttribute(ATTR_REPOSITORY);
		link.removeAttribute(ATTR_OBJECT_ID);
		link.removeAttribute(ATTR_CHANNEL_ID);
		link.removeAttribute(ATTR_FILE_URL);
		link.removeAttribute(ATTR_URL);
		link.removeAttribute(ATTR_TARGET_LABEL);
	}

	/**
	 * 
	 * @param {LinkTarget} data The LinkTarget value
	 * @param {HTMLAnchorElement} link The anchor element
	 * @returns {ExtendedLinkTarget} An extended form of the LinkTarget with internal properties for the GCMSUI set.
	 */
	function extractLinkTargetFromElement(data, link) {
		var objId = link.getAttribute(ATTR_OBJECT_ID);
		// Anchor value is always stored separately
		data.anchor = link.getAttribute(ATTR_ANCHOR);

		// Not actually an internal link
		if (objId == null || objId === '') {
			return data;
		}

		var targetId = 0;
		var targetType = '';
		var targetLabel = link.getAttribute(ATTR_TARGET_LABEL) || '';
		var targetNodeId = null;

		/** @type {array.<string>} */
		var objData;
		if (objId.includes('.')) {
			objData = objId.split('.');
		} else {
			// Default to a page type if it has no specification
			objData = ['10007', objId];
		}

		switch (objData[0]) {
			case TYPE_FILE:
				targetType = 'file';
				break;

			case TYPE_IMAGE:
				targetType = 'image';
				break;

			default:
			case TYPE_FILE:
				targetType = 'page';
				break;
		}

		targetId = parseInt(objData[1], 10);
		if (!Number.isInteger(targetId)) {
			targetId = 0;
		}

		var channelId = link.getAttribute(ATTR_CHANNEL_ID);
		if (channelId != null && channelId !== '') {
			targetNodeId = parseInt(channelId, 10);
			if (!Number.isInteger(targetNodeId)) {
				targetNodeId = null;
			}
		}

		data.url = Object.assign({}, data.url, {
			isInternal: true,
			internalTargetLabel: targetLabel,
			internalTargetId: targetId,
			internalTargetType: targetType,
			internalTargetNodeId: targetNodeId,
		});

		return data;
	}

	/**
	 * @param {ExtendedLinkTarget} data The form data from the link form
	 * @param {HTMLAnchorElement} link The Link element to apply the data to
	 */
	function applyLinkTargetToElement(data, link) {
		var objId;

		switch (data.url.internalTargetType) {
			case 'image':
				objId = TYPE_IMAGE;
				break;

			case 'file':
				objId = TYPE_FILE;
				break;
			
			default:
			case 'page':
				objId = TYPE_PAGE;
				break;
		}

		link.setAttribute(ATTR_REPOSITORY, REPO_INTERNAL_LINK);
		link.setAttribute(ATTR_TARGET_LABEL, data.url.internalTargetLabel);
		link.setAttribute(ATTR_OBJECT_ID, objId + '.' + data.url.internalTargetId);
		link.setAttribute(ATTR_CHANNEL_ID, data.url.internalTargetNodeId);
		link.setAttribute(ATTR_URL, data.url.href);
		link.setAttribute(ATTR_HREF, '#' + data.url.anchor);
		link.setAttribute(ATTR_ANCHOR, data.url.anchor);
		link.setAttribute(ATTR_HREF_LANG, data.lang);
	};

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
			$link.attr(ATTR_ANCHOR, anchor);
		} else {
			//For saving the page we need to leave the 'data-gentics-gcn-anchor' attribute
			//empty and add the anchor to the href, if the anchorLinks setting is deactivated
			$link.attr(ATTR_ANCHOR,'');
			if (anchor) {
				href = href + '#' + anchor;
			}
		}

		if (!isInternal($link)) {
			$link.attr(ATTR_URL, href);
		} else {
			// Internal links would still work without this, because
			// they ignore the href attribute, but the attribute can
			// end up looking quite strange without some cleanup.
			$link.attr(ATTR_HREF, '#' + anchor);
		}
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

	PubSub.sub('aloha.link.changed', function (msg) {
		update($(msg.element), $.trim(msg.href));
	});
	
	PubSub.sub('aloha.link.pasted', function (msg) {
		update($(msg.element), $.trim(msg.href));
		// Default target is _blank
		$(msg.element).attr('target', '_blank');
	});

	return {
		isInternal: isInternal,
		interjectLinkPlugin: interjectLinkPlugin,
	};
});
