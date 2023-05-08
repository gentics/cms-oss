/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, document: true, console: true */
(function () {

	'use strict';

	var GLOBAL_IDS = window.GLOBAL_IDS;


	// bind an error handler (that will let the test fail)
	GCN.sub('error-occured', function (error) {
		if (error.code === 'READONLY_ATTRIBUTE') {
			// part of testPageMeta
			ok(true, 'error triggered when trying to write page id');
			return;
		}

		console.warn(error.toString());
		ok(false, error.toString());
	});

	/**
	 * This is actually not a real test, but will just authenticate the GCN JS
	 * Lib so we don't need to re-authenticate for each and every test
	 */
	function testLogin(next) {
		module('Chainback Ajax Test');

		asyncTest('Chainback Ajax Test', function () {
			GCN.login('node', 'node', function (success) {
				ok(true, 'login successful');

				start();
				next();
			});
		});
	}

	/**
	  * Test that will invoke handleUploadResponse with fake response data
	  */
	function testFileUploadCallback(next) {
		var testName = 'Load Folder Test';
		module(testName);

		asyncTest(testName, function () {

			function dummyError(result, textStatus, jqXHR) {
				ok(false, "The parsing of the provided json file data was not correct");
			}

			function dummySuccess(file, messages) {
				ok(true, 'Success handler was called');
				start();
				next();
			}

			GCN.file(GLOBAL_IDS.FILE, function (file) {
				ok(true, 'Testfile loaded');

				var fakeResponse = {};
				fakeResponse.file = file._data;
				fakeResponse.messages = [];
				fakeResponse.responseInfo = {
					responseCode : "OK",
					responseMessage : "Successfully saved file 41"
				};

				GCN.folder(GLOBAL_IDS.FOLDER, function (folder) {
					ok(true, 'Folder loaded');
					folder.handleUploadResponse(fakeResponse, dummySuccess, dummyError);
				});


			});

		});
	}

	/**
	 * This test will test the file load api
	 */
	function testLoad(next) {
		module('loadfile');

		asyncTest('loadfile', function () {
			GCN.file(GLOBAL_IDS.FILE, function (file) {
				equal(file.prop('name'), 'Gentics_Content_Node_Technologie.pdf', 'file was loaded from the server');
				console.log(file.prop('name'));
				ok(file.prop('name') !== '', 'name is: ' + file.prop('name'));
				start();
				next();
			});
		});
	}

	/**
	 * test changing properties and saving the file
	 */
	function testSave(next) {
		module('savefile');

		asyncTest('savefile', function () {
			GCN.file(GLOBAL_IDS.FILE, function (file) {
				ok(true, 'file loaded. will change it\'s name now.');

				var oldName = file.prop('name');
				var newName = 'AnyArbitraryNameWillDo.file';

				file.prop('name', newName);

				file.save(function (file) {
					file.clear();

					ok(true, 'name change save to the server. cache should ' +
						'be cleared by now.');

					GCN.file(GLOBAL_IDS.FILE, function (file) {
						equal(file.prop('name'), newName,
							'file name was updated and save to the server correctly');

						file.prop('name', oldName);
						file.save();

						start();
						next();
					});
				});
			});
		});
	}

	/**
	 * test changing properties and saving the file
	 */
	function testBinURL(next) {
		module('binurl');

		asyncTest('binurl', function () {
			GCN.image(GLOBAL_IDS.IMAGE, function (image) {
				ok(true, 'file loaded. will append image tag to body now');
				jQuery('body').append('<img id="binurl" src="' + image.binURL() + '">');

				// give the image 3 seconds to load was using
				// jQuery('#binurl').load first, but this does not work in IE 7
				// and might not work in other browsers - see
				// http://api.jquery.com/load-event
				window.setTimeout(function () {
					ok(document.getElementById('binurl').complete,
						'image was loaded from the server successfully');
					jQuery('#binurl').remove(); // cleanup
					start();
					next();
				}, 3000);
			});
		});
	}

	testOrder.push(
		testLogin,
		testLoad,
		testFileUploadCallback,
		testBinURL,
		testSave
	);

}());
