/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, console: true, window: true */
(function () {
	'use strict';

	var GLOBAL_IDS = window.GLOBAL_IDS;

	var ignoreErrorCode;

	// bind an error handler (that will let the test fail)
	GCN.sub('error-encountered', function (error) {
		// dynamically ignore error codes
		if (error.code === ignoreErrorCode) {
			ok(true, 'error triggered ' + ignoreErrorCode);
			return;
		}
		console.warn(error.toString());
		ok(false, error.toString());
	});

	/**
	 * This is actually not a real test, but will just authenticate the GCN JS API
	 * so we don't need to re-authenticate for each and every test
	 */
	function testLogin(next) {
		var testName = 'Login Test';

		function proceed() {
			start();
			next();
		}

		function fail(error) {
			ok(false, error.toString());
			proceed();
		}

		module(testName);

		asyncTest(testName, function () {
			GCN.login('node', 'node', function (success) {
				ok(true, 'login successful');
				proceed();
			}, fail);
		});
	}

	function testLoadNode(next) {
		var testName = 'Load Node Test';

		function proceed() {
			start();
			next();
		}

		function fail(error) {
			ok(false, error.toString());
			proceed();
		}

		module(testName);

		asyncTest(testName, function () {
			GCN.node(1, function (node) {
				ok(true, 'Node loaded');
				proceed();
			}, fail);
		});
	}

	/**
	 * Loads some properties of the loaded node
	 */
	function testLoadNodeProperties(next) {
		var testName = 'Load Node Properties Test';

		function proceed() {
			start();
			next();
		}

		function fail(error) {
			ok(false, error.toString());
			proceed();
		}

		module(testName);

		asyncTest(testName, function () {
			GCN.node(1, function (node) {
				ok(true, 'Node loaded');
				equal(node.prop('defaultFileFolderId'),2,
					"The default folder for this node did not match the " + 
					"expected value");
				equal(node.prop('host'),"gcn5demo.gentics.com",
					"The hostname of the node did not match the expected value");
				proceed();
			}, fail);
		});
	}
	
	/**
	 * Save the node
	 */
	function testSaveNode(next) {
		var testName = 'Save Node Test';

		var errorConstructor = GCN.createError().constructor;

		function proceed(arg) {
			if (arg instanceof errorConstructor) {
				ok(false, arg.toString());
			}
			start();
			next();
		}

		module(testName);

		asyncTest(testName, function () {
			GCN.node(1, function (node) {
				ok(true, 'Node loaded');
				ignoreErrorCode = 'READONLY_ATTRIBUTE';
				node.prop('host',"www.somewhere.office");
				ignoreErrorCode = 'NOT_YET_IMPLEMENTED';
				node.save(proceed, function (error) {
					equal(error.code, 'NOT_YET_IMPLEMENTED');
					proceed();
				});
			}, proceed);
		});
	}

	/**
	 * Loads constructs for the given node
	 */
	function testLoadConstruct(next) {
		var testName = 'Load ConstructInformation Test';

		function proceed() {
			start();
			next();
		}

		function fail(error) {
			ok(false, error.toString());
			proceed();
		}

		module(testName);

		asyncTest(testName, function () {
			GCN.node(1, function (node) {
				node.constructs(function (constructs) {
					ok(true, 'Constructs loaded');
					proceed();
				}, fail);
			}, fail);
		});
	}

	/**
	 * Get a node's folder
	 */
	function testNodeFolder(next) {
		module('Getting a node\'s folder');

		function proceed() {
			start();
			next();
		}

		function fail(error) {
			ok(false, error.toString());
			proceed();
		}

		asyncTest('node(...).folder()', function () {
			GCN.node(1).folder(function (folder) {
				ok(folder instanceof GCN.FolderAPI,
					'folder instanceOf GCN.FolderAPI');

				equal(jQuery.type(folder._data.id), 'number',
					'Passed folder object should contain a `data\' object ' +
					'number in the `id\' property');

				proceed();
			}, fail);
		});
	}

	testOrder.push(
		testLogin,
		testLoadNode,
		testSaveNode,
		testLoadNodeProperties,
		testLoadConstruct,
		testNodeFolder
	);

}());
