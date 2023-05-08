/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, window: true */
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
	 * This test will load a folder and fetch the upload url for that folder.
	 */
	function testFolderUploadURL(next) {
		var testName = 'Load Folder Upload URL Test';
		module(testName);

		asyncTest(testName, function () {
			GCN.folder(GLOBAL_IDS.FOLDER, function (folder) {
				ok(true, 'Folder loaded');
				var uploadUrl = folder.uploadURL();
				ok(uploadUrl.indexOf("folderId=" + 6) !== -1, "The folder id " +
											   "should be set within the url");
				ok(uploadUrl.indexOf("createSimple.json") !== -1, "The rest " +
										"method should be set within the url");
				ok(uploadUrl.indexOf("sid=" + GCN.sid) !== -1, "The sid " +
													"should be set correctly");
				start();
				next();
			});
		});
	}

	/**
	 * This test will load a folder and fetch the multipart formdata upload 
	 * url for that folder.
	 */
	function testFolderMultipartUploadURL(next) {
		var testName = 'Load Folder Multipart Formdata Upload URL Test';
		module(testName);

		asyncTest(testName, function () {
			GCN.folder(GLOBAL_IDS.FOLDER, function (folder) {
				ok(true, 'Folder loaded');
				var uploadUrl = folder.multipartUploadURL();
				ok(uploadUrl.indexOf("create.json") !== -1, "The rest " +
										"method should be set within the url");
				ok(uploadUrl.indexOf("sid=" + GCN.sid) !== -1, "The sid " +
													"should be set correctly");
													
				uploadUrl = folder.multipartUploadURL(false,"customtype");
				ok(uploadUrl.indexOf("=customtype") !== -1, "The customtype " + 
													"argument should be set");
				ok(uploadUrl.indexOf("content-wrapper-filter=false") !== -1, 
							"The wrapper filter argument could not be found");

				start();
				next();
			});
		});
	}


	function testLoadFolder(next) {
		var testName = 'Load Folder Test';
		module(testName);

		asyncTest(testName, function () {
			GCN.folder(GLOBAL_IDS.FOLDER, function (folder) {
				ok(true, 'Folder loaded');
				start();
				next();
			});
		});
	}

	/**
	 * This test will check of a parentFolder is loaded 
	 */
	function testLoadFolderParent(next) {
		var testName = 'Load Folder Parent Test';
		module(testName);

		var success = function (parentFolder) {
			ok(folder.prop('motherId') === parentFolder.id(), 'Folder Parent loaded');
			start();
			next();
		};

		var error = function () {
			ok(false, 'Folder Parent could not be loadedloaded');
			start();
			next();
		}

		asyncTest(testName, function () {
			GCN.folder(GLOBAL_IDS.FOLDER, function (folder) {
				folder.parent(success, error);
			});
		});
	}

	/**
	 * This test will check whether loading permissions for a folder is working.
	 */
	function testPerm(next) {
	    var testName = 'Folder Permissions';
		module(testName);

		asyncTest(testName, function () {
			GCN.folder(GLOBAL_IDS.FOLDER, function (folder) {
				ok(folder.perm('viewfolder'), 'Correct permission loaded');
				ok(jQuery.type(folder.perm()) === 'array', 'Returned array ' +
								'of permissions if no perm name provided');
				start();
				next();
			});

		});

	}

	/**
	 * Test whether the folder can be deleted or not.
	 */
	function testFolderRemove(next) {
		var testName = 'Folder Remove Test';
		module(testName);

		asyncTest(testName, function () {
			// 1. Create the folder
			GCN.folder(GLOBAL_IDS.FOLDER).createFolder('folderToBeDeleted', function (folder) {
				var folderThatCanBeDeletedId = folder.id();

				// 2. Load the folder
				GCN.folder(folderThatCanBeDeletedId, function (folder) {
					ok(true, 'The folder that should be removed could be loaded');

					// 3. Delete the folder
					folder.remove(function () {
						ok(true, 'Folder was deleted');

						ignoreErrorCode = 'NOTFOUND';
						// Loading should fail now
						GCN.folder(folderThatCanBeDeletedId, function (folder) {
							ok(false, 'The folder should not be loaded');
							start();
							next();
						}, function (error) {
							ok(true, 'Folder with id ' + folderThatCanBeDeletedId +
									 ' could not be loaded. This is good since it should' +
									 ' have been deleted.');

							ignoreErrorCode = 'NOTFOUND';
							start();
							next();
						});

					}, function () {
						ok(false, 'Folder was not deleted');
						start();
						next();
					});
				});
			});
		});

	}

	/**
	 * Test whether the folder can be deleted the simple way or not.
	 */
	function testFolderRemoveSimple(next) {
		var testName = 'Folder Remove Simple Test';
		module(testName);

		asyncTest(testName, function () {
			// 1. Create the folder
			GCN.folder(GLOBAL_IDS.FOLDER).createFolder('folderToBeDeleted', function (folder) {
				var folderThatCanBeDeletedId = folder.id();

				// 2. Load and delete the folder
				GCN.folder(folderThatCanBeDeletedId).remove(function () {
					ok(true, 'Folder was deleted');

					ignoreErrorCode = 'NOTFOUND';

					// Loading should fail now
					GCN.folder(folderThatCanBeDeletedId, function (folder) {
						ok(false, 'The folder should not be loaded');

						start();
						next();
					}, function (error) {
						ok(true, 'Folder with id ' + folderThatCanBeDeletedId +
							 ' could not be loaded. This is good since it ' +
							 ' should have been deleted.');

						ignoreErrorCode = 'NOTFOUND';

						start();
						next();
					});

				}, function () {
					ok(false, 'Folder was not deleted');

					start();
					next();
				});
			});
		});

	}

	/**
	 * Test whether the folder remove action fails due to missing permissions
	 */
	function testFolderRemoveMissingPermissions(next) {
		var testName = 'Folder Remove Missing Permission Test';
		var folderThatCantBeDeletedId = 4;
		module(testName);

		asyncTest(testName, function () {

			GCN.folder(folderThatCantBeDeletedId, function (folder) {
				folder.remove();
				start();
				next();
			});

		});
	}


	/**
	 * Test access to meta attributes of a folder
	 */
	function testFolderProp(next) {
		module('folderprop');

		asyncTest('folderprop', function () {
			GCN.folder(GLOBAL_IDS.FOLDER, function (folder) {
				equal(folder.prop('id'), folder.id(), 'folder prop id should be identical with id()');
				var oldFolderId = folder.prop('id');
				ok(!isNaN(oldFolderId), 'folder prop id should be a number');
				equal(folder.prop('id', 5), oldFolderId, 'folder id is not writeable');
				var name = folder.prop('name');
				equal(folder.prop('name', 'newname'), 'newname', 'folder name is writeable');

				start();
				next();
			});
		});
	}

	/**
	 * test clear() and save() functionality for folders
	 */
	function testFolderSaveClear(next) {
		module('foldersaveclear');

		asyncTest('foldersaveclear', function () {
			GCN.folder(GLOBAL_IDS.FOLDER).createFolder('foldersaveclear', function (folder) {
				ok(true, 'folder created');

				// Change folder name so we've got something to be saved.
				// We need to generate a random name here to prevent attempting
				// to change the folder's name to a pre-existing folder, and
				// thus having it prefixed with a number.
				var newName = folder.prop('name') + '-' + Math.random().toString(32);
				folder.prop('name', newName);

				folder.save(function (folder) {
					var id = folder.id();

					folder.clear();

					equal(JSON.stringify(folder._data), '{"id":' + id + '}',
						'folder data cleared correctly');

					equal(JSON.stringify(folder._shadow), '{}',
						'folder shadow cleared correctly');

					equal(folder._fetched, false, 'fetch flag reset correctly');

					// and reload the folder from the server
					GCN.folder(folder._data.id, function (folder) {
						equal(folder.prop('name'), newName,
							'folder has been fetched again correctly');

						// finally remove the folder
						folder.remove(function () {
							ok(true, 'folder removed to complete the test');
							start();
							next();
						});
					}, function (error) {
						start();
						next();
					});
				});
			}, function (error) {
				start();
				next();
			});
		});
	}

	/**
	 * load all pages from the folder
	 * @param next
	 * @return
	 */
	function testFolderPages(next) {
		module('folderpages');

		asyncTest('folderpages', function () {
			GCN.folder(GLOBAL_IDS.FOLDER).pages(function (pages) {
				// @TODO maybe this test should be a bit more specific.
				equal(pages.length > 0, true, pages.length +
					' pages were fetched.');

				if (pages.length) {
					equal(pages[0].prop('type'), 'page',
						'folder contains pages');
				}

				start();
				next();
			});
		});
	}

	/**
	 * Checks chaining from folder
	 * @TODO Use global id for page.
	 */
	function testFolderChaining(next) {
		module('Chainging');

		var err = function (error) {
			ok(false, error.toString());
			start();
			next();
			return false;
		};

		asyncTest('GCN.folder(...).page(35...)', function () {
			GCN.folder(GLOBAL_IDS.FOLDER).page(35, function (page) {
				equal(page._data.id, 35, 'GCN.folder(...).page(...) worked');
				start();
				next();
			}, err);
		});

		asyncTest('GCN.folder(...).page(27...)', function () {
			GCN.folder(GLOBAL_IDS.FOLDER).page(27, function (page) {
				equal(page._data.id, 27, 'GCN.folder(...).folder(...) worked');
				start();
				next();
			}, err);
		});
	}

	/**
	 * load all pages from the folder
	 * @param next
	 * @return
	 */
	function testFolderFolders(next) {
		module('folderfolders');

		asyncTest('folderfolders', function () {
			GCN.folder(GLOBAL_IDS.FOLDER).folders(function (folders) {
				// TODO maybe this test should be a bit more specific
				equal(folders.length > 0, true, 'retrieved folders');

				if (folders.length) {
					equal(folders[0].prop('type'), 'folder',
						'folder contains folders');
				}

				start();
				next();
			});
		});
	}

	/**
	 * load all pages from the folder
	 * @param next
	 * @return
	 */
	function testFolderFiles(next) {
		module('folderfiles');

		asyncTest('folderfiles', function () {
			GCN.folder(17).files(function (files) {
				// TODO maybe this test should be a bit more specific
				equal(files.length > 0, true, 'retrieved pages');

				if (files.length) {
					equal(files[0].prop('type'), 'file',
						'folder contains files');
				}

				start();
				next();
			});
		});
	}

	/**
	 * load all images from the folder
	 * @param next
	 * @return
	 */
	function testFolderImages(next) {
		module('folderimages');

		asyncTest('folderimages', function () {
			GCN.folder(GLOBAL_IDS.FOLDER).images(function (images) {
				// TODO maybe this test should be a bit more specific
				equal(images.length > 0, true, 'retrieved images');

				if (images.length) {
					equal(images[0].prop('type'), 'image',
						'folder contains images');
				}

				start();
				next();
			});
		});
	}

	/**
	 * Folder tags are a little bit tricker in the GCN JS API.
	 *
	 * Make sure that when using the GCN.FolderAPI.folders() methods that tags
	 * will be provided when needed.
	 */
	function testFolderTags(next) {
		var reportError = function (error) {
			ok(false, error.toString());
			start();
			next();
			return false;
		};

		module('.folders()');

		asyncTest('.folders()', function () {
			GCN.folder(GLOBAL_IDS.FOLDER).folders(function (folders) {
				equal(
					folders[0]._data.tags,
					undefined,
					'Expecting the folders to have no tag information'
				);
				folders[0].tag('object.image', function (tag) {
					equal(
						tag.prop('name'),
						'object.image',
						'Expecting tag with name "object.image"'
					);
					start();
					next();
				}, reportError);
			}, reportError);
		});
	}


	testOrder.push(
		testLogin,
		testLoadFolder,
		testLoadFolderParent,
		testFolderUploadURL,
		testFolderMultipartUploadURL,
		testFolderChaining,
		testFolderImages,
		testFolderFiles,
		testFolderFolders,
		testFolderPages,
		testFolderRemove,
		testFolderRemoveSimple,
		testPerm,
		testFolderProp,
		testFolderSaveClear,
		testFolderTags
	);

}());
