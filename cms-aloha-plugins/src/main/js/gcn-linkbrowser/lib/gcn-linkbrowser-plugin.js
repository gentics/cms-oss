define([
	'aloha',
	'aloha/console',
	'jquery',
	'aloha/plugin',
	'aloha/pluginmanager',
	'link/link-plugin',
	'RepositoryBrowser',
	'ui/ui',
	'ui/button',
	'i18n!gcn-linkbrowser/nls/i18n',
	'i18n!aloha/nls/i18n',
	'./gcn-tooltips',
	'gcn/gcn-plugin',
	'./vendor/tipsy',
	'css!gcn-linkbrowser/css/gcn-linkbrowser.css'
],
function(
	Aloha,
	Console,
	jQuery,
	Plugin,
	PluginManager,
	Links,
	Browser,
	Ui,
	Button,
	i18n,
	i18nCore,
	TOOLTIPS,
	GCNPlugin
) {
	'use strict';

	/**
	 * Reference to jQuery
	 *
	 * @type {jQuery}
	 */
	var $ = jQuery;

	/**
	 * RegEx to split a GCN id of the form `10009.12' into an object type id, and
	 * the object id.
	 *
	 * @type {number}
	 * @const
	 */
	var RX_ID_PARTS = /(\d+)\.(\d+)/;

	/**
	 * Max number of languages to displayed in the 'translations' colum;
	 * @type {number}
	 * @const
	 */
	var MAX_LANGUAGES = 6;

	/**
	 * Given an GCN repository object, will determin it's object id.
	 *
	 * @type {object} item An object that has an id string property.
	 * @return {string} The given item's object id.
	 */
	function getItemId(item) {
		var match = item.id.match(RX_ID_PARTS);
		return match ? match[2] : item.id;
	}

	/**
	 * Get the appropriate page icon src for the given language, status and
	 * heritage level.
	 *
	 * @param {number} status Page status.
	 * @param {string} language Page language.
	 * @param {boolean} inherited True if the page is inherited, false
	 *                            otherwise.
	 * @return {string} Source url of page icon.
	 */
	function getPageIcon( status, language, inherited ) {
		var filename;
		var url;
		var suffix = inherited ? '_mc' : '';

		switch( status ) {
		case 0:
			filename = 'doc_hi' + suffix + '.png';
			break;
		case 3:
			filename = 'doc_off' + suffix + '.png';
			break;
		case 4:
			filename = 'doc_queue' + suffix + '.png';
			break;
		case 5:
			filename = 'doc_bw' + suffix + '.png';
			break;
		case 6:
			filename = 'doc_timepub' + suffix + '.png';
			break;
		case 1:
		case 2:
		default:
			filename = 'doc' + suffix + '.png';
			break;
		}

		var sid;
		if (typeof GCN !== 'undefined' && typeof GCN !== 'null') {
			sid = GCN.sid;
		} else {
			sid =  GCNPlugin.settings.sid;
		}

		url = GCNPlugin.settings.stag_prefix + '?sid=' + sid +
		      '&do=14203&base=' + filename + '&module=content';

		if ( language ) {
			url += '&lang=' + language;
		}

		return url;
	}

	/**
	 * Gets id of the master folder of folder channel 'channelFolderId' from a list of items.
	 * @param {Object} items List of node and channels
	 * @param {number} channelFolderId Id of channel folder
	 * @return {number} Id of the master node.
	 */
	function getMasterNodeId(items, channelFolderId) {
		var i;

		for (i = 0; i < items.length; i++) {
			if (items[i].type === 'channel' && items[i].folderId === channelFolderId) {
				return items[i].masterFolderId;
			}
		}

		return channelFolderId;
	}

	/**
	 * Checks if auto target is configured.
	 *
	 * @return {boolean}
	 */
	function isAutoTargetConfigured() {
		return Links.target && $.trim(Links.target).length > 0
		    && Links.targetregex && $.trim(Links.targetregex).length > 0;
	}

	/**
	 * Get the translation from the given i18n object.
	 * The object should be composed like:
	 * {
	 *   "en": "Path",
	 *   "de": "Pfad"
	 * }
	 *
	 * If the translation in the current language is not found,
	 * the first translation will be returned
	 * @param i18nObject {Object} i18n Object
	 * @return translation {String}
	 */
	function _i18n(i18nObject) {
		if (!i18nObject) {
			return '';
		}
		if (i18nObject.hasOwnProperty(Aloha.settings.locale)) {
			return i18nObject[Aloha.settings.locale];
		}

		for (var lang in i18nObject) {
			if (i18nObject.hasOwnProperty(lang)) {
				return i18nObject[lang];
			}
		}

		return '';
	}

	/**
	 * Repository Browser implementation for browsing GCN links.
	 *
	 * @class {LinkBrowser}
	 * @extends {Browser}
	 */
	var LinkBrowser = Browser.extend({

		/**
		 * Initialize this link browser instance.
		 *
		 * @override
		 */
		init: function ( config ) {
			config.objectTypeFilter.push('images');
			this._super( config );

			var that = this;

			// add class aloha-dialog to avoid deactivating current editable when user clicks in repo browser
			that.element.addClass('aloha-dialog');

			this._gcnLinkBrowserButton = Ui.adopt('gcnLinkBrowser', Button, {
				tooltip: i18n.t('button.addlink.tooltip'),
				icon: 'aloha-icon-tree',
				click: function() {
					jQuery('body').addClass('gcn-stop-scrolling');
					that.show();
				}
			});

			if ( GCN && !GCN.sid ) {
				this._gcnLinkBrowserButton.show(false);
				var setupDone = false;
				GCN.sub( 'session-set', function () {
					if ( setupDone ) {
						return;
					}
					setupDone = true;
					Aloha.require( 'gcn/gcn-plugin' )
						.setupMagicLinkConstruct(function(magiclinkconstruct){
                            if ( null !== magiclinkconstruct &&
                                 typeof magiclinkconstruct !== 'undefined' ) {
								that._gcnLinkBrowserButton.show();
							}
						});
				});
			} else {
				this._gcnLinkBrowserButton.show();
			}

			Aloha.on( 'aloha-link-selected', function ( event, rangeObject ) {
				that._gcnLinkBrowserButton.show();
			});

			Aloha.on( 'aloha-link-unselected', function ( event, rangeObject ) {
				that._gcnLinkBrowserButton.show(false);
			});

			this.getList()
				.delegate('tr.jqgrow', 'mouseenter', function () {
					that.onItemEnter($(this));
				})
				.delegate('tr.jqgrow', 'mouseleave', function () {
					that.onItemLeave($(this));
				});
		},

		/**
		 * Reference to the item that is currently whose preview tooltip is currently on display.
		 *
		 * @type {jQuery.<HTMLElement>}
		 */
		$preview: null,

		/**
		 * A collision filter hash map of ids of Repository Documents items in
		 * the browser list have tooltips bound to them.
		 *
		 * @TODO: Encapsulate this detain in gcn-thumbs-previews.js
		 * @type {object<string, boolean>}
		 */
		previewBindings: null,

		/**
		 * Lists Repository Documents given in `data' into the Repository
		 * Brower's list and then initializes all necessary tooltips for
		 * previews and ellipsises.
		 *
		 * @override
		 */
		_listItems: function (data) {
			this._super(data);
			this.previews();
			this.ellipsises();
		},

		/**
		 * Closes the Repository Browser and immediately closes all visible
		 * tooltips.
		 *
		 * @override
		 */
		close: function () {
			jQuery('body').removeClass('gcn-stop-scrolling');
			TOOLTIPS.closeTooltips();
			this._super();
		},

		/**
		 * Copies the attributes specified in Links.settings.sidebar from
		 * the original item to the rendition.
		 *
		 * @param item {Object} The original item
		 * @param rendition {Object} The rendition to extend.
		 * @return {Object} The rendition extended with attributes of
		 *		<code>item</code> listed in Links.settings.sidebar.
		 */
		extendRendition: function (item, rendition) {
			var extended = $.extend({}, rendition);
			var attributes = Links.settings.sidebar;

			for (var idx in attributes) {
				var attr = attributes[idx].attr;
				var itemAttr = item[attr];

				if (itemAttr && !extended[attr]) {
					extended[attr] = itemAttr;
				}
			}

			return extended;
		},

		/**
		 * Handles click in an ellipses tooltip.
		 *
		 * Will determine whether a language icon was clicked, and if so,
		 * will invoke onSelect(), passing to it the Repository Rendition
		 * that corresponds with the selected language.
		 *
		 * @param {jQuery.<HTMLElement>} $target jQuery unit set containing
		 *                                       the DOM element that initiated
		 *                                       the click event.
		 * @param {jQuery.<HTMLElement>} $item jQuery unit set containing an
		 *                                     item in the browser list.  This
		 *                                     item is used to determine the
		 *                                     appropriate Repository Document
		 *                                     that corresponds with the
		 *                                     clicked icon.
		 */
		ellipsisClicked: function ($target, $item) {
			// Check whether user clicked onto a translation image.
			var transImg = $target.filter('img[id|="trans"]');
			if (0 === transImg.length) {
				return;
			}
			this.lastClickedRow = $item;
			var item = this.getCachedDocument($item);
			var renditions = item.renditions;
			var transId = transImg.attr('id').substr(6);
			var i;
			for (i = 0; i < renditions.length; i++) {
				if (renditions[i].id === transId) {
					this.onSelect(this.extendRendition(item, renditions[i]));
					break;
				}
			}
			TOOLTIPS.closeTooltips();
		},

		/**
		 * Initializes ellipsis tooltips on the items currently rendered in the
		 * browser list.
		 */
		ellipsises: function () {
			if (!$.fn.tipsy) {
				return;
			}
			var $list = this.getList();
			var browser = this;
			$list.find('.gtx-repobrowser-ellipsis').each(function () {
				var $item = jQuery(this);
				var $td = $item.closest('td');
				$td.attr('title', $item[0].outerHTML)
				   .tipsy(TOOLTIPS.TIPSY_ELLIPSIS_CONFIG);
				var $tr = $td.closest('tr');
				$item.remove();
				var onClicked = function (event) {
					browser.ellipsisClicked($(event.target), $tr);
				};
				$td.mouseenter(function ($event) {
					TOOLTIPS.showEllipsis($td, onClicked);
					$event.stopPropagation();
				});
			});
		},

		/**
		 * Gets the browser list
		 *
		 * @return {jQuery.<HTMLElement>} jQuery unit set of the browser list
		 *                                DOM element.
		 */
		getList: function () {
			return this.$_list;
		},

		/**
		 * Retreives the Repository Document from the cache.
		 *
		 * @param {jQuery.<HTMLElement>} $item The element whose document is
		 *                                     to be retrieved.
		 * @return {object} A Repository Document.
		 */
		getCachedDocument: function ($item) {
			return this._cachedRepositoryObjects[$item.attr('id')];
		},

		/**
		 * Checks whether the given list item has preview tooltip bound to it.
		 *
		 * @param {jQuery.<HTMLElement>} $item The element whose document is
		 *                                     to be retrieved.
		 * @return {boolean} True if the tooltip exists for the element.
		 */
		hasPreview: function ($item) {
			var item = this.getCachedDocument($item);
			return this.previewBindings && this.previewBindings[item.id];
		},

		/**
		 * Checks whether the given item's preview tooltip is currently on
		 * display.
		 *
		 * @param {jQuery.<HTMLElement>} $item A list element.
		 * @return {boolean} True if the element's preview is on display.
		 */
		isCurrentPreview: function ($item) {
			return this.$preview && (this.$preview[0] === $item[0]);
		},

		/**
		 * Initializes preview tooltips for all currently rendered browser list
		 * items.
		 */
		previews: function () {
			if (!TOOLTIPS.TIPSY) {
				return;
			}
			this.previewBindings = {};
			var $list = this.getList();
			var browser = this;
			$list.find('.gtx-repobrowser-thumbnail').each(function () {
				var $item = jQuery(this);
				var $tr = $item.closest('tr');
				if ($tr.length) {
					var item = browser.getCachedDocument($tr);
					browser.previewBindings[item.id] = true;
					$tr.attr('title', browser.renderPreview(item))
					   .tipsy(TOOLTIPS.TIPSY_PREVIEW_CONFIG);
				}
			});
		},

		/**
		 * Handles interaction when the cursor move into a list item.
		 *
		 * @param {jQuery.<HTMLElement>} $item The list item which initiated
		 *                                     the event.
		 */
		onItemEnter: function ($item) {
			TOOLTIPS.closeTooltips();
			if (this.hasPreview($item)) {
				this.$preview = TOOLTIPS.showPreview($item);
			}
		},

		/**
		 * Handles interaction when the cursor moves out of a list item.
		 *
		 * @param {jQuery.<HTMLElement>} $item The list item which initiated
		 *                                     the event.
		 */
		onItemLeave: function ($item) {
			TOOLTIPS.initiateTooltipClosing();
			if (this.isCurrentPreview($item)) {
				this.$preview = null;
			}
		},

		/**
		 * Invokes the onSelect() callback; passing it to the selected
		 * repository document.
		 *
		 * @param {object} item The selected Repository Document.
		 */
		onSelect: function (item) {
			// A magiclinkconstruct must be specified in order to create
			// backend links.
			if (null === GCNPlugin.settings.magiclinkconstruct ||
			     typeof GCNPlugin.settings.magiclinkconstruct === 'undefined') {
				Console.error('Creating backend links is not supported with your ' +
					'current configuration.  The `magiclinkconstruct\' ' +
					'property needs to be specified in your Aloha.settings .');
				return;
			}

			// This sets the href to # when a GCN page/file is selected.
			// Additionally we correctly take the link-plugins targetRegex
			// setting in account.
			var reference = '#';
			Links.hrefField.setAttribute('href', reference);
			if (isAutoTargetConfigured() && reference.match(new RegExp(Links.targetregex))) {
				Links.hrefField.setAttribute('target', Links.target, Links.targetregex, reference);
			}
			Links.hrefField.setItem(item);

			Links.automaticallySetTitle(
				Links.hrefField,
				Links.title,
				Links.titleregex
			);

			// Now create a selection within the editable since the user should
			// be able to type once the link has been created.

			// 1. We need to save the current cursor position since the a
			// activate editable event will be fired and this will set the
			// cursor in the upper left cornor of the editable.
			var	range = Aloha.Selection.getRangeObject();
			var currentStartContainer = range.startContainer = range.endContainer;
			var currentStartOffset = range.startOffset = range.endOffset;

			// 2. Do the first select - this will invoke the activate editable
			// event.
			range.select();

			// 3. Restore the range.
			range.startContainer = range.endContainer = currentStartContainer;
			range.startOffset = range.endOffset = currentStartOffset;

			// 4. Invoke the final selection.
			range.select();

			this.close();
		},

		/**
		 * Renders the given Repository Document's preview.
		 *
		 * @param {object} item A RepositoryDocument.
		 * @return {string} The preview content.
		 */
		renderPreview: function(item) {
			var target = TOOLTIPS.constrain(
					item.sizeX || TOOLTIPS.PREVIEW_SIZE,
					item.sizeY,
					TOOLTIPS.PREVIEW_SIZE
				);
			var id = getItemId(item);
			var previewSrc = TOOLTIPS.createGenticsImageStoreUrl(
					id, target.width, target.height, 'prop'
				);
			var thumbnailSrc = TOOLTIPS.createGenticsImageStoreUrl(
					id,
					TOOLTIPS.THUMBNAIL_WIDTH,
					TOOLTIPS.THUMBNAIL_HEIGHT,
					'smart'
				);
			var placeholder = '\
				<img src="' + thumbnailSrc + '"\
					class="gtx-repobrowser-preview-placeholder" alt=""\
					style="width:' + target.width + 'px;\
			               height:' + target.height + 'px;"\
					/>';
			var filesize = TOOLTIPS.format(item.fileSize);
			var html = null;
			if (item.gisResizable) {
				html = '<div>' + placeholder + '<img src="' + previewSrc
						 + '" alt="" class="gtx-repobrowser-preview-image"/>'
						 + '</div>';
			} else {
				html = '<div>' + i18n['No preview available'] + '<br /><br />'
						+ i18n['Not supported by all browsers'] + '</div><br />';
			}
			if (typeof item.sizeX !== 'undefined'
				&& typeof item.sizeY !== 'undefined' && item.sizeX > 0) {
				html += item.sizeX + ' × ' + item.sizeY;

				if (filesize) {
					html += ' - ';
				}
			}
			var filesize = TOOLTIPS.format(item.fileSize);
			if (filesize) {
				html += filesize;
			}

			return '<div class="gtx-repobrowser-preview">' + html + '</div>';
		},

		/**
		 * Renders the colums for the given Repository Document.
		 *
		 * @param {object} item A RepositoryDocument.
		 * @return {object<string, string>} A hash map of columns and their
		 *                                  corresponding content.
		 */
		renderRowCols: function (item) {
			var row  = {};
			var host = this.host;
			var url = '';
			var objTypeId;
			var idMatch = item.id.match(/(\d+)\.(\d+)/);

			if (idMatch) {
				objTypeId = idMatch[1];
			}

			jQuery.each( this.columns, function ( colName, v ) {
				switch ( colName ) {
				case 'icon':
					switch( objTypeId ) {
					case '10002':
						row.icon = '<img src="';
						if ( item.inherited ) {
							row.icon += GCNPlugin.settings.stag_prefix + '?do=11&img=folderclosed_mc.gif&module=content';
						} else {
							row.icon += GCNPlugin.settings.stag_prefix + '?do=11&img=folderclosed.gif&module=content';
						}
						row.icon += '"/>';
						break;
					case '10006':
						row.icon = '<img src="';
						if ( item.inherited ) {
							row.icon += GCNPlugin.settings.stag_prefix + '?do=11&img=template_mc.gif&module=content';
						} else {
							row.icon += GCNPlugin.settings.stag_prefix + '?do=11&img=template.gif&module=content';
						}
						row.icon += '"/>';
						break;
					case '10007':
						row.icon = '<img src="';
						row.icon += getPageIcon( item.status, null, item.inherited );
						row.icon += '"/>';
						break;
					case '10008':
						row.icon = '<img src="';
						if ( item.inherited ) {
							row.icon += GCNPlugin.settings.stag_prefix + '?do=11&img=file_mc.gif&module=content';
						} else {
							row.icon += GCNPlugin.settings.stag_prefix + '?do=11&img=file.gif&module=content';
						}
						row.icon += '"/>';
						break;
					case '10011':
						row.icon = TOOLTIPS.createThumbnail(
								idMatch ? idMatch[2] : item.id,
								item.inherited, item.gisResizable);
						break;
					}
					break;
				case 'translations':
					var renditions = item.renditions;
					if (renditions && renditions.length) {
						var rendition;
						var ellipsis = [];
						var strBldr = [];
						var count = 0;
						var i;
						var len = renditions.length;
						var img;
						for (i = 0; i < len; i++) {
							rendition = renditions[i];
							if ('translation' === rendition.kind) {
								img = '<img id="trans-' + rendition.id + '"'
								    + ' src="' + getPageIcon(rendition.status,
								    	rendition.language, rendition.inherited)
								    + '"'
								    + ' alt="' + rendition.languageName + '"'
								    + ' title="' + rendition.languageName + '" style="float: left; margin-right: 1px;"/>';
								count++;
								if ((count < MAX_LANGUAGES) || v.expandLanguages) {
									strBldr.push(img);
								} else {
									if (MAX_LANGUAGES === count) {
										strBldr.push('<span>...</span>');
									}
									ellipsis.push(img);
								}
							}
						}
						row.translations = strBldr.join(' ');
						if (ellipsis.length) {
							row.translations +=
								'<div class="gtx-repobrowser-ellipsis">'
							    + ellipsis.join('')
							    + '</div>';
						}
						// Keep the icons in one line. Same line-height as the height of the icons
						row.translations = "<div style='white-space: normal; line-height: 18px'>" + row.translations + "</div>";
					}
					break;
				case 'template':
					row[colName] = item[colName]? item[colName].name : '--';
					break;
				default:
					row[ colName ] = item[ colName ] || '--';
				}
			});

			return row;
		},

		/**
		 * Handles clicks on browser list items.
		 *
		 * Calls the onSelect() callback and passes to it the Repository
		 * Document that corresponds with the item that was clicked.
		 *
		 * @override
		 */
		rowClicked: function (event) {
			// Check whether user clicked onto a translation image.
			var transImg = jQuery(event.target).filter('img[id|="trans"]');
			var row = jQuery(event.target).closest('tr');
			var rends, uid, transId, i;
			this.lastClickedRow = row;
			if (transImg.length > 0 && row.length > 0) {
				// Special behaviour for click onto translation icons.
				transId = transImg.attr('id').substr(6);
				var item = this.getCachedDocument(row);
				rends = item.renditions;
				for (i = 0; i < rends.length; i++) {
					if (rends[i].id === transId) {
						this.onSelect(this.extendRendition(item, rends[i]));
						break;
					}
				}
			} else {
				this._super(event);
			}
		},

		/**
		 * Overwrite fetchItems (which is called whenever a folder in the tree was clicked)
		 * @param folder
		 * @param callback
		 */
		_fetchItems: function (folder, callback) {
			// if the root folder was selected, we show the search field, otherwise we hide it
			if (folder) {
				if (folder.id === 'com.gentics.aloha.GCN.Page') {
					this.$_grid.find('.repository-browser-search-field, .repository-browser-search-btn').hide();
				} else {
					this.$_grid.find('.repository-browser-search-field, .repository-browser-search-btn').show();
				}
			} else {
				this.$_grid.find('.repository-browser-search-field, .repository-browser-search-btn').hide();
			}
			// call the _super method to really fetch the items
			this._super(folder, callback);
		},

		/**
		 * Handle repository timeouts
		 */
		handleTimeout: function () {
			Console.error(i18n.t('repository.browser.timeout'));
		},

		/**
		 * Filters items before displaying it in the tree browser. Only channels from
		 * the master will be displayed.
		 * @override
		 */
		_processRepoResponse: function(items, metainfo, callback) {
			var filteredItems = [];
			var i;
			var masterNodeId = getMasterNodeId(items, parseInt(GCNPlugin.settings.nodeFolderId, 10));

			for (i = 0; i < items.length; i++) {
				if (items[i].type !== 'channel' || items[i].masterFolderId === masterNodeId) {
					filteredItems.push(items[i]);
				}
			}

			this._super(filteredItems, metainfo, callback);
		}
	});

	var LinkBrowserPlugin = Plugin.create( 'gcn-linkbrowser', {
		browser: null,
		init: function () {
			var totalWidth = jQuery( window ).width() * 0.93;
			var defaultConfig = {
				repositoryManager : Aloha.RepositoryManager,
				repositoryFilter  : [ 'com.gentics.aloha.GCN.Page' ],
				objectTypeFilter  : [ 'website' ],
				renditionFilter	  : [ '*' ],
				filter			  : [ 'language', 'status', 'inherited', 'sizeX', 'sizeY', 'fileSize', 'gisResizable' ],
				treeWidth		  : 232,
				totalWidth		  : totalWidth,
				adaptPageSize	  : true,
				rowHeight         : 26,
				viewsortcols: [true],
				types: {
					types: {
						node: {
							icon: {
								image: GCNPlugin.settings.stag_prefix + '?do=11&img=domain.gif&module=content'
							}
						},
						channel: {
							icon: {
								image: GCNPlugin.settings.stag_prefix + '?do=11&img=domain_mc.gif&module=content'
							}
						},
						folder: {
							icon: {
								image: GCNPlugin.settings.stag_prefix + '?do=11&img=folderclosed.gif&module=content'
							}
						},
						inheritedfolder: {
							icon: {
								image: GCNPlugin.settings.stag_prefix + '?do=11&img=folderclosed_mc.gif&module=content'
							}
						},
						repository: {
							icon: {
								image: GCNPlugin.settings.stag_prefix + '?do=11&img=content.gif&module=content'
							}
						}
					}
				},

				rootPath : Aloha.settings.baseUrl + '/vendor/repository-browser/'
			};

			var config = jQuery.extend(true, {}, defaultConfig, this.settings);

			// If the link plugin tries to display any special attributes
			// in the sidebar, make sure the repository does not filter
			// them out.
			if (Links.settings.sidebar) {
				$.each(Links.settings.sidebar, function (idx, val) {
					config.filter.push(val.attr);
				});
			}

			if (typeof config.expandLanguages === 'string') {
				config.expandLanguages = (config.expandLanguages.toLowerCase() === 'true');
			}

			if (typeof config.adaptPageSize === 'string') {
				config.adaptPageSize = (config.adaptPageSize.toLowerCase() === 'true');
			}

			if (typeof config.pageSize === 'string') {
				config.pageSize = parseInt(config.pageSize, 10);
			}

			var translationColumConfig = { title: '', width: 90, sortable: false, resizable: false };
			if (config.expandLanguages) {
				translationColumConfig.resizable = true;
				translationColumConfig.width = undefined;
				translationColumConfig.expandLanguages = config.expandLanguages;
			}

			// this come from the user settings. Look to perform_aloha_redirect.php, called by page_edit.php
			if (typeof window.top.repo_browser_paging !== 'undefined') {
				config.paging = window.top.repo_browser_paging;
			}

			// Set pageSize to 10000 to set the repository browser to no page the results
			if (config.paging === false) {
				config.pageSize = 10000;
				config.adaptPageSize = false;
			}

			if (config.columns) {
				var columns = {};

				jQuery.each(config.columns, function (i, value) {
					jQuery.each(value, function(k, v) {

						if (typeof v.resizable === 'string') {
							v.resizable = (v.resizable.toLowerCase() === 'true' || v.resizable.toLowerCase() === '1');
						}

						if (typeof v.sortable === 'string') {
							v.sortable = (v.sortable.toLowerCase() === 'true' || v.sortable.toLowerCase() === '1');
						}

						if (typeof v.fixed === 'string') {
							v.fixed = (v.fixed.toLowerCase() === 'true' || v.fixed.toLowerCase() === '1');
						}

						if (typeof v.title === 'object') {
							v.title = _i18n(v.title);
						}

						config.filter.push(k);
						columns[k] = v;
					});
				});
				config.columns = columns;
				if (typeof config.columns.translations !== 'undefined') {
					if (config.columns.translations) {
						translationColumConfig.title = config.columns.translations.title;
					}
					config.columns.translations = translationColumConfig;
				}
			} else {
				config.columns = {
					icon         : { title: '',     width: 22,  fixed: true, sortable: false, resizable: false },
					name         : { title: 'Name', width: 200, sorttype: 'text', sortable: true },
					language     : { title: '',     width: 22,  fixed: true, sortable: false, resizable: false },
					translations : translationColumConfig,
					path         : { title: i18n.t('column.folder'), width: 200, sortable: false }
				};
			}

			this.browser = new LinkBrowser(config);
		}
	});

	return LinkBrowserPlugin;
});
