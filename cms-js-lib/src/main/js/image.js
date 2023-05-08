(function (GCN) {

	'use strict';

	/**
	 * @TODO (petro): Where is this constant used.  Can it be removed?
	 *
	 * @const
	 * @type {number}
	 */
	var TYPE_ID = 10011;

	/**
	 * @private
	 * @const
	 * @type {object.<string, boolean>} Default image settings.
	 */
	var DEFAULT_SETTINGS = {
		// Load image for updating
		update: true
	};

	/**
	 * <strong>WARNING!</strong> The ImageAPI object is currently not implemented.
	 * 
	 * @class
	 * @name ImageAPI
	 * @extends FileAPI
	 * 
	 * @param {number|string}
	 *            id of the image to be loaded
	 * @param {function(ContentObjectAPI))=}
	 *            success Optional success callback that will receive this
	 *            object as its only argument.
	 * @param {function(GCNError):boolean=}
	 *            error Optional custom error handler.
	 * @param {object}
	 *            settings currently there are no additional settings to be used
	 */
	var ImageAPI = GCN.defineChainback({

		__chainbacktype__: 'ImageAPI',
		_extends: GCN.FileAPI,
		_type: 'image',

		/**
		 * writable properties for the page object
		 */
		WRITEABLE_PROPS: ['cdate',
		                  'description',
		                  'folderId', // @TODO Check if moving is implemented
		                              // correctly.
		                  'name' ],

		/**
		 * @see ContentObjectAPI.!_loadParams
		 */
		'!_loadParams': function () {
			return jQuery.extend(DEFAULT_SETTINGS, this._settings);
		}
	});

	/**
	* Creates a new instance of ImageAPI. See the {@link ImageAPI} constructor for detailed information.
	* 
	* @function
	* @name image
	* @memberOf GCN
	* @see ImageAPI
	*/
	GCN.image = GCN.exposeAPI(ImageAPI);
	GCN.ImageAPI = ImageAPI;

}(GCN));
