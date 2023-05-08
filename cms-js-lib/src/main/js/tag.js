/*global window: true, GCN: true, jQuery: true*/
(function (GCN) {

	'use strict';

	/**
	 * Searches for the an Aloha editable object of the given id.
	 *
	 * @TODO: Once Aloha.getEditableById() is patched to not cause an
	 *        JavaScript exception if the element for the given ID is not found
	 *        then we can deprecate this function and use Aloha's instead.
	 *
	 * @static
	 * @param {string} id Id of Aloha.Editable object to find.
	 * @return {Aloha.Editable=} The editable object, if wound; otherwise null.
	 */
	function getAlohaEditableById(id) {
		var Aloha = (typeof window !== 'undefined') && window.Aloha;
		if (!Aloha) {
			return null;
		}

		// If the element is a textarea then route to the editable div.
		var element = jQuery('#' + id);
		if (element.length &&
				element[0].nodeName.toLowerCase() === 'textarea') {
			id += '-aloha';
		}

		var editables = Aloha.editables;
		var j = editables.length;
		while (j) {
			if (editables[--j].getId() === id) {
				return editables[j];
			}
		}

		return null;
	}

	/**
	 * Helper function to normalize the arguments that can be passed to the
	 * `edit()' and `render()' methods.
	 *
	 * @private
	 * @static
	 * @param {arguments} args A list of arguments.
	 * @return {object} Object containing an the properties `element',
	 *                  `success', `error', `data' and `post'.
	 */
	function getRenderOptions(args) {
		var argv = Array.prototype.slice.call(args);
		var argc = args.length;
		var arg;
		var i;

		var element;
		var success;
		var error;
		var prerenderedData = false;
		var post = false;

		for (i = 0; i < argc; ++i) {
			arg = argv[i];

			switch (jQuery.type(arg)) {
			case 'string':
				element = jQuery(arg);
				break;
			case 'object':
				if (element) {
					prerenderedData = arg;
				} else {
					element = arg;
				}
				break;
			case 'function':
				if (success) {
					error = arg;
				} else {
					success = arg;
				}
				break;
			case 'boolean':
				post = arg;
				break;
			// Descarding all other types of arguments...
			}
		}

		return {
			element : element,
			success : success,
			error   : error,
			data    : prerenderedData,
			post    : post
		};
	}

	/**
	 * Exposes an API to operate on a Content.Node tag.
	 *
	 * @class
	 * @name TagAPI
	 */
	var TagAPI = GCN.defineChainback({

		__chainbacktype__: 'TagAPI',

		/**
		 * Type of the object
		 * 
		 * @type {string}
		 */
		_type: 'tag',

		/**
		 * A reference to the object in which this tag is contained.  This value
		 * is set during initialization.
		 *
		 * @type {GCN.ContentObject}
		 */
		_parent: null,

		/**
		 * Name of this tag.
		 *
		 * @type {string}
		 */
		_name: null,

		/**
		 * Gets this tag's information from the object that contains it.
		 *
		 * @param {function(TagAPI)} success Callback to be invoked when this
		 *                                   operation completes normally.
		 * @param {function(GCNError):boolean} error Custom error handler.
		 */
		'!_read': function (success, error) {
			var parent = this.parent();
			// Because tags always retrieve their data from a parent object,
			// this tag is only completely fetched if it's parent is also fetch.
			// The parent could have been cleared of all it's data using
			// _clearCache() while this tag was left in a _fetched state, so we
			// need to check.
			if (this._fetched && parent._fetched) {
				if (success) {
					this._invoke(success, [this]);
				}
				return;
			}

			// Because when loading folders via folder(1).folders() will
			// fetch them without any tag data.  We therefore have to refetch
			// them wit their tag data.
			if (parent._fetched && !parent._data.tags) {
				parent._data.tags = {};
				parent.fetch(function (response) {
					if (GCN.getResponseCode(response) !== 'OK') {
						GCN.handleResponseError(response);
						return;
					}
					var newTags = {};
					jQuery.each(
						response[parent._type].tags,
						function (name, data) {
							if (!GCN.TagContainerAPI.hasTagData(parent, name)) {
								newTags[name] = data;
							}
						}
					);
					GCN.TagContainerAPI.extendTags(parent, newTags);
					parent._read(success, error);
				});
				return;
			}

			var that = this;

			// Take the data for this tag from it's container.
			parent._read(function () {
				that._data = parent._getTagData(that._name);

				if (!that._data) {
					var err = GCN.createError('TAG_NOT_FOUND',
						'Could not find tag "' + that._name + '" in ' +
						parent._type + " " + parent._data.id, that);
					GCN.handleError(err, error);
					return;
				}

				that._fetched = true;

				if (success) {
					that._invoke(success, [that]);
				}
			}, error);
		},

		/**
		 * Retrieve the object in which this tag is contained.  It does so by
		 * getting this chainback's "chainlink ancestor" object.
		 *
		 * @function
		 * @name parent
		 * @memberOf TagAPI
		 * @return {GCN.AbstractTagContainer}
		 */
		'!parent': function () {
			return this._ancestor();
		},

		/**
		 * Initialize a tag object. Unlike other chainback objects, tags will
		 * always have a parent. If its parent have been loaded, we will
		 * immediately copy the this tag's data from the parent's `_data' object
		 * to the tag's `_data' object.
		 *
		 * @param {string|object}
		 *            settings
		 * @param {function(TagAPI)}
		 *            success Callback to be invoked when this operation
		 *            completes normally.
		 * @param {function(GCNError):boolean}
		 *            error Custom error handler.
		 */
		_init: function (settings, success, error) {
			if (jQuery.type(settings) === 'object') {
				this._name    = settings.name;
				this._data    = settings;
				this._data.id = settings.id;
				this._fetched = true;
			} else {
				// We don't want to reinitalize the data object when it
				// has not been fetched yet.
				if (!this._fetched) {
					this._data = {};
					this._data.id = this._name = settings;
				}
			}

			if (success) {
				var that = this;

				this._read(function (container) {
					that._read(success, error);
				}, error);

			// Even if not success callback is given, read this tag's data from
			// is container, if that container has the data available.
			// If we are initializing a placeholder tag object (in the process
			// of creating brand new tag, for example), then its parent
			// container will not have any data for this tag yet.  We know that
			// we are working with a placeholder tag if no `_data.id' or `_name'
			// property is set.
			} else if (!this._fetched && this._name &&
			           this.parent()._fetched) {
				this._data = this.parent()._getTagData(this._name);
				this._fetched = !!this._data;

			// We are propably initializing a placholder object, we will assign
			// it its own `_data' and `_fetched' properties so that it is not
			// accessing the prototype values.
			} else if (!this._fetched) {
				this._data = {};
				this._data.id = this._name = settings;
				this._fetched = false;
			}
		},

		/**
		 * Gets or sets a property of this tags. Note that tags do not have a
		 * `_shadow' object, and we update the `_data' object directly.
		 *
		 * @function
		 * @name prop
		 * @memberOf TagAPI
		 * @param {string}
		 *            name Name of tag part.
		 * @param {*=}
		 *            set Optional value. If provided, the tag part will be
		 *            replaced with this value.
		 * @return {*} The value of the accessed tag part.
		 * @throws UNFETCHED_OBJECT_ACCESS
		 */
		'!prop': function (name, value) {
			var parent = this.parent();

			if (!this._fetched) {
				GCN.error('UNFETCHED_OBJECT_ACCESS',
					'Calling method `prop()\' on an unfetched object: ' +
					parent._type + " " + parent._data.id, this);

				return;
			}

			if (jQuery.type(value) !== 'undefined') {
				this._data[name] = value;
				parent._update('tags.' + GCN.escapePropertyName(this.prop('name')),
					this._data);
			}

			return this._data[name];
		},

		/**
		 * <p>
		 * Gets or sets a part of a tag.
		 *
		 * <p>
		 * There exists different types of tag parts, and the possible value of
		 * each kind of tag part may differ.
		 *
		 * <p>
		 * Below is a list of possible kinds of tag parts, and references to
		 * what the possible range their values can take:
		 *
		 * <pre>
		 *      STRING : {@link TagParts.STRING}
		 *    RICHTEXT : {@link TagParts.RICHTEXT}
		 *     BOOLEAN : {@link TagParts.BOOLEAN}
		 *       IMAGE : {@link TagParts.IMAGE}
		 *        FILE : {@link TagParts.FILE}
		 *      FOLDER : {@link TagParts.FOLDER}
		 *        PAGE : {@link TagParts.PAGE}
		 *    OVERVIEW : {@link TagParts.OVERVIEW}
		 *     PAGETAG : {@link TagParts.PAGETAG}
		 * TEMPLATETAG : {@link TagParts.TEMPLATETAG}
		 *      SELECT : {@link TagParts.SELECT}
		 * MULTISELECT : {@link TagParts.MULTISELECT}
		 *        FORM : {@link TagParts.FORM}
		 * </pre>
		 *
		 * @function
		 * @name part
		 * @memberOf TagAPI
		 *
		 * @param {string} name Name of tag opart.
		 * @param {*=} value (optional)
		 *             If provided, the tag part will be update with this
		 *             value.  How this happens differs between different type
		 *             of tag parts.
		 * @return {*} The value of the accessed tag part.  Null if the part
		 *             does not exist.
		 * @throws UNFETCHED_OBJECT_ACCESS
		 */
		'!part': function (name, value) {
			if (!this._fetched) {
				var parent = this.parent();

				GCN.error(
					'UNFETCHED_OBJECT_ACCESS',
					'Calling method `prop()\' on an unfetched object: '
						+ parent._type + " " + parent._data.id,
					this
				);

				return null;
			}

			var part = this._data.properties[name];

			if (!part) {
				return null;
			}

			if (jQuery.type(value) === 'undefined') {
				return GCN.TagParts.get(part);
			}

			var partValue = GCN.TagParts.set(part, value);

			// Each time we perform a write operation on a tag, we will update
			// the tag in the tag container's `_shadow' object as well.
			this.parent()._update(
				'tags.' + GCN.escapePropertyName(this._name),
				this._data
			);

			return partValue;
		},

		/**
		 * Returns a list of all of this tag's parts.
		 *
		 * @function
		 * @memberOf TagAPI
		 * @name     parts
		 * @param    {string} name
		 * @return   {Array.<string>}
		 */
		'!parts': function (name) {
			var parts = [];
			jQuery.each(this._data.properties, function (key) {
				parts.push(key);
			});
			return parts;
		},

		/**
		 * Remove this tag from its containing object (it's parent).
		 *
		 * @function
		 * @memberOf TagAPI
		 * @name remove
		 * @param {function} callback A function that receive this tag's parent
		 *                            object as its only arguments.
		 */
		remove: function (success, error) {
			var parent = this.parent();

			if (!parent.hasOwnProperty('_deletedTags')) {
				parent._deletedTags = [];
			}

			GCN.pub('tag.before-deleted', {tag: this});

			parent._deletedTags.push(this._name);

			if (parent._data.tags &&
					parent._data.tags[this._name]) {
				delete parent._data.tags[this._name];
			}

			if (parent._shadow.tags &&
					parent._shadow.tags[this._name]) {
				delete parent._shadow.tags[this._name];
			}

			parent._removeAssociatedTagData(this._name);

			this._clearCache();

			if (success) {
				parent._persist(null, success, error);
			}
		},

		/**
		 * Given a DOM element, will generate a template which represents this
		 * tag as it would be if rendered in the element.
		 *
		 * @param {jQuery.<HTMLElement>} $element DOM element with which to
		 *                                        generate the template.
		 * @return {string} Template string.
		 */
		'!_makeTemplate': function ($element) {
			if (0 === $element.length) {
				return '<node ' + this._name + '>';
			}
			var placeholder =
					'-{(' + this.parent().id() + ':' + this._name + ')}-';
			var template = jQuery.trim(
					$element.clone().html(placeholder)[0].outerHTML
				);
			return template.replace(placeholder, '<node ' + this._name + '>');
		},

		/**
		 * Will render this tag in the given render `mode'.  If an element is
		 * provided, the content will be placed in that element.  If the `mode'
		 * is "edit", any rendered editables will be initialized for Aloha
		 * Editor.  Any editable that are rendered into an element will also be
		 * added to the tag's parent object's `_editables' array so that they
		 * can have their changed contents copied back into their corresponding
		 * tags during saving.
		 *
		 * @param {string} mode The rendering mode.  Valid values are "view",
		 *                      and "edit".
		 * @param {jQuery.<HTMLElement>} element DOM element into which the
		 *                                       the rendered content should be
		 *                                       placed.
		 * @param {function(string, TagAPI, object)} Optional success handler.
		 * @param {function(GCNError):boolean} Optional custom error handler.
		 * @param {boolean} post flag to POST the data for rendering
		 */
		'!_render': function (mode, $element, success, error, post) {
			var tag = this._fork();
			tag._read(function () {
				var template = ($element && $element.length)
				             ? tag._makeTemplate($element)
				             : '<node ' + tag._name + '>';

				var obj = tag.parent();
				var renderHandler = function (data) {
					// Because the parent content object needs to track any
					// blocks or editables that have been rendered in this tag.
					obj._processRenderedTags(data);

					GCN._handleContentRendered(data.content, tag,
						function (html) {
							if ($element && $element.length) {
								GCN.renderOnto($element, html);
								// Because 'content-inserted' is deprecated by
								// 'tag.inserted'.
								GCN.pub('content-inserted', [$element, html]);
								GCN.pub('tag.inserted', [$element, html]);
							}

							var frontendEditing = function (callback) {
								if ('edit' === mode) {
									// Because 'rendered-for-editing' is deprecated by
									// 'tag.rendered-for-editing'.
									GCN.pub('rendered-for-editing', {
										tag: tag,
										data: data,
										callback: callback
									});
									GCN.pub('tag.rendered-for-editing', {
										tag: tag,
										data: data,
										callback: callback
									});
								} else if (callback) {
									callback();
								}
							};

							// Because the caller of edit() my wish to do things
							// in addition to, or instead of, our frontend
							// initialization.
							if (success) {
								tag._invoke(
									success,
									[html, tag, data, frontendEditing]
								);
							} else {
								frontendEditing();
							}

							tag._merge();
						});
				};
				var errorHandler = function () {
					tag._merge();
				};

				if ('edit' === mode && obj._previewEditableTag) {
					obj._previewEditableTag(tag._name, renderHandler, errorHandler);
				} else {
					obj._renderTemplate(template, mode, renderHandler, errorHandler, post);
				}
			}, error);
		},

		/**
		 * <p>
		 * Render the tag based on its settings on the server. Can be called
		 * with the following arguments:<(p>
		 *
		 * <pre>
		 * // Render tag contents into div whose id is &quot;content-div&quot;
		 * render('#content-div') or render(jQuery('#content-div'))
		 * </pre>
		 *
		 * <pre>
		 * // Pass the html rendering of the tag in the given callback
		 * render(function(html, tag) {
		 *   // implementation!
		 * })
		 * </pre>
		 *
		 * Whenever a 2nd argument is provided, it will be taken as as custom
		 * error handler. Invoking render() without any arguments will yield no
		 * results.
		 *
		 * @function
		 * @name render
		 * @memberOf TagAPI
		 * @param {string|jQuery.HTMLElement}
		 *            selector jQuery selector or jQuery target element to be
		 *            used as render destination
		 * @param {function(string,
		 *            GCN.TagAPI)} success success function that will receive
		 *            the rendered html as well as the TagAPI object
		 * @param {boolean} post
		 *            True, when the tag shall be rendered by POSTing the data to
		 *            the REST API. Otherwise the tag is rendered with a GET call
		 */
		render: function () {
			var tag = this;
			var args = arguments;
			jQuery(function () {
				args = getRenderOptions(args);
				if (args.element || args.success) {
					tag._render(
						'view',
						args.element,
						args.success,
						args.error,
						args.post
					);
				}
			});
		},

		/**
		 * <p>
		 * Renders this tag for editing.
		 * </p>
		 *
		 * <p>
		 * Differs from the render() method in that it calls this tag to be
		 * rendered in "edit" mode via the REST API so that it is rendered with
		 * any additional content that is appropriate for when this tag is used
		 * in edit mode.
		 * </p>
		 *
		 * <p>
		 * The GCN JS API library will also start keeping track of various
		 * aspects of this tag and its rendered content.
		 * </p>
		 *
		 * <p>
		 * When a jQuery selector is passed to this method, the contents of the
		 * rendered tag will overwrite the element identified by that selector.
		 * All rendered blocks and editables will be automatically placed into
		 * the DOM and initialize for editing.
		 * </p>
		 *
		 * <p>
		 * The behavior is different when this method is called with a function
		 * as its first argument.  In this case the rendered contents of the tag
		 * will not be autmatically placed into the DOM, but will be passed onto
		 * the callback function as argmuments.  It is then up to the caller to
		 * place the content into the DOM and initialize all rendered blocks and
		 * editables appropriately.
		 * </p>
		 *
		 * @function
		 * @name edit
		 * @memberOf TagAPI
		 * @param {(string|jQuery.HTMLElement)=} element
		 *            The element into which this tag is to be rendered.
		 * @param {function(string,TagAPI)=} success
		 *            A function that will be called once the tag is rendered.
		 * @param {function(GCNError):boolean=} error
		 *            A custom error handler.
		 * @param {boolean} post
		 *            True, when the tag shall be rendered by POSTing the data to
		 *            the REST API. Otherwise the tag is rendered with a GET call
		 */
		edit: function () {
			var tag = this;
			var args = getRenderOptions(arguments);
			if (args.data) {

				// Because the parent content object needs to track any
				// blocks or editables that have been rendered in this tag.
				tag.parent()._processRenderedTags(args.data);

				// Because 'rendered-for-editing' is deprecated in favor of
				// 'tag.rendered-for-editing'
				GCN.pub('rendered-for-editing', {
					tag: tag,
					data: args.data,
					callback: function () {
						if (args.success) {
							tag._invoke(
								args.success,
								[args.content, tag, args.data]
							);
						}
					}
				});
				GCN.pub('tag.rendered-for-editing', {
					tag: tag,
					data: args.data,
					callback: function () {
						if (args.success) {
							tag._invoke(
								args.success,
								[args.content, tag, args.data]
							);
						}
					}
				});
			} else {
				jQuery(function () {
					if (args.element || args.success) {
						tag._render(
							'edit',
							args.element,
							args.success,
							args.error,
							args.post
						);
					}
				});
			}
		},

		/**
		 * Persists the changes to this tag on its container object. Will only
		 * save this one tag and not affect the container object itself.
		 * Important: be careful when dealing with editable contents - these
		 * will be reloaded from Aloha Editor editables when a page is saved
		 * and thus overwrite changes you made to an editable tag.
		 *
		 * @function
		 * @name save
		 * @memberOf TagAPI
		 * @param {object=} settings Optional settings to pass on to the ajax
		 *            function.
		 * @param {function(TagAPI)} success Callback to be invoked when this
		 *                                   operation completes normally.
		 * @param {function(GCNError):boolean} error Custom error handler.
		 */
		save: function (settings, success, error) {
			var tag = this;
			var parent = tag.parent();
			var type = parent._type;
			// to support the optional setting object as first argument we need
			// to shift the arguments when it is not an object
			if (jQuery.type(settings) !== 'object') {
				error = success;
				success = settings;
				settings = null;
			}
			var json = settings || {};
			// create a mockup object to be able to save only one tag
			// id is needed - REST API won't accept objects without id
			json[type] = { id: parent.id(), tags: {} };
			json[type].tags[tag._name] = tag._data;

			parent._authAjax({
				url   : GCN.settings.BACKEND_PATH + '/rest/' + type + '/save/'
				      + parent.id() + GCN._getChannelParameter(parent),
				type  : 'POST',
				error : error,
				json  : json,
				success : function onTagSaveSuccess(response) {
					if (GCN.getResponseCode(response) === 'OK') {
						tag._invoke(success, [tag]);
					} else {
						tag._die(GCN.getResponseCode(response));
						GCN.handleResponseError(response, error);
					}
				}
			});
		},

		/**
		 * TagAPI Objects are not cached themselves. Their _data object
		 * always references a tag in the _data of their parent, so that
		 * changes made to the TagAPI object will also change the tag in the
		 * _data of the parent.
		 * If the parent is reloaded and the _data refreshed, this would not
		 * clear or refresh the cache of the TagAPI objects. This would lead
		 * to a "broken" references and changes made to the cached TagAPI object
		 * would no longer change the tag in the parent.
		 *
		 * @private
		 * @return {Chainback} This Chainback.
		 */
		_addToCache: function () {
			return this;
		}
	});

	// Unlike content objects, tags do not have unique ids and so we uniquely I
	// dentify tags by their name, and their parent's id.
	TagAPI._needsChainedHash = true;

	GCN.tag = GCN.exposeAPI(TagAPI);
	GCN.TagAPI = TagAPI;

}(GCN));
