/*global define: true */
/*!
 * Aloha Editor
 * Author & Copyright (c) 2010 Gentics Software GmbH
 * aloha-sales@gentics.com
 * Licensed unter the terms of http://www.aloha-editor.com/license.html
 */

define([
    'aloha/core',
    'jquery',
    'aloha/repository',
    'gcn/gcn-plugin',
    'PubSub',
    'i18n!gcn/nls/i18n',
    'vendor/amplify.store'
],
function (Aloha, jQuery, repository, gcnplugin, PubSub, i18n, amplifyStore) {

	'use strict'

	var DEBUG = false;
	var amplifyStoreMainKey = 'GcnRepository';
	var listFolders = [];
	var selectedFolder = null;

	function debug() {
		if (DEBUG && console) {
			console.log('-- debug ------------------------------------------------\n');
			console.log(Array.prototype.slice.call(arguments));
			console.log('\n-- /debug -----------------------------------------------');
		}
	};

	/**
	 * Map which keeps information about a Node and its the ordered languages.
	 * @type Object.<string,number>
	 */
	var nodeLanguageIds = {};

	/**
	 * Updates the ordered languages for a Node.
	 * @param{Object} params Contains several information.
	 */
	function updateLanguages (params) {

		if (("" + params.nodeId) in nodeLanguageIds) {
			return;
		}

		var GCN = Aloha.require('gcn/gcn-plugin');
		var url = GCN.settings.stag_prefix + GCN.restUrl + '/node/load/' + params.nodeId;

		var that = this;

		GCN.performRESTRequest({
			url     :  url,
			params  : {update: false},
			timeout : params.timeout || 10000,
			type    : 'GET',
			error   : function (data) {
				that.handleRestResponse(data);
				if (data && (data.statusText === 'timeout')) {
					metainfo = metainfo || {};
					metainfo.timeout = true;
				}
				callback(collection, metainfo);
			},
			success	: function (data) {
				nodeLanguageIds["" + params.nodeId] = data.node.languagesId;
			}
		});
	}
	
	/**
	 * Creates Rendition object.
	 * @param {number} repositoryId
	 * @param {Object} variant language variant
	 * @param {number} nodeId
	 * @return {Object}
	 */
	function createRenditionObj(repositoryId, variant, nodeId) {
		return {
			id       : '10007.' + variant.id,
			nodeid   : nodeId,
			url      : variant.path + variant.fileName,
			name	 : variant.name,
			filename : variant.fileName,
			status	 : variant.status,
			inherited: variant.inherited,
			kind     : 'translation',
			language : variant.language,
			languageName : variant.languageName,
			mimeType : 'text/html',
			height   : null,
			width    : null,
			repositoryId : repositoryId,
			type	 : 'document'
		};
	}
	
	/**
	 * Builds renditions
	 * @param {Object} elem
	 * @param {Array.<string>} languageOrder ordered list with the order of the language
	 * @param {number} nodeId
	 */
	function buildRenditions(elem, languageIdsOrdered, nodeId) {
		var members = [];
		var repositoryId = elem.repositoryId;
		
		if (languageIdsOrdered) {
			var i = languageIdsOrdered.length;
			while (i--) {
				var variant = elem.languageVariants["" + languageIdsOrdered[i]];
				if (variant) {
					members.push(createRenditionObj(repositoryId, variant, nodeId));
				}
			}
		} else {
			jQuery.each(elem.languageVariants, function(key, variant) {
				members.push(createRenditionObj(repositoryId, variant, nodeId));
			});
		}
		
		return members;
	}

	var gcnRepo = new (repository.extend({

		_constructor: function() {
			this._super('com.gentics.aloha.GCN.Page', 'Content.Node');
		},

		init: function() {
			// Set a template for rendering objects.
			this.setTemplate('<span><b>{name}</b><br/>{path}</span>');
			this.loadState();
		},

		/**
		 * Searches a repository for items matching the given query.
		 * The results are passed as an array to the callback.
		 * If no objects are found the callback receives null.
		 *
		 * TODO: implement caching
		 *
		 * @param {object} query - query parameters.
		 *				   An example query parameters object could look something like this:
		 *						{
		 * 							filter		: ["language"],
		 * 							inFolderId	: 4,
		 * 							maxItems	: 10,
		 * 							objectTypeFilter
		 *										: ["website", "files"],
		 * 							orderBy		: null,
		 * 							queryString	: null,
		 * 							renditionFilter
		 *										: ["*"],
		 * 							skipCount	: 0,
		 *							recursive	: true // this is not according to the specification, but needed for gcn autocompletion and browser
		 *						}
		 * @param {function(Array)} callback
		 */
		query: function (query, callback) {
			var that = this;
			var GCN = Aloha.require('gcn/gcn-plugin');
			var p = query;
			var params = {
				links: GCN.settings.links || 'backend'
			};
			var folderId, folderIdParts;

			if (typeof p.recursive !== 'undefined') {
				params.recursive = p.recursive;
			} else {
				params.recursive = true;
			}

			if (p.queryString) {
				params.search = p.queryString;
			}

			if (p.maxItems) {
				params.maxItems = p.maxItems;
			} else {
				params.maxItems = 100;
			}

			if (p.skipCount) {
				params.skipCount = p.skipCount;
			}

			if (p.orderBy && p.orderBy.length) {
				var orderBy = this.buildSortPairs(p.orderBy);
				params.sortby = orderBy[0].by;
				params.sortorder = orderBy[0].order;
			}

			if (!p.inFolderId) {
				// Fixed: Get folderId of the node folder, because RestAPI requires it
				folderId = GCN.settings.nodeFolderId;
			} else if (p.inFolderId === this.repositoryId) {
				folderId = 0;
			} else {
				// when inFolderId contains a '/', the folder id is nodeId/folderId
				if (typeof p.inFolderId == 'string' && p.inFolderId.indexOf('/') >= 0) {
					folderIdParts = p.inFolderId.split('/');
					params.nodeId = folderIdParts[0];
					p.nodeId = params.nodeId;
					folderId = folderIdParts[1];
				} else {
					folderId = p.inFolderId;
				}
			}

			// If objectTypeFilter has been specified, then only check for
			// resources of types found in objectTypeFilter array.
			// Otherwise try check all types of objects, and collect
			// everthing we can.
			var documentTypesToCollect;

			// pages, images and files can be collected in a single REST API call (getItems),
			// so if at least two of them is requested, we will group them together in a sub-array
			var pageImageFileToCollect;

			if (p.objectTypeFilter && p.objectTypeFilter.length) {
				documentTypesToCollect = [];
				pageImageFileToCollect = [];

				if(jQuery.inArray('website', p.objectTypeFilter) > -1) {
					pageImageFileToCollect.push('page');
				}

				if(jQuery.inArray('text/html', p.objectTypeFilter) > -1) {
					pageImageFileToCollect.push('page');
				}


				if(jQuery.inArray('files', p.objectTypeFilter) > -1) {
					pageImageFileToCollect.push('file');
				}

				if(jQuery.inArray('images', p.objectTypeFilter) > -1) {
					pageImageFileToCollect.push('image');
				}

				if(jQuery.inArray('template', p.objectTypeFilter) > -1) {
					documentTypesToCollect.push('template');
				}

				if(jQuery.inArray('folder', p.objectTypeFilter) > -1) {
					documentTypesToCollect.push('folder');
				}

				if(jQuery.inArray('contenttag', p.objectTypeFilter) > -1) {
					documentTypesToCollect.push('contenttag');
				}

				if(jQuery.inArray('templatetag', p.objectTypeFilter) > -1) {
					documentTypesToCollect.push('templatetag');
				}

				if (pageImageFileToCollect.length == 1) {
					documentTypesToCollect.push(pageImageFileToCollect[0]);
				} else if (pageImageFileToCollect.length > 1) {
					documentTypesToCollect.push(pageImageFileToCollect);
				}
			} else {
				documentTypesToCollect = [['page', 'file', 'image'], 'template', 'folder'];
			}

			var collection = [];

			// Once all resources have been collected, we then call
			// processQueryResults, and pass to it the array of all repo
			// documents that we found, the original params object, and the
			// original callback expecting to receive the final processed
			// results
			var processResults = function (collection, metainfo) {
				that.processQueryResults(collection, metainfo, query, callback);
			};

			// Recursively query for object types specified in
			// documentTypesToCollect. We pop documentTypesToCollect on
			// each iteration until documentTypesToCollect is empty
			var collectResults = function (collection, metainfo) {
				var type = documentTypesToCollect.pop();

				that.getResources(
					type,
					folderId,
					params,
					collection,
					metainfo,
					documentTypesToCollect.length ? collectResults : processResults
				);
			};

			collectResults(collection, {numItems: 0, hasMoreItems: false});
		},

		/**
		 * Convert orderBy from [{field: order} ...], which is easier for the
		 * user to write into [{by:field, order:order} ...] which is easier for
		 * the sort comparison function to use
		 */
		buildSortPairs: function (orderBy) {
			if (!orderBy) {
				return [];
			}

			var i = orderBy.length;
			var sort;
			var field;
			var order;
			var newOrder = [];

			while (--i >= 0) {
				sort = orderBy[i];
				for (field in sort) {
					order = sort[field];
					if (typeof order === 'string') {
						order = order.toLowerCase();
						if (order == 'asc' || order == 'desc') {
							newOrder[i] = {by: field, order: order};
							break;
						}
					}
				}
			}

			return newOrder;
		},

		/**
		 * Prepares filters patterns according to:
		 *		http://docs.oasis-open.org/cmis/CMIS/v1.0/cd04/cmis-spec-v1.0.html#_Ref237323310
		 * Examples:
		 *		*				(include all Renditions)
		 *		cmis:thumbnail	(include only Thumbnails)
		 *		Image/*			(include all image Renditions)
		 *		application/pdf, application/x-shockwave-flash
		 *						(include web ready Renditions)
		 *		cmis:none		(exclude all Renditions)
		 */
		buildRenditionFilterChecks: function (filters) {
			var f;
			var pattern;
			var type;
			var subtype;
			var checks = [];
			var i = filters.length;
			// TODO: We should cache this rgxp so that we do not have recreate
			// it on each iteration
			var rgxpKind = /([a-z0-9]+)\/([a-z0-9\*]+)/i;

			while (--i >= 0) {
				f = filters[i];

				if (f == '*') {
					// all renditions
					return ['*'];
				} else if (f.substring(0, 5) == 'cmis:') {
					pattern = f.substring(5, f.length);

					// no renditions
					if (pattern == 'none') {
						return [];
					}

					// check against kind
					checks.push(['kind', pattern]);
				} else if (f.match(rgxpKind)) {
					type = RegExp. $1;
					subtype = RegExp.$2;

					// check against mimeType
					checks.push([
						'mimeType',
						subtype == '*'
							? new RegExp(type + '\/.*', 'i')
							: f.toLowerCase()
					]);
				}
			}

			return checks;
		},

		/**
		 * Transforms the given data, that was fetched from the backend, into a
		 * repository folder
		 * @param {object} data data of a folder fetched from the backend
		 * @param nodeId optional id of the node
		 * @return {Aloha.Repository.Object} repository item
		 */
		getFolder: function(data, nodeId) {
			var type, children = null;

			if (!data) {
				return null;
			}

			// we will distinguish the types here (node/channel/folder/inheritedfolder)
			switch (data.type) {
				case 'FOLDER':
				case 'folder':
					type = data.inherited ? 'inheritedfolder' : 'folder';
					break;
				case 'NODE':
				case 'node':
					type = 'node';
					break;
				case 'CHANNEL':
				case 'channel':
					type = 'channel';
					break;
				default:
					data.motherId ? type = 'folder' : type = 'node';
					break;
			}

			// get the nodeId, this is either the passed nodeId (which is the id of the node/channel), or the nodeId of the folder itself
			nodeId = nodeId || data.nodeId;

			// get the children, if attached
			if (jQuery.isArray(data.subfolders)) {
				children = [];
				for (var i in data.subfolders) {
					if (data.subfolders.hasOwnProperty(i)) {
						children.push(this.getFolder(data.subfolders[i], nodeId));
					}
				}
			}

			return new Aloha.RepositoryFolder({
				repositoryId   : this.repositoryId,
				type           : type,
				id             : nodeId ? nodeId + '/' + data.id : data.id,
				name           : data.name,
				children       : children,
				hasMoreItems   : data.hasOwnProperty('hasSubfolders') ? data.hasSubfolders : undefined,
				masterFolderId : data.masterId,
				folderId       : data.id,
				channelId      : data.channelId
			});
		},

		/**
		 * Transforms the given data, that was fetched from the backend, into a
		 * repository item
		 * @param {object} data data of a page fetched from the backend
		 * @return {Aloha.Repository.Object} repository item
		 */
		getDocument: function(data, objecttype) {
			if (!data) {
				return null;
			}

			objecttype = objecttype || 10007;
			switch(objecttype) {
			case 'CONTENTTAG':
				objecttype = 10111;
				break;
			case 'TEMPLATETAG':
				objecttype = 10112;
				break;
			case 'OBJECTTAG':
				objecttype = 10113;
				break;
			case 'page':
				objecttype = 10007;
				break;
			case 'image':
				objecttype = 10011;
				break;
			case 'file':
				objecttype = 10008;
				break;
			}

			// set the id
			data.id = objecttype + '.' + data.id;

			// make the path information shorter by replacing path parts in the middle with ...
			data.fullPath = data.path;
			if (typeof data.fullPath === 'string') {
				// add zero-width spaces after every / to allow longer lines to break
				data.fullPath = data.fullPath.replace(/\//g, '/\u200B');
			}
			var path = data.path;

			// let's strip the first path part (which is the node name, that is always the same)
			if (typeof path === 'string') {
				path = path.replace(/^\/[^\/]+\//, '/');
			}

			var pathLimit = 55;

			if (path && (path.length > pathLimit)) {
				path = path.substr(0, pathLimit/2)
					 + '...'
					 + path.substr(path.length - pathLimit/2);
			}

			data.path = path;

			var GCN = Aloha.require('gcn/gcn-plugin');

			if (data.url && GCN.settings.renderBlockContentURL) {
				data.url = GCN.renderBlockContent(data.url);
			}

			data.repositoryId = 'com.gentics.aloha.GCN.Page';

			return new Aloha.RepositoryDocument(data);
		},

		/**
		 * Queries for a resource of a given type against specified parameters.
		 * If we don't have a method to handle the type of resource requested,
		 * we invoke the callback, passing an empty array, and log a warning.
		 *
		 * @param {string}	 type - of resource to query for (page, file, image). May also be an array to get items in general
		 * @param {string}	 id
		 * @param {object}	 params - xhr query parameters
		 * @param {array}	 collection - the array to which we add found documents
		 * @param {object}   metainfo - metainfo object
		 * @param {function} callback
		 * @return {undefined}
		 */
		getResources: function (type, folderId, params, collection, metainfo, callback) {
			var that = this;
			var restMethod;
			var objName;
			var docTypeNum;
			var GCN = Aloha.require('gcn/gcn-plugin');

			if (jQuery.isArray(type)) {
				restMethod = 'folder/getItems';
				objName    = 'items';
				jQuery.extend(params, {'type': type});
				if (GCN.settings.pagelanguage) {
					jQuery.extend(params, {'langvars': 'true', 'language': GCN.settings.pagelanguage});
				}
			} else {
				switch (type) {
				case 'page':
					// check whether the "folder" is a page (this happens when
					// the repo is used in a minibrowser and the user wants to
					// select page tags)
					if (typeof folderId == 'string' && folderId.match(/^10007\./)) {
						callback(collection);
						return;
					}
					restMethod = 'folder/getPages';
					objName    = 'pages';
					docTypeNum = '10007';
					if (GCN.settings.pagelanguage) {
						jQuery.extend(params, {'langvars': 'true', 'language': GCN.settings.pagelanguage});
					}
					break;
				case 'file':
					restMethod = 'folder/getFiles';
					objName    = 'files';
					docTypeNum = '10008';
					break;
				case 'image':
					restMethod = 'folder/getImages';
					// objName = 'images';
					//
					// This is a temporary fix to accomodate the fact that the
					// REST-API returns a response for getImages with results in a
					// "files" object, rather than "images" which is what we would
					// expect
					objName    = 'files';
					docTypeNum = '10011';
					break;
				case 'template':
					// check whether the "folder" is a template (this happens when
					// the repo is used in a minibrowser and the user wants to
					// select template tags)
					if (folderId.match(/^10006\./)) {
						callback(collection);
						return;
					}
					restMethod = 'folder/getTemplates';
					objName	   = 'templates';
					docTypeNum = '10006';
					break;
				case 'folder':
					restMethod = 'folder/getFolders';
					objName	   = 'folders';
					docTypeNum = '10002';
					break;
				case 'contenttag':
					restMethod = 'page/getTags';
					objName	   = 'tags';
					if (folderId.match(/^10007\./)) {
						folderId = folderId.substr(6);
					} else {
						callback(collection);
						return;
					}
					break;
				case 'templatetag':
					restMethod = 'template/getTags';
					objName	   = 'tags';
					docTypeNum = '10112';
					if (folderId.match(/^10006\./)) {
						folderId = folderId.substr(6);
					} else {
						callback(collection);
						return;
					}
					break;
				default:
					callback(collection);
					return;
				};
			}


			debug(
				'Will query REST-API with method:', restMethod,
				'\nWill look for results in object: ', objName,
				'\nWill create repo doc with docTypeNum: ', docTypeNum
			);

			var GCN = Aloha.require('gcn/gcn-plugin');
			var url = GCN.settings.stag_prefix + GCN.restUrl + '/' + restMethod;

			// set the nodeId (if not already set in the params)
			if (!params.nodeId) {
				params.nodeId = GCN.settings.nodeId;
			}

			if (folderId !== null) {
				url	+= '/' + folderId;
			}
			
			params.template = 'true';

			updateLanguages(params);

			GCN.performRESTRequest({
				url     : url,
				params  : params,
				type    : 'GET',
				error   : function (data) {
					Aloha.Console.error('gcn-repository', 'REST-API status: "' + data.statusText + '"\n' + 'response text:\n' + data.responseText);
					alert(i18n.t('gcnrepository.error'));
					that.handleRestResponse(data);
					callback(collection);
				},
				success	: function (data) {
					if (that.handleRestResponse(data)) {
						if (typeof collection !== 'object') {
							collection = [];
						}

						var objs = data[objName];

						for (var i = 0, j = objs.length; i < j; ++i) {
							objs[i] = that.getDocument(objs[i], docTypeNum ? docTypeNum : objs[i].type);
							collection.push(objs[i]);
						}
					}
					if (metainfo) {
						if (jQuery.isNumeric(metainfo.numItems) && jQuery.isNumeric(data.numItems)) {
							metainfo.numItems += data.numItems;
						}
						if (typeof metainfo.hasMoreItems === 'boolean' && typeof data.hasMoreItems === 'boolean') {
							metainfo.hasMoreItems = metainfo.hasMoreItems || data.hasMoreItems;
						}
					}

					callback(collection, metainfo);
				}
			});
		},

		/**
		 * Handles queryString, filter, and renditionFilter which the REST-API
		 * doesn't at the moment
		 */
		processQueryResults: function (documents, metainfo, params, callback) {
			var skipCount = params.skipCount || 0;
			var l = documents.length;
			var num = 0;
			var results = [];
			var queryString = params.queryString;
			var elem;
			var obj;
			var hasQueryString = !!queryString; 
			var hasFilter = !!params.filter;
			var hasRenditionFilter = false;
			var contentSets = {};
			var contentsetDocs = {};

			if (queryString) {
				queryString = queryString.replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1");
			}

			var rgxp = new RegExp(queryString || '', 'i');

			if (params.renditionFilter && typeof params.renditionFilter === 'object') {
				hasRenditionFilter = params.renditionFilter.length > 0;
			}

			// Build renditions for the results
			var renditions = {};

			for (var i = 0; i < l; ++i) {
				elem = documents[i];

				// If a filter is specified, then filter out all
				// unrequired properties from each object, and leave
				// only those specified in the filter array. If no
				// filter is specified, then we take all properties of
				// each document
				if (hasFilter) {
					// Copy all fields that returned objects must
					// contain, according to Repository specification
					obj = {
						id       : elem.id,
						nodeId   : params.nodeId,
						name     : elem.name,
						baseType : 'document',
						path     : elem.path,
						// FIXME: elem.type seems to only return "document".
						// Could it be more specific?
						type     : elem.type
					};

					// Copy all requested fields specified as items in
					// the filter array from the document into the
					// object to be returned
					for (var f = 0; f < params.filter.length; ++f) {
						obj[params.filter[f]] = elem[params.filter[f]];
					}
				} else {
					obj = elem;
					obj.nodeId = params.nodeId;
				}

				// build the renditions (language variants)
				if (elem.languageVariants) {
					if (typeof params.inFolderId === 'string') {
						var folderIdParts = params.inFolderId.split('/');
						var nodeId = folderIdParts[0];
						renditions[elem.id] = buildRenditions(elem, nodeLanguageIds["" + nodeId], params.nodeId);
					} else {
						renditions[elem.id] = buildRenditions(elem, null, params.nodeId);
					}
				}

				results[num++] = obj;
			}

			// Truncate results at maxItems
			if (typeof params.maxItems === 'number') {
				results = results.slice(0, params.maxItems);
			}

			if (hasRenditionFilter && (i = results.length) && renditions) {
				var renditionChecks = this.buildRenditionFilterChecks(params.renditionFilter);
				var r;

				while (--i >= 0) {
					if (r = renditions[results[i].id]) {
						results[i].renditions = this.getRenditions(r, renditionChecks);
					} else {
						results[i].renditions = [];
					}
				}
			}

			debug('RESULTS: ', results);

			callback.call(this, results, metainfo);
		},

		getRenditions: function (renditions, renditionChecks) {
			var matches = [];
			var alreadyMatched = [];
			var check;
			var matched = false;

			for (var j = renditions.length - 1; j >= 0; --j) {
				for (var k = renditionChecks.length - 1; k >= 0; --k) {
					check = renditionChecks[k];

					if (check == '*') {
						matched = true;
					} else if (typeof check[1] === 'string') {
						matched = renditions[j][check[0]].toLowerCase() == check[1];
					} else {
						matched = renditions[j][check[0]].match(check[1]);
					}

					if (matched && jQuery.inArray(j, alreadyMatched) == -1) {
						matches.push(renditions[j]);
						alreadyMatched.push(j);
					}
				}
			}

			return matches;
		},

		/**
		 * We need to return a none truthy value. because the getChildren
		 * method in the repository abstract class returns true to signify that
		 * it is not implemented. He we must therefore do the opposite to
		 * indicated to the repository manager that that we are not unimplemented
		 *
		 * @param {Object} params - Associative array, where key is the
		 *							parameter name and value is the parameter
		 *							value
		 * @param {Function} callback
		 * @return {Boolean} false
		 */
		getChildren: function(params, callback) {
			var that = this;
			var folderIdParts, i, len;

			if (params.inFolderId === this.repositoryId) {
				params.inFolderId = 0;
			} else if (typeof params.inFolderId === 'string' &&
			           params.inFolderId.indexOf('/') >= 0) {
				// If inFolderId contains a '/' it is constructed as nodeId/folderId.
				folderIdParts = params.inFolderId.split('/');
				params.nodeId = folderIdParts[0];
				params.inFolderId = folderIdParts[1];
			}

			var GCN = Aloha.require('gcn/gcn-plugin');

			if (listFolders.length > 0) {
				params.recursive = true;
				params.tree = true;
				params.recId = [];
				len = listFolders.length;
				for (i = 0; i < len; i++) {
					params.recId.push(listFolders[i].nodeId + "/" + listFolders[i].folderId);
				}
			}

			GCN.performRESTRequest({
				params: params,
				url   : GCN.settings.stag_prefix + GCN.restUrl +
				        '/folder/getFolders/' + params.inFolderId,
				type  : 'GET',
				error : function (jqXHR, textStatus, errorThrown) {
					callback.call(that, []);
				},
				success: function(data) {
					if (that.handleRestResponse(data)) {
						var folders = data.folders;
						var i;
						for (i = 0; i < folders.length; i++) {
							folders[i] = that.getFolder(data.folders[i], params.nodeId);
						}

						// remove id's in "deleted" from "listFolders"
						if (jQuery.isArray(data.deleted)) {
							for (i = 0; i < data.deleted.length; i++) {
								that.folderClosed({id: data.deleted[i]});
							}
						}
					}
					callback.call(that, folders);

					if (params.inFolderId != 0) {
						that.folderOpened(params);
					}
				}
			});

			return false;
		},

		/**
		 * Get the repositoryItem with given id
		 * @param itemId {String} id of the repository item to fetch
		 * @param callback {function} callback function
		 * @return {Aloha.Repository.Object} item with given id
		 */
		getObjectById: function (itemId, callback) {
			var that = this;
			var GCN = Aloha.require('gcn/gcn-plugin');

			if (itemId.match(/^10007\./)) {
				GCN.performRESTRequest({
					url     : GCN.settings.stag_prefix + GCN.restUrl + '/page/load/' + itemId.substr(6),
					type    : 'GET',
					success : function(data) {
						if (data.page) {
							callback.call(that, [that.getDocument(data.page, 10007)]);
						}
					}
				});
			} else if (itemId.match(/^10008./)) {
				GCN.performRESTRequest({
					url     : GCN.settings.stag_prefix + GCN.restUrl + '/file/load/' + itemId.substr(6),
					type    : 'GET',
					success : function(data) {
						if (data.file) {
							callback.call(that, [that.getDocument(data.file, 10008)]);
						}
					}
				});
			} else if (itemId.match(/^10011./)) {
				GCN.performRESTRequest({
					url     : GCN.settings.stag_prefix + GCN.restUrl + '/image/load/' + itemId.substr(6),
					type    : 'GET',
					success : function(data) {
						if (data.image) {
							callback.call(that, [that.getDocument(data.image, 10011)]);
						}
					}
				});
			}
		},

		handleRestResponse: function (response) {
			if (!response) {
				debug('No response data received');
				return false;
			}

			if (response.responseInfo && response.responseInfo.responseCode != 'OK') {
				var l;
				var msg = [];
				var msgs = response.messages;

				if (msgs && (l = msgs.length)) {
					while (--l >= 0) {
						msgs[l].message && msg.push(msgs[l].message);
					}

					msg = msg.join('\n');
				} else {
					msg = 'REST-API Error';
				}

				debug(msg);

				return false;
			}

			return true;
		},

		/**
		 * Returns data from the client's storage
		 * 
		 * @param {string}	key
		 */
		getStoreData: function(key) {
			var storeKey = amplifyStoreMainKey + '.' + key;

			return amplifyStore.store(storeKey);
		},

		/**
		 * Saves data to the client's storage
		 * 
		 * @param {string}	key
		 * @param {mixed}	value
		 */
		setStoreData: function(key, value) {
			var storeKey = amplifyStoreMainKey + '.' + key;

			amplifyStore.store(storeKey, value)
		},

		/**
		 * Retrieves the data from the client's local storage
		 */
		loadState: function() {
			// Retrieve data from the client's browser data store
			listFolders		= this.getStoreData('tree');
			selectedFolder	= this.getStoreData('selectedFolder');

			if (typeof listFolders === 'undefined') {
				listFolders = [];
			}
		},

		/**
		 * Persists all storable data from this repository to the client's storage
		 */
		persistState: function() {
			this.setStoreData('tree', listFolders);
			this.setStoreData('selectedFolder', selectedFolder);
		},

		/**
		 * This function gets called by the manager when a folder is opened
		 * 
		 * @param {object}	folder
		 * 					Example data:
		 * 
		 *					folder {
		 *						inFolderId: "2251"
		 *						nodeId: "3"
		 *						repositoryId: "com.gentics.aloha.GCN.Page"
		 *					}
		 *
		 *					folder {
		 *						baseType: "folder"
		 *						children: null
		 *						id: "3/87"
		 *						loaded: true
		 *						name: "Ketchum"
		 *						repositoryId: "com.gentics.aloha.GCN.Page"
		 *						type: "folder"
		 *						uid: 1348755575044
		 *					}								
		 */
		folderOpened: function(folder) {
			var folderInfo = this.parseFolderInfo(folder);

			if (folderInfo == null) {
				return;
			}

			// check if that folder is already in our list for whatever reason
			var index = this.findFolderInFolderList(folderInfo);

			if (index === null) {
				listFolders.push(folderInfo);
			}

			// persist this state
			this.setStoreData('tree', listFolders);
		},

		/**
		 * This function gets called by the manager when a folder is closed
		 * 
		 * @param {object}	folder
		 * 					Example data: See function folderOpened()					
		 */
		folderClosed: function(folder) {
			var folderInfo = this.parseFolderInfo(folder);

			if (folderInfo == null) {
				return;
			}

			// get the index for this folder from our array
			var index = this.findFolderInFolderList(folderInfo);
			if (index !== null) {
				// This removes the specified item from the array
				listFolders.splice(index, 1);
			}

			// persist this state
			this.setStoreData('tree', listFolders);
		},

		/**
		 * This function gets called by the manager when a folder is selected
		 * 
		 * @param {object}	folder
		 * 					Example data: See function folderOpened()					
		 */
		folderSelected: function(folder) {
			var folderInfo = this.parseFolderInfo(folder);

			if (folderInfo == null) {
				return;
			}

			this.setSelectedFolder(folderInfo);
		},

		/**
		 * Searchs for the folder in our listFolders array and returns the index
		 * 
		 * @param {object}	folder
		 * 					Example data: See function folderOpened()	
		 * @return {int}	the index in the array if found, null otherwise				
		 */
		findFolderInFolderList: function(folder) {
			for (var i = 0; i < listFolders.length; i++) {

				if (listFolders[i].nodeId == folder.nodeId && listFolders[i].folderId == folder.folderId) {
					return i;
				}
			}

			return null;
		},

		/**
		 * Parses a folder object to get the nodeId and folderId
		 * 
		 * @param {object}	folder object to parse				
		 */
		parseFolderInfo: function(folder) {
			var nodeId = 0;
			var folderId = 0;

			if (typeof folder.id != 'undefined') {
				var tokens = folder.id.split('/');

				if (tokens.length == 2) {
					nodeId = tokens[0];
					folderId = tokens[1];
				}
			}
			else if (folder.nodeId && folder.inFolderId) {
				nodeId = folder.nodeId;
				folderId = folder.inFolderId;
			}

			if (nodeId <= 0 || folderId <= 0) {
				return null;
			}

			return { 'nodeId': nodeId, 'folderId': folderId };
		},

		/**
		 * Returns the last selected folder
		 * 
		 * @return {object}	the object of the last selected folder				
		 */
		getSelectedFolder: function() {
			if (selectedFolder) {
				return {id: selectedFolder.nodeId + "/" + selectedFolder.folderId, name: selectedFolder.name};
			}
		},

		/**
		 * Sets the last selected folder
		 * 
		 * @param {object} the object of the last selected folder			
		 */
		setSelectedFolder: function(folder) {
			selectedFolder = folder;

			// persist
			this.setStoreData('selectedFolder', folder);
		},

		/**
		 * Mark the object after an repository item was selected for it.
		 * This implementation will publish an event using PubSub
		 * 
		 * @param obj jQuery target object to which the repositoryItem will be applied
		 * @param repositoryItem The selected item. A class constructed from Document or Folder.
		 * @return void
		 */
		markObject: function (obj, repositoryItem) {
			obj.attr('data-gcn-channelid', repositoryItem.nodeId);
			PubSub.pub('gcn.repository.item.selected', {
				obj: obj,
				repositoryItem: repositoryItem
			});
		}
	}))();

});
