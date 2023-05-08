/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, window: true */
(function () {

	'use strict';

	var ignoreErrorCode;
	var GLOBAL_IDS = window.GLOBAL_IDS;

	// bind an error handler (that will let the test fail)
	GCN.sub('error-encountered', function (error) {
		// dynamically ignore error codes
		if (error.code === ignoreErrorCode) {
			ok(true, 'error triggered ' + ignoreErrorCode);
			return;
		}

		ok(false, error.toString());
	});

	/**
	 * This is actually not a real test, but will just authenticate the GCN JS API
	 * so we don't need to re-authenticate for each and every test
	 */
	function testLogin(next) {
		module('Login');

		asyncTest('Login', function () {
			GCN.login('node', 'node', function (success) {
				ok(true, 'login successful');

				start();
				next();
			});
		});
	}

	/**
	 * This test will test the page preview pageapi method
	 */
	function testPreview(next) {
		module('pagepreview');

		asyncTest('pagepreview', function () {
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				GCN.page(page.localId()).preview(function (preview) {
					ok(preview, 'Preview was loaded');
					start();
					next();
				});
			});
		});
	}

	/**
	 * test creation, publishing and removal of a page
	 */
	function testCreatePublishRemove(next) {
		module('createPublishRemove');

		asyncTest('createPublishRemove', function () {
			GCN.folder(GLOBAL_IDS.FOLDER).createPage(4, { language: 'en' }, function (page) {
				ok(true, 'page created with id ' + page._data.id);

				equal(page.prop('templateId'), 4, 'correct template set for the page');
				equal(page.prop('language'), 'en', 'correct language set for the page');

				// test publish
				equal(page.prop('status'), 0, 'page status is modified');
				page.publish(function (page) {
					equal(page.prop('status'), 2, 'page status was modified to be published');

					// test unlock
					page.unlock(function (page) {
						ok(true, 'page unlocked');

						// test remove
						page.remove(function () {
							ok(true, 'page was removed successfully');

							equal(JSON.stringify(page._data), '{}',
								'page _data should be emptied');

							equal(JSON.stringify(page._shadow), '{}',
								'page _shadow should be emptied');

							start();
							next();
						});
					});
				});
			}, function (error) {
				start();
				next();
			});
		});
	}

	/**
	 * This will test the takeOffline method.
	 */
	function testTakeOffline(next) {
		module('Take Offline Test');

		asyncTest('Take Offline Test', function () {
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				page.takeOffline(function () {
					ok(true, "Page was taken offline");
					equal(page.prop('status'), '3', 'page status was not changed');
					start();
					next();
				}, function (error) {
					start();
					next();
				});
			});
	    });
	}

	/**
	 * Test access to meta attributes of a page
	 */
	function testPageProp(next) {
		module('pagemeta');

		asyncTest('pagemeta', function () {
			window.myPage = GCN.page(GLOBAL_IDS.PAGE, function (page) {
				equal(page.prop('id'), 60, 'page id was read correctly');
				equal(page.prop('id', 123), 60, 'page id is not writeable');

				var name = page.prop('name');
				equal(page.prop('name', 'newname'), 'newname', 'page name is writeable');

				start();
				next();
			});
		});
	}

	/**
	 * Tests whether saving tags and meta properties of a page work.
	 */
	function testSaving(next) {
		module('page.save()');

		asyncTest('save', function () {
			var tmpDiv = jQuery('<div id="tmp"></div>');
			jQuery('body').append(tmpDiv);
			GCN.page(GLOBAL_IDS.PAGE).clear();
			var tag = GCN.page(GLOBAL_IDS.PAGE).tag('content').edit('#tmp', function () {
				tmpDiv.append('<p class="gcnjsapi-delete-me">tagwasrendered</p>');
			});
			tag.save(function (tag) {
				tag.parent().clear();
				GCN.page(GLOBAL_IDS.PAGE).tag('content').edit('#tmp', function () {
					equal(
						tmpDiv.find('.gcnjsapi-delete-me').remove().length,
						1,
						'Expecting 1 .gcnjsapi-delete-me'
					);
				}).save(function () {
					// Visual cleanup...
					tmpDiv.remove();
					start();
					next();
				});
			});
		});
	}
	
	/**
	 * Tests that the prop method will verify the value according to the 
	 * predefined constraints.
	 */
	function testPropertyConstraints(next) {
		module('page:prop(name, value)');
		asyncTest('prop(name, value)', function () {
			var longText = "";
			for (var i = 0;i < 254; i++) {
				longText += "5";
			}
			
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				page.prop('name', longText);
				equal(page.prop('name'), longText, 
					'Page should be renamed with the given name.');
				ignoreErrorCode = 'ATTRIBUTE_CONSTRAINT_VIOLATION';
				page.prop('name', longText + "0");
				equal(page.prop('name'), longText, 'Page should not be renamed.');
				
				// try to trigger the ATTRIBUTE_CONSTRAINT_VIOLATION
				page.prop('name', longText + "0", function (error) {
					ok(true, "Error handler was invoked");
					equal(error.code, 'ATTRIBUTE_CONSTRAINT_VIOLATION', 
						"The expected constraint error was invoked"); 
				});
				
				
				// try to set a read only property
				ignoreErrorCode = 'READONLY_ATTRIBUTE';
				page.prop('edate', 1, function (error) {
					ok(true, "Error handler was invoked");
					equal(error.code, 'READONLY_ATTRIBUTE', 
						"The read only attribute error was invoked");
					
				});
				
				// try to read a property on an unfetched page 
				ignoreErrorCode = 'UNFETCHED_OBJECT_ACCESS';
				GCN.page(GLOBAL_IDS.PAGE2).prop('name', 'blub', function (error) {
					ok(true, "Error handler was invoked");
					equal(error.code, 'UNFETCHED_OBJECT_ACCESS', 
						"The unfetched attribute error was invoked");
				});
				start();
				next();
			});
		});
	}

	/**
	 * Tests that the save() method of content objects properly receives an optional settings
	 * object for a first parameter.
	 * Will pass an unlock setting during page save to see if the page really does get locked and
	 * unlocked in the backend. If it does then the save() method is behaving as expected.
	 */
	function testSaveWithSettings(next) {
		module('page:save(settings)');

		asyncTest('save(settings)', function () {
			GCN.page(GLOBAL_IDS.PAGE).clear();
			GCN.page(GLOBAL_IDS.PAGE).save({unlock: false}, function (page) {
				page.clear();
				GCN.page(GLOBAL_IDS.PAGE, function (page) {
					equal(page.prop('locked'), true, 'Page should be locked');

					page.save({unlock: true}, function (page) {
						page.clear();
						GCN.page(GLOBAL_IDS.PAGE, function (page) {
							equal(page.prop('locked'), false, 'Page should be unlocked');
							start();
							next();
						});
					});
				});
			});
		});
	}

	testOrder.push(
		testLogin,
		testPreview,
		testCreatePublishRemove,
		testTakeOffline,
		testSaving,
		testPropertyConstraints,
		testSaveWithSettings
	);

}());
