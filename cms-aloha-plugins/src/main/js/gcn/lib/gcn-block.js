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
	'PubSub',
	'aloha/ephemera',
	'block/block',
	'gcn/gcn-util',
	'gcn/gcn-tags',
	'ui/dialog',
	'i18n!gcn/nls/i18n',
], function (
	Aloha,
	$,
	PubSub,
	Ephemera,
	Block,
	Util,
	Tags,
	Dialog,
	i18n,
) {
	'use strict';

	var STYLE_BLOCK_CONTENT_HEIGHT = '--gtx-block-content-height';
	var STYLE_BLOCK_CONTENT_WIDTH = '--gtx-block-content-width';
	var STYLE_BLOCK_HANDLE_HEIGHT = '--gtx-block-handle-height';
	var STYLE_BLOCK_HANDLE_WIDTH = '--gtx-block-handle-width';

	Ephemera.ephemera().pruneFns.push(function(node) {
		if (node.style) {
			node.style.removeProperty(STYLE_BLOCK_CONTENT_HEIGHT);
			node.style.removeProperty(STYLE_BLOCK_CONTENT_WIDTH);
			node.style.removeProperty(STYLE_BLOCK_HANDLE_HEIGHT);
			node.style.removeProperty(STYLE_BLOCK_HANDLE_WIDTH);

			// If no inline-styles are applied, remove the entire attribute
			if (node.style.length === 0) {
				node.removeAttribute('style');
			}
		}
		// Same goes for now empty class attributes
		if (node.classList) {
			if (node.classList.length === 0) {
				node.removeAttribute('class');
			}
		}
		return node;
	});

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
			Util.getConstructFromId(tag.prop('constructId')).then(function (construct) {
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
							sourceTagname: srcTagname,
							constructId: construct.id,
							keyword: construct.keyword,
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

	function doRenderBlockHandles(block, editable) {
		var $block = block.$element;
		// Check whether the handles have already been added (only
		// do add them "IfNeeded").
		if ($block.children('.aloha-construct-buttons-container').length) {
			var editableHost = Aloha.getEditableHost($block);

			PubSub.pub('gcn.block.handles-available', {
				host: editableHost,
				block: block,
				$el: $block,
			});

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

			var dragHandle$ = $('<div>', {
				class: 'gcn-construct-drag-handle aloha-block-handle',
			}).append($('<i>', {
				class: 'material-symbols-outlined aloha-block-button-icon',
				text: 'drag_pan'
			})).appendTo($blockHandleContainer);

			if (!$block.hasClass('ui-draggable-disabled')) {
				dragHandle$.addClass('aloha-block-draghandle');
			}
		}

		if (editable) {
			var $editButton = $('<button>', {
				class: 'gcn-block-button gcn-construct-button-edit',
				title: i18n.t('edit.msg').replace('$1', tagname || ''),
			}).append($('<i>', {
				class: 'material-symbols-outlined aloha-block-button-icon',
				text: 'edit'
			})).click(function ($event) {
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
		}

		if (canBeDeleted) {
			var $deleteButton = $('<button>', {
				class: 'gcn-block-button gcn-construct-button-delete',
				title: i18n.t('delete.msg').replace('$1', tagname || ''),
			}).append($('<i>', {
				class: 'material-symbols-outlined aloha-block-button-icon',
				text: 'delete'
			})).click(function ($event) {
				block.confirmedDestroy(function () {
					block.deleteInstance();
				}, $block.attr('data-gcn-tagname'));
				$event.preventDefault();
				return false;
			});
			$blockHandleContainer.append($deleteButton);
		}

		setTimeout(function () {
			$blockHandleContainer.show();
		}, 0);

		$block.prepend($blockHandleContainer);
		Ephemera.markElement($blockHandleContainer);

		function setSizeProperties() {
			$block.css(STYLE_BLOCK_CONTENT_HEIGHT, $block.height() + 'px');
			$block.css(STYLE_BLOCK_CONTENT_WIDTH, $block.width() + 'px');
			$block.css(STYLE_BLOCK_HANDLE_HEIGHT, $blockHandleContainer.height() + 'px');
			$block.css(STYLE_BLOCK_HANDLE_WIDTH, $blockHandleContainer.width() + 'px');
		}
		block._observer = new ResizeObserver(setSizeProperties);
		block._observer.observe($block[0]);
		block._observer.observe($blockHandleContainer[0]);
		setSizeProperties();

		// Additional double-click handler for click type construct blocks.
		$block.on('dblclick', function($event) {
			var type = $block.attr('data-gcn-construct-ctl-style');
			if (type !== 'click') {
				return;
			}

			Aloha.GCN.openTagFill(
				$block.attr('data-gcn-tagid'),
				$block.attr('data-gcn-pageid'),
				{ withDelete: canBeDeleted }
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

		var editableHost = Aloha.getEditableHost($block);
		if (editableHost) {
			editableHost.smartContentChange({type: 'block-change'});
		}

		PubSub.pub('gcn.block.handles-available', {
			host: editableHost,
			block: block,
			$el: $block,
		});
	}

	/**
	 * Gentics Content.Node block implementation.
	 *
	 * @class
	 * @extends AbstractBlock
	 */
	var GCNBlock = Block.AbstractBlock.extend({

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
						block.deleteInstance();
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

		destroy: function(force) {
			if (this._observer) {
				this._observer.disconnect();
				this._observer = null;
			}

			this._super(force);
		},

		deleteInstance: function() {
			deleteBlock(this);
		},

		/*
		 * @override
		 */
		renderBlockHandlesIfNeeded: function () {
			var block = this;
			var $block = block.$element;

			Util.getConstructFromId($block.attr('data-gcn-constructid')).then(function (construct) {
				const needsEditButton = construct.parts.some(part => !part.hideInEditor && part.editable && !part.liveEditable)
				doRenderBlockHandles(block, needsEditButton);
			}, function (error) {
				console.log(error);
				doRenderBlockHandles(block, true);
			});
		},

		/**
		 * Function to prompt the user if they really wish to delete the current tag.
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
