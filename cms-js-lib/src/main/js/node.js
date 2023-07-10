/*global GCN: true */
(function (GCN) {
	'use strict';

	/**
	 * Maps constructcategories that were fetched via the Rest API into a
	 * sorted nested array of constructs.
	 *
	 * @param {object<string, object>} constructs
	 * @return {object<string, object>}
	 */
	function mapConstructCategories(constructs) {
		var constructKeyword;
		var categoryMap = {
			categories: {},
			categorySortorder: []
		};
		var constructCategoryArray = [];
		for (constructKeyword in constructs) {
			if (constructs.hasOwnProperty(constructKeyword)) {
				var construct = constructs[constructKeyword];
				var constructCategory = construct.category;
				var constructCategoryName, categorySortorder;

				// Use a custom name for constructs that have not been assigned
				// to a category.
				if (constructCategory == null) {
					constructCategoryName = 'GCN_UNCATEGORIZED';
					categorySortorder = -1;
				} else {
					constructCategoryName = constructCategory.name;
					categorySortorder = constructCategory.sortOrder;
				}

				// Initialize the inner array of constructs.
				if (!categoryMap.categories[constructCategoryName]) {
					var newCategory = {};
					newCategory.constructs = {};
					newCategory.sortorder = categorySortorder;
					newCategory.name = constructCategoryName;
					categoryMap.categories[constructCategoryName] = newCategory;
					constructCategoryArray.push(newCategory);
				}

				// Add the construct to the category.
				categoryMap.categories[constructCategoryName]
				           .constructs[constructKeyword] = construct;
			}
		}

		
		categories.sort(function (a, b) {
			
			return a.sortorder - b.sortorder;
		});

		// Add the sorted category names to the sortorder field
		for (var category of categories) {
			var category = categories[i];
			if (category.sortorder == null || category.sortorder === -1) {
				category.sortorder = defaultCounter;
				defaultCounter++;
			}
			defaultCounter = Math.max(category.sortorder, defaultCounter);
			map.categorySortorder.push(category.name);
		}

		var defaultCounter = 1;
		// Sort the categories by the sortorder.
		constructCategoryArray.sort(function (a, b) {
			defaultCounter = Math.max(a.sortorder || 0, b.sortorder || 0, defaultCounter);
			return a.sortorder - b.sortorder;
		});

		// Add the sorted category names to the sortorder field.
		for (var category of constructCategoryArray) {
			var category = constructCategoryArray[k];
			if (category.sortorder == null || category.sortorder === -1) {
				category.sortorder = defaultCounter;
				defaultCounter++;
			}
			categoryMap.categorySortorder.push(category.name);
		}

		return categoryMap;
	}

	/**
	 * Represents a Node
	 *
	 * @name NodeAPI
	 * @class
	 * @augments Chainback
	 * 
	 * @param {number|string}
	 *            id of the file to be loaded
	 * @param {function(ContentObjectAPI))=}
	 *            success Optional success callback that will receive this
	 *            object as its only argument.
	 * @param {function(GCNError):boolean=}
	 *            error Optional custom error handler.
	 * @param {object}
	 *            settings currently there are no additional settings to be used
	 */
	var NodeAPI = GCN.defineChainback({
		/** @lends NodeAPI */

		__chainbacktype__: 'NodeAPI',
		_extends: GCN.ContentObjectAPI,
		_type: 'node',

		_data: {
			folderId: null
		},

		/**
		 * @private
		 * @type {object<string, number} Constructs for this node are cached
		 *                               here so that we only need to fetch
		 *                               this once.
		 */
		_constructs: null,

		/**
		 * List of success and error callbacks that need to be called
		 * once the constructs are loaded
		 * @private
		 * @type {array.<object>}
		 */
		_constructLoadHandlers: null,

		/**
		 * @private
		 * @type {object<string, object} Constructs categories for this node.
		 *                               Cached here so that we only need to
		 *                               fetch this once.
		 */
		_constructCategories: null,

		/**
		 * Retrieves a list of constructs and constructs categories that are
		 * assigned to this node and passes it as the only argument into the
		 * the `success()' callback.
		 *
		 * @param {function(Array.<object>)=} success Callback to receive an
		 *                                            array of constructs.
		 * @param {function(GCNError):boolean=} error Custom error handler.
		 * @return undefined
		 * @throws INVALID_ARGUMENTS
		 */
		constructs: function (success, error) {
			if (!success) {
				return;
			}
			var node = this;
			if (node._constructs) {
				node._invoke(success, [node._constructs]);
				return;
			}

			// if someone else is already loading the constructs, just add the callbacks
			node._constructLoadHandlers = node._constructLoadHandlers || [];
			if (node._constructLoadHandlers.length > 0) {
				node._constructLoadHandlers.push({success: success, error: error});
				return;
			}

			// we are the first to load the constructs, register the callbacks and
			// trigger the ajax call
			node._constructLoadHandlers.push({success: success, error: error});
			node._read(function () {
				node._authAjax({
					url: GCN.settings.BACKEND_PATH +
					     '/rest/construct?embed=category&nodeId=' + node.id(),
					type: 'GET',
					error: function (xhr, status, msg) {
						var i;
						for (i = 0; i < node._constructLoadHandlers.length; i++) {
							GCN.handleHttpError(xhr, msg, node._constructLoadHandlers[i].error);
						}
					},
					success: function (response) {
						var i;
						if (GCN.getResponseCode(response) === 'OK') {
							node._constructs = GCN.mapConstructs(response.constructs);
							for (i = 0; i < node._constructLoadHandlers.length; i++) {
								node._invoke(node._constructLoadHandlers[i].success, [node._constructs]);
							}
						} else {
							for (i = 0; i < node._constructLoadHandlers.length; i++) {
								GCN.handleResponseError(response, node._constructLoadHandlers[i].error);
							}
						}
					},

					complete: function () {
						node._constructLoadHandlers = [];
					}
				});
			}, error);
		},

		/**
		 * Removes this node object.
		 *
		 * @ignore
		 * @param {function=} success Callback function to be invoked when
		 *                            this operation has completed
		 *                            successfully.
		 * @param {function(GCNError):boolean=} error Custom error handler.
		 */
		remove: function (success, error) {
			GCN.handleError(
				GCN.createError(
					'NOT_YET_IMPLEMENTED',
					'This method is not yet implemented',
					this
				),
				error
			);
		},

		/**
		 * Saves the locally modified changes back to the system.
		 * This is currently not yet implemented.
		 * 
		 * @ignore
		 * @param {function=} success Callback function to be invoked when
		 *                            this operation has completed
		 *                            successfully.
		 * @param {function(GCNError):boolean=} error Custom error handler.
		 */
		save: function (success, error) {
			GCN.handleError(
				GCN.createError(
					'NOT_YET_IMPLEMENTED',
					'This method is not yet implemented',
					this
				),
				error
			);
		},

		/**
		 * Retrieves the top-level folders of this node's root folder.
		 *
		 * @function
		 * @name folders
		 * @memberOf NodeAPI
		 * @param {function(FolderAPI)=} success
		 * @param {function(GCNError):boolean=} error Custom error handler.
		 */
		'!folders': function (success, error) {
			return this.folder(null, error).folders(success, error);
		},

		/**
		 * Helper method that will load the constructs of this node.
		 * @ignore
		 * @private
		 * @this {NodeAPI}
		 * @param {function(Array.<object>)} success callback
		 * @param {function(GCNError):boolean=} error callback
		 */
		constructCategories: function (success, error) {
			if (!success) {
				return;
			}
			var node = this;
			if (node._constructCategories) {
				node._invoke(success, [node._constructCategories]);
			} else {
				node._read(function () {
					node._data.id = node._chain._data.nodeId;
					node.constructs(function (constructs) {
						node._constructCategories =
								mapConstructCategories(constructs);
						node._invoke(success, [node._constructCategories]);
					}, error);
				}, error);
			}
		}
	});

	/**
	* Creates a new instance of NodeAPI. See the {@link NodeAPI} constructor for detailed information.
	* 
	* @function
	* @name node
	* @memberOf GCN
	* @see NodeAPI
	*/
	GCN.node = GCN.exposeAPI(NodeAPI);
	GCN.NodeAPI = NodeAPI;

}(GCN));
