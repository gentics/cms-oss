/*global define: true, window: true */
/*!
* Aloha Editor
* Author & Copyright (c) 2011 Gentics Software GmbH
* aloha-sales@gentics.com
* Licensed under the terms of http://www.aloha-editor.com/license.html
*/

define(
[
	'jquery',
	'aloha/plugin', 
	'i18n!gcnfilelink/nls/i18n',
	'i18n!link/nls/i18n',
	'block/blockmanager',
	'gcnfilelink/gcn-fileblock',
	'aloha/console',
	'PubSub',
	'link/link-plugin',
	'gcn/gcn-plugin',
	'gcn/gcn-tags',
	'util/dom'
],
function (
	jQuery,
	Plugin,
	i18n,
	linkI18n,
	BlockManager,
	GCNFileBlock,
	Console,
	PubSub,
	LinkPlugin,
	GCNPlugin,
	Tags,
	Dom
) {
	"use strict";
	
	var $ = jQuery,
		GCN = window.GCN,
		GENTICS = window.GENTICS,
		Aloha = window.Aloha;

	/**
	 * Get the fileblock, the given object is part of.
	 *
	 * @param {jQuery} obj object
	 * @return {GCNFileBlock} block or false
	 */
	function getFileBlock(obj) {
		if (!obj) {
			return false;
		}

		var $blockElement = obj.closest('.aloha-block');

		if ($blockElement.size() == 0) {
			return false;
		}

		var block = BlockManager.getBlock($blockElement);

		if (!block) {
			return false;
		}

		if (block.attr('aloha-block-type') !== 'GCNFileBlock') {
			return false;
		}

		return block;
	}

	/**
	 * Check whether the given item is a file.
	 *
	 * @param {jQuery} obj The object to check.
	 * @return {Boolean|String} If the given object is a file
	 *		(or an image) the result is the ID of the object,
	 *		otherwise this function returns <code>false</code>.
	 */
	function getFileId(obj) {
		var objId = obj.attr('data-gentics-aloha-object-id');

		if (!objId) {
			return false;
		}

		var objIdParts = objId.split('.');

		if (objIdParts.length != 2
				|| (objIdParts[0] !== '10008' && objIdParts[0] !== '10011')) {
			return false;
		}

		return objIdParts[1];
	}

	/**
	 * Get the construct, that is configured to be the filelink construct
	 * @param {Objects} constructs list of constructs
	 * @param {String] construct ID or keyword
	 * @return {Object} file link construct or false if not found
	 */
	function getFileLinkConstruct(constructs, construct) {
		var c, tmpConstruct;

		tmpConstruct = constructs[construct];
		if (tmpConstruct) {
			return tmpConstruct;
		}

		for (c in constructs) {
			if (constructs.hasOwnProperty(c)) {
				tmpConstruct = constructs[c];

				if (tmpConstruct.id == construct) {
					return tmpConstruct;
				}
			}
		}

		return false;
	}

	/**
	 * Remove the given file block.
	 *
	 * @param {FileBlock} fileBlock The block to remove.
	 * @param {Boolean|Object} linkRemoveEvent The <code>aloha.link.remove</code> 
	 *	event that caused removal of this block, or <code>false</code> if the
	 *	block is removed, because the link does no longer point to a file.
	 */
	function removeFileBlock(fileBlock, linkRemoveEvent) {
		if (!fileBlock) {
			return;
		}

		if (typeof linkRemoveEvent == 'undefined') {
			linkRemoveEvent = false;
		}

		// find and remove the tag
		var tag = GCNPlugin.page.tag(fileBlock.$element.attr('data-gcn-tagname'));

		tag.remove();

		// remove the block (or replace with the link text)
		// it is important to use the custom doUnblock() method, because it
		// will "mark" the block as being unblocked, which will prevent
		// bad things happening in update(), etc.
		fileBlock.doUnblock();

		if (linkRemoveEvent) {
			var textNode = fileBlock.$element.after(linkRemoveEvent.text).get(0).nextSibling;
			var range = Aloha.Selection.getRangeObject();

			range.startContainer = range.endContainer = textNode;
			range.startOffset = Math.min(linkRemoveEvent.range.startOffset, textNode.length);
			range.endOffset = Math.min(linkRemoveEvent.range.endOffset, textNode.length);
			range.select();
		} else {
			var copyAtt;
			var $anchor = fileBlock.$element.find('a');
			var $newAnchor = $('<a>', { text: $anchor.text(), class: 'aloha-new-link' });
			var copyAttrs = ['data-gentics-aloha-repository', 'data-gentics-aloha-object-id', 'title', 'target', 'href'];

			for (copyAtt in copyAttrs) {
				if (copyAttrs.hasOwnProperty(copyAtt)) {
					$newAnchor.attr(copyAttrs[copyAtt], $anchor.attr(copyAttrs[copyAtt]));
				}
			}

			fileBlock.$element.after($newAnchor);
		}

		fileBlock.$element.remove();
	}

	/**
	 * Update the file ID of the given block or remove
	 * it if necessary.
	 *
	 * @param {FileBlock} block The block to check.
	 */
	function checkBlock(block) {
		if (!block) {
			return;
		}

		if (block.$anchor.attr('data-gentics-aloha-object-id')) {
			block.updateFileId();
		} else {
			removeFileBlock(block);
		}
	}

	/**
	 * Create the plugin
	 */
	return Plugin.create('gcnfilelink', {

		/**
		 * Configure the available languages (i18n) for this plugin
		 */
		languages: ['en', 'de'],

		/**
		 * Construct which is used for the file link blocks
		 */
		construct: null,

		/**
		 * Initialize the plugin
		 */
		init: function () {
			var plugin = this;

			// check whether a construct has been configured
			if (!this.settings.construct) {
				Console.error(this, 'No construct has been configured. Plugin will not be available.');
				return;
			}

			// load the file link construct
			GCN.Admin.constructs(function (constructs) {
				plugin.construct = getFileLinkConstruct(constructs, plugin.settings.construct);
			});

			BlockManager.registerBlockType('GCNFileBlock', GCNFileBlock);

			// Check all fileblocks when the editable is deactivated, whether
			// they should be updated or removed.
			PubSub.sub('aloha.editable.deactivated', function () {
				$('.aloha-block[data-aloha-block-type=GCNFileBlock]').each(function () {
					checkBlock(getFileBlock($(this)));
				});
			});

			// the detection whether or not we need a fileblock for the selected
			// item will be made in the handler of the aloha.link.changed event.
			// Here we just replace the default link text.
			PubSub.sub('gcn.repository.item.selected', function (event) {
				var $link = event.obj;

				// when the linkText is the default link text for new links, we replace it by the filename
				if ($link.text() === linkI18n.t('newlink.defaulttext')) {
					$link.text(event.repositoryItem.name);
				}
			});

			// when a link is removed and was part of a file block, we need to
			// remove the file block as well
			PubSub.sub('aloha.link.remove', function (event) {
				removeFileBlock(getFileBlock($(event.range.startContainer)), event);
			});

			// When a link changes we need to check whether it was changed from
			// a file to a page or external link or the other way round and add
			// or remove the filelink block accordingly.
			PubSub.sub('aloha.link.changed', function (event) {
				if (plugin.lastLink && !plugin.lastLink.is(event.element)) {
					checkBlock(getFileBlock(plugin.lastLink));
				}

				plugin.lastLink = event.element;

				if (!event.element) {
					return;
				}

				var tag;
				var fileBlock = getFileBlock(event.element);
				var fileId = getFileId(event.element);

				if (fileBlock) {
					if (fileId) {
						// update the block
						fileBlock.updateFileId();
					} else {
						// the block is a file block, and the selected item is no file any more,
						// so we need to transform this block into a normal link
						removeFileBlock(fileBlock);
					}
				} else if (fileId) {
					// Ignore this link change event, when we are already converting it
					// to a filelink.
					if (event.element.attr('data-gcn-filelink-converting')) {
						return;
					}

					// the link is not contained in a file block, but a file has been
					// selected now, so we need to transform the link into a file block

					var linkText = event.element.text();
					var success = function (html, tag, data, frontendEditing) {
						var $link = $(event.element);
						var range = Aloha.Selection.getRangeObject();
						// this is kind of a hack: we replace the link by a placeholder
						// which is a span having the block id. The gcn plugin will later
						// replace this placeholder with the rendered block.
						// This placeholder must already "look like an aloha block".
						var $placeHolder = $('<span>', { id: $(html).attr('id'), class: 'aloha-block' });

						$link.after($placeHolder);
						$link.remove();

						// let the gcn plugin handle the block, which will insert it
						// instead of the placeholder
						GCN.PageAPI.trackRenderedTags(tag.parent(), data);
						GCNPlugin.handleBlock(data, true, function () {
							Tags.decorate(tag, data);

							// set the selection into the file block
							var block = getFileBlock(Tags.getBlockElement(tag.prop('id')));

							if (block) {
								block.select(range.startOffset, range.endOffset);
							}

							event.element.removeAttr('data-gcn-filelink-converting');
						}, html);
					};

					event.element.attr('data-gcn-filelink-converting', 'true');

					// prepare data for creating the construct
					var data = { magicValue: linkText };

					// the set construct can either be the id (if it is numeric)
					// or the keyword (if it is not numeric)
					if (isNaN(parseInt(plugin.settings.construct, 10))) {
						data.keyword = plugin.settings.construct;
					} else {
						data.constructId = plugin.settings.construct;
					}

					GCNPlugin.page.createTag(data, function (tag) {
						// after creating the tag, we set the file id, save the tag
						// and make it editable (which will insert it at the placeholder
						// created before)
						tag.part('file', fileId);
						tag.save(function (tag) {
							tag.edit(success);
						});
					});
				}
			});
		},

		/**
		 * Get the schema for editing blocks in the sidebar
		 * @return {Object} schema object
		 */
		getSchema: function () {
			var schema = {}, part;
			if (this.construct) {
				for (part in this.construct.parts) {
					if (this.construct.parts.hasOwnProperty(part)) {
						part = this.construct.parts[part];
						if (part.keyword === 'title') {
							schema[part.keyword] = {
								label: part.name,
								fieldsetLabel: true,
								type: 'string'
							};
						} else if (part.keyword === 'class') {
							schema[part.keyword] = {
								label: part.name,
								fieldsetLabel: true,
								type: 'string'
							};
						} else if (part.keyword === 'target') {
							schema[part.keyword] = {
								label: part.name,
								fieldsetLabel: true,
								type: 'radio',
								values: [
									{
										key: '_self',
										label: i18n.t('gcnfilelink.target.self')
									},
									{
										key: '_blank',
										label: i18n.t('gcnfilelink.target.blank')
									},
									{
										key: '_parent',
										label: i18n.t('gcnfilelink.target.parent')
									},
									{
										key: '_top',
										label: i18n.t('gcnfilelink.target.top')
									}
								]
							};
						}
					}
				}
			}
			return schema;
		},

		/**
		 * Fill the attributes from the block into the tag
		 * @param {Object} tag
		 * @param {Object} block
		 */
		fillTag: function (tag, block) {
			var part;
			if (this.construct) {
				for (part in this.construct.parts) {
					if (this.construct.parts.hasOwnProperty(part)) {
						part = this.construct.parts[part];
						if (part.keyword === 'title' || part.keyword === 'class' || part.keyword === 'target') {
							tag.part(part.keyword, block.attr(part.keyword));
						}
					}
				}
			}
		},

		/**
		 * Fill the attributes from the tag into the block
		 * @param {Object} block
		 * @param {Object} tag
		 */
		fillBlock: function (block, tag) {
			var part;
			if (this.construct) {
				for (part in this.construct.parts) {
					if (this.construct.parts.hasOwnProperty(part)) {
						part = this.construct.parts[part];
						if (part.keyword === 'title' || part.keyword === 'class' || part.keyword === 'target') {
							block.attr(part.keyword, tag.part(part.keyword));
						}
					}
				}
			}
		},

		getFileBlock: getFileBlock
	});
});
