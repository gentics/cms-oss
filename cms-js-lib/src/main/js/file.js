(function (GCN) {

	'use strict';

	/**
	 * @private
	 * @const
	 * @type {number}
	 */
	var TYPE_ID = 10008;

	/**
	 * @private
	 * @const
	 * @type {object.<string, boolean>} Default file settings.
	 */
	var DEFAULT_SETTINGS = {
		// Load file for updating
		update: true
	};

	/**
	 * @class
	 * @name FileAPI
	 * @extends ContentObjectAPI
	 * @extends TagContainerAPI
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
	var FileAPI = GCN.defineChainback({
		/** @lends FileAPI */

		__chainbacktype__: 'FileAPI',
		_extends: [ GCN.ContentObjectAPI, GCN.TagContainerAPI ],
		_type: 'file',

		/**
		 * Writable properties for the folder object. Currently the following
		 * attributes are writeable: cdate, description, folderId, name.
		 * WARNING: changing the folderId might not work as expected.
		 * 
		 * @type {Array.string}
		 */
		WRITEABLE_PROPS: [ 'cdate',
		                    'description',
		                    'folderId', // @TODO Check if moving is implemented
							            // correctly.
		                    'name' ],

		/**
		 * <p>
		 * Retrieve the URL of this file's binary contents. You can use this to
		 * download the file from the server or to display an image.
		 * </p>
		 * <p>
		 * <b>WARNING!</b> Never ever store this URL in a page for users to
		 * load an image or to provide a download link. You should always refer
		 * to published files from the backend. Referencing a file directly
		 * using this link will put heavy load on you Gentics Content.Node CMS
		 * Server.
		 * </p>
		 * 
		 * @function
		 * @name binURL
		 * @memberOf FileAPI
		 * @return {string} Source URL of this file.
		 */
		'!binURL': function () {
			return (
				GCN.settings.BACKEND_PATH + '/rest/file/content/load/'
				+ this.id() + '?sid=' + GCN.sid
				+ GCN._getChannelParameter(this, '&')
			);
		},

		/**
		 * @see ContentObjectAPI.!_loadParams
		 */
		'!_loadParams': function () {
			return jQuery.extend(DEFAULT_SETTINGS, this._settings);
		}
	});

	/**
	* Creates a new instance of FileAPI. See the {@link FileAPI} constructor for detailed information.
	* 
	* @function
	* @name file
	* @memberOf GCN
	* @see FileAPI
	*/
	GCN.file = GCN.exposeAPI(FileAPI);
	GCN.FileAPI = FileAPI;

}(GCN));
