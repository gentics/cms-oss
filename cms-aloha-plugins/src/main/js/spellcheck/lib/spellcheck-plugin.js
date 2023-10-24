define([
	'jquery',
	'aloha/plugin',
	'aloha/core',
	'util/html',
	'util/dom',
	'util/dom2',
	'PubSub',
	'aloha/ephemera',
	'ui/ui',
	'ui/button',
	'i18n!spellcheck/nls/i18n',
	'gcn/gcn-plugin',
	'css!spellcheck/css/main'
], function (
	$,
	Plugin,
	Aloha,
	Html,
	Dom,
	Dom2,
	PubSub,
	Ephemera,
	Ui,
	Button,
	i18n
) {
	'use strict';

	/**
	 * Name of this plugin
	 */
	var pluginName           = 'spellcheck';
	var textNodeOffsetMap    = null;
	var spellCheckInProgress = false;
	var $menu                = null;
	var lastActiveEditable   = null;

	var plugin = Plugin.create(pluginName, {
		/**
		 * Default config: plugin active for all editables
		 */
		config: [pluginName],

		/**
		 * The default plugin settings.
		 *
		 * <ul>
		 *  <li><code>languageCodes</code>: a mapping from two character language codes (as used by the GCMS) to
		 * 		long codes including the country. The long codes are needed to supply the language
		 * 		to languagetool (it claims to support "en" for example, but it doesn't detect any errors.</li>
		 * 	<li><code>defaultLanguage</code>: is the default language to be used, when no mapping could be
		 * 		found or loaded for the current page language.</li>
		 * </ul>
		 */
		defaults: {
			languageCodes: {
				de: 'de-AT',
				en: 'en-US'
			},
			defaultLanguage: 'en-US',
			disabledRules: ''
		},

		/**
		 * Initialize the plugin
		 */
		init: function () {
			var plugin = this;
			Aloha.bind('aloha-editable-created', function (event, editable) {
				if (!editable.obj[0]) {
					return;
				}

				// Disable the browsers own spellchecking feature on this editable
				editable.obj[0].setAttribute('spellcheck', false);
			});

			$(window).click(function() {
				if ($menu !== null) {
					$menu.remove();
					$menu = null;
				}
			});

			var button = Ui.adopt('gcnSpellCheck', Button, {
				tooltip: i18n.t('button.spellchecking.tooltip'),
				icon: 'aloha-icon aloha-icon-abbr',
				'class': 'aloha-spellcheck-marker',
				scope: 'Aloha.continuoustext',
				click: function () {
					button.element.blur();
					checkContent(plugin);
				}
			});

			this.disabledRules = 'WHITESPACE_RULE';
			if (typeof this.settings.disabledRules == 'string') {
				this.disabledRules += ',' + this.settings.disabledRules.replace(/ /g, '');
			}

			// Map the page language code to the codes needed for language tool. If no
			// mapping is found in the settings, the list of supported languages is
			// loaded from languagetool directly. When there is still no mapping, the
			// default language from the settings is used.
			var pageLanguage = Aloha.require('gcn/gcn-plugin').settings.pagelanguage;
			var languageCode = this.settings.languageCodes[pageLanguage];

			if (languageCode) {
				this.language = languageCode;
			} else {
				$.ajax({
					type: 'GET',
					url: '/spelling/languages',

					success: function(languages) {
						var numLanguages = languages.length;

						for (var i = 0; i < languages.length; i++) {
							var lang = languages[i];

							if (lang.code == pageLanguage && lang.longCode.indexOf('-') >= 0) {
								if (plugin.language) {
									console.warn('Multiple language codes found for "' + pageLanguage + "'");
									console.warn('Only the first value will be used (' + plugin.language + ')');
								} else {
									plugin.language = lang.longCode
								}
							}
						}

						if (!plugin.language || plugin.language.indexOf('-') < 0) {
							console.warn('Could not determine long code for language "' + pageLanguage + '"');
							console.warn('Falling back to "' + plugin.settings.defaultLanguage);

							plugin.language = plugin.settings.defaultLanguage;
						}
					},

					error: function(jqXHR, textStatus, errorThrown) {
						console.error('Could not load language information from languagetool: "' + errorThrown + '"');
						console.error('Falling back to default language "' + plugin.settings.defaultLanguage);

						plugin.language = plugin.settings.defaultLanguage;
					}
				})
			}
		}
	});

	/**
	 * Check spelling in the active editable.
	 *
	 * @param {plugin} The configured spellcheck-plugin.
	 */
	function checkContent (plugin) {
		if (spellCheckInProgress) {
			return;
		}

		var activeEditable = Aloha.getActiveEditable();

		if (!activeEditable) {
			return;
		}

		// Clean any previous markers
		cleanupMarkers(activeEditable.obj[0]);

		var text = processTextNodeOffsetMappings(activeEditable.obj[0]);

		$.ajax({
			type: 'POST',
			url: '/spelling/check',
			data: {
				language: plugin.language,
				text: text,
				allowIncompleteResults: true,
				disabledRules: plugin.disabledRules
			},
			success: function (data) {
				spellCheckResultReceived(data, activeEditable);
			},
			complete: spellCheckCompleted
		});

		spellCheckInProgress = true;
	}

	/**
	 * Clean up after a request to the languagetool has finished.
	 */
	function spellCheckCompleted () {
		spellCheckInProgress = false;
	}

	/**
	 * Get the parents of the given node, up to but not including
	 * the first block element.
	 *
	 * @param {node} The node to gather the parents for.
	 * @returns {Array} All parents of the given node up to but not
	 * 	including the first block level element.
	 */
	function getNonBlockParents(node) {
		var parents = [ node ];
		var parentBlock = findParentBlock(node);

		while (node = node.parentNode) {
			parents.unshift(node);

			if (node == parentBlock) {
				break;
			}
		}

		return parents;
	}

	/**
	 * Get the lowest common ancestor as well as all non-common ancestors
	 * for the given nodes.
	 *
	 * @param {nodes} An array of DOM nodes to get the ancestors for.
	 * @returns {Object} An object containing the <code>lowestCommonAncestor</code>
	 * 	of the given nodes, as well as an array <code>parents</code> which lists all
	 * 	non-common parents for the respective nodes in the parameter.
	 */
	function getAncestors(nodes) {
		var minCount = Number.POSITIVE_INFINITY;
		var allParents = [];
		var numNodes = nodes.length;

		for (var i = 0; i < numNodes; i++) {
			var nodeParents = getNonBlockParents(nodes[i]);

			if (nodeParents.length < minCount) {
				minCount = nodeParents.length;
			}

			allParents.push(nodeParents);
		}

		var idx = 0;

		outerLoop:
		while (idx < minCount) {
			for (i = 1; i < numNodes; i++) {
				if (allParents[i - 1][idx] != allParents[i][idx]) {
					break outerLoop;
				}
			}

			idx++;
		}

		var lca = allParents[0][idx - 1];

		for (i = 0; i < numNodes; i++) {
			allParents[i] = allParents[i].slice(idx, -1);
		}

		return {
			lowestCommonAncestor: lca,
			parents: allParents
		};
	}

	/**
	 * Creates a node with the given text and the markup from the ancestors.
	 *
	 * @param {ancestors} An array containing the relevant ancestors (to recreate
	 * 	the correct markup)
	 * @param {text} The text value for the newly created node
	 * @returns {jQuery} A jQuery object containing either a single text node,
	 * 	or the markup recreated from the ancestors and the text.
	 */
	function createNodeWithMarkup(ancestors, text) {
		var markup = text;

		if (ancestors.length == 0) {
			return $(document.createTextNode(text));
		}

		for (var i = ancestors.length - 1; i >= 0; i--) {
			var nodeName = ancestors[i].nodeName.toLowerCase();
			markup = '<' + nodeName + '>' + markup + '</' + nodeName + '>';
		}

		return $(markup);
	}

	/**
	 * Splits the matching text nodes if necessary so that error markers
	 * can be placed around possible markup.
	 *
	 * The error markers should be placed as close to the spelling error
	 * as possible, but outside of any formating tags if possible.
	 *
	 * Since only a part of formatted text might be erroneous, the first
	 * and the last text node might require being split.
	 *
	 * @param {matchOffset} The offset of a found error in the text
	 * 	sent to the languagetool
	 * @param {matchLength} The length of a found error in the text
	 * 	sent to the languagetool.
	 * @param {matchingTextNodes} An error of matching text nodes in the
	 * 	DOM along with their respective offset in the text sent to
	 * 	the language tool.
	 * @returns {Range} The Range around which the error marker can be
	 * 	placed.
	 */
	function fixMatchRange(matchOffset, matchLength, matchingTextNodes) {
		var numMatches = matchingTextNodes.length;
		var range = document.createRange();
		var match = matchingTextNodes[0];
		var nodeMarkOffset = matchOffset - match.offset;

		if (numMatches == 1) {
			range.setStart(match.node, nodeMarkOffset);
			range.setEnd(match.node, nodeMarkOffset + matchLength);

			return range;
		}

		var newNode;
		var ancestors = getAncestors(matchingTextNodes.map(function (textNodeMatch) { return textNodeMatch.node }));
		var matchParents = ancestors.parents[0];

		if (nodeMarkOffset > 0) {
			newNode = createNodeWithMarkup(
				matchParents,
				match.node.data.substring(nodeMarkOffset));

			if (matchParents.length == 0) {
				newNode.insertAfter(match.node);
			} else {
				newNode.insertAfter(matchParents[0]);
			}

			match.node.data = match.node.data.substring(0, nodeMarkOffset);
			while (match.node.nextSibling && !newNode.is(match.node.nextSibling)) {
				newNode.append(match.node.nextSibling);
			}

			range.setStart(ancestors.lowestCommonAncestor, Dom.getIndexInParent(newNode[0]));
		} else {
			range.setStart(
				ancestors.lowestCommonAncestor,
				Dom.getIndexInParent(matchParents.length > 0 ? matchParents[0] : match.node));
		}

		match = matchingTextNodes[numMatches - 1];
		nodeMarkOffset = matchOffset + matchLength - match.offset;
		matchParents = ancestors.parents[numMatches - 1];

		if (nodeMarkOffset < match.node.data.length) {
			newNode = createNodeWithMarkup(
				matchParents,
				match.node.data.substring(0, nodeMarkOffset));

			if (matchParents.length == 0) {
				newNode.insertBefore(match.node);
			} else {
				newNode.insertBefore(matchParents[0]);
			}

			match.node.data = match.node.data.substring(nodeMarkOffset);
			while (match.node.previousSibling && !newNode.is(match.node.previousSibling)) {
				newNode.prepend(match.node.previousSibling);
			}

			range.setEnd(ancestors.lowestCommonAncestor, Dom.getIndexInParent(newNode[0]) + 1);
		} else {
			range.setEnd(
				ancestors.lowestCommonAncestor,
				Dom.getIndexInParent(matchParents.length > 0 ? matchParents[0] : match.node) + 1);
		}

		return range;
	}

	/**
	 * Add markers to the text in the given editable according
	 * to the received spell check result.
	 *
	 * @param {data} The result from the languagetool.
	 * @param {editable} The editable for wich to apply the result.
	 */
	function spellCheckResultReceived (data, editable) {
		if (!editable) {
			return;
		}

		// This will be filled
		var matchRanges = [];

		var matches = data.matches;
		for (var i = 0; i < matches.length; i++) {
			var match = matches[i];
			var matchingTextNodeOffsets = findMatchingTextNodeOffsets(match.offset, match.length);
			var range = fixMatchRange(match.offset, match.length, matchingTextNodeOffsets);

			matchRanges.push({ range: range, match: match });
		}

		for (i = 0; i < matchRanges.length; i++) {
			var matchRange = matchRanges[i];
			var markerElement = document.createElement('span');

			markerElement.classList.add('aloha-spellcheck-marker');
			Ephemera.markWrapper(markerElement);

			matchRange.range.surroundContents(markerElement);
			addMarkerClickHandler(markerElement, matchRange);
		}

		textNodeOffsetMap = null;
	}

	/**
	 * Adds a context menu handler to the given element
	 * containing a spelling error.
	 *
	 * @param {element} The element to attach the handler to.
	 * @param {matchRange} An object containing the match returned from the
	 * language tool (those contain replacement suggestions if	applicable).
	 */
	function addMarkerClickHandler (element, matchRange) {
		var $element = $(element);

		$element.contextmenu(function(event) {
			event.stopPropagation();
			event.preventDefault();
			createMenuForMarker(event, this, matchRange);
		});
	}

	/**
	 * Create a context menu for the given element and spellchecking match.
	 *
	 * @param {event} The event that caused the call.
	 * @param {element} The element the event was attached to.
	 * @param {matchRange} An object containing a match result from the spellchecking.
	 */
	function createMenuForMarker(event, element, matchRange) {
		if ($menu !== null) {
			// Delete the old menu first
			$menu.remove();
		}

		var $element = $(element);

		var content = '<span class="aloha-spellcheck-message">' + matchRange.match.message
			+ '<span class="aloha-spellcheck-subject">' + $element.text() + '</span></span>';

		if (matchRange.match.replacements.length > 0) {
			content += '<span class="aloha-spellcheck-replacements-title">' + i18n.t('did.you.mean') + '</span>\n<ul class="aloha-spellcheck-replacements-list">'
				+ matchRange.match.replacements.map(function(elem) {
					return '<li class="spellcheck-correction">' + elem.value + '</li>\n';
				}).join('\n')
			+ '</ul>';
		}

		var html = '<div id="aloha-spellcheck-marker-menu">' + content + '</div>';

		$menu = $(html).appendTo('body');
		$menu.css({left: event.pageX - $('body').offset().left + 10, top: event.pageY - $('body').offset().top});

		$('.spellcheck-correction', $menu).mousedown(function (event) {
			event.stopPropagation();
		}).click(function () {
			// select the marker
			$element.text($(this).text());
			$element.contents().unwrap();
			$menu.remove();
		});

		$menu.click(function(event){
			event.stopPropagation();
		});
	}

	/**
	 * Removes all error markers from previous spell checkings.
	 *
	 * @param {rootElement} The container element in which to search for and remove
	 * 	old spell check markers.
	 */
	function cleanupMarkers (rootElement) {
		var treeWalker = document.createTreeWalker(
			rootElement,
			NodeFilter.SHOW_ELEMENT,
			function (node) {
				return (node.classList.contains('aloha-spellcheck-marker') ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP);
			},
			false
		);
		var markersToUnwrap = [];

		while (treeWalker.nextNode()) {
			markersToUnwrap.push(treeWalker.currentNode);
		}

		$(markersToUnwrap).each(function () {
			var r = document.createRange();
			var contents = $(this).contents();

			r.selectNode(this);
			r.deleteContents();

			$.each(contents.get().reverse(), function () {
				r.insertNode(this);
			});
		});
	}

	/**
	 * Gathers all text nodes contained in the given element and prepares
	 * the text to be sent to the language tool.
	 *
	 * The offsets of the original text nodes in the resulting text are
	 * stored along with the text nodes themselves in the <code>textNodeOffsetMap</code>.
	 *
	 * @param {domELement} The root element from which to gather the text nodes.
	 * @returns {String} The string to be sent to languagetool.
	 */
	function processTextNodeOffsetMappings (domElement) {
		var text = '';
		var currentOffset = 0;
		var lastNode = null;

		textNodeOffsetMap = [];

		var treeWalker = document.createTreeWalker(
			domElement,
			NodeFilter.SHOW_TEXT,
			function () {
				return NodeFilter.FILTER_ACCEPT;
			},
			false);

		while (treeWalker.nextNode()) {
			var currentNode = treeWalker.currentNode;
			var currentText = currentNode.textContent;

			if (currentText === '') {
				continue;
			}

			if (lastNode !== null && !areTheTextNodesInSameBlock(lastNode, currentNode)) {
				// If the last node and this one are not in the same DOM block element container
				// make sure they are separated by whitespace to not confuse the spellchecker.
				if (/\S$/.test(text) && /^\S/.test(currentText)) {
					text += ' ';
					currentOffset++;
				}
			}

			text += currentText;
			textNodeOffsetMap.push({ offset: currentOffset, node: currentNode });

			currentOffset += currentText.length;
			lastNode = currentNode;
		}

		return text;
	}

	/**
	 * Searches text nodes which fall into the interval defined by the
	 * given offset and length.
	 *
	 * @param {offset} The offset in the text sent to languagetool.
	 * @param {length} The length of the match in the text sent to languagetool.
	 * @returns {Array} An array of objects containing an offset in the text
	 * 	sent to language tool an the respective text node.
	 */
	function findMatchingTextNodeOffsets (offset, length) {
		var textNodeOffsets = [];

		var x1 = offset;
		var x2 = x1 + length;
		for (var i = 0; i < textNodeOffsetMap.length; i++) {
			var textNodeOffset = textNodeOffsetMap[i];
			var y1 = textNodeOffset.offset;
			var y2 = y1 + textNodeOffset.node.textContent.length;

			// Test range intersection
			if (x1 < y2 && y1 < x2) {
				textNodeOffsets.push(textNodeOffset);
			}
		}

		return textNodeOffsets;
	}

	/**
	 * Tests if two text nodes are in the same block element.
	 *
	 * @param {node} The first node to compare.
	 * @param {node2} The other node to compare.
	 * @returns {boolean} true if the respective first parents of the nodes
	 * 	that is a block element are the same, and false otherwise.
	 */
	function areTheTextNodesInSameBlock(node, node2) {
		var nodeParentBlock = findParentBlock(node);

		if (nodeParentBlock == null) {
			return false;
		}

		return nodeParentBlock.isSameNode(findParentBlock(node2));
	}

	/**
	 * Find the first parent element of the given node with a display style
	 * that does <em>not</em> start with <code>inline</code>.
	 *
	 * @param {node} The element to search a block parent for.
	 * @returns {Element} The first element which is a parent of <code>node</code>
	 * 	and a block element.
	 */
	function findParentBlock(node) {
		var currentParent = node.parentNode;
		while (currentParent != null &&
				window.getComputedStyle(currentParent).display.lastIndexOf('inline', 0) == 0) {
			currentParent = currentParent.parentNode;
		}

		return currentParent;
	}

	/**
	 * @type {Aloha.Plugin}
	 */
	return plugin;
});
