(function (GCN) {

	'use strict';

	// One needs to be cautious when using channel-inherited ids to load content
	// objects.  The object data that is loaded varies depending on whether or
	// not you are in a channel, and whether or not a channel local copy of the
	// inherited object exists in a given channel.
	//
	// Consider working in a channel (with a node id 2 for example).  Further
	// consider that this channel inherits a page with the id 123, and that at
	// some point in time a channel-local copy of this page is made with the id
	// 456.  Now consider the following statements:
	//
	// GCN.channel(2);
	// var p1 = GCN.page(123, function () {});
	// var p2 = GCN.page(456, function () {});
	// GCN.channel(false);
	// var p3 = GCN.page(123, function () {});
	// var p4 = GCN.page(456, function () {});
	//
	// The fact that page 456 is a channel-local copy of page 123 in the
	// channel 2, means that p1, and p2 will be two difference objects on
	// the client which reference the exact same object on the backend.
	//
	// If client changes were made to p1 and p2, and both objects are saved(),
	// one set of changes will be overridden, with the last changes to reach
	// the server clobbering those of the first.
	//
	// In this senario out library cache would contain the following entries:
	//
	// "PageAPI:0/123" = @clientobject1 => @serverobject1
	// "PageAPI:0/456" = @clientobject2 => @serverobject2
	// "PageAPI:2/123" = @clientobject3 => @serverobject2
	// "PageAPI:2/456" = @clientobject4 => @serverobject2
	//
	// What we should do in the future is have object p1, and p2 be references
	// to the same instance on the client side.  This would make it symmetrical
	// with the REST-API.  The challenge in the above scenario is that 2
	// requests will be made to the server for 2 different id, and not until
	// the requests complete will be know whether a channel-local copy was
	// returned.
	//
	// What we should do is that in _processResponse() we should
	// check if there exists any object matching any of the values of
	// getCacheKeyVariations(), and if so, we should through away the incoming
	// data and use the data that is previously loaded.  We would need to make
	// sure that _clearCache() does in fact clear references for a given
	// instance.
	//
	// Example:
	// !_processResponse': function (data) {
	//    var keys = GCN.cache.getCacheKeyVariations(this, data[this._type]);
	//    GCN.cache.addCacheReferences(keys, this);
	//    ...

	// === multichannelling ===================================================

	/**
	 * Constructs the nodeId query parameter for REST-API calls.
	 *
	 * @param {ContentObjectAPI} obj A content object instance.
	 * @param {string=} delimiter Optional delimiter character.
	 * @return {string} Query parameter string.
	 */
	function getChannelParameter(obj, delimiter) {
		if (false === obj._channel) {
			return '';
		}
		return (delimiter || '?') + 'nodeId=' + obj._channel;
	}

	/**
	 * Adds the given instance to its constructor's cache.
	 *
	 * @TODO: Should be a factory method GCN.Cache.addToConstructorCache()
	 * @param {ChainbackAPI} instance
	 * @param {function} ctor The constructor of `instance`.
	 */
	function addToConstructorCache(instance, ctor) {
		ctor.__gcncache__[instance.__gcnhash__] = instance;
	}

	/**
	 * Removes the given instance from its ancestor's temporary cache.
	 *
	 * @TODO: Should be a factory method GCN.Cache.removeFromAncestorCache()
	 * @param {ChainbackAPI} instance
	 * @param {ChainbackAPI} ancestor Ancestor object of `instance`.
	 */
	function removeFromAncestorCache(instance, ancestor) {
		var hash;
		var cache = ancestor.__gcntempcache__;
		for (hash in cache) {
			if (cache.hasOwnProperty(hash) && instance === cache[hash]) {
				delete cache[hash];
			}
		}
	}

	/**
	 * Initializes a placeholder content object by populating it with its
	 * fetched data, assigning it a hash, and tranferring it from its ancestor's
	 * cache into its constructor's cache.
	 *
	 * The localized object must mask the original object from which the
	 * channel-local copy was derived.  All further attempts to access the
	 * original object should now return the channel-local copy.  This is done
	 * by assigning the channel-local instance the identical id and overriding
	 * it in the cache.
	 *
	 * @param {Chainback} localized A placeholder for localized content object.
	 * @param {object} data The data of the localized object that was returned
	 *                      by the "localize/" REST-API call.
	 */
	function initialize(localized, data) {
		var original = localized.multichannelling.derivedFrom;

		// Are tags localizable? If they are not then this is not needed
		localized._name = data.name;

		localized._data = data;
		localized._fetched = true;
		localized._setHash(original.id());

		// Because the cache entry that contained `original` will be overwritten
		// with `localized`, the data in `original` is therefore stale.  Any
		// further attemtps to access an object using the id of `original` will
		// return `localized`.
		original.clear();

		removeFromAncestorCache(localized, original);
		addToConstructorCache(localized, localized._constructor);
	}

	/**
	 * Fetches the channel-local data represented by the given placeholder object.
	 *
	 * @param {Chainback} obj A placeholder for a localized content object.
	 * @param {function(ContentObjectAPI)} success A callback function to
	 *                                             receive the channel-local
	 *                                             data.
	 * @param {function(GCNError)=} error Optional custom error handler.
	 */
	function fetch(obj, success, error) {
		obj._authAjax({
			url: GCN.settings.BACKEND_PATH + '/rest/' + obj._type + '/load/'
			     + obj.id() + getChannelParameter(obj),
			data: obj.multichannelling.derivedFrom._loadParams(),
			error: error,
			success: function (response) {
				if (GCN.getResponseCode(response) !== 'OK') {
					GCN.handleResponseError(response, error);
				} else {
					success(response);
				}
			}
		});
	}

	/**
	 * Create a channel-local version of a content object represented by `obj`.
	 *
	 * @param {ContentObjectAPI} obj Place holder for a localized content
	 *                               object.
	 * @param {function(ContentObjectAPI)} success A function invoked the
	 *                                             object is successfully
	 *                                             localized.
	 * @param {function(gcnerror)=} error Optional custom error handler.
	 */
	function createLocalizedVersion(obj, success, error) {
		var derived = obj.multichannelling.derivedFrom;
		obj._authAjax({
			url: GCN.settings.BACKEND_PATH + '/rest/' + derived._type
			     + '/localize/' + derived.id(),
			type: 'POST',
			json: { channelId: derived._channel },
			error: error,
			success: function (response) {
				if (GCN.getResponseCode(response) !== 'OK') {
					GCN.handleResponseError(response, error);
				} else {
					success();
				}
			}
		});
	}

	/**
	 * Delete the localized version of a content object.
	 *
	 * @param {ContentObjectAPI} obj Content Object
	 * @param {function(ContentObjectAPI)} success a callback function when the
	 *                                             object is successfully
	 *                                             localized.
	 * @param {function(gcnerror)=} error optional custom error handler.
	 */
	function deleteLocalizedVersion(obj, success, error) {
		var derived = obj.multichannelling.derivedFrom;
		derived._authAjax({
			url: GCN.settings.BACKEND_PATH + '/rest/' + derived._type
			     + '/unlocalize/' + derived.id(),
			type: 'POST',
			json: { channelId: derived._channel },
			error: error,
			success: function (response) {
				if (GCN.getResponseCode(response) !== 'OK') {
					GCN.handleResponseError(response, error);
				} else {
					success();
				}
			}
		});
	}

	/**
	 * Fetches the contents of the object from which our placeholder content
	 * object is derived from.
	 *
	 * @TODO: rename to fetchOriginal()
	 *
	 * @param {ContentObjectAPI} obj A placeholder content object waiting for
	 *                               data.
	 * @param {function(Chainback)} success A callback function that will
	 *                                      receive the content object when it
	 *                                      has been successfully read from the
	 *                                      backend.
	 * @param {function(GCNError)=} error Optional custom error handler.
	 */
	function fetchDerivedObject(obj, success, error) {
		obj.multichannelling.derivedFrom._read(success, error);
	}

	/**
	 * Poplates a placeholder that represents an localized content object with
	 * with its data to such that it becomes a fully fetched object.
	 *
	 * Will first cause the inherited object from which this placeholder is
	 * derived to be localized.
	 *
	 * @TODO: If fetching the object that is to be localized results in an
	 *        object with a different id being returned, then the returned
	 *        object is the channel-local version of the object we wanted to
	 *        localize.  The object should not be re-localized therefore, rather
	 *        the channel-local copy should simply be returned.
	 *        See isLocalizedData() at bottom of file.
	 *
	 * @param {Chainback} obj A placholder content object what is waiting for
	 *                        data.
	 * @param {function(Chainback)} success A callback function that will
	 *                                      receive the content object when it
	 *                                      has been successfully read from the
	 *                                      backend.
	 * @param {function(GCNError)=} error Optional custom error handler.
	 */
	function localize(obj, success, error) {
		fetchDerivedObject(obj, function (derived) {
			if (!derived.prop('inherited')) {
				var err = GCN.createError(
					'CANNOT_LOCALIZE',
					'Cannot localize an object which is not inherited',
					obj
				);
				GCN.handleError(err, error);
				return;
			}
			createLocalizedVersion(obj, function () {
				fetch(obj, function (response) {
					initialize(obj, response[obj._type]);
					success(obj);
				}, error);
			}, error);
		}, error);
	}

	/**
	 * Poplates a placeholder that represents an inherited content object with
	 * with its data to such that it becomes a fully fetched object.
	 *
	 * Will first cause the local object from which this placeholder is derived
	 * to be deleted so that the inherited object data is re-exposed.
	 *
	 * @param {Chainback} obj A placholder content object what is waiting for
	 *                        data.
	 * @param {function(Chainback)} success A callback function that will
	 *                                      receive the content object when it
	 *                                      has been successfully read from the
	 *                                      backend.
	 * @param {function(GCNError)=} error Optional custom error handler.
	 */
	function unlocalize(obj, success, error) {
		fetchDerivedObject(obj, function (derived) {
			if (derived.prop('inherited')) {
				var err = GCN.createError(
					'CANNONT_UNLOCALIZE',
					'Cannot unlocalize an object that was not first localized',
					obj
				);
				GCN.handleError(err, error);
				return false;
			}
			deleteLocalizedVersion(obj, success, error);
		}, error);
	}

	GCN.multichannelling =  {
		localize: localize,
		unlocalize: unlocalize
	};

	// === caching ============================================================

	/**
	 * Determines whether the incoming data is that of a multi-channel
	 * localized copy.
	 *
	 * When loading content objects in multi-channel nodes, there exists the
	 * possibility that the object data that is returned does not match the id
	 * of the one being requested.  This is the case when the request is made
	 * with a channel specified, and the requested id is of an inherited page
	 * which has previously been localized.  In such situations, the response
	 * will contain the data for the local copy and not the original inherited
	 * data.
	 *
	 * @param {ContentObjectAPI} obj The content object whose data was
	 *                               requested.
	 * @param {object} data The content object data from the server response.
	 * @return {boolean} True if the data object contains data for the local
	 *                   copy of this content object.
	 */
	function isLocalizedData(obj, data) {
		return obj._channel && (data.id !== obj.id());
	}

	/**
	 * Generates a hash from the given parameters.
	 *
	 * @param {?string} prefix
	 * @param {string} type The Chainback object type.
	 * @param {number} channel A node id.
	 * @param {number} id An object id.
	 * @return {string} A hash key.
	 */
	function makeCacheHash(prefix, type, channel, id) {
		return (prefix ? prefix + '::' : '') + type + ':' + channel + '/' + id;
	}

	/**
	 * Generates a list of hash keys which should map to the given obj. 
	 *
	 * @param {Chainback} obj A content object instance.
	 * @param {object} data The object's data received from the REST-API.
	 * @param {Array.<string>} An array of hash keys.
	 */
	function getCacheKeyVariations(obj, data) {
		var ctor = obj._constructor;
		var channel = obj._channel;
		var idFromObj = obj.id();
		var idFromData = data.id;
		var type = ctor.__chainbacktype__;
		var prefix = (ctor._needsChainedHash && obj._chain)
		           ? obj._chain.__gcnhash__
		           : '';
		var keys = [];
		keys.push(makeCacheHash(prefix, type, channel, idFromObj));
		if (isLocalizedData(obj, data)) {
			keys.push(makeCacheHash(prefix, type, 0, idFromData));
			keys.push(makeCacheHash(prefix, type, channel, idFromData));
		}
		return keys;
	}

	/**
	 * Maps an obj into its constructor's cache against a list of hash keys.
	 *
	 * @param {Array.<string>} keys A set of hash keys.
	 * @param {Chainback} obj A chainback instance which the given keys should
	 *                        should be mapped to. 
	 */
	function addCacheReferences(keys, obj) {
		var cache = obj._constructor.__gcncache__;
		var i;
		for (i = 0; i < keys.length; i++) {
			cache[keys[i]] = obj;
		}
	}

	GCN.cache = {
		getKeyVariations: getCacheKeyVariations,
		addReferences: addCacheReferences
	};

}(GCN));
