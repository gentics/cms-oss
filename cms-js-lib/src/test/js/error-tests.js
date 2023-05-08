/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, test: true, setTimeout: true */
(function () {

	'use strict';

	var NON_EXISTANT_PAGE_ID = 1045;
	var expected_error;
	var password = 'node';

	GCN.sub('error-encountered', function (error) {
		if (!error) {
			ok(false, 'No error object received');

			return;
		}

		switch (expected_error) {
		case 'NO_AUTH_1':
			equal(jQuery.type(error), 'object',
				'Error object received in error handler');

			equal(jQuery.type(error.code), 'string',
				'`error.code\' should be a string');

			equal(error.code, 'NO_AUTH_HANDLER', '"NO_AUTH_HANDLER" error ' +
				'should be received because no handler has yet been ' +
				'registered');
			return;
		case 'NO_AUTH_2':
			equal(error.code, 'NO_AUTH_HANDLER', '"NO_AUTH_HANDLER" error ' +
				'should be propagated');
			return;
		case 'AUTH_FAIL_1':
			equal(error.code, 'AUTHENTICATION_FAILED', 'Receive deliberate ' +
				'authentication error');
			return;
		case 'NOT_FOUND_1':
			equal(error.code, 'NOTFOUND', '"NOTFOUND" error should be ' +
				'propagated to global error handler');
			return;
		case 'NOT_FOUND_2':
			equal(error.code, 'NOTFOUND', 'There should be no more ' +
				'"NO_AUTH_HANDLER" errors anymore since a handler has been ' +
				'registered');
			return;
		default:
			ok(false, 'Whe should not be here');
			return;
		}
	});

	function noAuthHandler(next) {
		asyncTest('NO_AUTH_HANDLER', function () {
			// 1. Trigger `NO_AUTH_HANDLER' error.
			expected_error = 'NO_AUTH_1';
			GCN.page(NON_EXISTANT_PAGE_ID, function () {});

			// 2. Intercept error with custom error handler.  And stop
			// propagation.
			GCN.page(NON_EXISTANT_PAGE_ID, function () {}, function (error) {
				if (error) {
					equal(error.code, 'NO_AUTH_HANDLER', 'Custom error ' +
						'handler should receive "NO_AUTH_HANDLER" error code');
				} else {
					ok(false, 'Custom error handler should receive an error ' +
						'object');
				}

				// Allow a sequence point to allow this function to return
				// first.
				setTimeout(function () {
					// 3. Trigger `NO_AUTH_HANDLER' error that will be caught
					//    and propagated to global handler.
					GCN.page(NON_EXISTANT_PAGE_ID, function () {},
						function (error) {
							start();
							next();

							expected_error = 'NO_AUTH_2';
							return true;
						});
				}, 1);

				return false;
			});
		});
	}

	function registerAuthHandler(next) {
		// Register an authentication handler.
		// First time, cause an `AUTHENTICATION_FAILED' error.
		GCN.sub('authentication-required', function (proceed, cancel) {
			GCN.login('node', password, function (success) {
				if (success) {
					proceed();
				} else {
					cancel();
				}
			});
		});

		next();
	}

	function notFound(next) {
		asyncTest('NOT_FOUND', function () {
			// 4. Intercept `NOT_FOUND' error but let it bubble to the global
			//    handler.
			GCN.page(NON_EXISTANT_PAGE_ID, function () {}, function (error) {
				if (error) {
					equal(error.code, 'NOTFOUND', 'Custom error handler ' +
						'should receive "NOTFOUND" error code');
				}

				// Allow this function to return
				setTimeout(function () {
					// 5. Trigger `NOT_FOUND' error.
					expected_error = 'NOT_FOUND_2';

					GCN.page(NON_EXISTANT_PAGE_ID, function () {});

					start();
					next();
				}, 1);

				expected_error = 'NOT_FOUND_1';

				return true;
			});
		});
	}

	function authFailed(next) {
		GCN.clearSession();

		password = 'wrong';

		asyncTest('AUTHENTICATION_FAILED', function () {
			// 6. Trigger `AUTHENTICATION_FAILED' error.
			GCN.page(NON_EXISTANT_PAGE_ID, function () {}, function (error) {
				if (error) {
					equal(error.code, 'AUTHENTICATION_FAILED',
						'Catch "AUTHENTICATION_FAILED" error.');
				}

				// Allow this function to return first.
				setTimeout(function () {
					// 7. Trigger `AUTHENTICATION_FAILED' error.
					expected_error = 'AUTH_FAIL_1';

					GCN.page(NON_EXISTANT_PAGE_ID, function () {});

					setTimeout(function () {
						start();
						next();
					}, 500);
				}, 1);

				return false;
			});
		});
	}

	testOrder.push(
		noAuthHandler,
		registerAuthHandler,
		notFound,
		authFailed
	);

}());
