/*global define: true, top: true, confirm: true */

/**
 * @typedef {object} TagFillOptions
 * @property {boolean=} skipInsert If it sohuld not update/insert the DOM element. Will skip the rendering request of the tag as well.
 * @property {boolean=} withDelete If the tag-fill/user should be able to delete the tag in question.
 */

/*!
 * Aloha Editor
 * Author & Copyright (c) 2011-2013 Gentics Software GmbH
 * aloha-sales@gentics.com
 * Licensed under the terms of http://www.aloha-editor.com/license.html
 */
define([
	// FIXME: causing Chrome to sometimes crash.
	// Aloha.settings.plugins.gcn.gcnLibVersion + '/gcnjsapi'
	'/gcnjsapi/' + window.Aloha.settings.plugins.gcn.buildRootTimestamp + '/' + (
		window.Aloha.settings.plugins.gcn.gcnLibVersion || 'bin'
	) + '/gcnjsapi.js',
	'aloha/core',
	'jquery',
	'PubSub',
	'aloha/plugin',
	'aloha/ephemera',
	'aloha/contenthandlermanager',
	'ui/ui-plugin',
	'ui/dialog',
	'block/blockmanager',
	'util/misc',
	'link/link-plugin',
	'gcn/gcn-block',
	'gcn/tagcopycontenthandler',
	'gcn/gcn-tags',
	'gcn/gcn-links',
	'gcn/gcmsui-surface',
	'i18n!gcn/nls/i18n',
	'css!gcn/css/aloha-gcn.css'
], function (
	_GCN_,
	Aloha,
	jQuery,
	PubSub,
	Plugin,
	Ephemera,
	ContentHandlerManager,
	UiPlugin,
	Dialog,
	BlockManager,
	Misc,
	LinkPlugin,
	GCNBlock,
	TagCopyContentHandler,
	Tags,
	GCNLinks,
	GCMSUISurface,
	i18n,
) {
	'use strict';

	var $ = jQuery;
	var GCN = window.GCN;
	var gcnPlugin;

	var INTERNAL_LINK_PATH = '/alohapage';

	/**
	 * When there are modified editables and one would click a link to
	 * change the current page, it would complain about unsaved changes.
	 * In IE the modified editable detection doesn't always work well for some
	 * reasons. Set this to true before loading another page, to make sure
	 * the dialog will not appear.
	 */
	var suppressNextUnsavedChangesDialog = false;

	/**
	 * Validate the given editables if validation feature is available.
	 *
	 * @param {Array.<editables>} editables Editables to validate.
	 * @param {function} callback Function to invoke when validation is
	 *                            complete.
	 */
	function validate(editables, callback) {
		if (Aloha.features.validation) {
			Aloha.require(['validation/validation-plugin'], function (Validation) {
				if (Validation.validate(editables).length) {
					Dialog.alert({
						title: 'Gentics Content.Node',
						text: i18n.t(
							'error.one-or-more-editables-failed-validation'
						)
					});
				} else {
					callback();
				}
			});
		} else {
			callback();
		}
	}

	/**
	 * Determines whether or not the given block is a "standalone" block,
	 * which is one that is not contained within a parent editable.
	 *
	 * @param {AlohaBlock} block
	 * @return {boolean} True if the block has not been rendered inside an
	 *                   editable.
	 */
	function isStandalone(block) {
		return 0 === block.$element.closest('.aloha-editable').length;
	}

	/**
	 * Do (system-wide) search for a construct matching a specific id.
	 *
	 * @param {PageAPI} page The page from which to search for constructs.
	 * @param {number} id Id of construct to retrieve.
	 * @param {function} success Callback function that will receive the
	 *                           construct when on is successfully retrieved.
	 * @param {function} error Custom error handler.
	 */
	function getConstructById(page, id, success, error) {
		page.constructs(function (constructs) {
			var construct;
			for (construct in constructs) {
				if (constructs.hasOwnProperty(construct)
					&& id === constructs[construct].id) {
					success(constructs[construct]);
					return;
				}
			}
			error('Could not find construct for tag id ' + id + '.');
		}, error);
	}

	var OBJECT_PROPERTY_PREFIX = /^object\.[a-zA-Z0-9]+/i;

	function isObjectTag(tag) {
		return OBJECT_PROPERTY_PREFIX.test(tag.prop('name'));
	}

	/**
	 * Actually opens the tag-fill in either the new ui (GCMSUI API) or via a lightbox and the
	 * old tag-fill from the backend.
	 *
	 * @param {*} tag The gcn-tag instance to edit
	 * @param {*} gcn The GCN-Plugin instance
	 * @param {TagFillOptions=} options The options for opening the tag-fill.
	 */
	function openTagFill(tag, gcn, options) {
		var page = tag.parent();
		var tagname = tag.prop("name");
		getConstructById(
			page,
			tag.prop('constructId'),
			function (construct) {
				// make sure, that the tag contains the current data from the editables
				// not passing a filter function as a parameter here means,
				// that all inline editables on the page will be stored in the page object
				// this is needed to keep changes made in inline editable tags embedded in the tag
				// that will potentially re-rendered after closing the tagfill
				page._updateEditableBlocks();

				GCMSUI.openTagEditor(tag._data, construct, page._data, options).then(function (result) {

					if (result.doDelete) {
						var $block = $('.aloha-block[id="GENTICS_BLOCK_' + tag._data.id + '"]');
						var block = BlockManager.getBlock($block);

						block.unblock();
						$block.remove();
						tag.remove();
						return;
					}

					var newtag = result.tag;

					page.tag(tagname, function (tag) {
						var parts = tag.parts();
						var partslength = parts.length;
						for (var i = 0; i < partslength; i++) {
							var newpart = newtag.properties[parts[i]];
							// extract the new part value from the updated tag specific to the
							// part type
							if (typeof GCN.TagParts[newpart.type] === 'function') {
								var newvalue = GCN.TagParts[newpart.type](newpart);
								// special hack for parts of type "PAGE" for preserving the nodeId
								if (newpart.type === "PAGE" && jQuery.type(newvalue) === 'number') {
									tag.part(parts[i], { pageId: newvalue, nodeId: newpart.nodeId });
								} else {
									tag.part(parts[i], newvalue);
								}
							}
						}
					});

					if (options && options.skipInsert) {
						return;
					}

					// we render that tag in edit mode by POSTing the modified data to the server,
					// in order to render what is currently stored in the tag object, not in the DB
					page.tag(tagname).edit(true, function (html, tag, data) {
						gcn.handleBlock(data, false, function () {
							Tags.decorate(tag, data);
						}, html);
					});
				}).catch(function () { });
			},
			function (msg) {
				GCN.handleError(GCN.createError('COULD_NOT_OPEN_TAG', msg, gcn));
			}
		);
	}

	/**
	 * Returns true if the Aloha Toolbar is configured to use responsiveMode.
	 * @return {boolean}
	 */
	function isResponsiveMode() {
		if (Aloha.settings.toolbar && Aloha.settings.toolbar.hasOwnProperty('responsiveMode')) {
			var value = Aloha.settings.toolbar.responsiveMode;
			return value.toString() === '1' || value === true || value.toString().toLowerCase() === 'true';
		}
		return false;
	}

	/**
	 * A list of page object handlers.  Handlers are registered via
	 * gcnPlugin.addPageObjectHandler(), and will be executed in the context of
	 * a gcn plugin instance.  Each handler will receive a page object as the
	 * only argument.
	 *
	 * @type {Array.<function(GCN.PageAPI)>}
	 */
	var pageObjectHandlers = [];

	/**
	 * Gentics Content.Node Integration Plugin.
	 */
	gcnPlugin = Plugin.create('gcn', {

		/**
		 * @type {Array.<string>} This plugin's `init()' method will not be
		 *                        invoked until these dependencies have been
		 *                        initialized.
		 */
		dependencies: [],

		/**
		 * @type {Array.<string>} Available languages.
		 */
		languages: ['en', 'de', 'fr', 'eo', 'fi', 'it'],

		/**
		 * @type {boolean} True if Aloha has been maximized in GCN.
		 */
		maximized: false,

		/**
		 * @type {Aloha.Editable} The last active editable since we disable all
		 *                        editables when a lightbox opens. We can use
		 *                        this property to reactivate the last active
		 *                        editable.
		 */
		lastActiveEditable: null,

		/**
		 * @type {Object} The scroll positions for the current window and for each
		 *                  of the last active editable's parents.
		 */
		lastActiveEditableScrollPositions: null,

		/**
		 * @type {string} Base URL for the REST API.
		 * @todo use gcnjsapi
		 * @deprecated
		 */
		restUrl: '../rest',

		/**
		 * @type {GCN.PageAPI} An API object to the page we will be working on.
		 */
		page: null,

		/**
		 * allows you to specify an error handler function by setting
		 * Aloha.settings.plugins["gcn"].errorHandler = function () {
		 * whatever... };
		 *
		 * The error handler will be called in any case of error that is
		 * handled by the GCNIntegrationPlugin, before the plugin will execute
		 * it's own error behaviour. Use the following parameters and return
		 * values
		 *
		 * An example implementation of your error handler could be:
		 *
		 * Aloha.settings.plugins["gcn"].errorHandler = function (id, data) {
		 *   alert(id); // alert the error id that occured
		 * };
		 *
		 * @param {String} errorId an id for the error. Currently, those are
		 *                         the possible ids you might encounter:
		 *		"welcomemessages" : an error occured, when the page was loaded
		 *                          - e.g. it could be locked by another user
		 *		"restcall.cancelpage" : an error occured, when canceling the
		 *                              edit process on the server. the page
		 *                              will not be unlocked
		 *		"restcall.savepage" : an error occured while saving the page
		 *		"restcall.createtag" : an error occurred while creating a new
		 *                             contenttag
		 *		"restcall.reloadtag" : an error occured while reloading an
		 *                             existing contenttag from the server
		 *		"restcall.publishpage" : error while publishing the page
		 *		"restcall.publishpageat" : error while publishing the page at a
		 *                                 certain timeframe
		 *		"restcall.renderblock" : error while rendering a contenttag or block
		 * @param {Object} data additional information regarding the error
		 * @return true the have the GCNIntegrationPlugin continue with it's
		 *         own error handling process, or false to prevent default
		 *         error handling
		 * @TODO: Deprecate this altogether
		 */
		errorHandler: function (errorId, data) {
			return true;
		},

		/**
		 * Subscribe listeners to GCN JS API message channels.
		 */
		registerGCNLibHandlers: function () {
			var plugin = this;
			if (typeof plugin.settings.errorHandler === 'function') {
				GCN.sub('error-encountered', plugin.settings.errorHandler);
			} else {
				GCN.sub('error-encountered', function (error) {
					Aloha.Log.error(GCN, error.toString());
				});
			}
			GCN.sub('tag.rendered-for-editing', function (msg) {
				Tags.decorate(msg.tag, msg.data, msg.callback);
			});
		},

		/**
		 * Display and log messages start up messages.
		 */
		showStartupMessages: function () {
			var messages;
			var message;
			var i;
			var j;

			// Display welcome messages -- Simple alerts for testing.
			if (this.settings.welcomeMessages) {
				messages = this.settings.welcomeMessages;
				j = messages.length;

				for (i = 0; i < j; ++i) {
					Dialog.alert({
						title: 'Gentics Content.Node',
						text: messages[i].message
					});
				}
			}

			// Log render messages in the console, if available.
			if (this.settings.renderMessages) {
				messages = this.settings.renderMessages;
				j = messages.length;

				for (i = 0; i < j; ++i) {
					message = messages[i];
					if (message.level &&
						Aloha.Log.isLogLevelEnabled(message.level.toLowerCase())) {
						Aloha.Log.log(message.level.toLowerCase(), this,
							message.message);
					}
				}
			}

			if (Aloha.Log.isDebugEnabled()) {
				var numEditables = this.settings.editablesNode ?
					this.settings.editablesNode.length : 0;

				var numBlocks = this.settings.blocks ?
					this.settings.blocks.length : 0;

				var numTags = this.settings.tags ?
					this.settings.tags.length : 0;

				Aloha.Log.debug(this,
					'Loaded page with id { ' + this.settings.id + ' }.');

				Aloha.Log.debug(this, 'Found ' + numEditables +
					' editables and ' + numBlocks + ' blocks.');

				Aloha.Log.debug(this, 'Found ' + numTags + ' tags.');
			}
		},

		/**
		 * registers plugin-internal handlers to Aloha Editor Events
		 */
		registerAlohaHandlers: function () {
			var that = this;

			Aloha.bind('aloha-editable-activated', function (e, params) {
				// if an editable is activated all block icons have to be

				// hidden jQuery('.aloha-editicons').fadeOut('normal');
				Misc.addEditingHelpers(params.editable.obj);
				params.editable.obj.parents('.aloha-editable').each(function () {
					Misc.addEditingHelpers(jQuery(this));
				});
			});

			Aloha.bind('aloha-smart-content-changed', function (event, data) {
				if (data.editable.isActive && data.triggerType === 'block-change') {
					Misc.addEditingHelpers(data.editable.obj);
				}
			});

			// when focus leaves the editable, the helper elements are removed
			// (if empty) or cleaned of editing-attributes
			Aloha.bind('aloha-editable-deactivated', function (e, params) {
				// remove editing helpers from the deactivated element and all it's parent editables,
				// but not if the element will be the new editable (or any of it's parents)
				var doNotDeactivate = jQuery();
				if (params.newEditable) {
					doNotDeactivate = doNotDeactivate.add(params.newEditable.obj).add(params.newEditable.obj.parents('.aloha-editable'));
				}
				params.editable.obj.add(params.editable.obj.parents('.aloha-editable')).not(doNotDeactivate).each(function () {
					Misc.removeEditingHelpers(jQuery(this));
				});
			});
		},

		/**
		 * Resolve the callback function which checks if an URL is internal
		 */
		resolveCheckForInternalLink: function () {
			var callback = false;

			if (this.settings.checkForInternalLinkFunction) {
				var path = this.settings.checkForInternalLinkFunction.split('.');
				var cur = window;

				for (var idx in path) {
					var next = path[idx];

					if (!cur.hasOwnProperty(next)) {
						cur = false;

						break;
					}

					cur = cur[next];
				}

				if (typeof cur == 'function') {
					callback = cur;
				}
			}

			if (callback) {
				GCN.settings.checkForInternalLink = callback;
			}
		},

		/**
		 * determine if a node is an Aloha Block
		 * @param {object} a DOM node to be checked
		 * @return {boolean} true if the node is an Aloha Block, false otherwise
		 */
		isAlohaBlockNode: function isAlohaBlockNode(node) {
			if (jQuery(node).hasClass("aloha-block")) {
				return true;
			}
			return false;
		},

		/**
		 * Determine if we are operating in backend mode.  Backend mode means
		 * that backend GCN facilities are available to use.
		 *
		 * @return {boolean} True if settings suggest that we are working from
		 *                   the backend.
		 */
		isBackendMode: function () {
			return !!(this.settings && this.settings.sid);
		},

		_deferred: $.Deferred(),

		/**
		 * Initializes the frontend editing scaffolding and all editables.
		 */
		init: function () {
			var that = this;

			// Create the GCMSUI Surface and set it as active.
			// This forces the UI to be rendered in the GCMS UI instead of the Aloha Page/context.
			var gcmsuiSurface = new GCMSUISurface(UiPlugin.getContext(), UiPlugin.getToolbarSettings());
			UiPlugin.setActiveSurface(gcmsuiSurface, true, true);

			// make some classes ephemeral. Those classes may be added to tags while initializing them
			// if they are not ephemeral, the modification check of the editables would always detect them
			// as modifications
			Ephemera.classes('GENTICS_block');
			Ephemera.classes('aloha-block-GCNBlock');

			GCNLinks.interjectLinkPlugin(LinkPlugin);

			// Set the proxy_prefix setting
			if (this.settings.proxy_prefix) {
				GCN.settings.proxy_prefix = this.settings.proxy_prefix;
			} else {
				GCN.settings.proxy_prefix = "";
			}

			// Set rendermode (frontend/backend)
			GCN.settings.linksRenderMode = this.settings.links || 'backend';

			// Ensure that any REST requests performed by the GCN JS API will
			// go to the correct URL in both backend and frontend mode.
			if (this.settings.webappPrefix) {
				GCN.settings.BACKEND_PATH
					= this.settings.webappPrefix.replace(/\/$/, '');
			}

			BlockManager.registerBlockType('GCNBlock', GCNBlock);

			this.showStartupMessages();
			this.registerGCNLibHandlers();
			this.registerAlohaHandlers();
			this.resolveCheckForInternalLink();

			if (this.isBackendMode()) {
				GCN.setSid(this.settings.sid);

				// Set the GCN JS API to the right channel context.
				if (this.settings.nodeId) {
					GCN.channel(parseInt(this.settings.nodeId, 10));
				}

				// When in preview mode, before doing anything with the page,
				// make sure it is loaded with update=false so that subsequent
				// accesses to the page don't lock it (it's locked by default
				// if update=false is not given).
				if (this.settings.id && Aloha.settings.readonly) {
					// TODO Error handling
					this.page = GCN.page(this.settings.id, {
						update: false
					}, function () { });
				}

				if (this.settings.id && !Aloha.settings.readonly) {
					this.setupMagicLinkConstruct();
					this._loadForEditing(function () {
						that.startKeepalivePing();
						that._deferred.resolve();
					});
				} else {
					this._deferred.resolve();
				}
			} else {
				this._deferred.resolve();
			}

			// Check for unsaved changes in the editor upon leaving the page
			// because saving is done asynchronously, we cannot save the page before leaving the page,
			// so we can just warn the editor
			// we do not use jQuery here, because for some unknown reason this won't work in IE7 mode.
			window.onbeforeunload = function () {
				if (suppressNextUnsavedChangesDialog) {
					suppressNextUnsavedChangesDialog = false;
				} else {
					if (Aloha.isModified()) {
						return (i18n.t('confirm.leavepage'));
					}
				}
			};

			// register the handler for copying tags
			if (this.settings.copy_tags) {
				ContentHandlerManager.register('gcn-tagcopy', TagCopyContentHandler);
			}

			// preview forms (mainly for page preview or in non-blocks)
			if (gcnPlugin.settings.forms) {
				this.previewForms($('body'));
			}

			return this._deferred;
		},

		/**
		 * Registers a callback function as a page object handler.  The
		 * function will be called during saving and receive, as the only
		 * argument, the page that is being saved, so that it can operate on it
		 * before its data is sent to the server.
		 * 
		 * @param {function(GCN.PageAPI)} callback The function to register.
		 */
		addPageObjectHandler: function (callback) {
			if (typeof callback === 'function') {
				pageObjectHandlers.push(callback);
			}
		},

		/**
		 * Call all registered pageObjectHandlers to operate on the given page.
		 * This plugin instance will be bound to `this' in the execution
		 * context of each handler.
		 *
		 * @param {GCN.PageAPI} page The page on which the handlers are to
		 *                           operate on.
		 */
		_callPageObjectHandlers: function (page) {
			var i;
			var that = this;
			for (i = 0; i < pageObjectHandlers.length; i++) {
				pageObjectHandlers[i].call(that, page);
			}
		},

		/**
		 * will open & highlight the correct folder in the tree if we're in
		 * the backend
		 */
		openFolderTree: function () {
			var that = this;
			GCN.page(this.settings.id).folder(function (folder) {
				// Currently the GCN JSAPI / RestAPI can't distinguish between 
				// master objects and inherited channel objects. It will always return the master object.
				// We therefor need to modify the atposidx parameter and replace the node folder id.
				var atposidx = folder.prop('atposidx');
				atposidx = atposidx.replace(new RegExp("^-[0-9]+", "g"), "-" + that.settings.nodeFolderId);
				if (top && top.tree && top.tree.openFolder) {
					top.tree.openFolder(
						that.settings.folderId,
						that.settings.nodeId,
						atposidx);
				}
			});
		},

		/**
		 * Passes the magiclinkconstruct value, that was received from the
		 * settings, on to the GCN JS API.  The magic link is the the
		 * construct which will automatically be used to create link tags in
		 * the backend.
		 *
		 * @param {object<string, object>} constructs A hash map of constructs.
		 * @param {number} magiclinkconstrcut The id of the the magic link
		 *                                    construct.
		 * @return {string|null} The keyword for the given magic link
		 *                       construct or null if there is none available.
		 */
		setMagicLinkOnGCNLib: function (constructs, magiclinkconstruct) {
			var areIdAndKeywordMatched = constructs[GCN.settings.MAGIC_LINK] &&
				constructs[GCN.settings.MAGIC_LINK].constructId ===
				magiclinkconstruct;

			if (!areIdAndKeywordMatched) {
				var keyword;

				for (keyword in constructs) {
					if (constructs.hasOwnProperty(keyword) &&
						constructs[keyword].constructId ===
						magiclinkconstruct) {
						GCN.settings.MAGIC_LINK = keyword;
						break;
					}
				}
			}
			return constructs[GCN.settings.MAGIC_LINK] ?
				constructs[GCN.settings.MAGIC_LINK].keyword : null;
		},

		/**
		 * Sets the value of the magic link construct id in the settings
		 * object.
		 *
		 * @param {number} constructId The Id of the construct to use as the
		 *                             magic link construct.
		 */
		setMagicLinkOnIntergrationPlugin: function (constructId) {
			this.settings.magiclinkconstruct = constructId;
		},

		/**
		 * Synchronises the magic link constructs between the GCN intergraction
		 * plugin and the GCN JS API.
		 *
		 * @param {function(number)=} callback Optional callback function that
		 *                                     will receive the magic link
		 *                                     construct id as its only
		 *                                     argument.
		 */
		setupMagicLinkConstruct: function (callback) {
			var hasPageIdSetting = !(
				typeof this.settings.id === 'null' ||
				typeof this.settings.id === 'undefined'
			);
			var source = hasPageIdSetting
				? GCN.page(this.settings.id).node()
				: GCN.Admin;
			var that = this;
			source.constructs(function (constructs) {
				if (that.settings.magiclinkconstruct) {
					that.setMagicLinkOnGCNLib(constructs, parseInt(
						that.settings.magiclinkconstruct, 10));
				}
				if (constructs[GCN.settings.MAGIC_LINK]) {
					that.setMagicLinkOnIntergrationPlugin(
						constructs[GCN.settings.MAGIC_LINK].constructId);
				}
				if (callback) {
					callback(that.settings.magiclinkconstruct);
				}
			});
		},

		/**
		 * Loads the page that is to be edited and places its editable tags in
		 * their designated elements on the page.
		 *
		 * @private
		 * @param {function} callback Function that is to be called when
		 *                            the page is fully loaded and rendered for
		 *                            editing.
		 */
		_loadForEditing: function (callback) {
			var plugin = this;
			plugin.page = GCN.page(plugin.settings.id, function (page) {
				if (!plugin.settings.tags) {
					if (callback) {
						callback();
					}
					return;
				}

				var tags = plugin.settings.tags;
				var numTags = tags.length;
				var tagsMap = {};
				var selectors = [];
				var isNested;
				var block;
				var data;
				var tag;
				var i;
				var j;
				var $element;
				var $blocks;
				var $elements;

				for (i = 0; i < tags.length; i++) {
					selectors.push('#' + tags[i].element);
					tagsMap[tags[i].element] = tags[i];
				}

				$elements = $(selectors.join(','));

				if (tags.length === 0 && callback) {
					callback();
				} else {
					var onRender = function () {
						if (0 === --numTags && callback) {
							callback();
						}
					};

					for (i = 0; i < tags.length; i++) {
						tag = tags[i];
						$element = jQuery('#' + tag.element);
						if (0 === $element.length) {
							onRender();
							continue;
						}
						isNested = $elements.find($element).length > 0;
						if (isNested) {
							onRender();
							continue;
						}
						$blocks = $element.find($elements);
						data = {
							tags: [tag],
							content: $element[0].outerHTML
						};
						tag.editables = tag.editablesNode;
						for (j = 0; j < $blocks.length; j++) {
							block = tagsMap[$blocks[j].id];
							block.editables = block.editablesNode;
							data.tags.push(block);
						}
						page.tag(tag.tagname)
							.edit('#' + tag.element, data, onRender);
					}
				}
				// A helper is added that searches each editable for editing-
				// paragraphs/breaks, and tidies them up when the page is saved
				plugin.addPageObjectHandler(function (page) {
					for (i = 0; i < Aloha.editables.length; i++) {
						Misc.removeEditingHelpers(Aloha.editables[i].obj);
					}
				});

				tags = plugin.settings.metaeditables;

				// The `page.name' property is special in that even though it
				// not a tag it will nevertheless have an inline editable
				// rendered for it.
				for (i = 0; i < tags.length; i++) {
					jQuery('#' + tags[i].element).aloha();
				}
			});
		},

		alohaBlocks: Tags.initializeBlocks,

		/**
		 * Cancel editing of the current page and call the callback function
		 * afterwards.
		 *
		 * @param {function} callback Function after editing was successfully
		 *                            cancelled.
		 */
		cancelEdit: function (callback) {
			var i;

			// Set editables unmodified after successful save.
			for (i in Aloha.editables) {
				if (Aloha.editables.hasOwnProperty(i) &&
					Aloha.editables[i] instanceof Aloha.Editable) {
					Aloha.editables[i].setUnmodified();
				}
			}

			this.page.unlock(callback);
		},

		/**
		 * Save the current page to the backend.
		 *
		 * @param data might contain the following settings
		 *                unlock  Whether the page shall be unlocked (defaults
		 *                        to false).
		 *             onsuccess  Handler function for saving success (defaults
		 *                        to just showing a message).
		 *             onfailure  Handler function for saving failure (defaults
		 *                        to just showing a message).
		 *                silent  Do not display any messages when saving was
		 *                        successful.
		 *                 async  Whether the page saving shall be done
		 *                        asyncronously (true), which is the defaults.
		 *         createVersion  Whether a new version shall be created (if the
		 *                        page is modifiedd). When this is not given, a
		 *                        new version will be created when the page is
		 *                        unlocked during saving.
		 */
		savePage: function (data) {
			var plugin = this;
			// If plugin.page is not set, it can be assumed that we are in
			// preview mode. In this case, the saving process is
			// short-circuited.
			if (!plugin.page) {
				if (data.onsuccess) {
					data.onsuccess(data);
				}
				return;
			}

			validate(Aloha.editables, function () {
				if (!data) {
					data = {};
				}
				if (data.asksynctrans && plugin.settings.translation_master) {
					// Check whether the page was translated from another page.
					Dialog.confirm({
						title: 'Gentics Content.Node',
						text: i18n.t('save.synctrans.confirm'),
						answer: function (answer) {
							data.synctrans = answer;
							plugin._savePage(data);
						}
					});
				} else {
					plugin._savePage(data);
				}
			});
		},

		/**
		 * Finds and specially handles editables of meta attributes like
		 * `page.name' to update the internal page object.
		 *
		 * @return {boolean} True if no error occured during this process.
		 */
		_updateMetaProperties: function () {
			var i;
			var prop;
			var propValue;
			var gcnEditable;
			var editable;
			var editables = Aloha.editables;
			var page = this.page;

			for (i in editables) {
				if (editables.hasOwnProperty(i) &&
					editables[i] instanceof Aloha.Editable) {
					editable = editables[i];
					gcnEditable = this.findLoadedEditable(editable.getId());

					if (gcnEditable && gcnEditable.metaproperty &&
						gcnEditable.metaproperty.substring(0, 5) === 'page.') {
						prop = gcnEditable.metaproperty.substring(5);
						var errorOccurred = false;
						propValue = page.prop(prop, editable.getContents(), function (error) {
							errorOccurred = true;
							var errorMessage = "Property '" + prop + "' could not be saved."
							if (error.code === 'ATTRIBUTE_CONSTRAINT_VIOLATION') {
								errorMessage = "Constraint check for property '" + prop + "' failed with reason: " + error.message;
							}

							Dialog.alert({
								title: 'Gentics Content.Node',
								text: errorMessage
							});
							return false;
						});

						if (errorOccurred) {
							return false;
						}
						editable.setUnmodified();
					}
				}
			}

			return true;
		},

		/**
		 * Internal method to save the page.
		 */
		_savePage: function (data) {
			if (!data) {
				data = {};
			}

			var showMessages = !data.silent;
			var onsuccess = data.onsuccess;
			var onfailure = data.onfailure;
			var unlock = !!data.unlock;
			var createVersion = typeof (data.createVersion) == "undefined" ? unlock : !!data.createVersion;

			if (!onfailure) {
				onfailure = function (data) {
					Dialog.alert({
						title: 'Gentics Content.Node',
						text: i18n.t('restcall.savepage.error')
					});
				};
			}

			var cancelSaveProgess;

			if (typeof data.async === 'undefined') {
				data.async = true;
			}

			// If the saving is done synchronously, we show a progress dialog.
			if (!data.async) {
				cancelSaveProgess = Dialog.progress({
					title: 'Gentics Content.Node',
					text: i18n.t('save.progress')
				});
			} else {
				cancelSaveProgess = null;
			}

			if (data.synctrans && this.settings.translation_master) {
				var translation = {
					pageId: this.settings.translation_master
				};

				if (this.settings.translation_version) {
					translation.versionTimestamp =
						this.settings.translation_version;
				}

				this.page.prop('translationStatus', translation);
			}

			if (!this._updateMetaProperties()) {
				return;
			}

			this._callPageObjectHandlers(this.page);

			this.page.save({ unlock: unlock, createVersion: createVersion }, function (page) {
				if (cancelSaveProgess) {
					cancelSaveProgess();
				}

				if (showMessages && data.messages) {
					var messages = data.messages;
					var i;
					for (i = 0; i < messages.length; ++i) {
						Dialog.alert({
							title: 'Gentics Content.Node',
							text: messages[i].message
						});
					}
				}

				if (onsuccess) {
					if (data.suppressNextUnsavedChangesDialog) {
						suppressNextUnsavedChangesDialog = true;
					}

					onsuccess(data);
				}

			}, function (error) {
				if (cancelSaveProgess) {
					cancelSaveProgess();
				}

				if (onfailure) {
					onfailure(data, error);
				}
			});
		},

		/**
		 * Deletes the current page.
		 *
		 * @param data might contain the following settings
		 * - onsuccess: handler function for deletion success
		 * - onfailure: handler function for deletion failure
		 * @param confirmed OPTIONAL whether the user has confirmed deletion.
		 * will trigger a confirmation dialog if undefined or false
		 */
		deletePage: function (data, confirmed) {
			if (!confirmed) {
				var that = this;
				Dialog.confirm({
					title: i18n.t('confirm.delete.title'),
					text: i18n.t('confirm.delete'),
					yes: function () {
						that.deletePage(data, true);
					}
				});
				return;
			}

			this.page.remove(data.onsuccess, data.onfailure);
		},

		/**
		 * Creates a folder.
		 *
		 * @deprecated Use the GCN JS API here
		 */
		createFolder: function (data) {
			// GCN.node(nodeId).createFolder(data.onsuccess, data.onfailure);

			if (typeof data.success === 'undefined') {
				data.success = function () { };
			}
			if (typeof data.failure === 'undefined') {
				data.failure = function () { };
			}

			data.url = this.settings.stag_prefix + this.restUrl + '/folder/create/';
			this.performRESTRequest(data);
		},

		/**
		 * Deletes a folder.
		 *
		 * @param data might contain the following settings
		 * - id: folder id
		 * - onsuccess (optional): handler function for deletion success
		 * - onfailure (optional): handler function for deletion failure
		 * @param confirmed OPTIONAL wheter the user has confirmed deletion.
		 * will trigger a confirmation dialog if undefined or false
		 */
		deleteFolder: function (data, confirmed) {
			if (!data.id) {
				return false;
			}

			if (!confirmed) {
				var that = this;

				Dialog.confirm({
					title: i18n.t('confirm.delete.title'),
					text: i18n.t('confirm.deletefolder'),
					yes: function () {
						that.deleteFolder(data, true);
					}
				});

				return false;
			}

			GCN.folder(data.id).remove(data.onsuccess, data.onerror);
		},

		/**
		 * Get the loaded editable with given id.
		 *
		 * @param id Aloha editable id
		 * @return the loaded editable from the settings with this id (if found)
		 */
		findLoadedEditable: function (id) {
			if (this.settings.editablesNode) {
				var i;
				for (i = 0; i < this.settings.editablesNode.length; ++i) {
					if (this.settings.editablesNode[i].id === id) {
						return this.settings.editablesNode[i];
					}
				}
			}
		},

		/**
		 * Strips unwanted classes before saving (eg. aloha-block)
		 * @param classes String that contains all the classes sperated by spaces
		 * @return the sanitized classes string
		 */
		cleanBlockClasses: function (classes) {

			if (classes) {
				return classes.replace(/aloha-block\s|aloha-block$|^aloha-block$/g, "");
			}
			return classes;
		},

		/**
		 * Make the given jQuery object (representing an editable) clean for saving
		 * Find all tables and deactivate them
		 * @param obj jQuery object to make clean
		 * @return void
		 */
		makeClean: function (obj) {
			// Remove sizset and sizecache attributes that are set by sizzle and not removed when using ie.
			obj.find("[sizset]").removeAttr("sizset");
			obj.find("[sizcache]").removeAttr("sizcache");
			// find all external links and remove their attributes
			obj.find('[data-gentics-gcn-url]').each(function () {
				jQuery(this).removeAttr('data-gentics-gcn-url');
			});
			Misc.removeEditingHelpers(obj);
		},

		/**
		 * Perform a REST request to the GCN backend REST Service.
		 * The method will automatically add the sid as request parameters, additional parameters may be given.
		 * The data may contain the following properties:
		 * - url: URL for the specific request, must start with / and must not contain request parameters
		 * - params: additional request parameters
		 * - body: request body as object, will be transformed into JSON and sent to the server
		 * - success: callback method for successful requests
		 * - error: callback method for errors
		 * - async: whether the request shall be done asynchronously (true by default)
		 * - description: i18n key of the readable description of this request (for display of end user messages in case of an error)
		 * - type: POST or GET (defaults to POST)
		 * - timeout: timeout for the AJAX request (defaults to 60000 ms)
		 * - contentType: contentType (defaults to application/json; charset=utf-8)
		 * - dataType: dataType (defaults to 'json')
		 * @param data data of the REST request
		 * @return void
		 */
		performRESTRequest: function (data) {
			if (!GCN.sid) {
				var that = this;
				GCN.sub('session.sid-set', function () {
					that.performRESTRequest(data);
				});
				return;
			}

			if (!data.type) {
				data.type = 'POST';
			}

			var ajaxSettings = {
				type: data.type,
				dataType: data.dataType || 'json',
				timeout: data.timeout || 60000,
				contentType: data.contentType || 'application/json; charset=utf-8',
				data: JSON.stringify(data.body)
			};

			ajaxSettings.url = data.url + '?sid=' + GCN.sid + '&time=' + (new Date()).getTime();

			// add requestParams if given
			if (data.params) {
				var paramName;
				for (paramName in data.params) {
					if (data.params.hasOwnProperty(paramName)) {
						if (jQuery.isArray(data.params[paramName])) {
							var i;
							var paramVal;
							for (i = 0; i < data.params[paramName].length; i++) {
								paramVal = data.params[paramName][i];
								ajaxSettings.url += '&' + paramName +
									'=' + encodeURIComponent(paramVal);
							}
						} else {
							ajaxSettings.url += '&' + paramName +
								'=' + encodeURIComponent(data.params[paramName]);
						}
					}
				}
			}

			if (typeof data.async !== 'undefined') {
				ajaxSettings.async = data.async;
			}

			if (data.success) {
				ajaxSettings.success = data.success;
			}

			if (data.error) {
				ajaxSettings.error = data.error;
			}

			jQuery.ajax(ajaxSettings);
		},

		/**
		 * Determines the editdo for a given tagid.
		 * 
		 * @param {number} tagid The id of the tag.
		 * @param {function} success Callback, which will receive the editdo as
		 *                           the first parameter.
		 * @param {function} error Callback, which will receive the
		 *                         errormessage as the first parameter.
		 */
		_getEditDo: function (tagid, success, error) {
			var page = this.page;
			Tags.getById(page, tagid, function (tag) {
				getConstructById(
					page,
					tag.prop('constructId'),
					function (construct) {
						success(construct.editdo);
					},
					error
				);
			}, error);
		},


		/**
		 * Saves the page and open the tagfill dialog for the given tag in a
		 * new window.
		 *
		 * @param {number|string} tagId The id of the tag.
		 * @param {number|string} pageId The id of page the tag belongs to.
		 *                               Note that this page must already have
		 *                               had its data fetched.
		 * @param {TagFillOptions=} options The options for opening the tag-fill.
		 */
		openTagFill: function (tagId, pageId, options) {
			var gcn = this;

			Tags.getById(GCN.page(pageId), tagId, function (tag) {
				openTagFill(tag, gcn, options);
			});
		},

		/**
		 * Create a new tag in the page
		 *
		 * @param constructId
		 *            construct id
		 * @param async
		 *            true if the call shall be made asynchronously (default), false for
		 *            synchronous call. DEPRECATED.
		 * @param success
		 *            callback function to be called, when the tag was created, if not
		 *            set, the tag will be inserted into the page
		 * @return void
		 */
		createTag: function (constructId, async, success) {
			var plugin = this;
			var selection = Aloha.Selection.getRangeObject();
			var magicValue = (selection && selection.getText)
				? selection.getText()
				: '';
			if (typeof success !== 'function') {
				success = function (html, tag, data, frontendEditing) {
					plugin.handleBlock(data, true, function () {
						Tags.decorate(tag, data);
					}, html);
				}
			}

			// after the tag was created, we need to add it (its placeholder) to the editable before the tag is
			// rendered the first time, because when the tag is rendered by POSTing the current page data to the
			// /page/renderTag/ endpoint, and rendering is done using a Mesh Portal URL, this only works, if the
			// tag is already referenced
			plugin.page.createTag({
				constructId: constructId,
				magicValue: magicValue
			}, function (tag) {
				// insert a placeholder for the tag at the current position
				var blockId = "GENTICS_BLOCK_" + tag.prop("id");
				var tagname = tag.prop("name");
				Tags.insert({
					content: "<span id='" + blockId + "' class='aloha-block'></span>"
				}, function () {
					// track the block for the new tag
					GCN.PageAPI.trackRenderedTags(plugin.page, {
						tags: [
							{
								element: blockId,
								tagname: tagname,
								onlyeditables: true
							}
						]
					});
					var currentEditable = Aloha.activeEditable.getId();
					// have the current editable updated, so that the page's data contains the placeholder for the new tag
					plugin.page._updateEditableBlocks(function (editable) {
						return editable.element == currentEditable;
					});
					tag.edit(success);
				});
			});
		},

		/**
		 * Gets the settings for a block.
		 *
		 * @param {string} tagid The tagid of the block.
		 * @return {object} Block settings or null if none is found.
		 */
		getBlockSettings: function (tagid) {
			if (!this.settings.blocks) {
				return null;
			}
			var i;
			for (i = 0; i < this.settings.blocks.length; ++i) {
				if (this.settings.blocks[i].tagid === tagid) {
					return this.settings.blocks[i];
				}
			}
			return null;
		},

		/**
		 * Gets the block with given id.
		 *
		 * @param {string} id Id of a block.
		 * @return {object} The block information or null if none is found.
		 */
		getBlockById: function (id) {
			if (!this.settings.blocks) {
				return null;
			}
			var i;
			for (i = 0; i < this.settings.blocks.length; ++i) {
				if (this.settings.blocks[i].id === id) {
					return this.settings.blocks[i];
				}
			}
			return null;
		},

		/**
		 * Reload the block with given tagid.
		 *
		 * @public
		 * @param {string} tagid Id of the tag.
		 */
		reloadBlock: function (tagid) {
			var plugin = this;
			var $block = Tags.getBlockElement(tagid);
			if (!$block) {
				return;
			}
			var page = GCN.page($block.attr('data-gcn-pageid'));
			var tagname = $block.attr('data-gcn-tagname');
			page.fetch(function (response) {
				if (GCN.getResponseCode(response) !== 'OK') {
					GCN.handleResponseError(response);
					return;
				}

				GCN.ContentObjectAPI.update(
					page.tag(tagname),
					response.page.tags[tagname]
				);

				// Because new tags may have been added to the page via the
				// tagfill dialog.
				var newTags = {};
				$.each(response.page.tags, function (name, data) {
					if (tagname !== name
						&& !GCN.TagContainerAPI.hasTagData(page, name)) {
						newTags[name] = data;
					}
				});
				GCN.TagContainerAPI.extendTags(page, newTags);

				page.tag(tagname).edit(function (html, tag, data) {
					plugin.handleBlock(data, false, function () {
						Tags.decorate(tag, data);
					}, html);
				});
			});
		},

		appendBlock: Tags.append,

		/**
		 * Insert a new block at the given selection
		 *
		 * @param {object} data Data with the following properties
		 *    content : HTML content of the block
		 *  editables : array of editables contained in the tag
		 *     blocks : array of blocks contained in the tag (including the
		 *              tag itself)
		 * @param {boolean} insert True, when the block shall be inserted at
		 *                         the current selection, false if not.
		 * @param {function} onInsert
		 * @param {string} content Optional. Processed version of data.content.
		 */
		handleBlock: function (data, insert, onInsert, content) {
			var plugin = this;

			var handle = function (data) {
				var $handled = Tags.insert(data, onInsert);

				if (0 === $handled.length) {
					plugin.log('error', 'Could not insert new tag');
				}

				$handled.find('>.aloha-editicons:not(.aloha-editicons-hover)')
					.fadeIn('fast');

				Aloha.trigger('gcn-block-handled', $handled);

				PubSub.pub('gcn.block.handled', {
					$block: $handled
				});
			};

			if (content) {
				data.content = content;
			}
			if (data.content && plugin.settings.renderBlockContentURL) {
				plugin.renderBlockContent(data.content, function (content) {
					data.content = content;
					handle(data);
				});
			} else {
				handle(data);
			}
		},

		/**
		 * Render the block content by posting it to the renderBlockContentURL
		 * configured in the plugin settings.
		 *
		 * @param {string} content Content of the block to be rendered.
		 * @param {function(string)} callback Function to receive the rendered
		 *                                    content.
		 * @return {string} Response from posting `content` to
		 *                  renderBlockContentURL.
		 */
		renderBlockContent: function (content, callback) {
			var plugin = this;
			var output = '';
			jQuery.ajax({
				url: plugin.settings.renderBlockContentURL,
				type: 'POST',
				timeout: 10000,
				data: { content: content },
				dataType: 'text',
				async: false,
				success: function (response) {
					if (callback) {
						callback(response);
					}
					output = response;
				},
				error: function (data) {
					plugin.errorHandler('restcall.renderblock', data);
					if (callback) {
						callback('');
					}
				}
			});
			return output;
		},

		/**
		 * When the page is openend in edit mode, a request to "save" the page (empty)
		 * is sent to the REST API for keeping the page lock alive
		 */
		startKeepalivePing: function () {
			if (!Aloha.settings.readonly) {
				var plugin = this;
				window.setInterval(function () {
					var data = {
						url: plugin.settings.stag_prefix + plugin.restUrl + '/page/save/' + plugin.settings.id,
						body: {
							page: {
								id: plugin.settings.id
							}
						},
						type: 'POST'
					};
					plugin.performRESTRequest(data);
				}, 60000);
			}
		},

		/**
		 * Render previews for the forms found under the given element.
		 * 
		 * @param {jQuery} $element jQuery root element
		 */
		previewForms: function ($element) {
			var plugin = this;
			$element.find('[data-gcn-formid]').each(function () {
				var $elem = $(this);
				var formId = $elem.attr('data-gcn-formid');
				var formLanguage = $elem.attr('data-gcn-formlanguage');
				var preview = {
					url: plugin.settings.stag_prefix + plugin.restUrl + '/form/' + formId + '/preview/' + formLanguage,
					type: 'GET',
					dataType: 'html',
					success: function (html) {
						var $form = $(html);
						$form.filter('form').each(function () {
							$(this).submit(false);
						});
						$form.find('form').each(function () {
							$(this).submit(false);
						});
						$elem.replaceWith($form);
					},
					error: function (jqXHR, status, error) {
						Aloha.Log.error(plugin, "Error while rendering form " + formId + ": " + error);
					}
				};
				plugin.performRESTRequest(preview);
			});
		}
	}); // End of Create Plugin

	window.Aloha.GCN = gcnPlugin;

	return gcnPlugin;
});
