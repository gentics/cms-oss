/*global global: true, process: true, require: true, module: true */

/**
 * Establishes the `GCN' object and exposes it in the global context.
 */
GCN = (function (global) {
	'use strict';

	// Check whether we are in nodeJS context.
	if (typeof process !== 'undefined' && process.versions
			&& process.versions.node) {
		global.isNode = true;
		var XMLHttpRequest = require('xmlhttprequest').XMLHttpRequest;
		jQuery = global.$ = global.jQuery = require('jquery');
		global.jQuery.ajaxSettings.xhr = function createNodeXHRForGCN() {
			return new XMLHttpRequest();
		};
		// http://stackoverflow.com/a/6432602
		global.jQuery.support.cors = true;
	}

	// Temporarily deactivate JS Lints "dangerous '.'" test for regular expressions.
	/*jslint regexp: false*/
	/**
	 * Match URL to internal page
	 *
	 * @const
	 * @type {RegExp}
	 */
	var INTERNAL_PAGEURL = /\/alohapage\?.*realid=([0-9]+).*$/;

	/**
	 * Match URL to internal file
	 *
	 * @const
	 * @type {RegExp}
	 */
	var INTERNAL_FILEURL = /^http.*\?.*&do=16000&id=([0-9]+).*$/;
	/*jslint regexp: true*/

	/**
	 * @private
	 * @type {object} An object to indicate which handlers have been
	 *                 registered through the `GCN.onRender()' function.
	 */
	var onRenderHandler = {};

	/**
	 * @private
	 * @type {boolean} A flag to indicate whether or not a handler has been
	 *                 registerd through the `GCN.onError()' function.
	 */
	var hasOnErrorHandler = false;

	/**
	 * @ignore
	 * @type {boolean} An internal flag that stores whether an authentication
	 *                 handler has been set.
	 */
	var hasAuthenticationHandler = false;

	/**
	 * GCN JS API error object.  This is the object passed to error handlers.
	 *
	 * @class 
	 * @name GCNError
	 * @param {string} code error code for the error
	 * @param {string} message descriptive error message
	 * @param {object} data additional data
	 */
	var GCNError = function (code, message, data) {
		this.code = code;
		this.message = message;
		this.data = data;
	};

	/**
	 * Returns a human-readable representation of this error object.
	 *
	 * @public
	 * @return {string}
	 */
	GCNError.prototype.toString = function () {
		return 'GCN ERROR (' + this.code + '): "' + (this.message || '') + '"';
	};

	/**
	 * @name GCN
	 * @class
	 *
	 * Base namespace for the Gentics Content.Node JavaScript API.
	 */
	var GCN = global.GCN || {};

	jQuery.extend(GCN, {
		/** @lends GCN */

		/**
		 * Reference to the global context.
		 * 
		 * @type {object} 
		 */
		global: global,

		/**
		 * Settings for the Gentics Content.Node JavaScript API.
		 * 
		 * @type {object<string, object>}
		 */
		settings: {

			/**
			 * The language code with which to render tags.
			 * 
			 * @const
			 * @name settings.lang
			 * @default 'en'
			 * @memberOf GCN
			 * @type {string} 
			 */
			lang: 'en',

			/**
			 * Default GCN backend path. Do not add a trailing slash here.
			 * 
			 * @const
			 * @default '/'
			 * @name settings.BACKEND_PATH
			 * @memberOf GCN
			 * @type {string}
			 */
			BACKEND_PATH: '/',

			/**
			 * The keyword for the construct that defines Aloha Editor links. In
			 * most Content.Node installations this will be "gtxalohapagelink",
			 * but can be otherwise defined.
			 * 
			 * @const
			 * @default 'gtxalohapagelink'
			 * @name settings.MAGIC_LINK
			 * @memberOf GCN
			 * @type {string}
			 */
			MAGIC_LINK: 'gtxalohapagelink',

			/**
			 * Determines whether links will be rendered as back-end urls or
			 * front-end urls. Can either be set to "backend" or "frontend".
			 * 
			 * @const
			 * @default 'backend'
			 * @name settings.linksRenderMode
			 * @memberOf GCN
			 * @type {string}
			 */
			linksRenderMode: 'backend',

			/**
			 * The default callback to determine if a URL is an internal link.
			 *
			 * Matches the given href agains <code>INTERNAL_PAGEURL</code> and
			 * <code>INTERNAL_FILEURL</code> respectively.
			 *
			 * @const
			 * @type {function}
			 * @memberOf GCN
			 * @name settings.checkForInternalLink
			 * @param {string} href The URL to be checked.
			 * @return {object} An object containing the fields
			 * <ul>
			 *   <li>match (boolean): indicating if the URL is internal</li>
			 *   <li>url (string|integer): The ID of the internal page or 0 if
			 *       the URL points to an internal file, or the external URL</li>
			 *   <li>fileurl (string|integer): The ID of the internal file, and
			 *       zero if the URL is external or points to a page.</li>
			 * </ul>
			 */
			checkForInternalLink: function (href) {
				var urlMatch = href.match(INTERNAL_PAGEURL);

				if (urlMatch) {
					return { match: true, url: parseInt(urlMatch[1], 10), fileurl: 0 };
				}

				urlMatch = href.match(INTERNAL_FILEURL);

				if (urlMatch) {
					return { match: true, url: 0, fileurl: parseInt(urlMatch[1], 10) };
				}

				return { match: false, url: href, fileurl: 0 };
			},

			/**
			 * Set a channelid to work on for multichannelling or false if no
			 * channel should be used
			 * 
			 * @memberOf GCN
			 * @default false
			 * @type {bool|int|string}
			 */
			channel: false
		},

		/**
		 * Publish a message
		 *
		 * @param {string} message channel name
		 * @param {*=} params
		 */
		pub: function (channel, params) {
			if (!hasOnErrorHandler && channel === 'error-encountered') {
				// throw an error if there is no subscription to
				// error-encountered.
				throw params;
			}

			switch (channel) {
			case 'tag.rendered':
			case 'page.rendered':
			case 'content-rendered':
				// for these channels, we need to have a custom implementation:
				// param[0] is the html of the rendered tag
				// param[1] is the tag object
				// param[2] is the callback, that must be called from the event handler
				// If more than one handler subscribed, we will chain them by calling the
				// next handler in the callback of the previous. Only the callback of the
				// last handler will call the original callback
				if (jQuery.isArray(onRenderHandler[channel])) {
					var handlers = onRenderHandler[channel];
					// substitute callback function with wrapper
					var callback = params[2], i = 0;
					params[2] = function (html) {
						if (++i < handlers.length) {
							// call the next handler. Pass the html returned from the
							// previous callback to the next handler
							params[0] = html;
							handlers[i].apply(null, params);
						} else {
							// there are no more handlers, so call the original
							// callback with the final html
							callback(html);
						}
					};
					handlers[0].apply(null, params);
				}
				return;
			}
			// errors and exceptions in (custom-)event-handler should not stop the execution of GCN-JS-API methods
			try {
				jQuery(GCN).trigger(channel, params);
			} catch (err) {
				// include the original error and stack in the error message
				var msg = 'Encountered error when publishing message for channel "' + channel + '" with following details: \n';
				msg += 'original error: ' + err + '\n';
				// check if the stack property exists on the error object because it is non-standard
				if (typeof err.stack !== 'undefined') {
					msg += 'and stack: \n' + err.stack;
				}
				GCN.error('PUBSUB_HANDLER_FAILED', msg);
			}
		},

		/**
		 * Subscribe to a message channel
		 *
		 * @param {string} message channel name
		 * @param {function} handler function - message parameters will be
		 *                           passed.
		 */
		sub: function (channel, handler) {
			// register default handlers
			switch (channel) {
			case 'error-encountered':
				hasOnErrorHandler = true;
				break;
			case 'tag.rendered':
			case 'page.rendered':
			case 'content-rendered':
				// store all the handlers in an array.
				onRenderHandler[channel] = onRenderHandler[channel] || [];
				onRenderHandler[channel].push(handler);
				return;
			case 'authentication-required':
			case 'session.authentication-required':
				hasAuthenticationHandler = true;
				break;
			}

			jQuery(GCN).bind(channel, function (event, param1, param2, param3) {
				handler(param1, param2, param3);
			});
		},

		/**
		 * Tigger an error message 'error-encountered'.
		 *
		 * @param {string} error code
		 * @param {string} error message
		 * @param {object} additional error data
		 */
		error: function (code, message, data) {
			var error = new GCNError(code, message, data);
			this.pub('error-encountered', error);
		},

		/**
		 * Returns an object containing the formal error fields.  The object
		 * contains a `toString' method to print any uncaught exceptions
		 * nicely.
		 *
		 * @param {string} code
		 * @param {string} message
		 * @param {object} data
		 * @return {GCNError}
		 */
		createError: function (code, message, data) {
			return new GCNError(code, message, data);
		},

		/**
		 * Wraps the `jQuery.ajax()' method.
		 *
		 * @public
		 * @param {object} settings
		 * @throws HTTP_ERROR
		 */
		ajax: function (settings) {
			if (settings.json) {
				settings.data = JSON.stringify(settings.json);
				delete settings.json;
			}
			settings.dataType = 'json';
			settings.contentType = 'application/json; charset=utf-8';
			jQuery.ajax(settings);
		},

		/**
		 * Set links render mode if a parameter is given
		 * retrieve it if not
		 *
		 * @param {string} mode
		 * @return {string} mode
		 */
		linksRenderMode: function (mode) {
			if (mode) {
				GCN.settings.linksRenderMode = mode;
			}
			return GCN.settings.linksRenderMode;
		},

		/**
		 * Set channel if a parameter is given retrieve it otherwise.
		 *
		 * If you don't want to work on a channel just set it to false, which
		 * is the default value.
		 *
		 * @param {string|boolean} channel The id of the channel to be set or false to unset the channel.
		 * @return {string} current channel id.
		 */
		channel: function (channel) {
			if (channel || false === channel) {
				GCN.settings.channel = channel;
			}
			return GCN.settings.channel;
		},

		/**
		 * Constructs the nodeId query parameter for rest calls.
		 *
		 * @param {AbstractContentObject} contentObject A content object instance.
		 * @param {string=} delimiter Optional delimiter character.
		 * @return {string} Query parameter string.
		 */
		_getChannelParameter: function (contentObject, delimiter) {
			if (false === contentObject._channel) {
				return '';
			}
			return (delimiter || '?') + 'nodeId=' + contentObject._channel;
		},

		/**
		 * @param {string} html Rendered content
		 * @param {Chainback} obj The rendered ContentObject.
		 * @param {function(html)} callback Receives the processed html.
		 */
		_handleContentRendered: function (html, obj, callback) {
			var channel = obj._type + '.rendered';
			if (onRenderHandler[channel]) {
				GCN.pub(channel, [html, obj, callback]);
			} else if (onRenderHandler['content-rendered']) {
				// Because 'content-rendered' has been deprecated in favor of
				// '{tag|page}.rendered'.
				GCN.pub('content-rendered', [html, obj, callback]);
			} else {
				callback(html);
			}
		},

		/**
		 * Handles the ajax transport error.  It will invoke the custom error
		 * handler if one is provided, and propagate the error onto the global
		 * handler if the an error handler does not return `false'.
		 *
		 * @param {object} xhr
		 * @param {string} msg The error message
		 * @param {function} handler Custom error handler.
		 * @throws HTTP_ERROR
		 */
		handleHttpError: function (xhr, msg, handler) {
			var throwException = true;

			if (handler) {
				throwException = handler(GCN.createError('HTTP_ERROR', msg,
					xhr));
			}

			if (throwException !== 'false') {
				GCN.error('HTTP_ERROR', msg, xhr);
			}
		},

		/**
		 * Handles error that occur when an ajax request succeeds but the
		 * backend responds with an error.
		 *
		 * @param {object} reponse The REST API response object.
		 * @param {function(GCNError):boolean} handler Custom error handler.
		 */
		handleResponseError: function (response, handler) {
			var info = response.responseInfo;
			var throwException = true;

			if (handler) {
				throwException = handler(GCN.createError(
					info.responseCode,
					info.responseMessage,
					response
				));
			}

			if (throwException !== false) {
				GCN.error(info.responseCode, info.responseMessage, response);
			}
		},

		/**
		 * Tiggers the GCN error event.
		 *
		 * @param {GCNError} error
		 * @param {function(GCNError):boolean} handler Custom error handler.
		 * @return {boolean} Whether or not to the exception was thrown.
		 */
		handleError: function (error, handler) {
			var throwException = true;

			if (handler) {
				throwException = handler(error);
			}

			if (throwException !== false) {
				GCN.error(error.code, error.message, error.data);
			}

			return throwException;
		},

		/**
		 * Check if an authentication handler has been registered.
		 *
		 * @return {boolean} True if an handler for the
		 *                   'authentication-required' message has been
		 *                   registered.
		 */
		_hasAuthenticationHandler: function () {
			return hasAuthenticationHandler;
		}

	});

	// Expose the Gentics Content.Node JavaScript API to the global context.
	// This will be `window' in most cases.
	return (global.GCN = GCN);

}(typeof global !== 'undefined' ? global : window));
