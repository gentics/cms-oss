(function (GCN) {

	'use strict';

	/**
	 * <strong>WARNING!</strong> Currently the template API is still in
	 * development and has no specific implementation. Do not use the
	 * TemplateAPI as it is subject to change.
	 * 
	 * @class
	 * @name TemplateAPI
	 * @extends ContentObjectAPI
	 * @extends TagContainerAPI
	 * 
	 * @param {number|string}
	 *            id of the template to be loaded
	 * @param {function(ContentObjectAPI))=}
	 *            success Optional success callback that will receive this
	 *            object as its only argument.
	 * @param {function(GCNError):boolean=}
	 *            error Optional custom error handler.
	 * @param {object}
	 *            settings currently there are no additional settings to be used
	 */
	var TemplateAPI = GCN.defineChainback({
		/** @lends TemplateAPI */

		__chainbacktype__: 'TemplateAPI',
		_extends: [ GCN.TagContainerAPI, GCN.ContentObjectAPI ],
		_type: 'template',

		//---------------------------------------------------------------------
		// Surface tag container methods that are applicable for GCN page
		// objects.
		//---------------------------------------------------------------------

		/**
		 * Creates a tag of a given tagtype in this template.
		 *
		 * Exmaple:
		 * <pre>
		 *	createTag('link', 'http://www.gentics.com', onSuccess, onError);
		 * </pre>
		 * or
		 * <pre>
		 *	createTag('link', onSuccess, onError);
		 * </pre>
		 *
		 * @public
		 * @function
		 * @name createTag
		 * @memberOf TemplateAPI
		 * @param {string} construct The name of the construct on which the tag
		 *                           to be created should be derived from.
		 * @param {string=} magicValue Optional property that will override the
		 *                             default values of this tag type.
		 * @param {function(TagAPI)=} success Optional callback that will
		 *                                    receive the newly created tag as
		 *                                    its only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 * @return {TagAPI} The newly created tag.
		 * @throws INVALID_ARGUMENTS
		 */
		'!createTag': function () {
			return this._createTag.apply(this, arguments);
		},

		/**
		 * Deletes the specified tag from this template.
		 *
		 * @public
		 * @function
		 * @name removeTag
		 * @memberOf TemplateAPI
		 * @param {string} id The id of the tag to be deleted.
		 * @param {function(TemplateAPI)=} success Optional callback that
		 *                                         receive this object as its
		 *                                         only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		removeTag: function () {
			this._removeTag.apply(this, arguments);
		},

		/**
		 * Deletes a set of tags from this template.
		 *
		 * @public
		 * @function
		 * @name removeTags
		 * @memberOf TemplateAPI
		 * @param {Array.<string>} ids The ids of the set of tags to be
		 *                             deleted.
		 * @param {function(TemplateAPI)=} success Optional callback that
		 *                                         receive this object as its
		 *                                         only argument.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                            handler.
		 */
		removeTags: function () {
			this._removeTags.apply(this, arguments);
		},

		/**
		 * Not yet implemented.
		 * 
		 * @public
		 * @TODO: Not yet implemented.
		 */
		remove: function (success, error) {

		},

		/**
		 * Not yet implemented.
		 * 
		 * @public
		 * @TODO: Not yet implemented.
		 */
		save: function (success, error) {

		},

		/**
		 * Not yet implemented.
		 * 
		 * @public
		 * @TODO: Not yet implemented.
		 */
		folder: function (success, error) {

		}

	});

	/**
	* Creates a new instance of TemplateAPI. See the {@link TemplateAPI} constructor for detailed information.
	* 
	* @function
	* @name template
	* @memberOf GCN
	* @see TemplateAPI
	*/
	GCN.template = GCN.exposeAPI(TemplateAPI);
	GCN.TemplateAPI = TemplateAPI;

}(GCN));
