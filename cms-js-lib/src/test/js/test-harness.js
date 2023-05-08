/*global testOrder: true */

/**
 * A synchronous "test runner" that makes it possible to better control how
 * async tests are executed.
 */

(function () {
	'use strict';

	window.testOrder = [];

	window.GLOBAL_IDS = {
		// Aloha-Editor.de.html
		PAGE: 'A547.72692',
		// Scrum-Einfuehrung.de.html
		PAGE2: 'A547.72402',
		// Page Project-Wiki inside the Projects-Wiki folder
		PAGE3: 'A547.72418',

		// Folder: Projects-Wiki (Has two subfolders)
		FOLDER: 'A547.69462',

		// Folder /[Media]/[Files] (Has no subfolders)
		FOLDER2: 'A547.69880',

		// Node with id 3
		//NODE: 'A547.76375',            // @FIXME

		// GCNDemo Node
		NODE: 1,

		// GCNDemo Static Node
		NODE2: 2,

		// GCNDemo Channel Node
		NODE3: 3,

		// File Gentics_Content_Node_Technologie.pdf (41)
		FILE: 'A547.74274', // @FIXME

		// Image Hawaii.jpg (13)
		IMAGE: 'A547.69967',  // @FIXME

		// ID of a user
		USER: 3
	};

	function runNextTests() {
		if (testOrder.length) {
			testOrder.shift()(runNextTests);
		}
	}

	jQuery(runNextTests);
}());
