/*global GCN: true, testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, console: true, window: true */
(function () {
	'use strict';

	var GLOBAL_IDS = window.GLOBAL_IDS;

	var ignoreErrorCode;
	var messageList = [];

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
		module(testName);

		asyncTest(testName, function () {
			GCN.login('node', 'node', function (success) {
				ok(true, 'login successful');

				start();
				next();
			});
		});
	}

	/*
	 * Send a test message to yourself
	 */
	function testSendMessages(next) {
		var testName = 'Send messages';
		module(testName);

		asyncTest(testName, function () {
			GCN.Message.send(
				{ users: window.GLOBAL_IDS.USER },
				'This is a test message',
				function (response) {
					// This function is called when the request was successful

					// Output the response message
					ok(true, response.responseInfo.responseCode + ':' +
							response.responseInfo.responseMessage);

					start();
					next();
				}
			);
		});
	}

	/*
	 * List all messages in the user's inbox
	 */
	function testListMessages(next) {
		var testName = 'List messages';
		module(testName);

		asyncTest(testName, function () {
			GCN.Message.list(
				true,
				function (messages) {
					// Iterate trough all messages in the inbox
					// and find out if our previously sent messages
					// is one of them.
					// Additionally we store all message ID's in an
					// array for later tests.
					var testMessageFound = false;
					for (var i = 0; i < messages.length; i++) {
						messageList.push(messages[i].id);

						if (messages[i].message === 'This is a test message') {
							testMessageFound = true;
						}
					}

					ok(testMessageFound, 'Previously sent test message found');

					var count = messageList.length;
					ok(count > 0, 'Listing ' + count + ' messages');

					start();
					next();
				}
			);
		});
	}

	/*
	 * Read all messages which aren't read in the inbox
	 */
	function testReadMessages(next) {
		var testName = 'Read all messages';
		module(testName);

		asyncTest(testName, function () {
			GCN.Message.read(
				messageList,
				function (messages) {
					ok(true, 'Request to set all messages read sent sucessfully');

					GCN.Message.list(
						true,
						function (messages) {
							ok(messages.length === 0, 'No unread messages in the inbox anymore');

							start();
							next();
						}
					);
				}
			);
		});
	}

	testOrder.push(
		testLogin,
		testSendMessages,
		testListMessages,
		testReadMessages
	);

}());
