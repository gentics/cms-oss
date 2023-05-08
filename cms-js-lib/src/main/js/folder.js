(function (GCN) {

	'use strict';

	/**
	 * @private
	 * @const
	 * @type {number}
	 */
	//var TYPE_ID = 10002;

	/**
	 * @const
	 * @private
	 * @type {object<string, *>} Default folder settings.
	 */
	var DEFAULT_SETTINGS = {
		// Load folder privileges as well
		privileges: true,
		update: true
	};

	/**
	 * @class
	 * @name FolderAPI
	 * @extends ContentObjectAPI
	 * @extends TagContainerAPI
	 * 
	 * @param {number|string}
	 *            id of the file to be loaded
	 * @param {function(ContentObjectAPI))=}
	 *            success Optional success callback that will receive this
	 *            object as its only argument.
	 * @param {function(GCNError):boolean=}
	 *            error Optional custom error handler.
	 * @param {object}
	 *            settings currently there are no additional settings to be used
	 */
	var FolderAPI = GCN.defineChainback({
		/** @lends FolderAPI */

		__chainbacktype__: 'FolderAPI',
		_extends: [ GCN.TagContainerAPI, GCN.ContentObjectAPI ],
		_type: 'folder',

		/**
		 * Writable properties for the folder object.
		 * Currently description, motherId, name and publishDir are writeable.
		 * 
		 * @public
		 * @type {Array.<string>} 
		 */
		WRITEABLE_PROPS: [ 'description',
		                   'motherId',
		                   'name',
		                   'publishDir' ],

		/**
		 * Persist changes made to the object in Gentics Content.Node .
		 * 
		 * @public
		 * @param {function(FolderAPI)=}
		 *            success Optional callback that will receive this object as
		 *            its only argument.
		 * @param {function(GCNError):boolean}
		 *            error Optional custom error handler.
		 */
		save: function (success, error) {
			this._save(null, success, error);
		},

		/**
		 * Removes the folder and all its children
		 *
		 * @public
		 * @param {function(FolderAPI)=} success Optional callback that will
		 *                                       receive this object as its
		 *                                       only argument.
		 * @param {function(GCNError):boolean} error Optional custom error
		 *                                           handler.
		 */
		remove: function (success, error) {
			this._remove(success, error);
		},

		/**
		 * Gets this folder's parent folder. If this folder does not have a
		 * parent, then the returned object will be an API to an object that
		 * does not exists. Only when attempting to perform read/write
		 * operations on this object on the server will a `NOTFOUND' error be
		 * encountered. We recognize that this is relatively late for the use to
		 * find out that this folder has no parent; if the use need to guarantee
		 * that a parent folder exists before further operations, they are
		 * simply to pass a callback into this function.
		 * 
		 * @name parent
		 * @function
		 * @memberOf FolderAPI
		 * @public
		 * @param {function(FolderAPI)=}
		 *            success Optional callback that will receive this object as
		 *            its only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 * @return {FolderAPI} The parent folder
		 */
		'!parent': function (success, error) {
			this._continue(GCN.FolderAPI, this.prop('motherId'), success, error);
		},

	    /**
		 * Check if a given permission is available for a folder. If no name is
		 * provided an array of available permissions is returned.
		 * 
		 * @name perm
		 * @memberOf FolderAPI
		 * @public
		 * @function
		 * @param {name}
		 *            optional Privilege name to be checked. possible values
		 *            are: "viewfolder" "createfolder" "updatefolder"
		 *            "deletefolder" "viewpage" "createpage" "updatepage"
		 *            "deletepage" "publishpage" "viewtemplate" "createtemplate"
		 *            "linktemplate" "updatetemplate" "deletetemplate"
		 * @return {boolean|Array.<string>} Permission value for the given name
		 *         or an array of permissions
		 */
		'!perm': function (name) {
			var i;

			if (!name) {
				return this._data.privileges;
			}

			for (i in this._data.privileges) {
				if (this._data.privileges.hasOwnProperty(i) &&
						this._data.privileges[i] === name) {
					return true;
				}
			}

			return false;
		},

		/**
		 * Get this content object's node.
		 * 
		 * @public
		 * @function
		 * @name node
		 * @memberOf FolderAPI
		 * @param {funtion(NodeAPI)=}
		 *            success Optional callback to receive a {@link NodeAPI}
		 *            object as the only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 * @return {NodeAPI} This object's node.
		 */
		'!node': function (success, error) {
			return this._continue(GCN.NodeAPI, this._data.nodeId, success, error);
		},

		// ====================================================================
		// Pages
		// ====================================================================

		/**
		 * Returns page of the given id which resides in this folder.
		 * 
		 * @public
		 * @function
		 * @name page
		 * @memberOf FolderAPI
		 * @param {number}
		 *            id
		 * @param {function(PageAPI)=}
		 *            success Optional callback that will receive a
		 *            {@link PageAPI} object as its only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 * @return {PageAPI}
		 */
		'!page': function (id, success, error) {
			return this._continue(GCN.PageAPI, id, success, error);
		},

		/**
		 * Retreive a list of all pages this folder.
		 *
		 * @param {function(Array.PageAPI)=} success Optional callback that
		 *                                           will receive an array of
		 *                                           {@link PageAPI} objects as
		 *                                           its only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		pages: function (success, error) {
			this._getItems('page', success, error);
		},

		/**
		 * Creates a new page inside this folder.
		 *
		 * @param {number} templateId The id of the template to be used for
		 *                            the page.
		 * @param {object} options Set all the options to create a page the
		 *                         following options are allowed:
		 * <pre>
		 * GCN.folder(4711).createPage(13, {
		 *     // set a language code for the new page like 'en', 'de', ...
		 *     // if you don't supply a language code the page will have
		 *     // no language assigned
		 *     language: 'en',
		 *     // id of the page this page should be a variant of
		 *     variantId: 42
		 *   });
		 * </pre>
		 * @param {function(PageAPI)=} success Optional callback that will
		 *                                     receive a {@link PageAPI} object
		 *                                     as its only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 * @return {PageAPI} The newly created page.
		 */
		createPage: function () {
			var args = Array.prototype.slice.call(arguments);
			var templateId = args[0];
			var options;
			var success;
			var error;
			var j = args.length;
			var i;

			// Determine `options', `success', `error'
			for (i = 1; i < j; ++i) {
				switch (jQuery.type(args[i])) {
				case 'function':
					if (success) {
						error = args[i];
					} else {
						success = args[i];
					}
					break;
				case 'object':
					options = args[i];
					break;
				}
			}

			var that = this;
			var page = that._continue(GCN.PageAPI)._procure();

			this._read(function () {
				if (!options) {
					options = {};
				}

				// default settings
				if (that.nodeId()) {
					options.nodeId = that.nodeId();
				}
				options.folderId   = that.id();
				options.templateId = templateId;

				that._authAjax({
					url   : GCN.settings.BACKEND_PATH + '/rest/page/create/',
					type  : 'POST',
					json  : options,
					error : function (xhr, status, msg) {
						GCN.handleHttpError(xhr, msg, error);
					},
					success : function (response) {
						if (GCN.getResponseCode(response) === 'OK') {
							var data = response.page;
							page._data    = data;
							page._fetched = true;
							page._removeFromTempCache(page);
							page._setHash(data.id)._addToCache();
							if (success) {
								that._invoke(success, [page]);
							}
						} else {
							page._die(GCN.getResponseCode(response));
							GCN.handleResponseError(response, error);
						}

						// Halt the call chain until this object has been fully
						// realized.
						page._vacate();
					}
				}, error);
			}, error);
		},

		// ====================================================================
		// Templates
		// ====================================================================

//		'!template': function (id, success, error) {
//			return this._continue(GCN.TemplateAPI, id, success, error);
//		},
//
//		'!templates': function (ids, success, error) {
//			//FIXME: Not implemented
//		},
//
//		createTemplate: function (settings, success, error) {
//			//FIXME: Not implemented
//		},

		/**
		 * Retreive a list of all files in this folder.
		 *
		 * @param {function(Array.FileAPI)=} success Optional callback that
		 *                                           will receive an array of
		 *                                           {@link FileAPI} objects as
		 *                                           its only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		files: function (success, error) {
			this._getItems('file', success, error);
		},

		/**
		 * Retreive a list of all images in this folder.
		 *
		 * @param {function(Array.ImageAPI)=} success Optional callback that
		 *                                            will receive an array of
		 *                                            {@link ImageAPI} objects
		 *                                            as its only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		images: function (success, error) {
			this._getItems('image', success, error);
		},

		// ====================================================================
		// Folders
		// ====================================================================

		/**
		 * @override
		 * @see ContentObjectAPI._loadParams
		 */
		'!_loadParams': function () {
			return jQuery.extend(DEFAULT_SETTINGS, this._settings);
		},

		/**
		 * @FIXME(petro) Why on do we need this method inside FolderAPI?
		 */
		'!folder': function (id, success, error) {
			return this._continue(GCN.FolderAPI, id, success, error);
		},

		/**
		 * Retreive a list of all sub folders of this folder.
		 *
		 * @param {function(Array.FolderAPI)=} success Optional callback that
		 *                                             will receive an array of
		 *                                             {@link FolderAPI}
		 *                                             objects as its only
		 *                                             argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		folders: function (success, error) {
			this._getItems('folder', success, error);
		},

		/**
		 * Create a sub folder within this folder, with the option of also
		 * automatically creating a startpage for this folder.
		 *
		 * @param {string} name the folder name
		 * @param {object} settings pass in an optional settings object
		 *                          possible options are:
		 *     <pre>
		 *     {
		 *        // optional description for the folder
		 *        description: 'this is my folder',
		 *        // set a publish directory for the folder
		 *        publishDir: '/this/is/my/folder/',
		 *        // adding a template id will automatically create a new
		 *        // startpage for the folder
		 *        templateId: 5,
		 *        // provide a language code for the start page. optional.
		 *        language: 'en',
		 *        // when true creating the folder will fail if a folder with
		 *        // that name exists. otherwise conflicting names will be
		 *        // postfixed with an increasing number. defaults to false.
		 *        failOnDuplicate: false
		 *     }
		 *     </pre>
		 * @param {function(FolderAPI)=} success Optional callback that
		 *                                       will receive a
		 *                                       {@link FolderAPI} object as
		 *                                       its only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 * @throws UNKNOWN_ARGUMENT Thrown when unexpected arguments are
		 *                          provided.
		 */
		createFolder: function () {
			var that = this;
			var success;
			var error;
			var settings;
			var name;
			var i;
			var j = arguments.length;

			// parse arguments
			for (i = 0; i < j; ++i) {
				switch (jQuery.type(arguments[i])) {
				case 'function':
					if (!success) {
						success = arguments[i];
					} else if (success && !error) {
						error = arguments[i];
					} else {
						GCN.error('UNKNOWN_ARGUMENT',
							'success and error handler already set. Don\'t ' +
							'know what to do with arguments[' + i + ']');
					}
					break;
				case 'object':
					if (!settings) {
						settings = arguments[i];
					} else {
						GCN.error('UNKNOWN_ARGUMENT',
							'settings already set. Don\'t know what to do ' +
							'with arguments[' + i + '] value ' + arguments[i]);
					}
					break;
				case 'string':
					if (!name) {
						name = arguments[i];
					} else {
						GCN.error('UNKNOWN_ARGUMENT',
							'name already set. Don\'t know what to do with ' +
							'arguments[' + i + '] value ' + arguments[i]);
					}
					break;
				default:
					GCN.error('UNKNOWN_ARGUMENT',
						'Don\'t know what to do with arguments[' + i + '] ' +
						'value ' + arguments[i]);
				}
			}

			// initialize basic settings object
			if (!settings) {
				settings = {};
			}

			// set default parameters
			settings.name = name;
			settings.motherId = this.id();
			if (this.nodeId()) {
				settings.nodeId = this.nodeId();
			}

			// automatically enable startpage generation if a template is set
			if (settings.templateId) {
				settings.startpage = true;
			}

			this._authAjax({
				url     : GCN.settings.BACKEND_PATH + '/rest/folder/create/',
				type    : 'POST',
				error   : error,
				json    : settings,
				success : function (response) {
					that._continue(GCN.FolderAPI, response.folder, success,
						error);
				}
			});
		},

		/**
		 * Get a URL for uploading files into this folder.
		 *
		 * @public
		 * @function
		 * @name uploadURL
		 * @memberOf FolderAPI
		 * @return {string} Rest API url for file uploading.
		 */
		'!uploadURL': function () {
			return (
				GCN.settings.BACKEND_PATH
				+ '/rest/file/createSimple.json?sid=' + GCN.sid
				+ '&folderId=' + this.id()
				+ GCN._getChannelParameter(this, '&')
			);
		},

		/**
		 * Get the upload URL that is capable of dealing with multipart form 
		 * data.
		 *
		 * @public
		 * @name multipartUploadURL
		 * @function
		 * @memberOf FolderAPI
		 * @param {boolean} applyContentWrapperFilter When true the reponse will 
		 *                  be wrapped so that it can be interpreted by various 
		 *                  upload implementations. 
		 * @param {string} filterContentType Define a custom content type that
		 *                  should be used for the server side implementation.
		 *                  The defined content type will modify the response
		 *                  'Content-Type' header value.
		 * @return {string} Rest API url for multipart file upload.
		 */
		'!multipartUploadURL' : function (applyContentWrapperFilter, filterContentType) {
			var restURL = GCN.settings.BACKEND_PATH
			            + '/rest/file/create.json?sid='
						+ GCN.sid;

			if (typeof applyContentWrapperFilter !== 'undefined') {
				if (jQuery.type(applyContentWrapperFilter) === 'boolean') {
					restURL += '&content-wrapper-filter='
					        + applyContentWrapperFilter;
				} else {
					GCN.error('INVALID_ARGUMENTS', 'the `multipartUploadURL()\' method ' +
						'only accepts boolean values for the `applyContentWrapperFilter\' parameter');
				}
			}

			if (filterContentType) {
				restURL += '&filter-contenttype=' + filterContentType;
			}

			return restURL;
		},

		/**
		 * This method will inspect the json and decide whether the onSuccess or
		 * onError should be called. A file or image api object will be passed
		 * to the success handler.
		 * 
		 * @TODO(petro): The success callback should not receive a second
		 *               argument containing messages. It is not consitanct with
		 *               out API.
		 * @ignore does not make sense for me to have this being called from implementers
		 * @public
		 * @name handleUploadResponse
		 * @memberOf FolderAPI
		 * @param {object}
		 *            response The REST-API reponse object that was given in
		 *            response to the upload request.
		 * @param {function(FileAPI,
		 *            Array.string)=} success Optional callback that will
		 *            receive as its first argument, a {@link FileAPI} object of
		 *            the uploaded file. The second argument is an array of
		 *            message strings returned in response to the upload
		 *            request.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 */
		'!handleUploadResponse': function (response, success, error) {
			if (GCN.getResponseCode(response) === 'OK') {
				if (success) {
					var that = this;
					GCN.file(response.file, function (file) {
						that._invoke(success, [file, response.messages]);
					}, error);
				}
			} else {
				GCN.handleResponseError(response, error);
			}
		},

		/**
		 * Fetch items inside this folder.
		 *
		 * @param {string} type One of: "file"
		 *                              "folder"
		 *                              "page"
		 *                              "image"
		 *                              "template"
		 * @param {function(Array.<ContentObjectAPI>)} success Callback that
		 *                                              will receive an array
		 *                                              of the requested items.
		 * @param {function(GCNError):boolean=} success Custom error handler.
		 */
		'!_getItems': function (type, success, error) {
			var that = this;

			if (!this._fetched) {
				this._read(function () {
					that._getItems(type, success, error);
				}, error);

				return;
			}

			var api;
			var url = GCN.settings.BACKEND_PATH + '/rest/' + this._type
				    + '/getItems/' + this.id() + '?type=' + type
					+ GCN._getChannelParameter(that, '&');

			switch (type) {
			case 'page':
				api = GCN.PageAPI;
				break;
			case 'file':
				api = GCN.FileAPI;
				break;
			case 'image':
				api = GCN.ImageAPI;
				break;
			case 'folder':
				api = GCN.FolderAPI;
				url = GCN.settings.BACKEND_PATH + '/rest/' + this._type
					+ '/getFolders/' + this.id()
					+ GCN._getChannelParameter(that);
				break;
			default:
				var err = GCN.createError('UNEXPECTED_TYPE',
					'Unknown object type ' + type, this);

				GCN.handleError(err, error);
				return;
			}

			this._authAjax({
				url     : url,
				type    : 'GET',
				error   : error,
				success : function (response) {
					var items = [];
					var i;
					var j = response.numItems;

					for (i = 0; i < j; i++) {
						items.push(that._continue(api,
							(type === 'folder') ? response.folders[i] :
							                      response.items[i],
							null, error));
					}

					that._invoke(success, [items]);
				}
			});
		}

	});

	/**
	* Creates a new instance of FolderAPI. See the {@link FolderAPI} constructor for detailed information.
	* 
	* @function
	* @name folder
	* @memberOf GCN
	* @see FolderAPI
	*/
	GCN.folder = GCN.exposeAPI(FolderAPI);
	GCN.FolderAPI = FolderAPI;

}(GCN));
