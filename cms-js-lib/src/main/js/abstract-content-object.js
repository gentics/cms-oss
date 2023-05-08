(function (GCN) {

	'use strict';

	/**
	 * Updates the internal data of the given content object.
	 *
	 * This function extends and overwrites properties of the instances's
	 * internal data structure.  No property is deleted on account of being
	 * absent from the given `props' object.
	 *
	 * @param {ContentObjectAPI} obj An instance whose internal data is to be
	 *                               reset.
	 * @param {object} props The properties with which to replace the internal
	 *                       data of the given chainback instance.
	 */
	function update(obj, props) {
		jQuery.extend(obj._data, props);
	}

	/**
	 * The prefix that will be temporarily applied to block tags during an
	 * encode() process.
	 *
	 * @type {string}
	 * @const
	 */
	var BLOCK_ENCODING_PREFIX = 'GCN_BLOCK_TMP__';

	/**
	 * Will match <span id="GENTICS_block_123"></span>" but not "<node abc123>"
	 * tags.  The first backreference contains the tagname of the tag
	 * corresponding to this block.
	 *
	 * Limitation: Will not work with unicode characters.
	 *
	 * @type {RexExp}
	 * @const
	 */
    var CONTENT_BLOCK = new RegExp(
			// "<span" or "<div" but not "<node"
			'<(?!node)[a-z]+'            +
				// "class=... data-*..."
				'(?:\\s+[^/<>\\s=]+(?:=(?:"[^"]*"|\'[^\']*\'|[^>/\\s]+))?)*?' +
				// " id = "
				'\\s+id\\s*=\\s*["\']?'  +
				// "GCN_BLOCK_TMP__"
				BLOCK_ENCODING_PREFIX    +
				// "_abc-123"
				'([^"\'/<>\\s=]*)["\']?' +
				// class=... data-*...
				'(?:\\s+[^/<>\\s=]+(?:=(?:"[^"]*"|\'[^\']*\'|[^>/\\s]+))?)*' +
				// "' ...></span>" or "</div>"
				'\\s*></[a-z]+>',
			'gi'
		);

	/**
	 * Will match <node foo> or <node bar_123> or <node foo-bar> but not
	 * <node "blah">.
	 *
	 * @type {RegExp}
	 * @const
	 */
	var NODE_NOTATION = /<node ([a-z0-9_\-]+?)>/gim;

	/**
	 * Examines a string for "<node>" tags, and for each occurance of this
	 * notation, the given callback will be invoked to manipulate the string.
	 *
	 * @private
	 * @static
	 * @param {string} str The string that will be examined for "<node>" tags.
	 * @param {function} onMatchFound Callback function that should receive the
	 *                                following three parameters:
	 *
	 *                    name:string The name of the tag being notated by the
	 *                                node substring.  If the `str' arguments
	 *                                is "<node myTag>", then the `name' value
	 *                                will be "myTag".
	 *                  offset:number The offset where the node substring was
	 *                                found within the examined string.
	 *                     str:string The string in which the "<node *>"
	 *                                substring occured.
	 *
	 *                                The return value of the function will
	 *                                replace the entire "<node>" substring
	 *                                that was passed to it within the examined
	 *                                string.
	 */
	function replaceNodeTags(str, onMatchFound) {
		var parsed = str.replace(NODE_NOTATION, function (substr, tagname,
		                                                  offset, examined) {
				return onMatchFound(tagname, offset, examined);
			});
		return parsed;
	}

	/*
	 * have a look at _init 
	 */
	GCN.ContentObjectAPI = GCN.defineChainback({
		/** @lends ContentObjectAPI */

		/**
		 * @private
		 * @type {string} A string denoting a content node type.  This value is
		 *                used to compose the correct REST API ajax urls.  The
		 *                following are valid values: "node", "folder",
		 *                "template", "page", "file", "image".
		 */
		_type: null,

		/**
		 * @private
		 * @type {object<string,*>} An internal object to store data that we
		 *                          get from the server.
		 */
		_data: {},

		/**
		 * @private
		 * @type {object<string,*>} An internal object to store updates to
		 *                          the content object.  Should reflect the
		 *                          structural typography of the `_data'
		 *                          object.
		 */
		_shadow: {},

		/**
		 * @type {boolean} Flags whether or not data for this content object have
		 *                 been fetched from the server.
		 */
		_fetched: false,

		/**
		 * @private
		 * @type {object} will contain an objects internal settings
		 */
		_settings: null,

		/**
		 * An array of all properties of an object that can be changed by the
		 * user. Writeable properties for all content objects.
		 * 
		 * @public
		 * @type {Array.string}
		 */
		WRITEABLE_PROPS: [],

		/**
		 * <p>This object can contain various contrains for writeable props. 
		 * Those contrains will be checked when the user tries to set/save a
		 * property. Currently only maxLength is beeing handled.</p>
		 *
		 * <p>Example:</p>
		 * <pre>WRITEABLE_PROPS_CONSTRAINTS: {
		 *    'name': {
		 *        maxLength: 255
		 *     } 
		 * }</pre>
		 * @type {object}
		 * @const
		 *
		 */
		WRITEABLE_PROPS_CONSTRAINTS: {},

		/**
		 * Fetches this content object's data from the backend.
		 *
		 * @ignore
		 * @param {function(object)} success A function to receive the server
		 *                                   response.
		 * @param {function(GCNError):boolean} error Optional custrom error
		 *                                           handler.
		 */
		'!fetch': function (success, error, stack) {
			var obj = this;
			var ajax = function () {
				obj._authAjax({
					url: GCN.settings.BACKEND_PATH + '/rest/' + obj._type +
					     '/load/' + obj.id() + GCN._getChannelParameter(obj),
					data: obj._loadParams(),
					error: error,
					success: success
				});
			};

			// If this chainback object has an ancestor, then invoke that
			// parent's `_read()' method before fetching the data for this
			// chainback object.
			if (obj._chain) {
				var circularReference =
						stack && -1 < jQuery.inArray(obj._chain, stack);
				if (!circularReference) {
					stack = stack || [];
					stack.push(obj._chain);
					obj._chain._read(ajax, error, stack);
					return;
				}
			}

			ajax();
		},

		/**
		 * Internal method, to fetch this object's data from the server.
		 *
		 * @ignore
		 * @private
		 * @param {function(ContentObjectAPI)=} success Optional callback that
		 *                                              receives this object as
		 *                                              its only argument.
		 * @param {function(GCNError):boolean=} error Optional customer error
		 *                                            handler.
		 */
		'!_read': function (success, error, stack) {
			var obj = this;
			if (obj._fetched) {
				if (success) {
					obj._invoke(success, [obj]);
				}
				return;
			}

			if (obj.multichannelling) {
				obj.multichannelling.read(obj, success, error);
				return;
			}

			var id = obj.id();

			if (null === id || undefined === id) {
				obj._getIdFromParent(function () {
					obj._read(success, error, stack);
				}, error, stack);
				return;
			}

			obj.fetch(function (response) {
				obj._processResponse(response);
				obj._fetched = true;
				if (success) {
					obj._invoke(success, [obj]);
				}
			}, error, stack);
		},

		/**
		 * Retrieves this object's id from its parent.  This function is used
		 * in order for this object to be able to fetch its data from the
		 * backend.
		 *
		 * FIXME: If the id that `obj` aquires results in it having a hash that
		 * is found in the cache, then `obj` should not replace the object that
		 * was in the cache, rather, `obj` should be masked by the object in the
		 * cache.  This scenario will arise in the following scenario:
		 *
		 * page.node().constructs();
		 * page.node().folders();
		 *
		 * The above will cause the same node to be fetched from the server
		 * twice, each time, clobbering the previosly loaded data in the cache.
		 *
		 * @ignore
		 * @private
		 * @param {function(ContentObjectAPI)=} success Optional callback that
		 *                                              receives this object as
		 *                                              its only argument.
		 * @param {function(GCNError):boolean=} error Optional customer error
		 *                                            handler.
		 * @throws CANNOT_GET_OBJECT_ID
		 */
		'!_getIdFromParent': function (success, error, stack) {
			var parent = this._ancestor();

			if (!parent) {
				var err = GCN.createError('CANNOT_GET_OBJECT_ID',
					'Cannot get an id for object', this);
				GCN.handleError(err, error);
				return;
			}

			var that = this;

			parent._read(function () {
				if ('folder' === that._type) {
					// There are 3 possible property names that an object can
					// use to hold the id of the folder that it is related to:
					//
					// "folderId": for pages, templates, files, and images.
					// "motherId": for folders
					// "nodeId":   for nodes
					//
					// We need to see which of this properties is set, the
					// first one we find will be our folder's id.
					var props = ['folderId', 'motherId', 'nodeId'];
					var prop = props.pop();
					var id;

					while (prop) {
						id = parent.prop(prop);
						if (typeof id !== 'undefined') {
							break;
						}
						prop = props.pop();
					}

					that._data.id = id;
				} else {
					that._data.id = parent.prop(that._type + 'Id');
				}

				if (that._data.id === null || typeof that._data.id === 'undefined') {
					var err = GCN.createError('CANNOT_GET_OBJECT_ID',
						'Cannot get an id for object', this);
					GCN.handleError(err, error);
					return;
				}

				that._setHash(that._data.id)._addToCache();

				if (success) {
					success();
				}
			}, error, stack);
		},

		/**
		 * Gets this object's node id. If used in a multichannelling is enabled
		 * it will return the channel id or 0 if no channel was set.
		 * 
		 * @public
		 * @function
		 * @name nodeId
		 * @memberOf ContentObjectAPI
		 * @return {number} The channel to which this object is set. 0 if no
		 *         channel is set.
		 */
		'!nodeId': function () {
			return this._channel || 0;
		},

		/**
		 * Gets this object's id. We'll return the id of the object when it has
		 * been loaded - this can only be a localid. Otherwise we'll return the
		 * id which was provided by the user. This can either be a localid or a
		 * globalid.
		 *
		 * @name id
		 * @function
		 * @memberOf ContentObjectAPI
		 * @public
		 * @return {number}
		 */
		'!id': function () {
			return this._data.id;
		},

		/**
		 * Alias for {@link ContentObjectAPI#id}
		 *
		 * @name localId
		 * @function
		 * @memberOf ContentObjectAPI
		 * @private
		 * @return {number}
		 * @decprecated
		 */
		'!localId': function () {
			return this.id();
		},

		/**
		 * Update the `_shadow' object that maintains changes to properties
		 * that reflected the internal `_data' object.  This shadow object is
		 * used to persist differential changes to a REST API object.
		 *
		 * @ignore
		 * @private
		 * @param {string} path The path through the object to the property we
		 *                      want to modify if a node in the path contains
		 *                      dots, then these dots should be escaped.  This
		 *                      can be done using the GCN.escapePropertyName()
		 *                      convenience function.
		 * @param {*} value The value we wish to set the property to.
		 * @param {function=} error Custom error handler.
		 * @param {boolean=} force If true, no error will be thrown if `path'
		 *                         cannot be fully resolved against the
		 *                         internal `_data' object, instead, the path
		 *                         will be created on the shadow object.
		 */
		'!_update': function (pathStr, value, error, force) {
			var boundary = Math.random().toString(8).substring(2);
			var path = pathStr.replace(/\./g, boundary)
			                  .replace(new RegExp('\\\\' + boundary, 'g'), '.')
			                  .split(boundary);
			var shadow = this._shadow;
			var actual = this._data;
			var i = 0;
			var numPathNodes = path.length;
			var pathNode;
			// Whether or not the traversal path in `_data' and `_shadow' are
			// at the same position in the respective objects.
			var areMirrored = true;

			while (true) {
				pathNode = path[i++];

				if (areMirrored) {
					actual = actual[pathNode];
					areMirrored = jQuery.type(actual) !== 'undefined';
				}

				if (i === numPathNodes) {
					break;
				}

				if (shadow[pathNode]) {
					shadow = shadow[pathNode];
				} else if (areMirrored || force) {
					shadow = (shadow[pathNode] = {});
				} else {
					break; // goto error
				}
			}

			if (i === numPathNodes && (areMirrored || force)) {
				shadow[pathNode] = value;
			} else {
				var err = GCN.createError('TYPE_ERROR', 'Object "' +
					path.slice(0, i).join('.') + '" does not exist',
					actual);
				GCN.handleError(err, error);
			}
		},

		/**
		 * Receives the response from a REST API request, and adds any new data
		 * in the internal `_data' object.
		 *
		 * Note that data already present in `_data' will not be removed or
		 * overwritten.
		 *
		 * @private
		 * @param {object} data Parsed JSON response data.
		 */
		'!_processResponse': function (data) {
			this._data = jQuery.extend(true, {}, data[this._type], this._data);
		},

		/**
		 * Specifies a list of parameters that will be added to the url when
		 * loading the content object from the server.
		 *
		 * @private
		 * @return {object} object With parameters to be appended to the load
		 *                         request
		 */
		'!_loadParams': function () {},

		/**
		 * Reads the property `property' of this content object if this
		 * property is among those in the WRITEABLE_PROPS array. If a second
		 * argument is provided, them the property is updated with that value.
		 *
		 * @name prop
		 * @function
		 * @memberOf ContentObjectAPI
		 * @param {String} property Name of the property to be read or updated.
		 * @param {String} value Optional value to set property to. If omitted the property will just be read.
		 * @param {function(GCNError):boolean=} error Custom error handler to 
		 *                                      stop error propagation for this
		 *                                      synchronous call. 
		 * @return {?*} Meta attribute.
		 * @throws UNFETCHED_OBJECT_ACCESS if the object has not been fetched from the server yet
		 * @throws READONLY_ATTRIBUTE whenever trying to write to an attribute that's readonly
		 */
		'!prop': function (property, value, error) {
			if (!this._fetched) {
				GCN.handleError(GCN.createError(
					'UNFETCHED_OBJECT_ACCESS',
					'Object not fetched yet.'
				), error);
				return;
			}

			if (typeof value !== 'undefined') {
				// Check whether the property is writable
				if (jQuery.inArray(property, this.WRITEABLE_PROPS) >= 0) {
					// Check wether the property has a constraint and verify it
					var constraint = this.WRITEABLE_PROPS_CONSTRAINTS[property];
					if (constraint) {
						// verify maxLength
						if (constraint.maxLength && value.length >= constraint.maxLength) {
							var data = { name: property, value: value, maxLength: constraint.maxLength };
							var constraintError = GCN.createError('ATTRIBUTE_CONSTRAINT_VIOLATION',
								'Attribute "' + property + '" of ' + this._type +
								' is too long. The \'maxLength\' was set to {' + constraint.maxLength + '} ', data);
							GCN.handleError(constraintError, error);
							return;
						}
					}
					this._update(GCN.escapePropertyName(property), value);
				} else {
					GCN.handleError(GCN.createError('READONLY_ATTRIBUTE',
						'Attribute "' + property + '" of ' + this._type +
						' is read-only. Writeable properties are: ' +
						this.WRITEABLE_PROPS, this.WRITEABLE_PROPS), error);
					return;
				}
			}

			return (
				(jQuery.type(this._shadow[property]) !== 'undefined'
					? this._shadow
					: this._data)[property]
			);
		},

		/**
		 * Sends the a template string to the Aloha Servlet for rendering.
		 *
		 * @ignore
		 * @TODO: Consider making this function public.  At least one developer
		 *        has had need to render a custom template for a content
		 *        object.
		 *
		 * @private
		 * @param {string} template Template which will be rendered.
		 * @param {string} mode The rendering mode.  Valid values are "view",
		 *                      "edit", "pub."
		 * @param {function(object)} success A callback the receives the render
		 *                                   response.
		 * @param {function(GCNError):boolean} error Error handler.
		 * @param {boolean} post flag to POST the data
		 */
		'!_renderTemplate' : function (template, mode, success, error, post) {
			var channelParam = GCN._getChannelParameter(this);
			var url = GCN.settings.BACKEND_PATH +
					'/rest/' + this._type +
					(post ? '/render' : '/render/' + this.id()) +
			        channelParam +
			        (channelParam ? '&' : '?') +
			        'edit=' + ('edit' === mode) +
			        '&template=' + encodeURIComponent(template);
			if (mode === 'edit') {
				url += '&links=' + encodeURIComponent(GCN.settings.linksRenderMode);
			}
			if (post) {
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
			} else {
				this._authAjax({
					url: url,
					error: error,
					success: success
				});
			}
		},

		/**
		 * Wrapper for internal chainback _ajax method.
		 * 
		 * @ignore
		 * @private
		 * @param {object<string, *>} settings Settings for the ajax request.
		 *                                     The settings object is identical
		 *                                     to that of the `GCN.ajax'
		 *                                     method, which handles the actual
		 *                                     ajax transportation.
		 * @throws AJAX_ERROR
		 */
		'!_ajax': function (settings) {
			var that = this;

			// force no cache for all API calls
			settings.cache = false;
			settings.success = (function (onSuccess, onError) {
				return function (data) {
					// Ajax calls that do not target the REST API servlet do
					// not response data with a `responseInfo' object.
					// "/alohatag" is an example.  So we cannot
					// just assume that it exists.
					if (data.responseInfo) {
						switch (data.responseInfo.responseCode) {
						case 'OK':
							break;
						case 'AUTHREQUIRED':
							GCN.clearSession();
							that._authAjax(settings);
							return;
						default:
							// Since GCN.handleResponseError can throw an error,
							// we pass this function to _invoke, so the error is caught,
							// remembered and thrown in the end.
							that._invoke(GCN.handleResponseError, [data, onError]);
							return;
						}
					}

					if (onSuccess) {
						onSuccess(data);
					}
				};
			}(settings.success, settings.error, settings.url));

			this._queueAjax(settings);
		},

		/**
		 * Concrete implementatation of _fulfill().
		 *
		 * Resolves all promises made by this content object while ensuring
		 * that circularReferences, (which are completely possible, and valid)
		 * do not result in infinit recursion.
		 *
		 * @override
		 */
		'!_fulfill': function (success, error, stack) {
			var obj = this;
			if (obj._chain) {
				var circularReference =
						stack && -1 < jQuery.inArray(obj._chain, stack);
				if (!circularReference) {
					stack = stack || [];
					stack.push(obj._chain);
					obj._fulfill(function () {
						obj._read(success, error);
					}, error, stack);
					return;
				}
			}
			obj._read(success, error);
		},

		/**
		 * Similar to `_ajax', except that it prefixes the ajax url with the
		 * current session's `sid', and will trigger an
		 * `authentication-required' event if the session is not authenticated.
		 *
		 * @ignore
		 * @TODO(petro): Consider simplifiying this function signature to read:
		 *               `_auth( url, success, error )'
		 *
		 * @private
		 * @param {object<string, *>} settings Settings for the ajax request.
		 * @throws AUTHENTICATION_FAILED
		 */
		_authAjax: function (settings) {
			var that = this;

			if (GCN.isAuthenticating) {
				GCN.afterNextAuthentication(function () {
					that._authAjax(settings);
				});
				return;
			}

			if (!GCN.sid) {
				var cancel;

				if (settings.error) {
					/**
					 * @ignore
					 */
					cancel = function (error) {
						GCN.handleError(
							error || GCN.createError('AUTHENTICATION_FAILED'),
							settings.error
						);
					};
				} else {
					/**
					 * @ignore
					 */
					cancel = function (error) {
						if (error) {
							GCN.error(error.code, error.message, error.data);
						} else {
							GCN.error('AUTHENTICATION_FAILED');
						}
					};
				}

				GCN.afterNextAuthentication(function () {
					that._authAjax(settings);
				});

				if (GCN.usingSSO) {
					// First, try to automatically authenticate via
					// Single-SignOn
					GCN.loginWithSSO(GCN.onAuthenticated, function () {
						// ... if SSO fails, then fallback to requesting user
						// credentials: broadcast `authentication-required'
						// message.
						GCN.authenticate(cancel);
					});
				} else {
					// Trigger the `authentication-required' event to request
					// user credentials.
					GCN.authenticate(cancel);
				}

				return;
			}

			// Append "?sid=..." or "&sid=..." if needed.

			var urlFragment = settings.url.substr(
				GCN.settings.BACKEND_PATH.length
			);
			var isSidInUrl = /[\?\&]sid=/.test(urlFragment);
			if (!isSidInUrl) {
				var isFirstParam = (jQuery.inArray('?',
					urlFragment.split('')) === -1);
				settings.url += (isFirstParam ? '?' : '&') + 'sid='
				             +  (GCN.sid || '');
			}

			this._ajax(settings);
		},

		/**
		 * Recursively call `_continueWith()'.
		 *
		 * @ignore
		 * @private
		 * @override
		 */
		'!_onContinue': function (success, error) {
			var that = this;
			this._continueWith(function () {
				that._read(success, error);
			}, error);
		},

		/**
		 * Initializes this content object.  If a `success' callback is
		 * provided, it will cause this object's data to be fetched and passed
		 * to the callback.  This object's data will be fetched from the cache
		 * if is available, otherwise it will be fetched from the server.  If
		 * this content object API contains parent chainbacks, it will get its
		 * parent to fetch its own data first.
		 *
		 * <p>
		 * Basic content object implementation which all other content objects
		 * will inherit from.
		 * </p>
		 * 
		 * <p>
		 * If a `success' callback is provided,
		 * it will cause this object's data to be fetched and passed to the
		 * callback. This object's data will be fetched from the cache if is
		 * available, otherwise it will be fetched from the server. If this
		 * content object API contains parent chainbacks, it will get its parent
		 * to fetch its own data first.
		 * </p>
		 * 
		 * <p>
		 * You might also provide an object for initialization, to directly
		 * instantiate the object's data without loading it from the server. To
		 * do so just pass in a data object as received from the server instead
		 * of an id--just make sure this object has an `id' property.
		 * </p>
		 * 
		 * <p>
		 * If an `error' handler is provided, as the third parameter, it will
		 * catch any errors that have occured since the invocation of this call.
		 * It allows the global error handler to be intercepted before stopping
		 * the error or allowing it to propagate on to the global handler.
		 * </p>
		 * 
		 * @class
		 * @name ContentObjectAPI
		 * @param {number|string|object}
		 *            id
		 * @param {function(ContentObjectAPI))=}
		 *            success Optional success callback that will receive this
		 *            object as its only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional custom error handler.
		 * @param {object}
		 *            settings Basic settings for this object - depends on the
		 *            ContentObjetAPI Object used.
		 * @throws INVALID_DATA
		 *             If no id is found when providing an object for
		 *             initialization.
		 */
		_init: function (data, success, error, settings) {
			this._settings = settings;
			var id;

			if (jQuery.type(data) === 'object') {
				if (data.multichannelling) {
					this.multichannelling = data;
					// Remove the inherited object from the chain.
					if (this._chain) {
						this._chain = this._chain._chain;
					}
					id = this.multichannelling.derivedFrom.id();
				} else {
					if (!data.id) {
						var err = GCN.createError(
							'INVALID_DATA',
							'Data not sufficient for initalization: id is missing',
							data
						);
						GCN.handleError(err, error);
						return;
					}
					this._data = data;
					this._fetched = true;
					if (success) {
						this._invoke(success, [this]);
					}
					return;
				}
			} else {
				id = data;
			}

			// Ensure that each object has its very own `_data' and `_shadow'
			// objects.
			if (!this._fetched) {
				this._data = {};
				this._shadow = {};
				this._data.id = id;
			}
			if (success) {
				this._read(success, error);
			}
		},

		/**
		 * <p>
		 * Replaces tag blocks and editables with appropriate "&lt;node *&gt;"
		 * notation in a given string. Given an element whose innerHTML is:
		 *
		 * <pre>
		 *		&lt;span id="GENTICS_BLOCK_123"&gt;My Tag&lt;/span&gt;
		 * </pre>
		 *
		 * <p>
		 * encode() will return:
		 *
		 * <pre>
		 *		&lt;node 123&gt;
		 * </pre>
		 *
		 * @name encode
		 * @function
		 * @memberOf ContentObjectAPI
		 * @param {!jQuery} $element
		 *       An element whose contents are to be encoded.
		 * @param {?function(!Element): string} serializeFn
		 *       A function that returns the serialized contents of the
		 *       given element as a HTML string, excluding the start and end
		 *       tag of the element. If not provided, jQuery.html() will
		 *       be used.
		 * @return {string} The encoded HTML string.
		 */
		'!encode': function ($element, serializeFn) {
			var $clone = $element.clone();
			var id;
			var $block;
			var tags = jQuery.extend({}, this._blocks, this._editables);
			for (id in tags) {
				if (tags.hasOwnProperty(id)) {
					$block = $clone.find('#' + tags[id].element);
					if ($block.length) {
						// Empty all content blocks of their innerHTML.
						$block.html('').attr('id', BLOCK_ENCODING_PREFIX +
							tags[id].tagname);
					}
				}
			}
			serializeFn = serializeFn || function ($element) {
				return jQuery($element).html();
			};
			var html = serializeFn($clone[0]);
			return html.replace(CONTENT_BLOCK, function (substr, match) {
				return '<node ' + match + '>';
			});
		},

		/**
		 * For a given string, replace all occurances of "&lt;node&gt;" with
		 * appropriate HTML markup, allowing notated tags to be rendered within
		 * the surrounding HTML content.
		 *
		 * The success() handler will receives a string containing the contents
		 * of the `str' string with references to "<node>" having been inflated
		 * into their appropriate tag rendering.
		 *
		 * @name decode
		 * @function
		 * @memberOf ContentObjectAPI
		 * @param {string} str The content string, in which  "<node *>" tags
		 *                     will be inflated with their HTML rendering.
		 * @param {function(ContentObjectAPI))} success Success callback that
		 *                                              will receive the
		 *                                              decoded string.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		'!decode': function (str, success, error) {
			if (!success) {
				return;
			}

			var prefix = 'gcn-tag-placeholder-';
			var toRender = [];
			var html = replaceNodeTags(str, function (name, offset, str) {
				toRender.push('<node ', name, '>');
				return '<div id="' + prefix + name + '"></div>';
			});

			if (!toRender.length) {
				success(html);
				return;
			}

			// Instead of rendering each tag individually, we render them
			// together in one string, and map the results back into our
			// original html string.  This allows us to perform one request to
			// the server for any number of node tags found.

			var parsed = jQuery('<div>' + html + '</div>');
			var template = toRender.join('');
			var that = this;

			this._renderTemplate(template, 'edit', function (data) {
				var content = data.content;
				var tag;
				var tags = data.tags;
				var j = tags.length;
				var rendered = jQuery('<div>' + content + '</div>');

				var replaceTag = (function (numTags) {
					return function (tag) {
						parsed.find('#' + prefix + tag.prop('name'))
							.replaceWith(
								rendered.find('#' + tag.prop('id'))
							);

						if (0 === --numTags) {
							success(parsed.html());
						}
					};
				}(j));

				while (j) {
					that.tag(tags[--j], replaceTag);
				}
			}, error);
		},

		/**
		 * Clears this object from its constructor's cache so that the next
		 * attempt to access this object will result in a brand new instance
		 * being initialized and placed in the cache.
		 *
		 * @name clear
		 * @function
		 * @memberOf ContentObjectAPI
		 */
		'!clear': function () {
			// Do not clear the id from the _data.
			var id = this._data.id;
			this._data = {};
			this._data.id = id;
			this._shadow = {};
			this._fetched = false;
			this._clearCache();
		},

		/**
		 * Retrieves this objects parent folder.
		 * 
		 * @name folder
		 * @function
		 * @memberOf ContentObjectAPI
		 * @param {function(FolderAPI)=}
		 *            success Callback that will receive the requested object.
		 * @param {function(GCNError):boolean=}
		 *            error Custom error handler.
		 * @return {FolderAPI} API object for the retrieved GCN folder.
		 */
		'!folder': function (success, error) {
			return this._continue(GCN.FolderAPI, this._data.folderId, success,
				error);
		},

		/**
		 * Saves changes made to this content object to the backend.
		 * 
		 * @param {object=}
		 *            settings Optional settings to pass on to the ajax
		 *            function.
		 * @param {function(ContentObjectAPI)=}
		 *            success Optional callback that receives this object as its
		 *            only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional customer error handler.
		 */
		save: function () {
			var settings;
			var success;
			var error;
			var args = Array.prototype.slice.call(arguments);
			var len = args.length;
			var i;

			for (i = 0; i < len; ++i) {
				switch (jQuery.type(args[i])) {
				case 'object':
					if (!settings) {
						settings = args[i];
					}
					break;
				case 'function':
					if (!success) {
						success = args[i];
					} else {
						error = args[i];
					}
					break;
				case 'undefined':
					break;
				default:
					var err = GCN.createError('UNKNOWN_ARGUMENT',
						'Don\'t know what to do with arguments[' + i + '] ' +
						'value: "' + args[i] + '"', args);
					GCN.handleError(err, error);
					return;
				}
			}

			this._save(settings, success, error);
		},

		/**
		 * Persists this object's local data onto the server.  If the object
		 * has not yet been fetched we need to get it first so we can update
		 * its internals properly...
		 *
		 * @private
		 * @param {object} settings Object which will extend the basic
		 *                          settings of the ajax call
		 * @param {function(ContentObjectAPI)=} success Optional callback that
		 *                                              receives this object as
		 *                                              its only argument.
		 * @param {function(GCNError):boolean=} error Optional customer error
		 *                                            handler.
		 */
		'!_save': function (settings, success, error) {
			var obj = this;
			this._fulfill(function () {
				GCN.pub(obj._type + '.before-save');
				obj._persist(settings, success, error);
			}, error);
		},

		/**
		 * Returns the bare data structure of this content object.
		 * To be used for creating the save POST body data.
		 *
		 * @param {object<string, *>} Plain old object representation of this
		 *                            content object.
		 */
		'!json': function () {
			var json = {};

			if (this._deletedTags.length) {
				json['delete'] = this._deletedTags;
			}

			if (this._deletedBlocks.length) {
				json['delete'] = json['delete']
				               ? json['delete'].concat(this._deletedBlocks)
				               : this._deletedBlocks;
			}

			json[this._type] = jQuery.extend(true, {}, this._shadow);
			json[this._type].id = this._data.id;
			return json;
		},

		/**
		 * Sends the current state of this content object to be stored on the
		 * server.
		 *
		 * @private
		 * @param {function(ContentObjectAPI)=} success Optional callback that
		 *                                              receives this object as
		 *                                              its only argument.
		 * @param {function(GCNError):boolean=} error Optional customer error
		 *                                            handler.
		 * @throws HTTP_ERROR
		 */
		_persist: function (settings, success, error) {
			var that = this;

			if (!this._fetched) {
				this._read(function () {
					that._persist(settings, success, error);
				}, error);
				return;
			}

			this._authAjax({
				url   : GCN.settings.BACKEND_PATH + '/rest/'
				        + this._type + '/save/' + this.id()
				        + GCN._getChannelParameter(this),
				type  : 'POST',
				error : error,
				json  : jQuery.extend(this.json(), settings),
				success : function (response) {
					// We must not overwrite the `_data.tags' object with this
					// one.
					delete that._shadow.tags;

					// Everything else in `_shadow' should be written over to
					// `_data' before resetting the `_shadow' object.
					jQuery.extend(that._data, that._shadow);
					that._shadow = {};
					that._deletedTags = [];
					that._deletedBlocks = [];

					if (success) {
						that._invoke(success, [that]);
					}
				}
			});
		},

		/**
		 * Deletes this content object from its containing parent.
		 * 
		 * @param {function(ContentObjectAPI)=}
		 *            success Optional callback that receives this object as its
		 *            only argument.
		 * @param {function(GCNError):boolean=}
		 *            error Optional customer error handler.
		 */
		remove: function (success, error) {
			this._remove(success, error);
		},

		/**
		 * Get a channel-local copy of this content object.
		 *
		 * @public
		 * @function
		 * @name localize
		 * @memberOf ContentObjectAPI
		 * @param {funtion(ContentObjectAPI)=} success Optional callback to
		 *                                             receive this content
		 *                                             object as the only
		 *                                             argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		'!localize': function (success, error) {
			if (!this._channel && !GCN.channel()) {
				var err = GCN.createError(
					'NO_CHANNEL_ID_SET',
					'No channel is set in which to get the localized object',
					GCN
				);
				GCN.handleError(err, error);
				return false;
			}
			var local = this._continue(
				this._constructor,
				{
					derivedFrom: this,
					multichannelling: true,
					read: GCN.multichannelling.localize
				},
				success,
				error
			);
			return local;
		},

		/**
		 * Remove this channel-local object, and delete its local copy in the
		 * backend.
		 *
		 * @public
		 * @function
		 * @name unlocalize
		 * @memberOf ContentObjectAPI
		 * @param {funtion(ContentObjectAPI)=} success Optional callback to
		 *                                             receive this content
		 *                                             object as the only
		 *                                             argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		'!unlocalize': function (success, error) {
			if (!this._channel && !GCN.channel()) {
				var err = GCN.createError(
					'NO_CHANNEL_ID_SET',
					'No channel is set in which to get the unlocalized object',
					GCN
				);
				GCN.handleError(err, error);
				return false;
			}
			var placeholder = {
				multichannelling: {
					derivedFrom: this
				}
			};
			var that = this;
			GCN.multichannelling.unlocalize(placeholder, function () {
				// TODO: This should be done inside of
				// multichannelling.unlocalize() and not in this callback.
				// Clean cache & reset object to make sure it can't be used
				// properly any more.
				that._clearCache();
				that._data = {};
				that._shadow = {};
				if (success) {
					success();
				}
			}, error);
		},

		/**
		 * Performs a REST API request to delete this object from the server.
		 *
		 * @private
		 * @param {function()=} success Optional callback that
		 *                                              will be invoked once
		 *                                              this object has been
		 *                                              removed.
		 * @param {function(GCNError):boolean=} error Optional customer error
		 *                                            handler.
		 */
		'!_remove': function (success, error) {
			var that = this;
			this._authAjax({
				url     : GCN.settings.BACKEND_PATH + '/rest/'
				          + this._type + '/delete/' + this.id()
				          + GCN._getChannelParameter(that),
				type    : 'POST',
				error   : error,
				success : function (response) {
					// Clean cache & reset object to make sure it can't be used
					// properly any more.
					that._clearCache();
					that._data = {};
					that._shadow = {};

					// Don't forward the object to the success handler since
					// it's been deleted.
					if (success) {
						that._invoke(success);
					}
				}
			});
		},

		/**
		 * Removes any additionaly data stored on this objec which pertains to
		 * a tag matching the given tagname.  This function will be called when
		 * a tag is being removed in order to bring the content object to a
		 * consistant state.
		 * Should be overriden by subclasses.
		 *
		 * @param {string} tagid The Id of the tag whose associated data we
		 *                       want we want to remove.
		 */
		'!_removeAssociatedTagData': function (tagname) {},

		/**
		 * Return the replacement value, when this object is transformed to stringified JSON.
		 * This is necessary to avoid endless loops, because objects may have chainback objects
		 * stored in their _data.
		 * 
		 * @private
		 * @param {string} key
		 * @return {object} _data
		 */
		'!toJSON': function (key) {
			return this._data;
		}
	});

	GCN.ContentObjectAPI.update = update;

	/**
	 * Generates a factory method for chainback classes.  The method signature
	 * used with this factory function will match that of the target class'
	 * constructor.  Therefore this function is expected to be invoked with the
	 * follow combination of arguments ...
	 *
	 * Examples for GCN.pages api:
	 *
	 * To get an array containing 1 page:
	 * pages(1)
	 * pages(1, function () {})
	 *
	 * To get an array containing 2 pages:
	 * pages([1, 2])
	 * pages([1, 2], function () {})
	 *
	 * To get an array containing any and all pages:
	 * pages()
	 * pages(function () {})
	 *
	 * To get an array containing no pages:
	 * pages([])
	 * pages([], function () {});
	 *
	 * @param {Chainback} ctor The Chainback constructor we want to expose.
	 * @throws UNKNOWN_ARGUMENT
	 */
	GCN.exposeAPI = function (ctor) {
		return function () {
			// Convert arguments into an array
			// https://developer.mozilla.org/en/JavaScript/Reference/...
			// ...Functions_and_function_scope/arguments
			var args = Array.prototype.slice.call(arguments);
			var id;
			var ids;
			var success;
			var error;
			var settings;

			// iterate over arguments to find id || ids, succes, error and
			// settings
			jQuery.each(args, function (i, arg) {
				switch (jQuery.type(arg)) {
				// set id
				case 'string':
				case 'number':
					if (!id && !ids) {
						id = arg;
					} else {
						GCN.error('UNKNOWN_ARGUMENT',
							'id is already set. Don\'t know what to do with ' +
							'arguments[' + i + '] value: "' + arg + '"');
					}
					break;
				// set ids
				case 'array':
					if (!id && !ids) {
						ids = args[0];
					} else {
						GCN.error('UNKNOWN_ARGUMENT',
							'ids is already set. Don\'t know what to do with' +
							' arguments[' + i + '] value: "' + arg + '"');
					}
					break;
				// success and error handlers
				case 'function':
					if (!success) {
						success = arg;
					} else if (success && !error) {
						error = arg;
					} else {
						GCN.error('UNKNOWN_ARGUMENT',
							'success and error handler already set. Don\'t ' +
							'know what to do with arguments[' + i + ']');
					}
					break;
				// settings
				case 'object':
					if (!id && !ids) {
						id = arg;
					} else if (!settings) {
						settings = arg;
					} else {
						GCN.error('UNKNOWN_ARGUMENT',
							'settings are already present. Don\'t know what ' +
							'to do with arguments[' + i + '] value:' + ' "' +
							arg + '"');
					}
					break;
				default:
					GCN.error('UNKNOWN_ARGUMENT',
						'Don\'t know what to do with arguments[' + i +
						'] value: "' + arg + '"');
				}
			});

			// Prepare a new set of arguments to pass on during initialzation
			// of callee object.
			args = [];

			// settings should always be an object, even if it's just empty
			if (!settings) {
				settings = {};
			}

			args[0] = (typeof id !== 'undefined') ? id : ids;
			args[1] = success || settings.success || null;
			args[2] = error || settings.error || null;
			args[3] = settings;

			// We either add 0 (no channel) or the channelid to the hash
			var channel = GCN.settings.channel;

			// Check if the value is false, and set it to 0 in this case
			if (!channel) {
				channel = 0;
			}

			var hash = (id || ids)
			         ? ctor._makeHash(channel + '/' + (ids ? ids.sort().join(',') : id))
			         : null;

			return GCN.getChainback(ctor, hash, null, args);
		};

	};

}(GCN));
