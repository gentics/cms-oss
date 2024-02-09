/*global define: true, GCN: true, setTimeout: true */
/*!
 * Aloha Editor
 * Author & Copyright (c) 2010-2012 Gentics Software GmbH
 * aloha-sales@gentics.com
 * Licensed unter the terms of http://www.aloha-editor.com/license.html
 */
define([
	'aloha/core',
	'jquery',
	'block/block',
	'block/blockmanager',
	'gcn/gcn-plugin',
	'gcn/gcn-util',
	'gcn/gcn-tags',
	'ui/dialog',
	'i18n!gcn/nls/i18n',
	'PubSub',
	'util/browser'
], function (
	Aloha,
	$,
	block,
	BlockManager,
	gcn,
	Util,
	Tags,
	Dialog,
	i18n,
	PubSub,
	Browser
) {
	'use strict';

	/**
	 * Returns an element that represents the tag's corresponding DOM element.
	 *
	 * Beware that the returned element may not be the tag's actual DOM
	 * element.
	 *
	 * This function provides a work-around for #51971, where IE 7 would
	 * sometimes not return the correct values for attributes of a DOM
	 * element. Cloning the element with jQuery, using the elment's outerHTML
	 * circumvents this problem.
	 *
	 * @param {GCNBlock} block A block instance.
	 * @return {jQuery.<HTMLElement>} A jQuery object containing a DOM element
	 *                                that is either the block's DOM element or
	 *                                a copy of it.
	 */
	function getBlockElement(block) {
		return (
			(Browser.ie7 && !block.$element.attr('data-gcn-tagid'))
				? $(block.$element[0].outerHTML)
				: block.$element
		);
	}

	/**
	 * Get the page object with given Id stored in the plugin or
	 * retrieve one from the CMS.
	 *
	 * @param pageId the pageId to get
	 * @param settings an settings object used if a new page is requested (otherwise this will be ignored)
	 * @returns {GCN.Page} a page object
	 */
	function getPageFromPlugin(pageId, settings) {
		var gcnPlugin = Aloha.require('gcn/gcn-plugin');
		if (gcnPlugin.page && gcnPlugin.page.id() === parseInt(pageId, 10)) {
			return gcnPlugin.page;
		} else {
			return GCN.page(pageId, settings);
		}
	}

	/**
	 * Deletes a block and removes its corresponding DOM element from the
	 * document body.
	 *
	 * @param {GCNBlock} block A GCN Block.
	 */
	function deleteBlock(block) {
		// There should be only one element with a certain ID, but it is
		// possible to create more for example by copying tags in the tag
		// fill dialog. Using this selector instead of '#BLOCK_ID' makes
		// sure we get all of them.
		var blocks = $('.aloha-block[id = ' + block.$element.attr('id') + ']');

		// Only delete the tag from the page, if this block is the last
		// instance of this tag.
		if (blocks.length == 1 && blocks.is(block.$element)) {
			getPageFromPlugin(block.$element.attr('data-gcn-pageid'))
				.tag(block.$element.attr('data-gcn-tagname'))
				.remove();
		}
		
		block.unblock();
		block.$element.remove();
	}

	/**
	 * Checks whether or not the given block is a copied block.
	 *
	 * @param {GCNBlock} block
	 * @return {boolean} True if the block is a copy of another.
	 */
	function isCopy(block) {
		return 'true' === block.$element.attr('data-gcn-copy');
	}

	/**
	 * Gets the construct matching the specified construct id from the given
	 * node or page.
	 *
	 * @param {NodeAPI|PageAPI} object The object for which to query for the
	 *                                 construct.
	 * @param {number} constructId The id of the construct to be retreived.
	 * @param {function(object)} success Callback function that will recieved
	 *                                  the successfully retreived construct.
	 * @param {function} error Function that will be invoked if construct
	 *                         cannot be retreived.
	 */
	function getConstruct(object, constructId, success, error) {
		object.constructs(function (constructs) {
			var keyword;
			for (keyword in constructs) {
				if (constructs.hasOwnProperty(keyword) &&
						constructs[keyword].id === constructId) {
					success(constructs[keyword]);
					return;
				}
			}
			error();
		}, error);
	}

	/**
	 * Checks whether a construct is permitted inside an editable based on the
	 * editable's white-list configuration.
	 *
	 * @param {object} construct
	 * @param {object} config
	 * @return {boolean} True if the given construct is permitted.
	 */
	function isAllowed(construct, config) {
		if (config && config.tagtypeWhitelist) {
			return $.inArray(construct.keyword, config.tagtypeWhitelist) >= 0;
		}
		return true;
	}

	/**
	 * Copies a tag and swaps the block's DOM element in the document and on
	 * the object.
	 *
	 * It is very important that we "edit()" this tag because only then will
	 * the tag's block(s) be tracked with he page object!  If the tag is not
	 * rendered for editing in this way, it will not be saved properly when
	 * changes to the page are persisted.
	 *
	 * @param {Aloha.Block} block The block with the $element to be replaced
	 * @param {object} options The information about the source tag from which
	 *                         the copy will be derived.  Should contain the
	 *                         properties: `sourcePageId' and `sourceTagname'.
	 * @param {function} success Callback function that will be invoked once
	 *                           the tag is successfully copied.
	 * @param {function} error Function that will be invoked should an error
	 *                         arise during the copy process.
	 */
	function copyTagInto(block, options, success, error) {
		var gcnPlugin = Aloha.require('gcn/gcn-plugin');
		gcnPlugin.page.createTag(options, function (copy) {
			// determine the root tag to be used for the placeholder
			// it will be the root tag of the block, or div as a fallback
			var rootTag = 'div';
			if (block && block.$element && block.$element.length > 0) {
				rootTag = block.$element[0].nodeName;
			}
			var $placeholder = $('<' + rootTag + '>');
			copy.edit($placeholder, function (html, tag, data) {
				var selector = '[data-gcn-tagname=' + copy.prop('name') + ']';
				// If the placeholder was appropriated as the root element
				// during rendering, then use it.  Otherwise find the rendered
				// tag inside the placeholder.
				var $block = $placeholder.is(selector)
				           ? $placeholder
				           : $placeholder.find(selector).remove();
				block.$element.after($block).remove();
				block.$element = $block;
				Tags.decorate(tag, data, success);
			}, error);
		}, error);
	}

	/**
	 * Initializes a copied block.
	 *
	 * Will create a tag in the page which is currently being edited, and will
	 * replace the given block's element with the contents of the newly created
	 * tag.
	 *
	 * @param {GCNBlock} block The copied block that is to be initialized.
	 * @param {function} success Callback function that will be called once
	 *                           the block is successfully copied and
	 *                           initialized.
	 * @param {function} error Function that will be invoked upon any error
	 *                         during the copying or initialization of the
	 *                         block.
	 */
	function copyBlock(block, success, error) {
		var $block = block.$element;
		var srcPageId = $block.attr('data-gcn-copy-pageid');
		var srcTagname = $block.attr('data-gcn-copy-tagname');
		var editableHost = Aloha.getEditableHost($block);
		var srcPage = getPageFromPlugin(srcPageId, {update: false});
		// Enable tag copying for the top-most editable in which this block is
		// rendered.
		$(editableHost.obj).attr('data-gcn-copy', 'true');

		srcPage.tag(srcTagname, function (tag) {
			getConstruct(srcPage, tag.prop('constructId'), function (construct) {
				var gcnPlugin = Aloha.require('gcn/gcn-plugin');
				var $editable = $block.closest('.aloha-editable');
				var config = gcnPlugin.getEditableConfig($editable);
				var allowed = !!(construct && construct.mayBeSubtag
						&& isAllowed(construct, config));

				if (allowed && $editable) {
					copyTagInto(
						block,
						{
							sourcePageId: srcPageId,
							sourceTagname: srcTagname
						},
						success,
						error // Tag not copied
					);
				} else {
					error(); // Tag not permitted
				}
			}, error); // Construct not found
		}, error); // Tag not found
	}

	/**
	 * Gentics Content.Node block implementation.
	 *
	 * @class
	 * @extends AbstractBlock
	 */
	var GCNBlock = block.AbstractBlock.extend({

		/**
		 * Initializes a GCN block.  If annotations on the block indicate that
		 * it is a copied block, a new tag will be created on page that is
		 * currently in edit mode, and the tag will be rendered into the block
		 * element in the document.
		 *
		 * @override
		 */
		init: function ($element, postProcessFn) {
			var block = this;
			if (!isCopy(block)) {
				Aloha.bind('aloha-editable-delete', function (_, event) {
					var $el = $(event.element)
					if (!$el.is(block.$element)) {
						return;
					}
					event.preventDefault();
					var name = $el.attr('data-gcn-tagname');
					block.confirmedDestroy(function() {
						deleteBlock(block);
					}, name);
				});

				postProcessFn();
				return;
			}

			copyBlock(block, postProcessFn, function () {
				block.destroy();

				// Since `gcn-block-handled' is not triggered when a
				// failure happens, it is necessary to check if we are
				// finished copying here.
				Util.finishedCopyingBlock(Aloha.getEditableHost($element));
			});
		},

		/*
		 * @override
		 */
		renderBlockHandlesIfNeeded: function () {
			var block = this;
			var $block = getBlockElement(block);

			// Check whether the handles have already been added (only
			// do add them "IfNeeded").
			if ($block.children('.aloha-construct-buttons-container').length) {
				return;
			}

			// Do not render block handles for blocks that are currently copied
			if (!$block.attr('data-gcn-constructid')) {
				return;
			}

			var canBeDeleted = block.shouldDestroy();

			var tagname = $block.attr('data-gcn-tagname');

			// Escape possible html elements and quotes in construct name
			var constructname = $('<div/>').text($block.attr('data-gcn-i18n-constructname')).html().replace(/"/g, '&quot;');
			if (tagname && constructname) {
				tagname += ' (' + constructname + ')';
			}

			var $blockHandleContainer = $('<span>', {
				class: 'aloha-block-handle aloha-construct-buttons-container aloha-cleanme', 
				title: tagname,
			});

			if (block.isDraggable()) {
				// Make the block dragable on default
				$blockHandleContainer.addClass('aloha-block-draghandle');

				$('<div>', {
					class: 'gcn-construct-drag-handle aloha-block-handle',
				}).append($('<i>', {
					class: 'material-symbols-outlined aloha-block-button-icon',
					text: 'drag_pan'
				})).appendTo($blockHandleContainer);
			}

			var $editButton = $('<button>', {
				class: 'gcn-block-button gcn-construct-button-edit',
				title: i18n.t('edit.msg').replace('$1', tagname || ''),
			}).append($('<i>', {
				class: 'material-symbols-outlined aloha-block-button-icon',
				text: 'edit'
			})).click(function ($event) {
				var $block = getBlockElement(block);
				Aloha.GCN.openTagFill(
					$block.attr('data-gcn-tagid'),
					$block.attr('data-gcn-pageid')
				);

				// Aloha Blocks sets the
				// `Aloha.Selection.preventSelectionChangedFlag' when a click
				// event is initialized on a block element.  This is done in
				// order to be able to select nested blocks.  When the click
				// event is triggered by cause we clicked on the edit button
				// inside the block, however, we must reset the flag so that
				// when the TagFill modal is closed, the next
				// `aloha-selection-changed' event will be processed as needed.
				//
				// See rt#51554
				Aloha.Selection.preventSelectionChangedFlag = false;

				$event.preventDefault();
				return false;
			});

			$blockHandleContainer.append($editButton);

			if (canBeDeleted) {
				var $deleteButton = $('<button>', {
					class: 'gcn-block-button gcn-construct-button-delete',
					title: i18n.t('delete.msg').replace('$1', tagname || ''),
				}).append($('<i>', {
					class: 'material-symbols-outlined aloha-block-button-icon',
					text: 'delete'
				})).click(function ($event) {
					block.confirmedDestroy(function () {
						deleteBlock(block);
					}, block.$element.attr('data-gcn-tagname'));
					$event.preventDefault();
					return false;
				});
				$blockHandleContainer.append($deleteButton);
			}

			setTimeout(function () {
				$blockHandleContainer.show();
			}, 0);

			block.$element.prepend($blockHandleContainer);
			var editableHost = Aloha.getEditableHost(block.$element);
			if (editableHost) {
				editableHost.smartContentChange({type: 'block-change'});
			}
		},

		/**
		 * Function which is executed when a user tries to delete a block with the keyboard
		 * 
		 * @param {Function} destroyFn function, that is executed, if the block shall be destroyed
		 * @param {String} name Name of the tag
		 */
		confirmedDestroy: function (destroyFn, name) {
			Dialog.confirm({
				title: 'Gentics CMS',
				text: i18n.t('delete.confirm')
					.replace('$1', typeof name !== 'string' || name.trim().length < 1 ? '' : ' "' + name + '"'),
				yes: function () {
					destroyFn();
				}
			});
		},
	});

	return GCNBlock;
});
