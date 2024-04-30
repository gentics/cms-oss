/*!
 * Aloha Editor
 * Author & Copyright (c) 2013 Gentics Software GmbH
 * aloha-sales@gentics.com
 * Licensed under the terms of http://www.aloha-editor.com/license.html
 *
 * @overfiew Implements tag handeling for Content.Node's specific needs.
 */
define('gcn/gcn-tags', [
	'/gcnjsapi/' + window.Aloha.settings.plugins.gcn.buildRootTimestamp + '/' + (
		window.Aloha.settings.plugins.gcn.gcnLibVersion || 'bin'
	) + '/gcnjsapi.js',
	'jquery',
	'aloha',
	'aloha/editable',
	'aloha/ephemera',
	'block/block-plugin',
	'ui/dialog',
	'util/dom',
	'util/dom2',
	'util/browser',
	'gcn/gcn-util',
	'util/misc',
	'block/block-utils'
], function (
	_GCN_,
	$,
	Aloha,
	Editable,
	Ephemera,
	BlockPlugin,
	Dialog,
	Dom,
	Dom2,
	Browser,
	Util,
	Misc,
	BlockUtils
) {
	'use strict';

	var GCN = window.GCN;

	/**
	 * Searches for the an Aloha editable object of the given id.
	 *
	 * @TODO: Once Aloha.getEditableById() is patched to not cause an
	 *        JavaScript exception if the element for the given ID is not found
	 *        then we can deprecate this function and use Aloha's instead.
	 *
	 * @param {string} id Id of Aloha.Editable object to find.
	 * @return {Aloha.Editable=} The editable object, if found; null otherwise.
	 */
	function getEditableById(id) {
		var i;
		var len = Aloha.editables.length;
		var $element = $('#' + id);
		// Because if the element is a textarea then route to the editable div.
		if ($element.length
				&& $element[0].nodeName.toLowerCase() === 'textarea') {
			id += '-aloha';
		}
		for (i = 0; i < len; i++) {
			if (Aloha.editables[i].getId() === id) {
				return Aloha.editables[i];
			}
		}
		return null;
	}

	/**
	 * Ensures the given element has a block-compatible root element.
	 *
	 * At this time only div and span elements can be used as the root
	 * element of blocks. The reason is that edit-icons can't otherwise
	 * be added (edit-icons in an a tag?). The reason that edit-icons
	 * have to be added to the blockified element instead of being
	 * absolutely positioned is that absolutely positioned edit-icons
	 * may overlap with content.
	 *
	 * If the given $element is not block-compatible, it will be wrapped
	 * with a div element and the block-specific id attribute and
	 * classes will be lifted to the wrapper div. The wrapper div will
	 * also wrap the given $element in the DOM (in case it is attached
	 * to the DOM).
	 *
	 * The auto-wrapping of the given element is a convenience only. It
	 * is still an error that a block has no wrapper div or span, and an
	 * error is logged accordingly.
	 *
	 * @param {!jQuery} $element
	 *        The root element of the block.
	 * @return {!jQuery}
	 *        The given $element if it is block-compatible, or the given
	 *        $element wrapped with a div.
	 */
	function ensureCorrectBlockRoot($element) {
		if (0 === $element.length) {
			return $element;
		}
		var nodeName = $element[0].nodeName;
		if ($.inArray(nodeName.toLowerCase(), BlockPlugin.settings.rootTags) !== -1) {
			return $element;
		}

		if (BlockPlugin.settings.rootTags) {
			Aloha.Log.error('gcn/gcn-plugin',
					' Encountered a tag implementation that has a root element of ' + nodeName + '.' +
					' Since blocks can only have root elements of [' +
					BlockPlugin.settings.rootTags.join(', ') + ']' +
					' it will be automatically wrapped with a <div>.' +
				    ' The automatic wrapping may result in unpredictable behavior.' +
				    ' Please update the tagtype implementation accordingly.');
		} else {
			Aloha.Log.error('gcn/gcn-plugin',
					' No root tags found.');
		}

		var id = $element.attr('id');
		$element.removeAttr('id');
		$element.removeClass('aloha-block');
		var $wrapper = $('<div>', {
			'class': 'aloha-block-wrapper aloha-block',
			'id': id
		});

		$element.replaceWith($wrapper).appendTo($wrapper);

		$element.find('.aloha-editable').each(function (i, elem) {
			var editable = getEditableById(elem.id);
			if (editable) {
				Aloha.Editable.registerEvents(editable);
			}
		});

		return $wrapper;
	}

	/**
	 * Gets the GCN plugin's settings.
	 *
	 * @FIXME: We need a better approach to reading Aloha settings.
	 * @return {object}
	 */
	function getGCNPluginSettings() {
		return Aloha.GCN.settings;
	}

	/**
	 * Initialize the blocks by calling alohaBlock() on them.
	 *
	 * @param {Array.<object>} blocks
	 * @param {number|string} pageId The id of the page to which the tags
	 *                               belong.
	 * @param {function=} callback A optional function to invoke once all
	 *                             blocks have been processed.
	 */
	function initializeBlocks(blocks, pageId, callback) {
		if (!blocks || 0 === blocks.length) {
			if (typeof callback !== 'undefined') {
				callback();
			}

			return;
		}

		var settings = getGCNPluginSettings();
		var page = GCN.page(pageId);
		var deleteIcon = '/images/system/delete.gif';

		page.constructs(function (constructs) {
			var numBlocksToInitialize = blocks.length;
			var processTag = function (tag, block) {
				--numBlocksToInitialize;

				// Because valid magiclink blocks should not be blockified
				var $element;
				var constructId = tag.prop('constructId');
				var magicLinkId = constructs[GCN.settings.MAGIC_LINK]
					&& constructs[GCN.settings.MAGIC_LINK].constructId;

				if (constructId === magicLinkId) {
					$element = $('#' + block.element);

					if ($element[0].nodeName == 'A') {
						if ($element.hasClass('aloha-block')) {
							$element.removeClass('aloha-block');
						}
					} else {
						Aloha.log(
							'error',
							'gcn-tags',
							'Root element for gtxalohapagelinks must be A but was ' + $element[0].nodeName);
					}

					if (0 === numBlocksToInitialize && callback) {
						callback();
					}

					return;
				}

				var construct;
				var constructName;
				for (constructName in constructs) {
					if (constructs.hasOwnProperty(constructName)
							&& constructs[constructName].constructId
								=== constructId) {
						construct = constructs[constructName];
						break;
					}
				}

				if (!construct) {
					Dialog.alert({
						title : 'Gentics Content.Node',
						text  : 'Cannot determine construct '
						      + tag.prop('constructId')
					});
					if (0 === numBlocksToInitialize && callback) {
						callback();
					}
					return;
				}

				// There should be only one element with a certain ID, but it is
				// possible to create more for example by copying tags in the tag
				// fill dialog. Using this selector instead of '#BLOCK_ID' makes
				// sure we get all of them.
				var $elements = $('.aloha-block[id = ' + block.element + ']')
					.map(function () {
						return ensureCorrectBlockRoot($(this))[0];
					});
				// before blockifying the tag, collect all set attributes
				// all attributes that are added while blockifying will be made
				// ephemeral. Otherwise the check for (user) modifications would
				// always detect those attributes
				var origAttributeNames = [];
				$.each($elements, function (i, elem) {
					origAttributeNames[elem] = [];
					$.each(elem.attributes, function (j, attr) {
						origAttributeNames[elem].push(attr.name);
					});
				});

				$elements.addClass('GENTICS_block')
					.contentEditable(false);

				if (!Aloha.settings.readonly) {
					var options = {
						'aloha-block-type': 'GCNBlock',
						'gcn-tagname': tag.prop('name'),
						'gcn-tagid': tag.prop('id'),
						'gcn-pageid': tag.parent().id(),
						'gcn-constructid': construct.id,
						'gcn-construct-ctl-style': (construct.editorControlStyle || 'ASIDE').toLowerCase(),
						'gcn-construct-ctl-inside': (!!construct.editorControlsInside).toString(),
					};
					$elements.alohaBlock(options);

					// make everything ephemeral, that was added
					$.each($elements, function (i, elem) {
						$.each(elem.attributes, function (j, attr) {
							if (origAttributeNames[elem].indexOf(attr.name) < 0) {
								Ephemera.markAttr($(elem), attr.name);
							}
						});
					});
				}

				// preview forms in block
				if (Aloha.GCN.settings.forms) {
					Aloha.GCN.previewForms($elements);
				}

				if (0 === numBlocksToInitialize) {
					if (callback) {
						callback();
					}
				}
			};

			function loadAndProcessTag(block) {
				page.tag(block.tagname, function (tag) {
					processTag(tag, block);
				});
			}

			var i;

			for (i = 0; i < blocks.length; i++) {
				loadAndProcessTag(blocks[i]);
			}
		});
	}

	/**
	 * Attempts to place the selection into the nearest content editable to the
	 * given container element.
	 *
	 * @param {Range} range Range object for the current selection.
	 */
	function forceSelectionIntoEditable(range) {
		var container = range.startContainer;
		var offset = range.startOffset;
		while (container && !$(container).contentEditable()) {
			offset = Dom.getIndexInParent(container) + 1;
			container = container.parentNode;
		}
		if (container) {
			range.startContainer = range.endContainer = container;
			range.startOffset = range.endOffset = offset;
			range.select();
		}
	}

	/**
	 * Inserts the given element into the DOM.
	 *
	 * @TODO: Pass the range as the second parameter, and don't modify it,
	 *        instead, return a modified copy of the given range.
	 *
	 * @param {HTMLElement<HTMLElement>} $element
	 * @return {boolean} True if element was inserted
	 */
	function insertElement($element) {
		var range = Aloha.Selection.getRangeObject();
		if (!range || typeof range.isCollapsed !== 'function') {
			return false;
		}
		if (!range.isCollapsed()) {
			Dom.removeRange(range);
			range.select();
		}
		if (!$(range.startContainer).contentEditable()) {
			forceSelectionIntoEditable(range);
		}
		var limit = Aloha.activeEditable ? $(Aloha.activeEditable.obj) : null;
		var inserted = Dom.insertIntoDOM($element, range, limit, false, true);
		if (inserted) {
			// Scripts will have their own entry in the set, even if they were
			// defined inside another element.
			$element = $element.not('script');

			var newContainer = null;

			// Padding is only done automatically on .aloha-block elements,
			// but only after the range would be placed inside the tag.
			if (Dom.isBlockNode($element[0])) {
				var editable = $element.closest('.aloha-editable, .aloha-table-cell-editable');

				if (editable.length > 0) {
					Misc.addEditingHelpers(editable);

					var p = $element.next('p');

					if (p.length > 0) {
						newContainer = p[0];
					}
				}
			} else {
				BlockUtils.pad($element);
			}

			if (newContainer == null) {
				newContainer = Dom.searchAdjacentTextNode(
					$element[0].parentNode,
					Dom.getIndexInParent($element[0]) + 1,
					false,
					{},
					{ acceptUntrimmed: true });
			}

			var newOffset = 0;

			if (!newContainer) {
				// No appropriate text node found, so at least try to position
				// the caret somewhere after the tag.
				newContainer = $element[0].parentNode;
				newOffset = Dom.getIndexInParent($element[0]) + 1;
			}

			range.startContainer = range.endContainer = newContainer;
			range.startOffset = range.endOffset = newOffset;
			range.select();

			// At least Mozilla still has the focus on the button that inserted the tag.
			// TODO: Should Range.select() perhaps ensure that the editable
			// actually has the focus?
			var editable = $(range.startContainer).closest('.aloha-editable,.aloha-table-cell-editable');

			if (editable && Aloha.browser.mozilla && document.activeElement !== editable[0]) {
				editable.focus();
			}
		}
		return inserted;
	}

	/**
	 * Because IE7 will sometimes loses the values of attributes of cloned
	 * nodes.  Workaround for RT#51971.
	 *
	 * @param {jQuery.<Element>} $element
	 * @param {String} name
	 * @return {String}
	 */
	function readAttribute($element, name) {
		if (0 === $element.length) {
			return undefined;
		}
		var value = $element.eq(0).attr(name);
		return (
			(Browser.ie7 && typeof value === 'undefined')
				? $(Dom2.outerHtml($element[0])).attr(name)
				: value
		);
	}

	/**
	 * Overwrites the given block element with the incoming HTML content.
	 *
	 * @param {jQuery.<HTMLElement>} $block
	 * @param {HTML} content Markup which will replace the given block.
	 */
	function overwriteElement($element, content) {
		if ($element.length) {
			var $newElement = $(
				'<' + $(content)[0].nodeName + '>',
				{ id: readAttribute($element, 'id') });

			$element.replaceWith($newElement);
			GCN.renderOnto($newElement, content);
		}
	}

	/**
	 * Insert the tag block described by the given data into the DOM.
	 *
	 * @param {object} data An object containing the string property `content`.
	 * @param {function} callback A method to invoke after insertion.
	 * @return {jQuery<HTMLElement>} A jQuery object containing the DOM element
	 *                               that was inserted.
	 */
	function insertTagForEditing(data, callback) {
		var $block = $(data.content);
		// There should be only one element with a certain ID, but it is
		// possible to create more for example by copying tags in the tag
		// fill dialog. Using this selector instead of '#BLOCK_ID' makes
		// sure we get all of them.
		var selector = '.aloha-block[id = ' + $block.attr('id') + ']';
		var $current = $(selector);

		if ($block.hasClass('aloha-block')) {
			$block.contentEditable(false);
		}

		if ($current.length > 0) {
			var currentNodeName = $current[0].nodeName;

			// The placeholder that is inserted by createTag() in gcn-plugin is
			// a span. Inserting the span will _not_ split up the parent tag,
			// but if the actual root element of the rendered tag is a block
			// level element, the parent may need to be split (if it is a p for
			// example). The splitting logic is in insertElement(), so when we
			// already have a block, but it is a span while the new block is not,
			// we remove it completely and call insert again instead of just
			// overwriting the block.
			if (currentNodeName === 'SPAN' && currentNodeName != $block[0].nodeName) {
				$current.remove();
				insertElement($block);
			} else {
				$current.each(function () {
					overwriteElement($(this), data.content);
				});
			}
		} else {
			insertElement($block);
		}

		// Because CropnResize functions would have been detached
		// in overwriteElement()
		if (Aloha.CropNResize) {
			Aloha.CropNResize.attach(
				selector + ' ' + Aloha.CropNResize.settings.selector
			);
		}

		if (callback) {
			callback(data);
		}

		return $(selector);
	}

	/**
	 * Decorates the a rendered tag for edit icons.
	 *
	 * @param {TagAPI} tag The GCN JS API tag object.
	 * @param {object} data The REST API response data.
	 * @param {function} callback Function that will be invoked once all blocks
	 *                            and editables have been initialized.
	 */
	function decorateTagForEditing(tag, data, callback) {
		/*
		 * This function is sometimes called before aloha is ready.
		 * Attempting to create blocks/editables at this point would just break aloha with really strange errors.
		 */
		Aloha.ready(function() {
			var page = tag.parent();
			var pageId = page.id();
			var contained = GCN.PageAPI.trackRenderedTags(page, data);
			var blocks = contained.blocks;
			var editables = contained.editables;
			var editable;
			var i;
	
			for (i = 0; i < editables.length; i++) {
				$('#' + editables[i].element).aloha();
				editable = getEditableById(editables[i].element);
				if (editable && editables[i].readonly) {
					editable.disable();
				}
			}
	
			initializeBlocks(blocks, pageId, callback);
		});
	}

	/**
	 * Appends the html designating a Content.Node tag (as given by the data
	 * parameter) to a selector and renders it for editing by decorating
	 * contained blocks with handles and aloha()fying its editables.
	 *
	 * @param {jQuery.<HTMLElement>|string|HTMLElement} selector
	 *        The HTML element to which the information is appended.
	 * @param {object?} tag The tag object for which the data was fetched.
	 * @param {object} data The content to be rendered for editing.
	 * @param {function=} callback An optional function to be called after
	 *                             decorating blocks.
	 */
	function appendTagForEditing(selector, tag, data, callback) {
		var $element = selector.jquery ? selector : $(selector);
		if ($element.length) {
			$element.append(data.content);
			decorateTagForEditing(tag, data, callback);
		}
	}

	/**
	 * Finds a block element in the document that corresponds to the given
	 * tagid.
	 *
	 * Nearly equivalent to $('.GENTICS_block[data-gcn-tagid=' + tagid + ']'),
	 * except that this function provides a work-around for RT#51971, where IE 7
	 * would sometimes not return the correct values for attributes of a DOM
	 * element.  Cloning the element with jQuery, using the elment's outerHTML
	 * circumvents this problem.
	 *
	 * @param {string} tagid The id of the tag whose block element is to be
	 *                       retrieved.
	 * @return {jQuery.<HTMLElement>|null} A jQuery unit set containing a block
	 *                                     element that corresponds with the
	 *                                     tagid.  Null if no element is found
	 *                                     for the given tag id.
	 */
	function getBlockElementByTagId(tagid) {
		var $block;
		if (!Browser.ie7) {
			$block = $('.GENTICS_block[data-gcn-tagid=' + tagid + ']');
			return $block.length ? $block : null;
		}
		var $blocks = $('.GENTICS_block');
		var i;
		var blockTagId;
		for (i = 0; i < $blocks.length; i++) {
			blockTagId = $blocks.eq(i).attr('data-gcn-tagid');

			// Because IE7 will sometimes loses the values of attributes of
			// cloned nodes.  Workaround for RT#51971.
			if (!blockTagId) {
				$block = $(Dom2.outerHtml($blocks[i]));
				blockTagId = $block.attr('data-gcn-tagid');
			} else {
				$block = $blocks.eq(i);
			}

			if (blockTagId === tagid) {
				return $block;
			}
		}
		return null;
	}

	/**
	 * Find a tag by its id in a give page.
	 *
	 * @param {GCN.PageAPI} page The page object in which to search for the
	 *                           tag.
	 * @param {number} id Id of tag.
	 * @param {function} success Callback function to receive tag.
	 * @param {function} error Custom error handler.
	 */
	function getTagById(page, id, success, error) {
		page.tags(function (tags) {
			id = parseInt(id, 10);
			var tag = null;
			var i;
			for (i = 0; i < tags.length; i++) {
				if (id === tags[i].prop('id')) {
					success(tags[i]);
					return;
				}
			}
			error('Could not find tag id ' + id + ' in page tags.');
		}, error);
	}

	return {
		getBlockElement: getBlockElementByTagId,
		getById: getTagById,
		initializeBlocks: initializeBlocks,
		append: appendTagForEditing,
		insert: insertTagForEditing,
		decorate: decorateTagForEditing
	};
});
