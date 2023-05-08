/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, window: true */
(function () {

	'use strict';

	var GLOBAL_IDS = window.GLOBAL_IDS;
	var onAuthReqCalls = 0;
	var onSuccessCalls = 0;
	var onErrorCalls = 0;

	GCN.sub('error-encountered', function (error) {
		ok(false, error.toString());
	});

	GCN.usingSSO = false;

	GCN.sub('authentication-required', function (proceed, cancel) {
		if (++onAuthReqCalls > 1) {
			equal(onAuthReqCalls, 1, '`authentication-required\' should only have ' +
				'been triggered once');
		}

		ok(GCN.isAuthenticating, 'GCN.isAuthenticating should be `true\'');

		GCN.login('node', 'node', function (success) {
			if (success) {
				proceed();

				ok(!GCN.isAuthenticating,
					'GCN.isAuthenticating should now be `false\'');
			} else {
				cancel();

				ok(!GCN.isAuthenticating,
					'GCN.isAuthenticating should now be `false\'');
			}
		});
	});


	// ============================================================================
	// Concurrent chainback tests
	// ============================================================================

	function concurrentCalls(next) {
		module('Concurrent chainbacks');

		asyncTest('Two concurrent chainback objects', function () {

			// GCN.page and GCN.node will both call login this is a problem.  They
			// the later call should wait until the current login has finished in
			// order that we only do one call

			GCN.page(65536, function () {
				++onSuccessCalls;
			}, function () {
				++onErrorCalls;
				return false;
			});

			GCN.node(65536, function () {
				++onSuccessCalls;
			}, function () {
				++onErrorCalls;
				return false;
			});

			window.setTimeout(function () {
				// If onAuthReqCall is > 1 then we have already done this test.
				if (onAuthReqCalls === 1) {
					equal(onAuthReqCalls, 1, '`authentication-required\' ' +
						'should only have been triggered once');
				}

				equal(onSuccessCalls + onErrorCalls, 2,
					'Two callbacks should have been invoked');

				start();
				next();
			}, 5000);

		});
	}

	function callInChain(next) {
		module('Ajax Calls in Chains');

		asyncTest('1 Ajax call in 1 chain', function () {
			GCN.folder(65536).page(65536, function () {}, function (error) {
				equal(error.code, 'NOTFOUND', 'Check for correct error code');
				return false;
			});

			GCN.page(GLOBAL_IDS.PAGE).tag('content', function (tag) {
				equal(tag._name, 'content', 'Tag has name "content"');

				equal(jQuery.type(tag._data), 'object',
					'Tag has a `_data\'object');

				ok(tag._fetched, 'Tag\'s `_fetched\' flag is set to `true\'');

				ok(tag.parent()._fetched, 'Tag\'s parent objects\'s `_fetched\' ' +
					'flag is also set to `true\'');

				start();
				next();
			});
		});
	}

	function multipleCallsInChain(next) {
		asyncTest('2 Ajax calls in 1 chain', function () {
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				equal(jQuery.type(page._data), 'object',
					'Page has a `_data\' object');

				equal(page && page._data && jQuery.type(page._data.folderId),
					'number', 'Page has a `folderId\' in the `_data\' object');
			}).tag('content', function (tag) {
				equal(tag._name, 'content', 'Tag has name "content"');

				equal(jQuery.type(tag._data), 'object',
					'Tag has a `_data\' object');

				start();
				next();
			});
		});
	}

	testOrder.push(
		concurrentCalls,
		callInChain,
		multipleCallsInChain
	);

}());
