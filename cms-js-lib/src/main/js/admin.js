/*global jQuery: true, GCN: true */
/**
 * @overview:
 * defines `AdminAPI', which gives access to global Content Node data (like all
 * existing constructs).  A single instance is of this API is exposed as
 * GCN.Admin.
 */
(function (GCN) {

	'use strict';

	/**
	 * Maps constructcategories that were fetched via the Rest API
	 * into a sorted nested array of constructs.
	 *
	 * @param {object<string, object>} constructs
	 * @return {object<string, object>}
	 */
	function mapConstructCategories(constructs) {
		var map = {
			categories: {},
			categorySortorder: []
		};
		var categories = [];
		var keyword;

		for (keyword in constructs) {
			if (constructs.hasOwnProperty(keyword)) {
				var construct = constructs[keyword];
				var name = construct.category;
				var sortorder = construct.categorySortorder;

				// Because constructs that have not been assigned to a category
				// have not real category name.
				if (!name) {
					name = 'GCN_UNCATEGORIZED';
					sortorder = -1;
				}

				// Initialize the inner array of constructs
				if (!map.categories[name]) {
					var newCategory = {};
					newCategory.constructs = {};
					newCategory.sortorder = sortorder;
					newCategory.name = name;
					map.categories[name] = newCategory;
					categories.push(newCategory);
				}

				// Add the construct to the category
				map.categories[name].constructs[keyword] = construct;
			}
		}

		categories.sort(function (a, b) {
			return a.sortorder - b.sortorder;
		});

		// Add the sorted category names to the sortorder field
		var i;
		for (i in categories) {
			if (categories.hasOwnProperty(i)) {
				var category = categories[i];
				if (typeof category.sortorder !== 'undefined'
						&& category.sortorder !== -1) {
					map.categorySortorder.push(category.name);
				}
			}
		}

		return map;
	}

	/**
	 * Maps constructs, that were fetched via the Rest API, using their keyword
	 * as the keys.
	 *
	 * @param {object<string, object>} constructs Consturcts mapped against
	 *                                            their id.
	 * @return {object<string, object>} Constructs mapped against their keys.
	 */
	function mapConstructs(constructs) {
		if (!constructs) {
			return {};
		}
		var map = {};
		var constructId;
		for (constructId in constructs) {
			if (constructs.hasOwnProperty(constructId)) {
				map[constructs[constructId].keyword] = constructs[constructId];
			}
		}
		return map;
	}

	/**
	 * The AdminAPI is exposed via {@link GCN.Admin}. The AdminAPI is not meant
	 * to be instatiated by the implementer. Stick to using GCN.Admin.
	 * 
	 * @name AdminAPI
	 * @class
	 * @augments Chainback
	 */
	var AdminAPI = GCN.defineChainback({
		/** @lends AdminAPI */

		__chainbacktype__: 'AdminAPI',
		_type: 'admin',

		/**
		 * @private
		 * @type {object<string, number} Constructs for this node are cached
		 *                               here so that we only need to fetch
		 *                               this once.
		 */
		_constructs: null,

		/**
		 * @private
		 * @type {object<string, object} Constructs categories for this node.
		 *                               Cached here so that we only need to
		 *                               fetch this once.
		 */
		_constructCategories: null,

		/**
		 * Initialize
		 * @private
		 */
		_init: function (id, success, error, settings) {
			if (typeof success === 'function') {
				this._invoke(success, [this]);
			}
		},

		/**
		 * Wrapper for internal chainback _ajax method.
		 *
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
						that._invoke(onSuccess, [data]);
					}
				};
			}(settings.success, settings.error, settings.url));

			this._queueAjax(settings);
		},

		/**
		 * Similar to `_ajax', except that it prefixes the ajax url with the
		 * current session's `sid', and will trigger an
		 * `authentication-required' event if the session is not authenticated.
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
				             + (GCN.sid || '');
			}

			this._ajax(settings);
		},

		/**
		 * Retrieves a list of all constructs and constructs categories and
		 * passes it as the only argument into the `success()' callback.
		 *
		 * @param {function(Array.<object>)=} success Callback to receive an
		 *                                            array of constructs.
		 * @param {function(GCNError):boolean=} error Custom error handler.
		 * @return undefined
		 * @throws INVALID_ARGUMENTS
		 */
		constructs: function (success, error) {
			var that = this;
			if (!success) {
				GCN.error('INVALID_ARGUMENTS', 'the `constructs()\' method ' +
					'requires at least a success callback to be given');
			}
			if (this._constructs) {
				this._invoke(success, [this._constructs]);
			} else {
				this._authAjax({
					url: GCN.settings.BACKEND_PATH + '/rest/construct/list.json',
					type: 'GET',
					error: function (xhr, status, msg) {
						GCN.handleHttpError(xhr, msg, error);
					},
					success: function (response) {
						if (GCN.getResponseCode(response) === 'OK') {
							that._constructs = mapConstructs(response.constructs);
							that._invoke(success, [that._constructs]);
						} else {
							GCN.handleResponseError(response, error);
						}
					}
				});
			}
		},

		/**
		 * Helper method that will load the constructs of this node.
		 *
		 * @private
		 * @this {AdminAPI}
		 * @param {function(Array.<object>)} success callback
		 * @param {function(GCNError):boolean=} error callback
		 */
		constructCategories: function (success, error) {
			if (this._constructCategories) {
				this._invoke(success, [this._constructCategories]);
			} else {
				var that = this;
				this.constructs(function (constructs) {
					that._constructCategories = mapConstructCategories(constructs);
					that._invoke(success, [that._constructCategories]);
				}, error);
			}
		},

		/**
		 * Internal method, to fetch this object's data from the server.
		 *
		 * @private
		 * @override
		 * @param {function(ContentObjectAPI)=} success Optional callback that
		 *                                              receives this object as
		 *                                              its only argument.
		 * @param {function(GCNError):boolean=} error Optional customer error
		 *                                            handler.
		 */
		'!_read': function (success, error) {
			this._invoke(success, [this]);
		}
	});

	/**
	 * Exposes an instance of the AdminAPI via GCN.Admin.
	 * 
	 * @see AdminAPI
	 */
	GCN.Admin = new AdminAPI();

}(GCN));
