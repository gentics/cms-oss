/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, test: true, console: true */
(function () {

	'use strict';

	// Swollow all errors "NOTFOUND" and "AUTHENTICATION_FAILED" errors.
	GCN.sub('error-encountered', function (error) {
		if (error.code !== 'NOTFOUND' &&
				error.code !== 'AUTHENTICATION_FAILED') {
			// throw 'Unexpected error: ' + error.toString();
			console.error(error.toString());
		} else {
			console.warn(error.toString());
		}
	});

	// =========================================================================
	// Log in test
	// =========================================================================

	function loginTests1(next) {
		module('Log in');

		GCN.clearSession();
		GCN.usingSSO = false;

		asyncTest('Login with invalid credentials', function () {
			GCN.login('node', 'invalidpassword', function (success, data) {
				ok(!GCN.isAuthenticating,
					'`GCN.isAuthenticating\' should be `false\'');
				ok(!success, 'Check that login failed');
				ok(!GCN.sid, 'Check that the `GCN.sid\' is empty');

				start();
				next();
			});
		});
	}

	function loginTests2(next) {
		GCN.clearSession();
		GCN.usingSSO = false;

		asyncTest('Login with valid credentials', function () {
			GCN.login('node', 'node', function (success, data) {
				ok(!GCN.isAuthenticating,
					'`GCN.isAuthenticating\' should be `false\'');
				ok(success, 'Check for login success');

				if (success) {
					ok(GCN.sid, 'Check whether the sid is set after login');
					ok(data && data.user, 'Check whether user was returned');
				}

				if (data && data.user) {
					equal(data.user.login, 'node', 'Check the login name');
				}

				start();
				next();
			});
		});
	}

	function ssoLoginTests(next) {
		GCN.clearSession();
		GCN.usingSSO = true;

		asyncTest('Login with SSO', function () {
			GCN.loginWithSSO(function (success, data) {
				// We do not test for login success here because, this is very
				// much dependent on whether or not SSO has been set up on the
				// server ok(success, 'Check for login success');

				ok(!GCN.isAuthenticating,
					'`GCN.isAuthenticating\' should be `false\'');

				if (success) {
					ok(GCN.sid, 'Check whether the sid is set now');
					ok(data && data.user, 'Check whether user was returned');

					if (data && data.user) {
						equal(jQuery.type(data.user.login), 'string',
							'`data.user.login\' should be a string');
					}
				}

				start();
				next();
			});
		});
	}

	// =========================================================================
	// Log out tests
	// =========================================================================

	function logoutTests1(next) {
		module('Log out');

		asyncTest('Logout of existing session', function () {
			// We should be signed in; now sign out.
			if (GCN.sid) {
				GCN.logout(function (success, error) {
					ok(success, 'Check that logout succeeded');
					ok(!GCN.sid, '`GCN.sid\' should be unset after logout');

					start();
					next();
				});
			} else {
				start();
				next();
			}
		});
	}

	function logoutTests2(next) {
		GCN.clearSession();

		asyncTest('Logout without session', function () {
			// Attempting to log out when no session exists should result in an
			// error being passed to the callback.
			GCN.logout(function (success, error) {
				ok(!success, 'Logout should fail');
				ok(error && error.code, 'An error object should be provided');

				start();
				next();
			});
		});
	}

	// =========================================================================
	// Control flow tests
	// =========================================================================

	var onAuthReqCallCount = 0;
	var proceedCallCount = 0;
	var cancelCallCount = 0;
	var passCount = 0;

	var authReqHandlerHolder = { handler: function () {} };

	GCN.sub('authentication-required', function (proceed, cancel) {
		authReqHandlerHolder.handler.apply(this, arguments);
	});

	function loginAndGoOn() {
		GCN.clearSession();
		GCN.usingSSO = false;

		GCN.login('node', 'node', function (success) {
			ok(!GCN.isAuthenticating,
				'`GCN.isAuthenticating\' should be `false\'');
		});
	}

	function authenticationRequired(next) {
		module('onAuthenticationRequired');

		GCN.clearSession();
		GCN.usingSSO = false;

		// We call our onAuthenticationRequired handler implementation in an
		// object so that we will be able to change this function during
		// runtime in order to test different handlers.  But currently we are
		// only testing one handler.
		authReqHandlerHolder.handler = function (proceed, cancel) {
			++onAuthReqCallCount;

			if (++passCount === 1) {
				ok(jQuery.type(proceed) === 'function', 'A `proceed\' ' +
					'function was passed to `onAuthenticationRequired\' ' +
					'handler');

				GCN.login('node', 'node', function (success, data) {
					proceed();
				});
			} else if (passCount > 1) {
				ok(jQuery.type(cancel) === 'function', 'A `cancel\' function' +
					'was passed to `onAuthenticationRequired\' handler');

				cancel();
			} else {
				ok(false, '`onAuthenticationRequired\' should have been ' +
					'called 2 times.  It was called ' + passCount + ' times.');
			}
		};

		var onError = function (error) {
			if (passCount === 1) {
				++proceedCallCount;
				GCN.clearSession();
				GCN.page(65536, function () {}, onError);
			} else if (passCount === 2) {
				++cancelCallCount;
				next();
			}
		};

		asyncTest('onAuthReq', function () {
			// Authentication should succeed but since the page does not exist
			// we expect to land in the error handler.
			GCN.page(65536, function () {}, onError);

			start();
		});
	}

	function authenticationRequired2(next) {
		test('onAuthReq2', function () {
			equal(onAuthReqCallCount, 2,
				'Number of `onAuthenticationRequired\' calls');
			equal(proceedCallCount, 1, 'Number of `proceed\' calls');
			equal(cancelCallCount, 1, 'Number of `cancel\' calls');
		});

		next();
	}

	testOrder.push(
		loginTests1,
		loginTests2,
		ssoLoginTests,
		logoutTests1,
		logoutTests2,
		authenticationRequired,
		authenticationRequired2
	);

}());
