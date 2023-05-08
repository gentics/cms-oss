(function (GCN) {

	'use strict';

	/**
	 * @type {Array.<function>} A set of functions to be invoked after the next
	 *                          `authenticated' message is broadcasted. Once
	 *                          this event is proceeded, all functions in this
	 *                          array will be invoked in order, and the array
	 *                          will be flushed in preparation for the next
	 *                          `authenticated' event.
	 */
	var afterAuthenticationQueue = [];

	/**
	 * Fetches the details of the user who is logged in to this session.
	 *
	 * @param {function} success Callback to receive the requested user data.
	 * @param {function} error Handler for failures
	 * @throws HTTP_ERROR
	 */
	function fetchUserDetails(success, error) {
		GCN.ajax({
			url: GCN.settings.BACKEND_PATH + '/rest/user/me?sid=' + GCN.sid,
			dataType: 'json',
			contentType: 'application/json; charset=utf-8',

			success: function (response) {
				if (GCN.getResponseCode(response) === 'OK') {
					success(response.user);
				} else {
					GCN.handleResponseError(response, error);
				}
			},

			error: function (xhr, status, msg) {
				GCN.handleHttpError(xhr, msg, error);
			}
		});
	}

	jQuery.extend(GCN, {
		/** @lends GCN */

		/**
		 * @const
		 * @type {boolean} Whether or not Single-SignOn is used for automatic
		 *                 authentication.
		 */
		usingSSO: false,

		isAuthenticating: false,

		/**
		 * @type {number} Stores the user's session id.  It is required for
		 *                making REST-API requests.
		 */
		sid: null,

		/**
		 * Sets the `sid'.  If one has already been set, the it will be
		 * overwritten.
		 *
		 * @param {id} sid The value to set the `sid' to.
		 */
		setSid: function (sid) {
			GCN.sid = sid;
			GCN.pub('session-set', [sid]);
			GCN.pub('session.sid-set', [sid]);
		},

		/**
		 * Log into Content.Node, with the given credentials.
		 *
		 * @param {string} username
		 * @param {string} password
		 * @param {function} success Invoked when login attempt completes
		 *                           regardless of whether or not
		 *                           authentication succeeded.
		 * @param {function} error Called if there an HTTP error occured when
		 *                         performing the ajax request.
		 */
		login: function (username, password, success, error) {
			GCN.isAuthenticating = true;
			GCN.ajax({

				/**
				 * Why we add a ".json" suffix to the login url: In the context
				 * of the GCN environment, the ".json" suffix is appended to
				 * REST-API requests to ensure that the server returns JSON
				 * data rather than XML data, which is what browsers seem to
				 * automatically request.  The usage of the ".json" suffix here
				 * is for an entirely different reason.  We use it as a (fairly
				 * innocent) hack to prevent this request from being processed
				 * by CAS filters that are targeting "rest/auth/login" .  In
				 * most production cases, this would not be necessary, since it
				 * is not common that this login url would be used for both
				 * credential based logins and SSO logins, but having it does
				 * not do anything bad.
				 */
				url: GCN.settings.BACKEND_PATH + '/rest/auth/login.json',
				type: 'POST',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',
				data: JSON.stringify({
					login: username || '',
					password: password || ''
				}),
				success: function (response, textStatus, jqXHR) {
					GCN.isAuthenticating = false;
					if (GCN.getResponseCode(response) === 'OK') {
						if (GCN.global.isNode) {
							var header = jqXHR.getResponseHeader('Set-Cookie');
							if (!header) {
								GCN.handleError(GCN.createError(
									'AUTHENTICATION_FAILED',
									'Could not find authentication cookie',
									jqXHR
								), error);
								return;
							}
							var secret = header.substr(19, 15);
							GCN.setSid(response.sid + secret);
						} else {
							GCN.setSid(response.sid);
						}

						if (success) {
							success(true, { user: response.user });
						}

						// Because 'authenticated' has been deprecated in favor
						// of 'session.authenticated'
						GCN.pub('authenticated', {user: response.user});
						GCN.pub('session.authenticated', {user: response.user});
					} else {
						var info = response.responseInfo;
						if (success) {
							success(false, {
								error: GCN.createError(info.responseCode,
									info.responseMessage, response)
							});
						}
					}
				},

				error: function (xhr, status, msg) {
					GCN.handleHttpError(xhr, msg, error);
				}

			});
		},

		/**
		 * Triggers the `authentication-required' event.  Provides the handler
		 * a `proceed' and a `cancel' function to branch the continuation of
		 * the program's control flow depending on the success or failure of
		 * the authentication attempt.
		 *
		 * @param {function(GCNError=)} cancelCallback A function to be invoked
		 *                                             if authentication fails.
		 * @throws NO_AUTH_HANDLER Thrown if now handler has been registered
		 *                         `onAuthenticatedRequired' method.
		 */
		authenticate: function (cancelCallback) {
			// Check whether an authentication handler has been set.
			if (!this._hasAuthenticationHandler()) {
				afterAuthenticationQueue = [];

				var error = GCN.createError('NO_AUTH_HANDLER', 'Could not ' +
					'authenticate because no authentication handler has been' +
					' registered.');

				if (cancelCallback) {
					cancelCallback(error);
				} else {
					GCN.error(error.code, error.message, error.data);
				}

				return;
			}

			GCN.isAuthenticating = true;

			var proceed = GCN.onAuthenticated;
			var cancel = function (error) {
				afterAuthenticationQueue = [];
				cancelCallback(error);
			};

			// Because 'authentication-required' has been deprecated in favor of
			// 'session.authentication-required'
			GCN.pub('authentication-required', [proceed, cancel]);
			GCN.pub('session.authentication-required', [proceed, cancel]);
		},

		afterNextAuthentication: function (callback) {
			if (callback) {
				afterAuthenticationQueue.push(callback);
			}
		},

		/**
		 * This is the method that is passed as `proceed()' to the handler
		 * registered through `onAuthenticationRequired()'.  It ensures that
		 * all functions that are pending authentication will be executed in
		 * FIFO order.
		 */
		onAuthenticated: function () {
			GCN.isAuthenticating = false;

			var i;
			var j = afterAuthenticationQueue.length;

			for (i = 0; i < j; ++i) {
				afterAuthenticationQueue[i]();
			}

			afterAuthenticationQueue = [];
		},

		/**
		 * Destroys the saved session data.
		 * At the moment this only involves clearing the stored SID.
		 */
		clearSession: function () {
			GCN.setSid(null);
		},

		/**
		 * Attemps to authenticate using Single-Sign-On.
		 *
		 * @param {function} success
		 * @param {function} error
		 * @throws HTTP_ERROR
		 */
		loginWithSSO: function (success, error) {
			GCN.isAuthenticating = true;

			// The following must happen after the dom is ready, and not before.
			jQuery(function () {
				var iframe = jQuery('<iframe id="gcn-sso-frame">').hide();

				jQuery('body').append(iframe);

				iframe.load(function () {
					GCN.isAuthenticating = false;

					var response = iframe.contents().text();

					switch (response) {
					case '':
					case 'FAILURE':
						var err = GCN.createError('HTTP_ERROR',
							'Error encountered while making HTTP request');

						GCN.handleError(err, error);
						break;
					case 'NOTFOUND':
						success(false);
						break;
					default:
						GCN.setSid(response);

						fetchUserDetails(function (user) {
							if (success) {
								success(true, {user: user});
							}

							// Because 'authenticated' has been deprecated in
							// favor of 'session.authenticated'
							GCN.pub('authenticated', {user: user});
							GCN.pub('session.authenticated', {user: user});
						});
					}

					iframe.remove();
				});

				iframe.attr('src', GCN.settings.BACKEND_PATH +
					'/rest/auth/ssologin?ts=' + (new Date()).getTime());
			});
		},

		/**
		 * Do a logout and clear the session id.
		 *
		 * @param {function} success
		 * @param {function} error A callback that will be invoked if an ajax
		 *                         error occurs while trying to accomplish the
		 *                         logout request.
		 */
		logout: function (success, error) {
			// If no `sid' exists, the logout fails.
			if (!GCN.sid) {
				success(false, GCN.createError('NO_SESSION',
					'There is no session to log out of.'));

				return;
			}

			GCN.ajax({
				url: GCN.settings.BACKEND_PATH + '/rest/auth/logout/' +
				     GCN.sid,
				type: 'POST',
				dataType: 'json',
				contentType: 'application/json; charset=utf-8',

				success: function (response) {
					if (GCN.getResponseCode(response) === 'OK') {
						GCN.clearSession();
						success(true);
					} else {
						var info = response.responseInfo;
						success(false, GCN.createError(info.responseCode,
							info.responseMessage, response));
					}
				},

				error: function (xhr, status, msg) {
					GCN.handleHttpError(xhr, msg, error);
				}
			});
		},

		/**
		 * Given a GCN ajax response object, return the response code.
		 *
		 * @param {object} response GCN response object return in the ajax
		 *                          request callback.
		 */
		getResponseCode: function (response) {
			return (response && response.responseInfo &&
				response.responseInfo.responseCode);
		}

	});

}(GCN));
