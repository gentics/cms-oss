/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, window: true */
(function () {

	'use strict';

	var GLOBAL_IDS = window.GLOBAL_IDS;

	var ignoreErrorCode;

	var testData = {
		// Since globalid's for folders are not supported (yet),
		// we specify the local id for nodes.
		// Check that this matches the ID's on your system.
		'nodes': {
			'master':  3,
			'derived': 4
		},

		'pages': {
			'master': {
				// foldernum - pagenum : globalid
				'1-1': '5D40.77245',
				'1-2': '5D40.77265',
				'2-1': '5D40.77259'
			},

			'derived': {
				// foldernum - pagenum : globalid
				'1-1': '5D40.77245', // master
				'1-2': '5D40.77280', // localized (from 1-2 / 5D40.77265)
				'1-3': '5D40.77285', // local
				'2-1': '5D40.77259'  // master
			}
		},

		'folders': {
			'master': {
				'1':  '5D40.77234',
				'2':  '5D40.77235',
			},

			'derived': {
				'1': '5D40.77234',
				'2': '5D40.77290'  // localized folder
			}
		},

		'files': {
			'master': {
				'1': '5D40.77291'
			},

			'derived': {
				'1': '5D40.77292' // derived from master (5D40.77291)
			}
		}
	};

	// bind an error handler (that will let the test fail)
	GCN.sub('error-encountered', function (error) {
		// dynamically ignore error codes
		if (error.code === ignoreErrorCode) {
			ok(true, 'error triggered ' + ignoreErrorCode);
			return;
		}

		// part of testPageMeta
		if (error.code === 'READONLY_ATTRIBUTE') {

			ok(true, 'error triggered when trying to write folder id');
			return;
		}

		ok(false, error.toString());
	});

	/**
	 * This is actually not a real test, but will just authenticate the GCN JS
	 * Lib so we don't need to re-authenticate for each and every test
	 */
	function testLogin(next) {
		module('Login Test');

		asyncTest('Login Test', function () {
			GCN.login('node', 'node', function (success) {
				ok(true, 'login successful');

				start();
				next();
			});
		});
	}

	/**
	 * This test will set a channel and try to load a folder on this channel.
	 * In The callback we set a new channel, but the folder object should be
	 * set to the channel like set before. Additionally GCN.page should load on the
	 * new channel set.
	 * 
	 */
	function testChainback(next) {
		var testName = 'Setting the channel and testing chainbacks for their correct channel usage';
		module(testName);

		asyncTest(testName, function () {

			// Set the channel
			GCN.channel(GLOBAL_IDS.NODE);

			GCN.folder(GLOBAL_IDS.FOLDER, function (folder) {
				// set a new channel
				GCN.channel(GLOBAL_IDS.NODE2);

				ok(folder._channel == GLOBAL_IDS.NODE, "The channel of the folder object is correct");

				// this should still use GLOBAL_IDS.NODE
				folder.page(GLOBAL_IDS.PAGE3, function (folder_page) {
					ok(folder_page._channel == GLOBAL_IDS.NODE, "The channel of the folder.page object is correct");

					// this should use GLOBAL_IDS.NODE2
					GCN.page(GLOBAL_IDS.PAGE3, function (gcn_page) {
						ok(gcn_page._channel == GLOBAL_IDS.NODE2, "The channel of the GCN.page object is correct");

						start();
						next();
					});
				});
			});
		});
	}


	/**
	 * This test will set a channel and try to load a folder on this channel.
	 * In The callback we set a new channel, but the folder object should be
	 * set to the channel like set before. Additionally GCN.page should load on the
	 * new channel set.
	 * 
	 */
	function testCacheHash(next) {
		var testName = 'Channel cache hash test';
		module(testName);

		asyncTest(testName, function () {

			// reset chanel
			GCN.channel(false);

			var page = GCN.page(GLOBAL_IDS.PAGE);

			var gcnHash_wanted = "PageAPI:" + 0 + "/" + GLOBAL_IDS.PAGE;
			ok(page.__gcnhash__ == gcnHash_wanted, "The page with channel " + GLOBAL_IDS.NODE + " has the gcnhash " + gcnHash_wanted);

			// Set the channel
			GCN.channel(GLOBAL_IDS.NODE);
			page = GCN.page(GLOBAL_IDS.PAGE);

			// Get the last object in the cache
			gcnHash_wanted = "PageAPI:" + GLOBAL_IDS.NODE + "/" + GLOBAL_IDS.PAGE;
			ok(page.__gcnhash__ == gcnHash_wanted, "The page with channel " + GLOBAL_IDS.NODE + " has the gcnhash " + gcnHash_wanted);

			start();
			next();
		});
	}

	/**
	 * Test for loading multichannelling pages
	 * 
	 */
	function testLoadPages(next) {
		var testName = 'Load pages test';
		module(testName);

		asyncTest(testName, function () {
			// Pages
			// Test loading a in a derived channel localized
			// page from the master and the derived node
			// from master
			GCN.channel(testData.nodes.master);

			GCN.page(testData.pages.master['1-2'], function (page) {
					ok(page.prop('name') == 'page1-2 master', "Loaded the page 'page1-2 master' from the master node");

					// from derived
					GCN.channel(testData.nodes.derived);
					GCN.page( testData.pages.derived['1-2'], function (page) {
						ok(page.prop('name') == 'page1-2 localized', "Loaded the page 'page1-2 localized' from the derived node");

						start();
						next();
					}, function (error) {
						ok(false, error.toString());
						start();
						next();
						return false;
					});
			}, function (error) {
				ok(false, error.toString());
				start();
				next();
				return false;
			});
		});
	}

	/**
	 * Test for loading multichannelling folders
	 * 
	 */
	function testLoadFolders(next) {
		var testName = 'Load folders test';
		module(testName);

		asyncTest(testName, function () {

			// Pages

			// Test loading a in a derived channel localized
			// folder from the master and the derived node

			// from master
			GCN.channel(testData.nodes.master);

			GCN.folder( testData.folders.master['2'], function (folder) {
				ok(folder.prop('name') == 'folder2', "Loaded the folder 'folder2' from the master node");

				// from derived
				GCN.channel(testData.nodes.derived);
				GCN.folder(testData.folders.derived['2'], function (folder) {
					ok(folder.prop('name') == 'folder2 localized', "Loaded the folder 'folder2 localized' from the derived node");

					start();
					next();
				}, function (error) {
					ok(false, error.toString());
					start();
					next();
				});
			}, function (error) {
				ok(false, error.toString());
				start();
				next();
			});
		});
	}

	/**
	 * Test for loading multichannelling files
	 * 
	 */
	function testLoadFiles(next) {
		var testName = 'Save pages test';
		module(testName);

		function proceed() {
			start();
			next();
		}

		function fail(error) {
			ok(false, error.toString());
			proceed();
		}

		asyncTest(testName, function () {

			// Files

			// Test loading a in a derived channel localized
			// file from the master and the derived node

			// from master
			GCN.channel(testData.nodes.master);

			GCN.file(
				testData.files.master['1'],
				function (object) {
					ok(
						object.prop('name') === 'testfile.txt',
						'Loaded the file "testfile.txt" from the master node'
					);

					// from derived
					GCN.channel(testData.nodes.derived);

					GCN.file(
						testData.files.derived['1'],
						function (object) {
							ok(
								object.prop('name') === 'testfile_derived.txt',
								'Loaded the file "testfile_derived.txt" from the derived node'
							);
							proceed();
						},
						fail
					);
				},
				fail
			);
		});
	}

	/**
	 * Test for saving localizedmultichannelling pages
	 * 
	 */
	function testSavePages(next) {
		var testName = 'Save a localized page with new description';
		module(testName);

		function proceed() {
			start();
			next();
		}

		function fail(error) {
			ok(false, error.toString());
			proceed();
		}

		asyncTest(testName, function () {

			// from derived
			GCN.channel(testData.nodes.derived);

			GCN.page(
				testData.pages.master['1-2'],
				function (object) {

					// Set the description to the current timestamp,
					// so we can compare it later reliable.
					var timestamp = Math.round((new Date()).getTime() / 1000);

					object.prop('description', timestamp.toString());

					// TODO: There is a bug with clear() that it takes away the data
					// the save() function needs, we will use a callback for now.
					object.save(function(object) {
						//object.clear();
						GCN.page(
							testData.pages.master['1-2'],
							function (object) {
								ok(
									object.prop('description') === timestamp,
									'The derived page was saved successfully with the changed data'
								);
								proceed();
							},
							fail
						);
					}, fail);
				},
				fail
			);
		});
	}

	testOrder.push(
		testLogin,
		testChainback,
		testCacheHash,

		testLoadPages,
		testLoadFolders,
		testLoadFiles,

		testSavePages
	);

}());
