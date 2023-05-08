/*global define: true, GCN: true, setTimeout: true, window: true */
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
	'gcn/gcn-plugin',
	'gcn/gcn-block',
	'gcn/gcn-util',
	'gcn/gcn-tags',
	'ui/dialog',
	'PubSub',
	'util/browser',
	'i18n!link/nls/i18n'
], function (
	Aloha,
	$,
	block,
	gcn,
	GCNBlock,
	Util,
	Tags,
	Dialog,
	PubSub,
	Browser,
	linkI18n
) {
	'use strict';

	/**
	 * Add event handlers to the given link object
	 * @param link object
	 */
	function addLinkEventHandlers(link) {
		// follow link on ctrl or meta + click
		$(link).click(function (e) {
			if (e.metaKey) {
				// blur current editable. user is waiting for the link to load
				Aloha.activeEditable.blur();
				// hack to guarantee a browser history entry
				window.setTimeout(function () {
					location.href = e.target;
				}, 0);
				e.stopPropagation();

				return false;
			} else {
				e.stopPropagation();
				return false;
			}
		});
	}

	/**
	 * Gentics Content.Node block implementation.
	 *
	 * @class
	 * @extends AbstractBlock
	 */
	var GCNFileBlock = GCNBlock.extend({
		/**
		 * Title is used for the panel in the sidebar
		 */
		title: linkI18n.t('floatingmenu.tab.link'),

		/**
		 * This is the anchor tag of the file link block
		 */
		$anchor: null,

		/**
		 * This marks, whether this block is still active.
		 * When the block is about to be removed, this will first be
		 * set to false to prevent any further magic.
		 */
		active: true,

		init: function ($element, postProcessFn) {
			var block = this;
			$element.find('a').each(function () {
				addLinkEventHandlers(this);
				block.$anchor = $(this);
				block.updateFileId();
				return false;
			});

			var gcnFileLinkPlugin = Aloha.require('gcnfilelink/gcnfilelink-plugin');
			var tag = gcn.page.tag(this.attr('gcn-tagname'));
			gcnFileLinkPlugin.fillBlock(this, tag);

			postProcessFn();
		},

		/*
		 * @override
		 */
		renderBlockHandlesIfNeeded: function () {
			// we don't want any block handles
		},

		/*
		 * @override
		 */
		isDraggable: function () {
			// file link blocks shall not be draggable
			return false;
		},

		/*
		 * @override
		 */
		getSchema: function () {
			var gcnFileLinkPlugin = Aloha.require('gcnfilelink/gcnfilelink-plugin');
			return gcnFileLinkPlugin.getSchema();
		},

		/*
		 * @override
		 */
		update: function ($element, postProcessFn) {
			// don't reload the block if it is about to be removed
			if (!this.active) {
				return;
			}
			var gcnFileLinkPlugin = Aloha.require('gcnfilelink/gcnfilelink-plugin');
			var range = Aloha.Selection.getRangeObject();
			// set the file id to the tag
			var tag = gcn.page.tag(this.attr('gcn-tagname'));
			var block = this, reSelect = false, fileId = this.fileId();

			// check whether the file property is changed
			if (tag.part('file') !== fileId) {
				// set the file property correctly
				tag.part('file', fileId);

				// only if the file changed (that means, the block was edited using the floating menu)
				// we will reselect the reloaded block.
				// otherwise (i.e. when the block was edited using the sidebar), we will not reselect
				// the block, because then the sidebar would lose the focus
				reSelect = true;
			}

			// set the other changeable properties
			gcnFileLinkPlugin.fillTag(tag, this);

			// save the tag and reload it, so that it will correctly reflect the changed file id
			gcn.page.save({createVersion: false}, function () {
				gcn.page.tag(block.attr('gcn-tagname')).edit(function (html, tag, data) {
					gcn.handleBlock(data, false, function () {
						Tags.decorate(tag, data);

						var $newBlockElement = $('[data-gcn-tagid=' + tag.prop('id') + ']');
						// set the selection into the first anchor found in the block
						$newBlockElement.find('a').each(function () {
							block.$anchor = $(this);
							return false;
						});
						if (reSelect) {
							block.select(range.startOffset, range.endOffset);
							block.activate();
						}
						
					}, html);
				});
			}, function () {
			});

			postProcessFn();
		},

		/**
		 * Update the file id from the attribute of the anchor
		 */
		updateFileId: function () {
			if (!this.active) {
				return;
			}
			this.attr('file', this.$anchor.attr('data-gentics-aloha-object-id'));
		},

		/**
		 * Get the file id
		 * @return {Integer} file id
		 */
		fileId: function () {
			var fileId, linkData;

			linkData = this.attr('file');

			var idParts = linkData.split('.');

			if (idParts.length === 2 && (idParts[0] === '10008' || idParts[0] === '10011')) {
				fileId = parseInt(idParts[1], 10);
			}

			return fileId;
		},

		/**
		 * Wrapper for unblock() that first sets active to false
		 */
		doUnblock: function () {
			this.active = false;
			this.unblock();
		},

		/**
		 * Select the block at the given offsets of the anchor tag's first child
		 * @param {Integer} startOffset start offset
		 * @param {Integer} endOffset end offset
		 */
		select: function (startOffset, endOffset) {
			var range = Aloha.Selection.getRangeObject();
			var anchor;
			if (!this.$anchor) {
				return;
			}
			anchor = this.$anchor.get(0);
			if (anchor.firstChild) {
				range.startContainer = anchor.firstChild;
				range.endContainer = anchor.firstChild;
				range.startOffset = Math.min(startOffset, range.startContainer.length);
				range.endOffset = Math.min(endOffset, range.endContainer.length);
			} else {
				range.startContainer = range.endContainer = anchor;
				range.startOffset = range.endOffset = 0;
			}
			range.select();
		}
	});

	return GCNFileBlock;
});
