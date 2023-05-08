/*global GCN: true */
(function (GCN) {
	'use strict';

	/**
	 * @class
	 * @name MessageAPI
	 * @extends ContentObjectAPI
	 */
	var MessageAPI = GCN.defineChainback({
		/** @lends message */

		__chainbacktype__: 'MessageAPI',
		_extends: GCN.ContentObjectAPI,
		_type: 'msg',

		/**
		 * Lists all message of the currently authenticated user.
		 * 
		 * @public
		 * @param {boolean}
		 *            Specifies wether to list unread messages only (true),
		 *            or to list all messages (false).
		 * @param {function(MessageAPI)=}
		 *            success Optional callback that will receive this object as
		 *            its only argument.
		 * @param {function(GCNError):boolean}
		 *            error Optional custom error handler.
		 */
		'!list': function (unreadOnly, success, error) {
			// Build the URL we will send to the RestAPI
			var url = GCN.settings.BACKEND_PATH + '/rest/' + this._type +
				'/list/?unread=' + (unreadOnly ? 'true' : 'false');

			var that = this;

			// Check that we are authenticated and send away
			// the RestAPI request
			this._authAjax({
				url     : url,
				type    : 'GET',
				error: function (xhr, status, msg) {
					GCN.handleHttpError(xhr, msg, error);
				},
				success: function (response) {
					if (GCN.getResponseCode(response) === 'OK') {
						that._invoke(success, [ response.messages ]);
					} else {
						GCN.handleResponseError(response, error);
					}
				}
			});
		},

		/**
		 * Marks one or multiple messages as read.
		 * 
		 * @public
		 * @param {array|number}
		 *            This either takes an array, which includes one
		 *            or multiple message ID's, or an integer with a
		 *            single ID.
		 * @param {function(MessageAPI)=}
		 *            success Optional callback that will receive the
		 *            response object.
		 * @param {function(GCNError):boolean}
		 *            error Optional custom error handler.
		 */
		'!read': function (messageIds, success, error) {
			if (messageIds === null ||
					(!(messageIds instanceof Array) && typeof messageIds !== 'number')) {
				GCN.handleError(
					GCN.error('Wrong parameter given',
							  '[Message.read] The first parameter should be an array or a number', this),
					error
				);
			}

			var ajaxMessageIds = [];

			// Whatever we just got: make an array out of it!
			if (messageIds instanceof Array) {
				ajaxMessageIds = messageIds;
			} else {
				ajaxMessageIds = [ messageIds ];
			}

			// Build the URL we will send to the RestAPI
			var url = GCN.settings.BACKEND_PATH + '/rest/' + this._type + '/read/';

			var that = this;

			// Check that we are authenticated and send away
			// the RestAPI request
			this._authAjax({
				url    : url,
				type   : 'POST',
				json   : { 'messages': ajaxMessageIds },
				error  : function (xhr, status, msg) {
					GCN.handleHttpError(xhr, msg, error);
				},
				success: function (response) {
					if (GCN.getResponseCode(response) === 'OK') {
						that._invoke(success, [ response ]);
					} else {
						GCN.handleResponseError(response, error);
					}
				}
			});
		},

		/**
		 * Sends a message to one or multiple users or groups.
		 * 
		 * @public
		 * @param {object}
		 *            An object containing a 'users' or/and a 'groups' value:
		 *            { users: [ userId1, ... ], groups: [ groupId2, ... ] }
		 *            Note: instead of arrays you can also just pass a single
		 *            integer for each of both.
		 * @param {function(MessageAPI)=}
		 *            success Optional callback that will receive the response
		 *            object.
		 * @param {function(GCNError):boolean}
		 *            error Optional custom error handler.
		 */
		'!send': function (receivers, message, success, error) {
			if (receivers === null || typeof receivers !== 'object') {
				GCN.handleError(
					GCN.error('Wrong parameter given',
							  '[Message.send] The first parameter should be an object', this),
					error
				);
			}

			// Define variables for the json object
			var
				toUserId  = [],
				toGroupId = [];

			// Whatever we just got: make an array out of it!
			if (typeof receivers.users !== 'undefined') {
				if (receivers.users instanceof Array) {
					toUserId = receivers.users;
				} else {
					toUserId = [ receivers.users ];
				}
			}
			
			// The same goes for the group-ID's
			if (typeof receivers.groups !== 'undefined') {
				if (receivers.groups instanceof Array) {
					toGroupId = receivers.groups;
				} else {
					toGroupId = [ receivers.groups ];
				}
			}

			var sendJsonObject = {
				'message'  : message,
				'toUserId' : toUserId,
				'toGroupId': toGroupId
			};

			// Build the URL we will send to the RestAPI
			var url = GCN.settings.BACKEND_PATH + '/rest/' + this._type + '/send/';

			var that = this;

			// Check that we are authenticated and do the rest call
			// to send our message away to the receiver(s).
			this._authAjax({
				url    : url,
				type   : 'POST',
				json   : sendJsonObject,
				error  : function (xhr, status, msg) {
					GCN.handleHttpError(xhr, msg, error);
				},
				success: function (response) {
					if (GCN.getResponseCode(response) === 'OK') {
						that._invoke(success, [ response ]);
					} else {
						GCN.handleResponseError(response, error);
					}
				}
			});
		}
	});

	/**
	* MessageAPI namespace. See the {@link MessageAPI} constructor for detailed information.
	* 
	* @function
	* @name Message
	* @memberOf GCN
	* @see MessageAPI
	*/
	GCN.Message = new MessageAPI();
}(GCN));
