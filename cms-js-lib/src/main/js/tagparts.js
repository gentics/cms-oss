/*global window: true, GCN: true, jQuery: true */

/**
 * IMPORTANT NOTE CONCERNING JSDOC-TOOLKIT WORKAROUNDS:
 *
 * It is necessary to define a local function and then write extra code to
 * expose it in the TagParts object rather than just doing it directy. Thi is
 * because of a bug in JsDoc-Toolkit prevents the function documentation from
 * being parsed correctly otherwise.
 *
 * function MULTISELECT(part, value) {
 *     return TagParts.SELECT(part, value);
 * }
 * TagParts.MULTISELECT = MULTISELECT;
 */

(function (GCN) {
	'use strict';

	/**
	 * Retrieves a copy of the properties of the select options which
	 * corresponds to the given value.
	 *
	 * @ignore
	 * @private
	 * @param {string} value Search for property of this value
	 * @param {object} part The part inwhich to search for select options.
	 * @return {object|null} A copy of properties or null if none is found for
	 *                       `value'.
	 * @throws VALUE_DOES_NOT_EXIST
	 */
	function getSelectPartOption(value, part) {
		var options = part.options;
		var i;
		for (i = 0; i < options.length; i++) {
			if (options[i].value === value) {
				return jQuery.extend({}, options[i]);
			}
		}
		GCN.error(
			'VALUE_DOES_NOT_EXIST',
			'The value `' + value + '\' does not exist in this part',
			part
		);
		return null;
	}

	/**
	 * Creates a basic getter/setter function for the given field.
	 *
	 * @private
	 * @ignore
	 * @param {string} field;
	 * @return {function(object, *=)} A getter/setter function.
	 */
	function createGetterSetter(field) {
		return function () {
			var args = Array.prototype.slice.call(arguments);
			var obj = args[0];
			if (args.length > 1) {
				obj[field] = args[1];
			}
			return obj[field];
		};
	}

	/**
	 * <p>
	 * Functions to access and modify various tag part types.
	 *
	 * <p>
	 * <b>IMPORTANT</b>: Getter and setters for the various part types should
	 * never be accessed directly.  If absolutely necessary it should be done
	 * via {@link TagParts.get} and {@link TagParts.set}.
	 *
	 * @public
	 * @namespace
	 * @name TagParts
	 * @type {object<string, function(object, (number|string|boolean|object)=)>}
	 */
	var TagParts = {};

	/**
	 * Gets or sets the value of a STRING tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name STRING
	 * @param {object} part The part whose value is to be accessed.
	 * @param {string=} value Optional. The value to set for this part.
	 * @return {string} The value held by this part.
	 */
	TagParts.STRING = createGetterSetter('stringValue');

	/**
	 * Gets or sets the value of a RICHTEXT tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name RICHTEXT
	 * @param {object} part The part whose value is to be accessed.
	 * @param {string=} value Optional. The value to set for this part.
	 * @return {string} The value held by this part.
	 */
	TagParts.RICHTEXT = createGetterSetter('stringValue');

	/**
	 * Gets or sets the value of a BOOLEAN tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name BOOLEAN
	 * @param {object} part The part whose value is to be accessed.
	 * @param {boolean=} value Optional. The value to set for this part.
	 * @return {boolean} The value held by this part.
	 */
	TagParts.BOOLEAN = createGetterSetter('booleanValue');

	/**
	 * Gets or sets the value of an IMAGE tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name IMAGE
	 * @param {object} part The part whose value is to be accessed.
	 * @param {number=} value Optional. The value to set for this part.
	 * @return {number} The value held by this part.
	 */
	TagParts.IMAGE = function (part, value) {
		if (jQuery.type(value) === 'object') {
			if (value.imageId) {
				part.imageId = value.imageId;
			} else {
				delete part.imageId;
			}
			if (value.nodeId) {
				part.nodeId = value.nodeId;
			} else {
				delete part.nodeId;
			}
			return value;
		}

		if (typeof value !== 'undefined' && value !== null) {
			part.imageId = value;
			delete part.nodeId;
			return value;
		}

		return part.imageId;
	};

	/**
	 * Gets or sets the value of a FILE tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name FILE
	 * @param {object} part The part whose value is to be accessed.
	 * @param {number=} value Optional. The value to set for this part.
	 * @return {number} The value held by this part.
	 */
	// (URL) file is the same as File (upload).
	TagParts.FILE = function (part, value) {
		if (jQuery.type(value) === 'object') {
			if (value.fileId) {
				part.fileId = value.fileId;
			} else {
				delete part.fileId;
			}
			if (value.nodeId) {
				part.nodeId = value.nodeId;
			} else {
				delete part.nodeId;
			}
			return value;
		}

		if (typeof value !== 'undefined' && value !== null) {
			part.fileId = value;
			delete part.nodeId;
			return value;
		}

		return part.fileId;
	};

	/**
	 * Gets or sets the value of a FOLDER tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name FOLDER
	 * @param {object} part The part whose value is to be accessed.
	 * @param {number=} value Optional. The value to set for this part.
	 * @return {number} The value held by this part.
	 */
	// (URL) folder is the same as Folder (upload).
	TagParts.FOLDER = function (part, value) {
		if (jQuery.type(value) === 'object') {
			if (value.folderId) {
				part.folderId = value.folderId;
			} else {
				delete part.folderId;
			}
			if (value.nodeId) {
				part.nodeId = value.nodeId;
			} else {
				delete part.nodeId;
			}
			return value;
		}

		if (typeof value !== 'undefined' && value !== null) {
			part.folderId = value;
			delete part.nodeId;
			return value;
		}

		return part.folderId;
	};

	/**
	 * Gets or sets the value of a NODE tag part.
	 * 
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name NODE
	 * @param {object} part The part whose value is to be accessed.
	 * @param {number=} value Optional. The value to set for this part.
	 * @return {number} The value held by this part.
	 */
	TagParts.NODE = createGetterSetter('nodeId');

	/**
	 * Gets or sets the value of an OVERVIEW tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name OVERVIEW
	 * @param {object} part The part whose value is to be accessed.
	 * @param {object=} value Optional. The value to set for this part.
	 * @return {object} The value held by this part.
	 */
	TagParts.OVERVIEW = createGetterSetter('overview');

	/**
	 * Gets or sets the value of a PAGE tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name PAGE
	 * @param {object} part The part whose value is to be accessed.
	 * @param {(number|string)=} value Optional.  A number denotes and internal
	 *                                 page within Content.Node, whereas a
	 *                                 string denotes an external url.
	 * @return {number|string} The value held by this part.
	 */
	function PAGE(part, value) {
		if (jQuery.type(value) === 'number') {
			part.pageId = value;
			delete part.stringValue;
			delete part.nodeId;
			return value;
		}

		if (jQuery.type(value) === 'string') {
			part.stringValue = value;
			delete part.pageId;
			delete part.nodeId;
			return value;
		}

		if (jQuery.type(value) === 'object') {
			if (value.pageId) {
				part.pageId = value.pageId;
			} else {
				delete part.pageId;
			}
			if (value.nodeId) {
				part.nodeId = value.nodeId;
			} else {
				delete part.nodeId;
			}
			delete part.stringValue;
			return value;
		}

		return part[
			jQuery.type(part.pageId) === 'number' && part.pageId !== 0 ? 'pageId' : 'stringValue'
		];
	}
	TagParts.PAGE = PAGE;

	/**
	 * <p>
	 * Gets or sets the value of a SELECT tag part.
	 *
	 * <p>
	 * There are several possible values that can be passed to this function:
	 *
	 * <pre>
	 *      undefined : When value arguments is not provided, then none of this
	 *                  part's data is changed.
	 *
	 *           null : selectedOptions will set to an empty array.
	 *
	 *         object : selectedOptions will be set to contain a single select
	 *                  option that corresponds with that of the
	 *                  `selectedOptions' property in the given object.  This
	 *                  allowance exists for backwards compatibility, and is not
	 *                  recommended.
	 *
	 *         string : selectedOptions will be set to contain a single select
	 *                  option whose `value' property corresponds with that of
	 *                  the argument.
	 *
	 *       string[] : selectedOptions will be set to contain zero or more
	 *                  select option whose `value' property corresponds with
	 *                  that of those in the given array.
	 * </pre>
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name SELECT
	 * @param {object} part The part whose value is to be accessed.
	 * @param {(string|string[]|object|null)=} value The values with which to
	 *                                         determined what this part's
	 *                                         `selectedOptions' property should
	 *                                         hold.
	 * @return {object} An object containing a copy of the value of this part.
	 * @throws VALUE_DOES_NOT_EXIST
	 */
	function SELECT(part, value) {
		var options = [];
		var option;
		var i;
		switch (jQuery.type(value)) {
		case 'string':
			option = getSelectPartOption(value, part);
			if (option) {
				options.push(option);
			} else {
				return part;
			}
			break;
		case 'array':
			for (i = 0; i < value.length; i++) {
				option = getSelectPartOption(value[i], part);
				if (option) {
					options.push(option);
				} else {
					return part;
				}
			}
			break;
		case 'object':
			for (i = 0; i < value.selectedOptions.length; i++) {
				option = getSelectPartOption(value.selectedOptions[i].value,
				                             part);
				if (option) {
					options.push(option);
				} else {
					return part;
				}
			}
			break;
		case 'undefined':
			options = part.selectedOptions;
			break;
		}

		part.selectedOptions = options;

		return {
			datasourceId: part.datasourceId,
			options: part.options,
			selectedOptions: part.selectedOptions
		};
	}
	TagParts.SELECT = SELECT;

	/**
	 * Gets or sets the value of a MULTISELECT tag part.
	 * Operates in the same was as {@link TagParts.SELECT}.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name MULTISELECT
	 * @param {object} part The part whose value is to be accessed.
	 * @param {(string|string[]|object|null)=} value The values with which to
	 *                                         determined what this part's
	 *                                         `selectedOptions' property should
	 *                                         hold.
	 * @return {object} An object containing a copy of the value of this part.
	 */
	function MULTISELECT(part, value) {
		return TagParts.SELECT(part, value);
	}
	TagParts.MULTISELECT = MULTISELECT;

	/**
	 * Gets or sets the value of a TEMPLATETAG tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name TEMPLATETAG
	 * @param {object} part The part whose value is to be accessed.
	 * @param {object=} value An object with the either the property `templateId'
	 *                        or `templateTagId'.
	 * @return {object} An object containing a copy of the value of this part.
	 *
	 */
	function TEMPLATETAG(part, value) {
		if (value) {
			if (typeof value.templateId !== 'undefined') {
				part.templateId = value.templateId;
			}

			if (typeof value.templateTagId !== 'undefined') {
				part.templateTagId = value.templateTagId;
			}
		}

		return {
			templateId: part.templateId,
			templateTagId: part.templateTagId
		};
	}
	TagParts.TEMPLATETAG = TEMPLATETAG;

	/**
	 * Gets or sets the value of a PAGETAG tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name PAGETAG
	 * @param {object} part The part whose value is to be accessed.
	 * @param {object=} value An object with the the property `pageId'
	 *                        and/or one of `contentTagId' or `templateTagId'.
	 * @return {object} An object containing a copy of the value of this
	 *                  part.
	 */
	function PAGETAG(part, value) {
		if (value) {
			if (typeof value.pageId !== 'undefined') {
				part.pageId = value.pageId;
			}

			var newContentTagIdValue;
			// support wrongly named 'pageTagId' property for backwards compatibility
			if (typeof value.pageTagId !== 'undefined') {
				newContentTagIdValue = value.pageTagId;
			} else if (typeof value.contentTagId !== 'undefined') {
				newContentTagIdValue = value.contentTagId;
			}

			// either a contenttag OR a templatetag must be specified
			if (typeof newContentTagIdValue !== 'undefined') {
				part.contentTagId = newContentTagIdValue;
				delete part.templateTagId;
			} else if (typeof value.templateTagId !== 'undefined') {
				part.templateTagId = value.templateTagId;
				delete part.contentTagId;
			}
		}

		var result = { pageId: part.pageId };

		if (typeof part.contentTagId !== 'undefined') {
			result.contentTagId = part.contentTagId;
		} else if (typeof part.templateTagId !== 'undefined') {
			result.templateTagId = part.templateTagId;
		}

		return result;
	}
	TagParts.PAGETAG = PAGETAG;

	/**
	 * Gets or sets the value of a LIST tag part.
	 * 
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name LIST
	 * @param {object} part The part whose value is to be accessed.
	 * @param {(string|string[]|object|boolean|null)=} value A string or string[] to set the listed values,
	 *                                                 or a boolean to set the 'ordered' flag or an object with the properties
	 *                                                 'booleanValue' and/or 'stringValues'
	 * @return {object} An object containing a copy of the value of this part.
	 */
	function LIST(part, value) {
		switch (jQuery.type(value)) {
		case 'array':
			part.stringValues = value;
			break;
		case 'string':
			part.stringValues = [value];
			break;
		case 'object':
			if (typeof value.stringValues !== 'undefined') {
				part.stringValues = value.stringValues;
			}
			if (typeof value.booleanValue !== 'undefined') {
				part.booleanValue = value.booleanValue;
			}
			break;
		case 'boolean':
			part.booleanValue = value;
			break;
		}

		var result = {
			booleanValue: part.booleanValue,
			stringValues: part.stringValues
		};
		return result;
	}
	TagParts.LIST = LIST;

	/**
	 * Gets or sets the value of a ORDEREDLIST tag part.
	 * 
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name ORDEREDLIST
	 * @param {object} part The part whose value is to be accessed.
	 * @param {object=} value A string or string[] to set the listed values
	 *                        or an object with the property 'stringValues'
	 * @return {object} An object containing a copy of the value of this part.
	 */
	function ORDEREDLIST(part, value) {
		switch (jQuery.type(value)) {
		case 'array':
			part.stringValues = value;
			break;
		case 'string':
			part.stringValues = [value];
			break;
		case 'object':
			if (typeof value.stringValues !== 'undefined') {
				part.stringValues = value.stringValues;
			}
			break;
		}

		var result = { stringValues: part.stringValues };
		return result;
	}
	TagParts.ORDEREDLIST = ORDEREDLIST;

	/**
	 * Gets or sets the value of a UNORDEREDLIST tag part.
	 * 
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name UNORDEREDLIST
	 * @param {object} part The part whose value is to be accessed.
	 * @param {object=} value A string or string[] to set the listed values
	 *                        or an object with the property 'stringValues'
	 * @return {object} An object containing a copy of the value of this part.
	 */
	function UNORDEREDLIST(part, value) {
		return TagParts.ORDEREDLIST(part, value);
	}
	TagParts.UNORDEREDLIST = UNORDEREDLIST;

	/**
	 * Gets or sets the value of a DATASOURCE tag part.
	 * 
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name DATASOURCE
	 * @param {object} part The part whose value is to be accessed.
	 * @param {object=} value An object containing the property 'options'
	 * @return {object} An object containing a copy of the value of this part.
	 */
	function DATASOURCE(part, value) {
		if (jQuery.type(value) === 'object') {
			part.options = value.options;
		}

		return { options: part.options };
	}
	TagParts.DATASOURCE = DATASOURCE;

	/**
	 * Gets or sets the value of a FORM tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name FORM
	 * @param {object} part The part whose value is to be accessed.
	 * @param {string=} value Optional. The value to set for this part.
	 * @return {string} The value held by this part.
	 */
	TagParts.FORM = createGetterSetter('stringValue');

	/**
	 * Gets or sets the value of a CMSFORM tag part.
	 *
	 * @public
	 * @function
	 * @memberOf TagParts
	 * @name CMSFORM
	 * @param {object} part The part whose value is to be accessed.
	 * @param {number=} value Optional. The value to set for this part.
	 * @return {number} The value held by this part.
	 */
	TagParts.CMSFORM = createGetterSetter('formId');

	/**
	 * Gets or sets the value of the given tag part.
	 *
	 * @private
	 * @ignore
	 * @param {object} part The part whose value is to be accessed.
	 * @param {*=} value The value to set the part to.
	 * @throws CANNOT_READ_TAG_PART
	 */
	function accessor() {
		var args = Array.prototype.slice.call(arguments);
		var part = args[0];
		if (!TagParts[part.type]) {
			GCN.error(
				'CANNOT_READ_TAG_PART',
				'Cannot read or write to tag part',
				part
			);
			return null;
		}
		return (
			args.length > 1
				? TagParts[part.type](part, args[1])
				: TagParts[part.type](part)
		);
	}

	/**
	 * Gets the value of the given tag part.
	 *
	 * @param {object} part The part whose value is to be retrieved.
	 * @param {*=} value The value to set the part to.
	 * @return {*} The value held in the given part.
	 * @throws CANNOT_READ_TAG_PART
	 */
	TagParts.get = function (part) {
		return accessor(part);
	};

	/**
	 * Sets the value of the given tag part.
	 *
	 * @param {object} part The part whose value is to be set.
	 * @param {*=} value The value to set the part to.
	 * @return {*} The value set to the given part.
	 * @throws CANNOT_READ_TAG_PART
	 */
	TagParts.set = function (part, value) {
		return accessor(part, value);
	};

	GCN.TagParts = TagParts;

}(GCN));
