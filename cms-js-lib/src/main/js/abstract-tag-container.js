/*global jQuery:true, GCN: true */
(function (GCN) {

	'use strict';

	/**
	 * Checks whether or not a tag container instance has data for a tag of the
	 * given name.
	 *
	 * @param {TagContainerAPI} container The container in which to look
	 *                                         for the tag.
	 * @param {string} tagname The name of the tag to find.
	 * @return {boolean} True if the container contains a data for the tag of
	 * the given name.
	 */
	function hasTagData(container, tagname) {
		return !!container._data.tags[tagname];
	}

	/**
	 * Extends the internal taglist with data from the given collection of tags.
	 * Will overwrite the data of any tag whose name is matched in the `tags'
	 * associative array.
	 *
	 * @param {TagContainerAPI} container The container in which to look
	 *                                         for the tag.
	 * @param {object} tags Associative array of tag data, mapped against the
	 *                      data's corresponding tag name.
	 */
	function extendTags(container, tags) {
		jQuery.extend(container._data.tags, tags);
	}

	/**
	 * Gets the construct matching the given keyword.
	 *
	 * @param {string} keyword Construct keyword.
	 * @param {NodeAPI} node The node inwhich to search for the construct.
	 * @param {function(object)} success Callback function to receive the
	 *                                   successfully found construct.
	 * @param {function(GCNError):boolean} error Optional custom error handler.
	 */
	function getConstruct(keyword, node, success, error) {
		node.constructs(function (constructs) {
			if (constructs[keyword]) {
				success(constructs[keyword]);
			} else {
				var err = GCN.createError(
					'CONSTRUCT_NOT_FOUND',
					'Cannot find construct `' + keyword + '` - Maybe the construct is not linked to this node?',
					constructs
				);
				GCN.handleError(err, error);
			}
		}, error);
	}

	/**
	 * Creates an new tag via the GCN REST-API.
	 *
	 * @param {TagAPI} tag A representation of the tag which will be created in
	 *                     the GCN backend.
	 * @param {object} data The request body that will be serialized into json.
	 * @param {function(TagAPI)} success Callback function to receive the
	 *                                   successfully created tag.
	 * @param {function(GCNError):boolean} error Optional custom error handler.
	 */
	function newTag(tag, data, success, error) {
		var obj = tag.parent();
		var url = GCN.settings.BACKEND_PATH + '/rest/' + obj._type
				+ '/newtag/' + obj._data.id + GCN._getChannelParameter(obj);
		obj._authAjax({
			type: 'POST',
			url: url,
			json: data,
			error: function (xhr, status, msg) {
				GCN.handleHttpError(xhr, msg, error);
				tag._vacate();
			},
			success: function (response) {
				obj._handleCreateTagResponse(tag, response, success,
				                             error);
			}
		}, error);
	}

	/**
	 * Creates multiple tags via the GCN REST API.
	 * 
	 * @param {TagContainerAPI} parent container object for the new tags
	 * @param {object} tagData data object for creating tags.
	 * @param {function(TagAPI)} success Optional success callback
	 * @param {function(GCNError):boolean} error Optional custom error handler.
	 */
	function newTags(parent, tagData, success, error) {
		var url = GCN.settings.BACKEND_PATH + '/rest/' + parent._type
			+ '/newtags/' + parent._data.id + GCN._getChannelParameter(parent);
		var id, data = {create: {}};

		for (id in tagData.create) {
			if (tagData.create.hasOwnProperty(id)) {
				data.create[id] = tagData.create[id].data;
			}
		}

		parent._authAjax({
			type: 'POST',
			url: url,
			json: data,
			error: function (xhr, status, msg) {
				var id;

				GCN.handleHttpError(xhr, msg, error);
				for (id in tagData.create) {
					if (tagData.create.hasOwnProperty(id)) {
						tagData.create[id].tag._vacate();
					}
				}
			},
			success: function (response) {
				var id;

				if (GCN.getResponseCode(response) === 'OK') {
					tagData.response = response;
					for (id in response.created) {
						if (response.created.hasOwnProperty(id)) {
							var data = response.created[id].tag, tag = tagData.create[id].tag;
							tagData.create[id].response = response.created[id];
							tag._name = data.name;
							tag._data = data;
							tag._fetched = true;

							// The tag's id is still the temporary unique id that was given
							// to it in _createTag().  We have to realize the tag so that
							// it gets the correct id. The new id changes its hash, so it
							// must also be removed and reinserted from the caches.
							tag._removeFromTempCache(tag);
							tag._setHash(data.id)._addToCache();

							// Add this tag into the tag's container `_shadow' object, and
							// `_tagIdToNameMap hash'.
							parent._update('tags.' + GCN.escapePropertyName(data.name),
									data, error, true);

							// TODO: We need to store the tag inside the `_data' object for
							// now.  A change should be made so that when containers are
							// saved, the data in the `_shadow' object is properly
							// transfered into the _data object.
							parent._data.tags[data.name] = data;

							if (!parent.hasOwnProperty('_createdTagIdToNameMap')) {
								parent._createdTagIdToNameMap = {};
							}

							parent._createdTagIdToNameMap[data.id] = data.name;

							tag.prop('active', true);
						}
					}

					if (success) {
						success();
					}
				} else {
					for (id in tagData) {
						if (tagData.hasOwnProperty(id)) {
							tagData[id].tag._die(GCN.getResponseCode(response));
						}
					}
					GCN.handleResponseError(response, error);
				}
			}
		}, error);
	}

	/**
	 * Checks whether exactly one of the following combination of options is
	 * provided:
	 *
	 * 1. `keyword' alone
	 * or
	 * 2. `constructId' alone
	 * or
	 * 3. `sourcePageId' and `sourceTagname' together.
	 *
	 * Each of these options are mutually exclusive.
	 *
	 * @param {Object} options
	 * @return {boolean} True if only one combination of the possible options
	 *                   above is contained in the given options object.
	 */
	function isValidCreateTagOptions(options) {
		// If the sum is 0, it means that no options was specified.
		//
		// If the sum is greater than 0 but less than 2, it means that either
		// `sourcePageId' or `sourceTagname' was specified, but not both as
		// required.
		//
		// If the sum is greater than 2, it means that more than one
		// combination of settings was provided, which is one too many.
		return 2 === (options.sourcePageId  ? 1 : 0) +
		             (options.sourceTagname ? 1 : 0) +
		             (options.keyword       ? 2 : 0) +
		             (options.constructId   ? 2 : 0);
	}

	/**
	 * Parse the arguments passed into createTag() into a normalized object.
	 *
	 * @param {Arguments} createTagArgumenents An Arguments object.
	 * @parma {object} Normalized map of arguments.
	 */
	function parseCreateTagArguments(createTagArguments) {
		var args = Array.prototype.slice.call(createTagArguments);
		if (0 === args.length) {
			return {
				error: '`createtag()\' requires at least one argument.  See ' +
				       'documentation.'
			};
		}

		var options;

		// The first argument must either be a string, number or an object.
		switch (jQuery.type(args[0])) {
		case 'string':
			options = {keyword: args[0]};
			break;
		case 'number':
			options = {constructId: args[0]};
			break;
		case 'object':
			if (!isValidCreateTagOptions(args[0])) {
				return {
					error: 'createTag() requires exactly one of the ' +
					       'following, mutually exclusive, settings to be' +
					       'used: either `keyword\', `constructId\' or a ' +
					       'combination of `sourcePageId\' and ' +
					       '`sourceTagname\'.'
				};
			}
			options = args[0];
			break;
		default:
			options = {};
		}

		// Determine success() and error(): arguments 2-3.
		var i;
		for (i = 1; i < args.length; i++) {
			if (jQuery.type(args[i]) === 'function') {
				if (options.success) {
					options.error = args[i];
				} else {
					options.success = args[i];
				}
			}
		}

		return {
			options: options
		};
	}

	/**
	 * Given an object containing information about a tag, determines whether
	 * or not we should treat a tag as a editabled block.
	 *
	 * Relying on `onlyeditables' property to determine whether or not a given
	 * tag is a block or an editable is unreliable since it is possible to have
	 * a block which only contains editables:
	 *
	 * {
	 *  "tagname":"content",
	 *  "editables":[{
	 *    "element":"GENTICS_EDITABLE_1234",
	 *    "readonly":false,
	 *    "partname":"editablepart"
	 *  }],
	 *  "element":"GENTICS_BLOCK_1234",
	 *  "onlyeditables":true
	 *  "tagname":"tagwitheditable"
	 * }
	 *
	 * In the above example, even though `onlyeditable' is true the tag is
	 * still a block, since the tag's element and the editable's element are
	 * not the same.
	 *
	 * @param {object} tag A object holding the sets of blocks and editables
	 *                     that belong to a tag.
	 * @return {boolean} True if the tag
	 */
	function isBlock(tag) {
		if (!tag.editables || tag.editables.length > 1) {
			return true;
		}
		return (
			(1 === tag.editables.length)
			&&
			(tag.editables[0].element !== tag.element)
		);
	}

	/**
	 * Given a data object received from a REST API "/rest/page/render"
	 * call maps the blocks and editables into a list of each.
	 *
	 * The set of blocks and the set of editables that are returned are not
	 * mutually exclusive--if a tag is determined to be both an editable
	 * and a block, it will be included in both sets.
	 *
	 * @param {object} data
	 * @return {object<string, Array.<object>>} A map containing a set of
	 *                                          editables and a set of
	 *                                          blocks.
	 */
	function getEditablesAndBlocks(data) {
		if (!data || !data.tags) {
			return {
				blocks: [],
				editables: []
			};
		}

		var tag;
		var tags = data.tags;
		var blocks = [];
		var editables = [];
		var i;
		var j;

		for (i = 0; i < tags.length; i++) {
			tag = tags[i];
			if (tag.editables) {
				for (j = 0; j < tag.editables.length; j++) {
					tag.editables[j].tagname = tag.tagname;
				}
				editables = editables.concat(tag.editables);
			}
			if (isBlock(tag)) {
				blocks.push(tag);
			}
		}

		return {
			blocks: blocks,
			editables: editables
		};
	}

	/**
	 * Abstract class that is implemented by tag containers such as
	 * {@link PageAPI} or {@link TemplateAPI}
	 * 
	 * @class
	 * @name TagContainerAPI
	 */
	GCN.TagContainerAPI = GCN.defineChainback({
		/** @lends TagContainerAPI */

		/**
		 * @private
		 * @type {object<number, string>} Hash, mapping tag ids to their
		 *                                corresponding names.
		 */
		_tagIdToNameMap: null,

		/**
		 * @private
		 * @type {object<number, string>} Hash, mapping tag ids to their
		 *                                corresponding names for newly created
		 *                                tags.
		 */
		_createdTagIdToNameMap: {},

		/**
		 * @private
		 * @type {Array.<object>} A set of blocks that are are to be removed
		 *                        from this content object when saving it.
		 *                        This array is populated during the save
		 *                        process.  It get filled just before
		 *                        persisting the data to the server, and gets
		 *                        emptied as soon as the save operation
		 *                        succeeds.
		 */
		_deletedBlocks: [],

		/**
		 * @private
		 * @type {Array.<object>} A set of tags that are are to be removed from
		 *                        from this content object when it is saved.
		 */
		_deletedTags: [],

		/**
		 * Searching for a tag of a given id from the object structure that is
		 * returned by the REST API would require O(N) time.  This function,
		 * builds a hash that maps the tag id with its corresponding name, so
		 * that it can be mapped in O(1) time instead.
		 *
		 * @private
		 * @return {object<number,string>} A hash map where the key is the tag
		 *                                 id, and the value is the tag name.
		 */
		'!_mapTagIdsToNames': function () {
			var name;
			var map = {};
			var tags = this._data.tags;
			for (name in tags) {
				if (tags.hasOwnProperty(name)) {
					map[tags[name].id] = name;
				}
			}
			return map;
		},

		/**
		 * Retrieves data for a tag from the internal data object.
		 *
		 * @private
		 * @param {string} name The name of the tag.
		 * @return {!object} The tag data, or null if a there if no tag
		 *                   matching the given name.
		 */
		'!_getTagData': function (name) {
			return (this._data.tags && this._data.tags[name]) ||
			       (this._shadow.tags && this._shadow.tags[name]);
		},

		/**
		 * Get the tag whose id is `id'.
		 * Builds the `_tagIdToNameMap' hash map if it doesn't already exist.
		 *
		 * @todo: Should we deprecate this?
		 * @private
		 * @param {number} id Id of tag to retrieve.
		 * @return {object} The tag's data.
		 */
		'!_getTagDataById': function (id) {
			if (!this._tagIdToNameMap) {
				this._tagIdToNameMap = this._mapTagIdsToNames();
			}
			return this._getTagData(this._tagIdToNameMap[id] ||
				 this._createdTagIdToNameMap[id]);
		},

		/**
		 * Extracts the editables and blocks that have been rendered from the
		 * REST API render call's response data.
		 *
		 * @param {object} data The response object received from the
		 *                      renderTemplate() call.
		 * @return {object} An object containing two properties: an array of
		 *                  blocks, and an array of editables.
		 */
		'!_processRenderedTags': getEditablesAndBlocks,

		// !!!
		// WARNING adding &nbsp; to folder is neccessary as jsdoc will report a
		// name confict otherwise
		// !!!
		/**
		 * Get this content object's node.
		 *
		 * @function
		 * @name node&nbsp;
		 * @memberOf TagContainerAPI
		 * @param {funtion(NodeAPI)=} success Optional callback to receive a
		 *                                    {@link NodeAPI} object as the
		 *                                    only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 * @return {NodeAPI} This object's node.
		 */
		'!node': function (success, error) {
			return this.folder().node();
		},

		/**
		 * Synchronizes the information about tags with the server.
		 *
		 * @private
		 * @param {function} callback A function that will be called after
		 *		reloading the tags.
		 */
		'!_syncTags': function (callback) {
			if (typeof callback !== 'function') {
				callback = function () {};
			}

			var that = this;
			// we clear the cache first, so that tmpObject will really be a new object
			this._clearCache();
			// call _read on a temporary API object and copy the tags from the loaded object onto this
			// we assume that every API object, that extends TagContainerAPI has _type set to the name of the
			// method on GCN that will create an according instance (e.g. for PageAPI, _type is 'page' and the method would be GCN.page())
			var tmpObject = GCN[this._type](this._data.id);
			tmpObject._read(function () {
				that._data.tags = tmpObject._data.tags;
				that._invoke(callback);
			});
		},

		// !!!
		// WARNING adding &nbsp; to folder is neccessary as jsdoc will report a
		// name confict otherwise
		// !!!
		/**
		 * Get this content object's parent folder.
		 * 
		 * @function
		 * @name folder&nbsp;
		 * @memberOf TagContainerAPI
		 * @param {funtion(FolderAPI)=}
		 *            success Optional callback to receive a {@link FolderAPI}
		 *            object as the only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 * @return {FolderAPI} This object's parent folder.
		 */
		'!folder': function (success, error) {
			var id = this._fetched ? this.prop('folderId') : null;
			return this._continue(GCN.FolderAPI, id, success, error);
		},

		/**
		 * Gets a tag of the specified id, contained in this content object.
		 *
		 * @name tag
		 * @function
		 * @memberOf TagContainerAPI
		 * @param {number} id Id of tag to retrieve.
		 * @param {function} success
		 * @param {function} error
		 * @return TagAPI
		 */
		'!tag': function (id, success, error) {
			return this._continue(GCN.TagAPI, id, success, error);
		},

		/**
		 * Retrieves a collection of tags from this content object.
		 *
		 * @name tags
		 * @function
		 * @memberOf TagContainerAPI
		 * @param {object|string|number} settings (Optional)
		 * @param {function} success callback
		 * @param {function} error (Optional)
		 * @return TagContainerAPI
		 */
		'!tags': function () {
			var args = Array.prototype.slice.call(arguments);

			if (args.length === 0) {
				return;
			}

			var i;
			var j = args.length;
			var filter = {};
			var filters;
			var hasFilter = false;
			var success;
			var error;

			// Determine `success', `error', `filter'
			for (i = 0; i < j; ++i) {
				switch (jQuery.type(args[i])) {
				case 'function':
					if (success) {
						error = args[i];
					} else {
						success = args[i];
					}
					break;
				case 'number':
				case 'string':
					filters = [args[i]];
					break;
				case 'array':
					filters = args[i];
					break;
				}
			}

			if (jQuery.type(filters) === 'array') {
				var k = filters.length;
				while (k) {
					filter[filters[--k]] = true;
				}
				hasFilter = true;
			}

			var that = this;

			if (success) {
				this._read(function () {
					var tags = that._data.tags;
					var tag;
					var list = [];

					for (tag in tags) {
						if (tags.hasOwnProperty(tag)) {
							if (!hasFilter || filter[tag]) {
								list.push(that._continue(GCN.TagAPI, tags[tag],
									null, error));
							}
						}
					}

					that._invoke(success, [list]);
				}, error);
			}
		},

		/**
		 * Internal method to create a tag of a given tagtype in this content
		 * object.
		 *
		 * Not all tag containers allow for new tags to be created on them,
		 * therefore this method will only be surfaced by tag containers which
		 * do allow this.
		 *
		 * @private
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
		 */
		'!_createTag': function () {
			var args = parseCreateTagArguments(arguments);

			if (args.error) {
				GCN.handleError(
					GCN.createError('INVALID_ARGUMENTS', args.error, arguments),
					args.error
				);
				return;
			}

			var obj = this;

			// We use a uniqueId to avoid a fetus being created.
			// This is to avoid the following scenario:
			//
			// var tag1 = container.createTag(...);
			// var tag2 = container.createTag(...);
			// tag1 === tag2 // is true which is wrong
			//
			// However, for all other cases, where we get an existing object,
			// we want this behaviour:
			//
			// var folder1 = page(1).folder(...);
			// var folder2 = page(1).folder(...);
			// folder1 === folder2 // is true which is correct
			//
			// So, createTag() is different from other chainback methods in
			// that each invokation must create a new instance, while other
			// chainback methods must return the same.
			//
			// The id will be reset as soon as the tag object is realized.
			// This happens below as soon as we get a success response with the
			// correct tag id.
			var newId = GCN.uniqueId('TagApi-unique-');

			// Create a new TagAPI instance linked to this tag container.  Also
			// acquire a lock on the newly created tag object so that any
			// further operations on it will be queued until the tag object is
			// fully realized.
			var tag = obj._continue(GCN.TagAPI, newId)._procure();

			var options = args.options;
			var copying = !!(options.sourcePageId && options.sourceTagname);

			var onCopy = function () {
				if (options.success) {
					// When the newly created tag is a copy of another tag
					// which itselfs contains nested tags, these nested
					// tags were created in the CMS but this object would
					// only know about the containing tag.
					obj._syncTags(function() {
						// update the tag with the synchronized data
						tag._data = obj._data.tags[tag._name];
						obj._invoke(options.success, [tag]);
						tag._vacate();
					});
				} else {
					tag._vacate();
				}
			};
			var onCreate =  function () {
				if (options.success) {
					obj._invoke(options.success, [tag]);
				}
				tag._vacate();
			};

			if (copying) {
				newTag(tag, {
					copyPageId: options.sourcePageId,
					copyTagname: options.sourceTagname
				}, onCopy, options.error);
			} else {
				if (options.constructId) {
					newTag(tag, {
						magicValue: options.magicValue,
						constructId: options.constructId
					}, onCreate, options.error);
				} else {
					// ASSERT(options.keyword)
					getConstruct(options.keyword, obj.node(), function (construct) {
						newTag(tag, {
							magicValue: options.magicValue,
							constructId: construct.constructId
						}, onCreate, options.error);
					}, options.error);
				}
			}

			return tag;
		},

		/**
		 * Internal method to create multiple tags at once.
		 * The tagData object must have a property "create" that contains a single property for each tag to be created. Each property contains
		 * the request "data" (consisting of constructId/keyword and magicvalue)
		 * 
		 * Each tag property will get the response object for that tag attached (when the request is successfull)
		 * The tagData object itself will get the whole response attached
		 * 
		 * @private
		 * @param {object} tagData tag data object.
		 * @param {function(TagAPI)=} success Optional callback success callback
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		'!_createTags': function (tagData, success, error) {
			var obj = this, id, newId;

			for (id in tagData.create) {
				if (tagData.create.hasOwnProperty(id)) {
					newId = GCN.uniqueId('TagApi-unique-');
					tagData.create[id].tag = obj._continue(GCN.TagAPI, newId);
				}
			}

			newTags(obj, tagData, success, error);
		},

		/**
		 * Internal helper method to handle the create tag response.
		 * 
		 * @private
		 * @param {TagAPI} tag
		 * @param {object} response response object from the REST call
		 * @param {function(TagContainerAPI)=} success optional success handler
		 * @param {function(GCNError):boolean=} error optional error handler
		 */
		'!_handleCreateTagResponse': function (tag, response, success, error) {
			var obj = this;

			if (GCN.getResponseCode(response) === 'OK') {
				var data = response.tag;
				tag._name = data.name;
				tag._data = data;
				tag._fetched = true;

				// The tag's id is still the temporary unique id that was given
				// to it in _createTag().  We have to realize the tag so that
				// it gets the correct id. The new id changes its hash, so it
				// must also be removed and reinserted from the caches.
				tag._removeFromTempCache(tag);
				tag._setHash(data.id)._addToCache();

				// Add this tag into the tag's container `_shadow' object, and
				// `_tagIdToNameMap hash'.
				var shouldCreateObjectIfUndefined = true;
				obj._update('tags.' + GCN.escapePropertyName(data.name),
					data, error, shouldCreateObjectIfUndefined);

				// TODO: We need to store the tag inside the `_data' object for
				// now.  A change should be made so that when containers are
				// saved, the data in the `_shadow' object is properly
				// transfered into the _data object.
				obj._data.tags[data.name] = data;

				if (!obj.hasOwnProperty('_createdTagIdToNameMap')) {
					obj._createdTagIdToNameMap = {};
				}

				obj._createdTagIdToNameMap[data.id] = data.name;

				tag.prop('active', true);

				if (success) {
					success();
				}
			} else {
				tag._die(GCN.getResponseCode(response));
				GCN.handleResponseError(response, error);
			}
		},

		/**
		 * Internal method to delete the specified tag from this content
		 * object.
		 *
		 * @private
		 * @param {string} keyword The keyword of the tag to be deleted.
		 * @param {function(TagContainerAPI)=} success Optional callback that
		 *                                             receive this object as
		 *                                             its only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		'!_removeTag': function (keyword, success, error) {
			this.tag(keyword).remove(success, error);
		},

		/**
		 * Internal method to delete a set of tags from this content object.
		 *
		 * @private
		 * @param {Array.<string>} keywords The keywords of the set of tags to be
		 *                             deleted.
		 * @param {function(TagContainerAPI)=} success Optional callback that
		 *                                             receive this object as
		 *                                             its only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		'!_removeTags': function (keywords, success, error) {
			var that = this;
			this.tags(keywords, function (tags) {
				var j = tags.length;
				while (j--) {
					tags[j].remove(null, error);
				}
				if (success) {
					that.save(success, error);
				}
			}, error);
		}

	});

	GCN.TagContainerAPI.hasTagData = hasTagData;
	GCN.TagContainerAPI.extendTags = extendTags;
	GCN.TagContainerAPI.getEditablesAndBlocks = getEditablesAndBlocks;

}(GCN));