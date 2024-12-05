/*jslint regexp: false */
(function (GCN) {

	'use strict';

	/**
	 * @private
	 * @const
	 * @type {object.<string, boolean>} Default page settings.
	 */
	var DEFAULT_SETTINGS = {
		// Load folder information
		folder: true,

		// Lock page when loading it
		update: true,

		// Have language variants be included in the page response.
		langvars: true,

		// Have page variants be included in the page response.
		pagevars: true
	};

	/**
	 * Match URL to anchor
	 *
	 * @const
	 * @type {RegExp}
	 */
	var ANCHOR_LINK = /([^#]*)#(.*)/;

	/**
	 * Checks whether the given tag is a magic link block.
	 *
	 * @param {TagAPI} tag A tag that must already have been fetched.
	 * @param {Object} constructs Set of constructs.
	 * @return {boolean} True if the given tag's constructId is equal to the
	 *                   `MAGIC_LINK' value.
	 */
	function isMagicLinkTag(tag, constructs) {
		return !!(constructs[GCN.settings.MAGIC_LINK]
					&& (constructs[GCN.settings.MAGIC_LINK].constructId
						=== tag.prop('constructId')));
	}

	/**
	 * Given a page object, returns a jQuery set containing DOM elements for
	 * each of the page's editable that is attached to the document.
	 *
	 * @param {PageAPI} page A page object.
	 * @return {jQuery.<HTMLElement>} A jQuery set of editable DOM elements.
	 */
	function getEditablesInDocument(page) {
		var id;
		var $editables = jQuery();
		var editables = page._editables;
		for (id in editables) {
			if (editables.hasOwnProperty(id)) {
				$editables = $editables.add('#' + id);
			}
		}
		return $editables;
	}

	/**
	 * Returns all editables associated with the given page that have been
	 * rendered in the document for editing.
	 *
	 * @param {PageAPI} page
	 * @return {object} A set of editable objects.
	 */
	function getEditedEditables(page) {
		return page._editables;
	}

	/**
	 * Derives a list of the blocks that were rendered inside at least one of
	 * the given page's edit()ed editables.
	 *
	 * @param {PageAPI} page Page object.
	 * @return {Array.<object>} The set of blocks contained in any of the
	 *                          page's rendered editables.
	 */
	function getRenderedBlocks(page) {
		var editables = getEditedEditables(page);
		var id;
		var renderedBlocks = [];
		for (id in editables) {
			if (editables.hasOwnProperty(id)) {
				if (editables[id]._gcnContainedBlocks) {
					renderedBlocks = renderedBlocks.concat(
						editables[id]._gcnContainedBlocks
					);
				}
			}
		}
		return renderedBlocks;
	}

	/**
	 * Gets the DOM element associated with the given block.
	 *
	 * @param {object} block
	 * @return {?jQuery.<HTMLElement>} A jQuery unit set of the block's
	 *                                 corresponding DOM element, or null if no
	 *                                 element for the given block exists in
	 *                                 the document.
	 */
	function getElement(block) {
		var $element = jQuery('#' + block.element);
		return $element.length ? $element : null;
	}

	/**
	 * Retrieves a jQuery set of all link elements that are contained in
	 * editables associated with the given page.
	 *
	 * @param {PageAPI} page
	 * @return {jQuery.<HTMLElement>} A jQuery set of link elements.
	 */
	function getEditableLinks(page) {
		return getEditablesInDocument(page).find('a[href]');
	}

	/**
	 * Determines all blocks that no longer need their tags to be kept in the
	 * given page's tag list.
	 *
	 * @param {PageAPI} page
	 * @param {function(Array.<object>)} success A callback function that
	 *                                           receives a list of obsolete
	 *                                           blocks.
	 * @param {function(GCNError):boolean=} error Optional custom error
	 *                                            handler.
	 */
	function getObsoleteBlocks(page, success, error) {
		var blocks = getRenderedBlocks(page);
		if (0 === blocks.length) {
			success([]);
			return;
		}
		var $links = getEditableLinks(page);
		var numToProcess = blocks.length;
		var obsolete = [];
		var onSuccess = function () {
			if ((0 === --numToProcess) && success) {
				success(obsolete);
			}
		};
		var onError = function (GCNError) {
			if (error) {
				return error(GCNError);
			}
		};
		page.constructs(function (constructs) {
			var processTag = function (block) {
				page.tag(block.tagname, function (tag) {
					if (isMagicLinkTag(tag, constructs) && !getElement(block)) {
						obsolete.push(block);
					}
					onSuccess();
				}, onError);
			};
			var i;
			for (i = 0; i < blocks.length; i++) {
				processTag(blocks[i]);
			}
		});
	}

	/**
	 * Checks whether or not the given block has a corresponding element in the
	 * document DOM.
	 *
	 * @private
	 * @static
	 * @param {object}
	 * @return {boolean} True if an inline element for this block exists.
	 */
	function hasInlineElement(block) {
		return !!getElement(block);
	}

	/**
	 * Matches "aloha-*" class names.
	 *
	 * @const
	 * @type {RegExp}
	 */
	var ALOHA_CLASS_NAMES = /\baloha-[a-z0-9\-\_]*\b/gi;

	/**
	 * Strips unwanted names from the given className string.
	 *
	 * All class names beginning with "aloha-block*" will be removed.
	 *
	 * @param {string} classes Space seperated list of classes.
	 * @return {string} Sanitized classes string.
	 */
	function cleanBlockClasses(classes) {
		return classes ? jQuery.trim(classes.replace(ALOHA_CLASS_NAMES, ''))
		               : '';
	}

	var ATTR_LINK_OBJECT_ID = 'data-gentics-aloha-object-id';
	var ATTR_LINK_HREF = 'href';
	var ATTR_LINK_ANCHOR = 'data-gentics-gcn-anchor';
	var ATTR_LINK_TITLE = 'title';
	var ATTR_LINK_TARGET = 'target';
	var ATTR_LINK_LANGUAGE = 'hreflang';
	var ATTR_LINK_NODE_ID = 'data-gcn-channelid';

	/**
	 * Determines the backend object that was set to the given link.
	 *
	 * @param {jQuery.<HTMLElement>} $link A link in an editable.
	 * @return {Object} An object containing the gtxalohapagelink part keyword
	 *                  and value.  The keyword may be either be "url" or
	 *                  "fileurl" depending on the type of object linked to.
	 *                  The value may be a string url ("http://...") for
	 *                  external links or an integer for internal links.
	 */
	function getTagPartsFromLink($link) {
		var linkData = $link.attr(ATTR_LINK_OBJECT_ID);
		var href = $link.attr(ATTR_LINK_HREF) || '';
		var anchorUrlMatch = href.match(ANCHOR_LINK);
		var tagparts = {
			text: $link.html(),
			anchor: $link.attr(ATTR_LINK_ANCHOR),
			title: $link.attr(ATTR_LINK_TITLE),
			target: $link.attr(ATTR_LINK_TARGET),
			language: $link.attr(ATTR_LINK_LANGUAGE),
			'class': cleanBlockClasses($link.attr('class')),
			channel: $link.attr(ATTR_LINK_NODE_ID)
		};

		if (anchorUrlMatch && tagparts.anchor) {
			href = anchorUrlMatch[1];
		}

		if (href === window.location.href) {
			href = '';
		}

		if (linkData) {
			var idParts = linkData.split('.');

			if (2 !== idParts.length) {
				tagparts.url = {
					'pageId' : linkData,
					'nodeId' : $link.attr(ATTR_LINK_NODE_ID)
				};
			} else if ('10007' === idParts[0]) {
				tagparts.url = {
					'pageId' : parseInt(idParts[1], 10),
					'nodeId' : $link.attr(ATTR_LINK_NODE_ID)
				};
				tagparts.fileurl = 0;
			} else if ('10008' === idParts[0] || '10011' === idParts[0]) {
				tagparts.url = 0;
				tagparts.fileurl = {
					'fileId' : parseInt(idParts[1], 10),
					'nodeId' : $link.attr(ATTR_LINK_NODE_ID)
				};
			} else {
				tagparts.url = href;
			}
		} else {
			// check whether the href contains links to internal pages or files
			var result = GCN.settings.checkForInternalLink(href);

			tagparts.url = result.url;
			tagparts.fileurl = result.fileurl;

			if (result.match) {
				href = '';
			}
		}

		if (!tagparts.anchor) {
			tagparts.anchor = anchorUrlMatch ? anchorUrlMatch[2] : '';
		}

		return tagparts;
	}

	/**
	 * Checks whether a page object has a corresponding tag for a given link
	 * DOM element.
	 *
	 * @param {PageAPI} page The page object in which to look for the link tag.
	 * @param {jQuery.<HTMLElement>} $link jQuery unit set containing a link
	 *                                     DOM element.
	 * @return {boolean} True if there is a tag on the page that corresponds with
	 *                   the givn link.
	 */
	function hasTagForLink(page, $link) {
		var id = $link.attr('id');
		return !!(id && page._getBlockById(id));
	}

	/**
	 * Checks whether or not the given part has a part type of the given
	 * name
	 *
	 * @param {TagAPI} tag
	 * @param {string} part Part name
	 * @return {boolean} True of part exists in the given tag.
	 */
	function hasTagPart(tag, part) {
		return !!tag._data.properties[part];
	}

	/**
	 * Updates the parts of a tag in the page object that corresponds to the
	 * given link DOM element.
	 *
	 * @param {PageAPI} page
	 * @param {jQuery.<HTMLElement>} $link jQuery unit set containing a link
	 *                                     DOM element.
	 */
	function updateTagForLink(page, $link) {
		var block = page._getBlockById($link.attr('id'));
		// ASSERT(block)
		var tag = page.tag(block.tagname);
		var parts = getTagPartsFromLink($link);
		var part;
		for (part in parts) {
			if (parts.hasOwnProperty(part) && hasTagPart(tag, part)) {
				tag.part(part, parts[part]);
			}
		}
	}

	/**
	 * Creates a new tag for the given link in the page.
	 *
	 * @param {PageAPI} page
	 * @param {jQuery.<HTMLElement>} $link jQuery unit set containing a link
	 *                                     element.
	 * @param {function} success Callback function that whill be called when
	 *                           the new tag is created.
	 * @param {function(GCNError):boolean=} error Optional custom error
	 *                                            handler.
	 */
	function createTagForLink(page, $link, success, error) {
		page.createTag({
			keyword: GCN.settings.MAGIC_LINK,
			magicValue: $link.html()
		}).edit(function (html, tag) {
			// Copy over the rendered id value for the link so that we can bind
			// the link in the DOM with the newly created block.
			$link.attr('id', jQuery(html).attr('id'));
			updateTagForLink(page, $link);
			success();
		}, error);
	}

	/**
	 * Create tags for all new links in the page
	 * 
	 * @param {PageApi} page
	 * @param {jQuery.<HTMLElement>} $gcnlinks jQuery unit set containing links
	 * @param {function} success Callback function that will be called when the tags are created
	 * @param {function(GCNError)} error Optional custom error handler
	 */
	function createMissingLinkTags(page, $gcnlinks, success, error) {
		var $newGcnLinks = $gcnlinks.filter(function () {
			return !hasTagForLink(page, jQuery(this));
		}), linkData = {create:{}}, i = 0, id;

		if ($newGcnLinks.length > 0) {
			$newGcnLinks.each(function (index) {
				id = 'link' + (i++);
				linkData.create[id] = {
					data: {
						keyword: GCN.settings.MAGIC_LINK,
						magicValue: jQuery(this).html()
					},
					obj: jQuery(this)
				};
			});
			page._createTags(linkData, function () {
				var id;
				for (id in linkData.create) {
					if (linkData.create.hasOwnProperty(id)) {
						linkData.create[id].obj.attr('id', jQuery(linkData.create[id].response.html).attr('id'));
					}
				}
				page._processRenderedTags(linkData.response);
				success();
			}, error);
		} else {
			success();
		}

	}

	/**
	 * Identifies internal GCN links in the given page's rendered editables,
	 * and updates their corresponding content tags, or create new tags for the
	 * if they are new links.
	 *
	 *  @param {PageAPI} page
	 *  @param {function} success
	 *  @param {function} error
	 */
	function processGCNLinks(page, success, error) {
		var $links = getEditableLinks(page);
		var $gcnlinks = $links.filter(function () {
			return this.isContentEditable;
		}).filter(':not(.aloha-editable)');
		if (0 === $gcnlinks.length) {
			if (success) {
				success();
			}
			return;
		}
		var numToProcess = $gcnlinks.length;
		var onSuccess = function () {
			if ((0 === --numToProcess) && success) {
				success();
			}
		};
		var onError = function (GCNError) {
			if (error) {
				return error(GCNError);
			}
		};

		// When a link was copied it would result in two magic link tags
		// with the same ID. We remove the id attribute from such duplicates
		// so that hasTagForLink() will return false and create a new tag
		// for the copied link.
		var alreadyExists = {};

		$links.each(function () {
			var $link = jQuery(this);
			var id = $link.attr('id');

			if (id) {
				if (alreadyExists[id]) {
					$link.removeAttr('id');
				} else {
					alreadyExists[id] = true;
				}
			}
		});

		createMissingLinkTags(page, $gcnlinks, function () {
			var i;
			for (i = 0; i < $gcnlinks.length; i++) {
				if (hasTagForLink(page, $gcnlinks.eq(i))) {
					updateTagForLink(page, $gcnlinks.eq(i));
					onSuccess();
				} else {
					onError(GCN.error('500', 'Missing Tag for Link', $gcnlinks.get(i)));
				}
			}
		}, onError);
	}

	/**
	 * Adds the given blocks into the page's list of blocks that are to be
	 * deleted when the page is saved.
	 *
	 * @param {PageAPI} page
	 * @param {Array.<object>} blocks Blocks that are to be marked for deletion.
	 */
	function deleteBlocks(page, blocks) {
		blocks = jQuery.isArray(blocks) ? blocks : [blocks];
		var i;
		var success = function(tag) {
			tag.remove();
		};
		for (i = 0; i < blocks.length; i++) {
			if (-1 ===
					jQuery.inArray(blocks[i].tagname, page._deletedBlocks)) {
				page._deletedBlocks.push(blocks[i].tagname);
			}
			delete page._blocks[blocks[i].element];

			page.tag(blocks[i].tagname, success);
		}
	}

	/**
	 * Removes all tags on the given page which belong to links that are no
	 * longer present in any of the page's rendered editables.
	 *
	 * @param {PageAPI} page
	 * @param {function} success Callback function that will be invoked when
	 *                           all obsolete tags have been successfully
	 *                           marked for deletion.
	 * @param {function(GCNError):boolean=} error Optional custom error
	 *                                            handler.
	 */
	function deleteObsoleteLinkTags(page, success, error) {
		getObsoleteBlocks(page, function (obsolete) {
			deleteBlocks(page, obsolete);
			if (success) {
				success();
			}
		}, error);
	}

	/**
	 * Searches for the an Aloha editable object of the given id.
	 *
	 * @TODO: Once Aloha.getEditableById() is patched to not cause an
	 *        JavaScript exception if the element for the given ID is not found
	 *        then we can deprecate this function and use Aloha's instead.
	 *
	 * @static
	 * @param {string} id Id of Aloha.Editable object to find.
	 * @return {Aloha.Editable=} The editable object, if wound; otherwise null.
	 */
	function getAlohaEditableById(id) {
		var Aloha = (typeof window !== 'undefined') && window.Aloha;
		if (!Aloha) {
			return null;
		}

		// If the element is a textarea then route to the editable div.
		var element = jQuery('#' + id);
		if (element.length &&
				element[0].nodeName.toLowerCase() === 'textarea') {
			id += '-aloha';
		}

		var editables = Aloha.editables;
		var j = editables.length;
		while (j) {
			if (editables[--j].getId() === id) {
				return editables[j];
			}
		}

		return null;
	}

	/**
	 * For a given list of editables and a list of blocks, determines in which
	 * editable each block is contained.  The result is a map of block sets.
	 * Each of these sets of blocks are mapped against the id of the editable
	 * in which they are rendered.
	 *
	 * @param {string} content The rendered content in which both the list of
	 *                         editables, and blocks are contained.
	 * @param {Array.<object>} editables A list of editables contained in the
	 *                                   content.
	 * @param {Array.<object>} blocks A list of blocks containd in the content.
	 * @return {object<string, Array>} A object whose keys are editable ids,
	 *                                  and whose values are arrays of blocks
	 *                                  contained in the corresponding
	 *                                  editable.
	 */
	function categorizeBlocksAgainstEditables(content, editables, blocks) {
		var i;
		var $content = jQuery('<div>' + content + '</div>');
		var sets = {};
		var editableId;

		var editablesSelectors = [];
		for (i = 0; i < editables.length; i++) {
			editablesSelectors.push('#' + editables[i].element);
		}

		var markerClass = 'aloha-editable-tmp-marker__';
		var $editables = $content.find(editablesSelectors.join(','));
		$editables.addClass(markerClass);

		var $block;
		var $parent;
		for (i = 0; i < blocks.length; i++) {
			$block = $content.find('#' + blocks[i].element);
			if ($block.length) {
				$parent = $block.closest('.' + markerClass);
				if ($parent.length) {
					editableId = $parent.attr('id');
					if (editableId) {
						if (!sets[editableId]) {
							sets[editableId] = [];
						}
						sets[editableId].push(blocks[i]);
					}
				}
			}
		}

		return sets;
	}

	/**
	 * Causes the given editables to be tracked, so that when the content
	 * object is saved, these editables will be processed.
	 *
	 * @private
	 * @param {PageAPI} page
	 * @param {Array.<object>} editables A set of object representing
	 *                                   editable tags that have been
	 *                                   rendered.
	 */
	function trackEditables(page, editables) {
		if (!page.hasOwnProperty('_editables')) {
			page._editables = {};
		}
		var i;
		for (i = 0; i < editables.length; i++) {
			page._editables[editables[i].element] = editables[i];
		}
	}

	/**
	 * Causes the given blocks to be tracked so that when the content object is
	 * saved, these editables will be processed.
	 *
	 * @private
	 * @param {PageAPI} page
	 * @param {Array.<object>} blocks An set of object representing block
	 *                                tags that have been rendered.
	 */
	function trackBlocks(page, blocks) {
		if (!page.hasOwnProperty('_blocks')) {
			page._blocks = {};
		}
		var i;
		for (i = 0; i < blocks.length; i++) {
			page._blocks[blocks[i].element] = blocks[i];
		}
	}

	/**
	 * Associates a list of blocks with an editable so that it can later be
	 * determined which blocks are contained inside which editable.
	 *
	 * @param object
	 */
	function associateBlocksWithEditable(editable, blocks) {
		if (!jQuery.isArray(editable._gcnContainedBlocks)) {
			editable._gcnContainedBlocks = [];
		}

		var i, j;

		outer:
		for (i = 0; i < blocks.length; i++) {
			for (j = 0; j < editable._gcnContainedBlocks.length; j++) {
				if (blocks[i].element === editable._gcnContainedBlocks[j].element
						&& blocks[i].tagname === editable._gcnContainedBlocks[j].tagname) {
					// Prevent duplicates
					continue outer;
				}
			}

			editable._gcnContainedBlocks.push(blocks[i]);
		}
	}

	/**
	 * Extracts the editables and blocks that have been rendered from the
	 * REST API render call's response data, and stores them in the page.
	 *
	 * @param {PageAPI} page The page inwhich to track the incoming tags.
	 * @param {object} data Raw data containing editable and block tags
	 * information.
	 * @return {object} A object containing to lists: one list of blocks, and
	 *                  another of editables.
	 */
	function trackRenderedTags(page, data) {
		var tags = GCN.TagContainerAPI.getEditablesAndBlocks(data);

		var containment = categorizeBlocksAgainstEditables(
			data.content,
			tags.editables,
			tags.blocks
		);

		trackEditables(page, tags.editables);
		trackBlocks(page, tags.blocks);

		jQuery.each(containment, function (editable, blocks) {
			if (page._editables[editable]) {
				associateBlocksWithEditable(page._editables[editable], blocks);
			}
		});

		return tags;
	}

	/**
	 * @private
	 * @const
	 * @type {number}
	 */
	//var TYPE_ID = 10007;

	/**
	 * @private
	 * @const
	 * @type {Enum}
	 */
	var STATUS = {

		// page was not found in the database
		NOTFOUND: -1,

		// page is locally modified and not yet (re-)published
		MODIFIED: 0,

		// page is marked to be published (dirty)
		TOPUBLISH: 1,

		// page is published and online
		PUBLISHED: 2,

		// Page is offline
		OFFLINE: 3,

		// Page is in the queue (publishing of the page needs to be affirmed)
		QUEUE: 4,

		// page is in timemanagement and outside of the defined timespan
		// (currently offline)
		TIMEMANAGEMENT: 5,

		// page is to be published at a given time (not yet)
		TOPUBLISH_AT: 6
	};

	/**
	 * @class
	 * @name PageAPI
	 * @extends ContentObjectAPI
	 * @extends TagContainerAPI
	 * 
	 * @param {number|string}
	 *            id of the page to be loaded
	 * @param {function(ContentObjectAPI))=}
	 *            success Optional success callback that will receive this
	 *            object as its only argument.
	 * @param {function(GCNError):boolean=}
	 *            error Optional custom error handler.
	 * @param {object}
	 *            settings additional settings for object creation. These
	 *            correspond to options available from the REST-API and will
	 *            extend or modify the PageAPI object.
	 *            <dl>
	 *            <dt>update: true</dt>
	 *            <dd>Whether the page should be locked in the backend when
	 *            loading it. default: true</dd>
	 *            <dt>template: true</dt>
	 *            <dd>Whether the template information should be embedded in
	 *            the page object. default: true</dd>
	 *            <dt>folder: true</dt>
	 *            <dd>Whether the folder information should be embedded in the
	 *            page object. default: true <b>WARNING</b>: do not turn this
	 *            option off - it will leave the API in a broken state.</dd>
	 *            <dt>langvars: true</dt>
	 *            <dd>When the language variants shall be embedded in the page
	 *            response. default: true</dd>
	 *            <dt>workflow: false</dt>
	 *            <dd>When the workflow information shall be embedded in the
	 *            page response. default: false</dd>
	 *            <dt>pagevars: true</dt>
	 *            <dd>When the page variants shall be embedded in the page
	 *            response. Page variants will contain folder information.
	 *            default: true</dd>
	 *            <dt>translationstatus: false</dt>
	 *            <dd>Will return information on the page's translation status.
	 *            default: false</dd>
	 *            </dl>
	 */
	var PageAPI = GCN.defineChainback({
		/** @lends PageAPI */

		__chainbacktype__: 'PageAPI',
		_extends: [ GCN.TagContainerAPI, GCN.ContentObjectAPI ],
		_type: 'page',

		/**
		 * A hash set of block tags belonging to this page.  This set grows as
		 * this page's tags are rendered.
		 *
		 * @private
		 * @type {Array.<object>}
		 */
		_blocks: {},

		/**
		 * A hash set of editable tags belonging to this page.  This set grows
		 * as this page's tags are rendered.
		 *
		 * @private
		 * @type {Array.<object>}
		 */
		_editables: {},

		/**
		 * Writable properties for the page object. Currently the following
		 * properties are writeable: cdate, description, fileName, folderId,
		 * name, priority, templateId. WARNING: changing the folderId might not
		 * work as expected.
		 * 
		 * @type {Array.string}
		 * @const
		 */
		WRITEABLE_PROPS: [
		                  'customCdate',
		                  'customEdate',
		                  'description',
		                  'fileName',
		                  'folderId', // @TODO Check if moving a page is
		                              //       implemented correctly.
		                  'name',
		                  'priority',
		                  'templateId',
		                  'timeManagement'
		                  ],

		/**
		 * @type {object} Constraints for writeable props
		 * @const
		 *
		 */
		WRITEABLE_PROPS_CONSTRAINTS: {
			'name': {
				maxLength: 255
			}
		},

		/**
		 * Gets all blocks that are associated with this page.
		 *
		 * It is important to note that the set of blocks in the returned array
		 * will only include those that are the returned by the server when
		 * calling  edit() on a tag that belongs to this page.
		 *
		 * @return {Array.<object>} The set of blocks that have been
		 *                          initialized by calling edit() on one of
		 *                          this page's tags.
		 */
		'!blocks': function () {
			return this._blocks;
		},

		/**
		 * Retrieves a block with the given id among the blocks that are
		 * tracked by this page content object.
		 *
		 * @private
		 * @param {string} id The block's id.
		 * @return {?object} The block data object.
		 */
		'!_getBlockById': function (id) {
			return this._blocks[id];
		},

		/**
		 * Extracts the editables and blocks that have been rendered from the
		 * REST API render call's response data, and stores them in the page.
		 *
		 * @override
		 */
		'!_processRenderedTags': function (data) {
			return trackRenderedTags(this, data);
		},

		/**
		 * Processes this page's tags in preparation for saving.
		 *
		 * The preparation process:
		 *
		 * 1. For all editables associated with this page, determine which of
		 *    their blocks have been rendered into the DOM for editing so that
		 *    changes to the DOM can be reflected in the corresponding data
		 *    structures before pushing the tags to the server.
		 *
		 * 2.
		 *
		 * Processes rendered tags, and updates the `_blocks' and `_editables'
		 * arrays accordingly.  This function is called during pre-saving to
		 * update this page's editable tags.
		 *
		 * @private
		 */
		'!_prepareTagsForSaving': function (success, error) {
			if (!this.hasOwnProperty('_deletedBlocks')) {
				this._deletedBlocks = [];
			}
			var page = this;
			processGCNLinks(page, function () {
				deleteObsoleteLinkTags(page, function () {
					page._updateEditableBlocks();
					if (success) {
						success();
					}
				}, error);
			}, error);
		},

		/**
		 * Writes the contents of editables back into their corresponding tags.
		 * If a corresponding tag cannot be found for an editable, a new one
		 * will be created for it.
		 *
		 * A reference for each editable tag is then added to the `_shadow'
		 * object in order that the tag will be sent with the save request.
		 *
		 * @private
		 */
		'!_updateEditableBlocks': function (filter) {
			var $element;
			var id;
			var editables = this._editables;
			var tags = this._data.tags;
			var tagname;
			var html;
			var alohaEditable;
			var $cleanElement;
			var customSerializer;

			for (id in editables) {
				if (editables.hasOwnProperty(id)) {
					$element = jQuery('#' + id);

					// Because the editable may not have have been rendered into
					// the document DOM.
					if (0 === $element.length) {
						continue;
					}

					if (typeof filter === 'function') {
						if (!filter.call(this, editables[id])) {
							continue;
						}
					}

					tagname = editables[id].tagname;

					if (!tags[tagname]) {
						tags[tagname] = {
							name       : tagname,
							active     : true,
							properties : {}
						};
					} else {
						// Because it is sensible to assume that every editable
						// that was rendered for editing is intended to be an
						// activate tag.
						tags[tagname].active = true;
					}

					// Because editables that have been aloha()fied, must have
					// their contents retrieved by getContents() in order to get
					// clean HTML.

					alohaEditable = getAlohaEditableById(id);

					if (alohaEditable) {
						// Avoid the unnecessary overhead of custom editable
						// serialization by calling html ourselves.
						$cleanElement = jQuery('<div>').append(
							alohaEditable.getContents(true)
						);
						alohaEditable.setUnmodified();
						// Apply the custom editable serialization as the last step.
						customSerializer = window.Aloha.Editable.getContentSerializer();
						html = this.encode($cleanElement, customSerializer);
					} else {
						html = this.encode($element);
					}
					// If the editable is backed by a parttype, that
					// would replace newlines by br tags while
					// rendering, remove all newlines before saving back
					if ($element.hasClass('GENTICS_parttype_text') ||
						$element.hasClass('GENTICS_parttype_texthtml') ||
						$element.hasClass('GENTICS_parttype_java_editor') ||
						$element.hasClass('GENTICS_parttype_texthtml_long')) {
						html = html.replace(/(\r\n|\n|\r)/gm,"");
					}

					tags[tagname].properties[editables[id].partname] =
						jQuery.extend({type: 'RICHTEXT'}, tags[tagname].properties[editables[id].partname], {stringValue: html});

					this._update('tags.' + GCN.escapePropertyName(tagname),
						tags[tagname]);
				}
			}
		},

		/**
		 * @see ContentObjectAPI.!_loadParams
		 */
		'!_loadParams': function () {
			return jQuery.extend(DEFAULT_SETTINGS, this._settings);
		},

		/**
		 * Get this page's template.
		 *
		 * @public
		 * @function
		 * @name template
		 * @memberOf PageAPI
		 * @param {funtion(TemplateAPI)=} success Optional callback to receive
		 *                                        a {@link TemplateAPI} object
		 *                                        as the only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 * @return {TemplateAPI} This page's parent template.
		 */
		'!template': function (success, error) {
			var id = this._fetched ? this.prop('templateId') : null;
			return this._continue(GCN.TemplateAPI, id, success, error);
		},

		/**
		 * Cache of constructs for this page.
		 * Should be cleared when page is saved.
		 */
		_constructs: null,

		/**
		 * List of success and error callbacks that need to be called
		 * once the constructs are loaded
		 * @private
		 * @type {array.<object>}
		 */
		_constructLoadHandlers: null,

		/**
		 * Retrieve the list of constructs of the tag that are used in this
		 * page.
		 *
		 * Note that tags that have been created on this page locally, but have
		 * yet to be persisted to the server (unsaved tags), will not have their
		 * constructs included in the list unless their constructs are used by
		 * other saved tags.
		 */
		'!constructs': function (success, error) {
			var page = this;
			if (page._constructs) {
				return success(page._constructs);
			}

			// if someone else is already loading the constructs, just add the callbacks
			page._constructLoadHandlers = page._constructLoadHandlers || [];
			if (page._constructLoadHandlers.length > 0) {
				page._constructLoadHandlers.push({success: success, error: error});
				return;
			}

			// we are the first to load the constructs, register the callbacks and
			// trigger the ajax call
			page._constructLoadHandlers.push({success: success, error: error});
			page._authAjax({
				url: GCN.settings.BACKEND_PATH
				     + '/rest/construct/list.json?pageId=' + page.id(),
				type: 'GET',
				error: function (xhr, status, msg) {
					var i;
					for (i = 0; i < page._constructLoadHandlers.length; i++) {
						GCN.handleHttpError(xhr, msg, page._constructLoadHandlers[i].error);
					}
				},
				success: function (response) {
					var i;
					if (GCN.getResponseCode(response) === 'OK') {
						page._constructs = GCN.mapConstructs(response.constructs);
						for (i = 0; i < page._constructLoadHandlers.length; i++) {
							page._invoke(page._constructLoadHandlers[i].success, [page._constructs]);
						}
					} else {
						for (i = 0; i < page._constructLoadHandlers.length; i++) {
							GCN.handleResponseError(response, page._constructLoadHandlers[i].error);
						}
					}
				},

				complete: function () {
					page._constructLoadHandlers = [];
				}
			});
		},

		/**
		 * @override
		 * @see ContentObjectAPI._save
		 */
		'!_save': function (settings, success, error) {
			var page = this;
			this._fulfill(function () {
				page._read(function () {
					var fork = page._fork();
					fork._prepareTagsForSaving(function () {
						GCN.pub('page.before-saved', fork);
						fork._persist(settings, function () {
							if (success) {
								page._constructs = null;
								fork._merge(false);
								page._invoke(success, [page]);
								page._vacate();
							} else {
								fork._merge();
							}
						}, function () {
							page._vacate();
							if (error) {
								error.apply(this, arguments);
							}
						});
					}, error);
				}, error);
			}, error);
		},

		//---------------------------------------------------------------------
		// Surface the tag container methods that are applicable for GCN page
		// objects.
		//---------------------------------------------------------------------

		/**
		 * Creates a tag of a given tagtype in this page.
		 * The first parameter should either be the construct keyword or ID,
		 * or an object containing exactly one of the following property sets:<br/>
		 * <ol>
		 *   <li><i>keyword</i> to create a tag based on the construct with given keyword</li>
		 *   <li><i>constructId</i> to create a tag based on the construct with given ID</li>
		 *   <li><i>sourcePageId</i> and <i>sourceTagname</i> to create a tag as copy of the given tag from the page</li>
		 * </ol>
		 *
		 * Exmaple:
		 * <pre>
		 *  createTag('link', onSuccess, onError);
		 * </pre>
		 * or
		 * <pre>
		 *  createTag({keyword: 'link', magicValue: 'http://www.gentics.com'}, onSuccess, onError);
		 * </pre>
		 * or
		 * <pre>
		 *  createTag({sourcePageId: 4711, sourceTagname: 'link'}, onSuccess, onError);
		 * </pre>
		 *
		 * @public
		 * @function
		 * @name createTag
		 * @memberOf PageAPI
		 * @param {string|number|object} construct either the keyword of the
		 *                               construct, or the ID of the construct
		 *                               or an object with the following
		 *                               properties
		 *                               <ul>
		 *                                <li><i>keyword</i> keyword of the construct</li>
		 *                                <li><i>constructId</i> ID of the construct</li>
		 *                                <li><i>magicValue</i> magic value to be filled into the tag</li>
		 *                                <li><i>sourcePageId</i> source page id</li>
		 *                                <li><i>sourceTagname</i> source tag name</li>
		 *                               </ul>
		 * @param {function(TagAPI)=} success Optional callback that will
		 *                                    receive the newly created tag as
		 *                                    its only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 * @return {TagAPI} The newly created tag.
		 * @throws INVALID_ARGUMENTS
		 */
		'!createTag': function () {
			return this._createTag.apply(this, arguments);
		},

		/**
		 * Deletes the specified tag from this page.
		 * You should pass a keyword here not an Id.
		 * 
		 * Note: Due to how the underlying RestAPI layer works,
		 * the success callback will also be called if the specified tag
		 * does not exist.
		 * 
		 * @public
		 * @function
		 * @memberOf PageAPI
		 * @param {string}
		 *            keyword The keyword of the tag to be deleted.
		 * @param {function(PageAPI)=}
		 *            success Optional callback that receive this object as its
		 *            only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 */
		removeTag: function () {
			this._removeTag.apply(this, arguments);
		},

		/**
		 * Deletes a set of tags from this page.
		 * 
		 * @public
		 * @function
		 * @memberOf PageAPI
		 * @param {Array.
		 *            <string>} keywords The keywords of the tags to be deleted.
		 * @param {function(PageAPI)=}
		 *            success Optional callback that receive this object as its
		 *            only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 */
		removeTags: function () {
			this._removeTags.apply(this, arguments);
		},

		/**
		 * Takes the page offline.
		 * If instant publishing is enabled, this will take the page offline
		 * immediately. Otherwise it will be taken offline during the next
		 * publish run.
		 *
		 * @public
		 * @function
		 * @memberOf PageAPI
		 * @param {funtion(PageAPI)=} success Optional callback to receive this
		 *                                    page object as the only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		takeOffline: function (success, error) {
			var page = this;
			page._fulfill(function () {
				page._authAjax({
					url: GCN.settings.BACKEND_PATH + '/rest/' + page._type +
					     '/takeOffline/' + page.id(),
					type: 'POST',
					json: {}, // There needs to be at least empty content
					          // because of a bug in Jersey.
					error: error,
					success: function (response) {
						if (success) {
							page._invoke(success, [page]);
						}
					}
				});
			});
		},

		/**
		 * Trigger publish process for the page.
		 *
		 * @public
		 * @function
		 * @memberOf PageAPI
		 * @param {funtion(PageAPI)=} success Optional callback to receive this
		 *                                    page object as the only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		publish: function (success, error) {
			var page = this;
			GCN.pub('page.before-publish', page);
			this._fulfill(function () {
				page._authAjax({
					url: GCN.settings.BACKEND_PATH + '/rest/' + page._type +
					     '/publish/' + page.id() + GCN._getChannelParameter(page),
					type: 'POST',
					json: {}, // There needs to be at least empty content
					          // because of a bug in Jersey.
					success: function (response) {
						page._data.status = STATUS.PUBLISHED;
						if (success) {
							page._invoke(success, [page]);
						}
					},
					error: error
				});
			});
		},

		/**
		 * Renders a preview of the current page.
		 * 
		 * @public
		 * @function
		 * @memberOf PageAPI
		 * @param {function(string,
		 *            PageAPI)} success Callback to receive the rendered page
		 *            preview as the first argument, and this page object as the
		 *            second.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 */
		preview: function (success, error) {
			var that = this;

			this._read(function () {
				that._authAjax({
					url: GCN.settings.BACKEND_PATH + '/rest/' + that._type +
					     '/preview/' + GCN._getChannelParameter(that),
					json: {
						page: that._data, // @FIXME Shouldn't this a be merge of
						                 //        the `_shadow' object and the
										 //        `_data'.
						nodeId: that.nodeId()
					},
					type: 'POST',
					error: error,
					success: function (response) {
						if (success) {
							GCN._handleContentRendered(response.preview, that,
								function (html) {
									that._invoke(success, [html, that]);
								});
						}
					}
				});
			}, error);
		},

		/**
		 * Unlocks the page when finishing editing
		 * 
		 * @public
		 * @function
		 * @memberOf PageAPI
		 * @param {funtion(PageAPI)=}
		 *            success Optional callback to receive this page object as
		 *            the only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 */
		unlock: function (success, error) {
			var that = this;
			this._fulfill(function () {
				that._authAjax({
					url: GCN.settings.BACKEND_PATH + '/rest/' + that._type +
					     '/cancel/' + that.id() + GCN._getChannelParameter(that),
					type: 'POST',
					json: {}, // There needs to be at least empty content
					          // because of a bug in Jersey.
					error: error,
					success: function (response) {
						if (success) {
							that._invoke(success, [that]);
						}
					}
				});
			});
		},

		/**
		 * @see GCN.ContentObjectAPI._processResponse
		 */
		'!_processResponse': function (data) {
			this._data = jQuery.extend(true, {}, data[this._type], this._data);

			// if data contains page variants turn them into page objects
			if (this._data.pageVariants) {
				var pagevars = [];
				var i;
				for (i = 0; i < this._data.pageVariants.length; i++) {
					pagevars.push(this._continue(GCN.PageAPI,
						this._data.pageVariants[i]));
				}
				this._data.pageVariants = pagevars;
			}
		},

		/**
		 * @override
		 */
		'!_removeAssociatedTagData': function (tagid) {
			var block;
			for (block in this._blocks) {
				if (this._blocks.hasOwnProperty(block) &&
						this._blocks[block].tagname === tagid) {
					delete this._blocks[block];
				}
			}

			var editable, containedBlocks, i;
			for (editable in this._editables) {
				if (this._editables.hasOwnProperty(editable)) {
					if (this._editables[editable].tagname === tagid) {
						delete this._editables[editable];
					} else {
						containedBlocks = this._editables[editable]._gcnContainedBlocks;
						if (jQuery.isArray(containedBlocks)) {
							for (i = containedBlocks.length -1; i >= 0; i--) {
								if (containedBlocks[i].tagname === tagid) {
									containedBlocks.splice(i, 1);
								}
							}
						}
					}
				}
			}
		},

		/**
		 * Render a preview for an editable tag by POSTing the current page to the REST Endpoint /page/renderTag/{tagname}
		 * 
		 * @param {string} tagname name of the tag to render
		 * @param {function} success success handler function
		 * @param {function} error error handler function
		 */
		'!_previewEditableTag': function (tagname, success, error) {
			var channelParam = GCN._getChannelParameter(this);
			var url = GCN.settings.BACKEND_PATH +
					'/rest/page/renderTag/' +
					tagname +
			        channelParam +
			        (channelParam ? '&' : '?') +
			        'links=' + encodeURIComponent(GCN.settings.linksRenderMode);
			var jsonData = jQuery.extend({}, this._data);
			// remove some data, we don't want to serialize and POST to the server
			jsonData.pageVariants = null;
			jsonData.languageVariants = null;
			this._authAjax({
				type: 'POST',
				json: jsonData,
				url: url,
				error: error,
				success: success
			});
		}

	});

	/**
	 * Creates a new instance of PageAPI.
	 * See the {@link PageAPI} constructor for detailed information.
	 * 
	 * @function
	 * @name page
	 * @memberOf GCN
	 * @see PageAPI
	 */
	GCN.page = GCN.exposeAPI(PageAPI);
	GCN.PageAPI = PageAPI;

	GCN.PageAPI.trackRenderedTags = trackRenderedTags;

}(GCN));
