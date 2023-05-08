(function (GCN) {

	'use strict';

	/**
	 * Enqueue a method into the given chainback objects's call chain. If a
	 * mutex is locking this chain, then place the call in the queued calls
	 * instead.
	 *
	 * @private
	 * @param {Chainback} chainback The object whose queue we want to push
	 *                                  the given method to.
	 * @param {function} method The method to chain.
	 */
	function addCallToQueue(chainback, method) {
		if (!chainback.__gcnmutex__) {
			chainback.__gcncallqueue__.push(method);
		} else {
			if ('__release__' === method.__gcncallname__) {
				return method.call(chainback);
			}
			chainback.__gcncallchain__.push(method);
			if (0 === chainback.__gcnajaxcount__
					&& 1 === chainback.__gcncallchain__.length) {
				method.call(chainback);
			}
		}
	}

	/**
	 * Dequeue the function at the top of the given chainback's call chain and
	 * invoke the next function in the queue.
	 *
	 * @private
	 * @param {Chainback} chainback
	 */
	function callNext(chainback) {
		// Waiting for an ajax call to complete.  Go away, and try again when
		// another call completes.
		if (chainback.__gcnajaxcount__ > 0) {
			return;
		}
		if (false === chainback.__gcnmutex__) {
			return;
		}
		if (0 === chainback.__gcncallchain__.length
				&& 0 === chainback.__gcncallqueue__.length) {
			return; // We should never reach here. Just so you know...
		}

		// Discard the empty shell...
		chainback.__gcncallchain__.shift();

		// Load and fire the next bullet...
		if (chainback.__gcncallchain__.length) {
			chainback.__gcncallchain__[0].call(chainback);
		} else if (chainback.__gcncallqueue__.length) {
			chainback.__gcncallqueue__.shift().call(chainback);
		}
	}

	/**
	 * Wraps the given method in a closure that provides scaffolding to chain
	 * invocations of the method correctly.
	 *
	 * @private
	 * @param {function} method The original function we want to wrap.
	 * @param {string} name The method name as it was defined in its object.
	 * @return {function} A function that wraps the original function.
	 */
	function makeMethodChainable(method, name) {
		return function () {
			var args = arguments;
			var that = this;
			var func = function () {
				method.apply(that, args);
				callNext(that);
			};
			func.__gcncallname__ = name; // For debugging
			addCallToQueue(this, func);
			return this;
		};
	}

	/**
	 * The Chainback constructor.
	 *
	 * Surfaces the chainback constructor in such a way as to be able to use
	 * call apply() on it.
	 *
	 * http://stackoverflow.com/questions/1606797/use-of-apply-with-new-operator-is-this-possible
	 *
	 * @private
	 * @param {Chainback} ctor The Chainback class we wish to initialize.
	 * @param {Array} args
	 * @param {object} continuation
	 * @return {Chainback}
	 */
	var Chainback = (function () {
		var Chainback = function (ctor, args) {
			return ctor.apply(this, args);
		};
		return function (ctor, args, continuation) {
			Chainback.prototype = ctor.prototype;
			return new Chainback(ctor, args);
		};
	}());

	/**
	 * Get a chainback instance's current channel.
	 *
	 * @param {Chainback} chainback
	 * @return {number} The channel id or 0 if no channel is set.
	 */
	function getChannel(chainback) {
		return chainback._channel || 0;
	}

	/**
	 * Get an instance of the given chainback class from its constructor's
	 * cache.  If the object for this hash does not exist in the cache, then
	 * instantiate a new object, place into the cache using the hash as the
	 * cache key.
	 *
	 * If no hash is passed to this function, then the Chainback instance that
	 * is returned will not be fully realized.  It will be a "fetus" instance
	 * that has yet to be bound to an id. Once it receives an id it will keep
	 * it for the remainder of its life.  These "fetus" instances are not be
	 * placed in the cache until they have aquired an id.
	 *
	 * @ignore
	 * @private
	 * @param {Chainback} ctor A chainback constructor.
	 * @param {string} hash A hash string that represents this chainback.
	 * @param {Chainback} callee The Chainback instance from which this
	 *                           invocation originated.
	 * @param {Array.<*>} args Arguments that will be applied to the chainback
	 *                         when (re-) initializing it.  This array should
	 *                         contain the following elements in the following
	 *                         order:
	 *                              id : string|array
	 *                         success : function|null
	 *                           error : function|null
	 *                         setting : object
	 * @return {Chainback}
	 */
	GCN.getChainback = function (ctor, hash, callee, args) {
		var chainback = hash && ctor.__gcncache__[hash];
		if (chainback) {
			// Reset the cached instance and re-initialize it.
			chainback._chain = callee;
			return chainback._init.apply(chainback, args);
		}
		args.push(callee);
		var isFetus = !hash;
		var hasCallee = !!callee;
		if (isFetus && hasCallee) {
			// TODO: Creating a hash form just the ctor argument is
			//       insufficient.  We must also consider
			//       ctor._needsChainedHash.  For example, createTag() will
			//       cause a new chanback to be created which will have a hash
			//       value of 'TagAPI:id' but it should be
			//       'Page::id::TagAPI:id'.
			hash = ctor._makeHash(getChannel(callee) + '/' + GCN.uniqueId(ctor.__chainbacktype__ + '-unique-'));
			chainback = callee.__gcntempcache__[hash];
			if (!chainback) {
				chainback =
					callee.__gcntempcache__[hash] =
						new Chainback(ctor, args);
			}
			return chainback;
		}
		return new Chainback(ctor, args);
	};

	/**
	 * @ignore
	 * Create a class which allows for chainable callback methods.
	 * @private
	 * @param {object<string, *>} props Definition of the class to be created.
	 *                                  All function are wrapped to allow them
	 *                                  to be as chainable callbacks unless
	 *                                  their name is prefixed with a "!" .
	 * @return {Chainback}
	 */
	GCN.defineChainback = function (props) {

		/**
		 * @ignore
		 * @TODO: use named arguments
		 *
		 * @constructor
		 * @param {number|string} id
		 * @param {?function(Chainback)} success
		 * @param {?function(GCNError):boolean} error
		 * @param {?object} chainlink
		 */
		var chainback = function () {
			var args = Array.prototype.slice.call(arguments);

			this._chain = args.pop();
			this._channel = this._chain ? this._chain._channel : GCN.channel();

			// Please note: We prefix and suffix these values with a double
			// underscore because they are not to be relied on whatsoever
			// outside of this file!  Although they need to be carried around
			// on chainback instances, they are nevertheless soley for internal
			// wiring.
			this.__gcnmutex__     = true;
			this.__gcncallchain__ = [];
			this.__gcncallqueue__ = [];
			this.__gcntempcache__ = {};

			// This is used to synchronize ajax calls with non-ajax calls in a
			// chainback call queue.
			//
			// It serves as a type of countdown latch, or reverse counting
			// semaphore (msdn.microsoft.com/en-us/magazine/cc163427.aspx).
			//
			// Upon each invocation of `_queueAjax()', on a chainback object,
			// its `__gcnajaxcount__' counter will be incremented.  And each
			// time a queued ajax call completes (successfully or otherwise),
			// the counter is decremented.  Before any chainback object can
			// move on to the next call in its call queue, it will check the
			// value of this counter to determine whether it is permitted to do
			// so.  If the counter's value is 0, access is granted;  otherwise
			// the requesting chainback will wait until a pending ajax call
			// completes to trigger a retry.
			this.__gcnajaxcount__ = 0;

			var obj = args[0];
			var ids;

			switch (jQuery.type(obj)) {
			case 'null':
			case 'undefined':
				break;
			case 'object':
				if (typeof obj.id !== 'undefined') {
					ids = [obj.id];
				}
				break;
			case 'array':
				ids = obj;
				break;
			default:
				ids = [obj];
			}

			// If one or more id is provided in the instantialization of this
			// object, only then will this instance be added to its class'
			// cache.
			if (ids) {
				this._setHash(ids.sort().join(','));
				this._addToCache();
			}

			this._init.apply(this, args);
		};

		/**
		 * Causes all promises that are made by this Chainback object, as well
		 * as all derived promises, to be resolved before the given success
		 * callback is invoked to receive this object in a "fulfilled" state.
		 * If a problem is encountered at any point during the resolution of a
		 * promise in the resolution chain, the error function is called, and
		 * further resolution is aborted.
		 *
		 * @private
		 * @param {function(Chainback)} success Callback function to be invoked
		 *                                      when all the promises on which
		 *                                      this object depends have been
		 *                                      completed.
		 * @param {function(GCNError):boolean=} error Optional custom error
		 *                                      handler.
		 */
		chainback.prototype._fulfill = function (success, error) {
			if (this._chain) {
				this._chain._fulfill(success, error);
			} else {
				success(this);
			}
		};

		// inheritance
		if (props._extends) {
			var inheritance = (jQuery.type(props._extends) === 'array')
			                ? props._extends
			                : [props._extends];
			var i;
			for (i = 0; i < inheritance.length; i++) {
				jQuery.extend(chainback.prototype, inheritance[i].prototype);
			}
			delete props._extends;
		}

		// static fields and methods
		jQuery.extend(chainback, {

			/**
			 * @private
			 * @static
			 * @type {object<string, Chainback>} An associative array holding
			 *                                   instances of this class.  Each
			 *                                   instance is mapped against a
			 *                                   hash key generated through
			 *                                   `_makehash()'.
			 */
			__gcncache__: {},

			/**
			 * @private
			 * @static
			 * @type {string} A string that represents this chainback's type.
			 *                It is used in generating hashs for instances of
			 *                this class.
			 */
			__chainbacktype__: props.__chainbacktype__ ||
				Math.random().toString(32),

			/**
			 * @private
			 * @static
			 * @type {boolean} Whether or not we need to use the hash of this
			 *                 object's parent chainback object in order to
			 *                 generate a unique hash key when instantiating
			 *                 objects for this class.
			 */
			_needsChainedHash: false,

			/**
			 * Given the arguments "one", "two", "three", will return something
			 * like: "one::two::ChainbackType:three".
			 *
			 * @private
			 * @static
			 * @param {...string} One or more strings to concatenate into the
			 *                    hash.
			 * @return {string} The hash string.
			 */
			_makeHash: function () {
				var ids = Array.prototype.slice.call(arguments);
				var id = ids.pop();
				ids.push(chainback.__chainbacktype__ + ':' + id);
				return ids.join('::');
			}
		});

		var DONT_MERGE = {
			__gcnmutex__     : true,
			__gcnorigin__    : true,
			__gcncallchain__ : true,
			__gcncallqueue__ : true,
			__gcnajaxcount__ : true,
			__gcntempcache__ : true
		};

		// Prototype chainback methods and properties

		jQuery.extend(chainback.prototype, {

			/**
			 * @type {Chainback} Each object holds a reference to its
			 *                   constructor.
			 */
			_constructor: chainback,

			/**
			 * Facilitates chaining from one chainback object to another.
			 *
			 * Uses "chainlink" objects to grow and internal linked list of
			 * chainback objects which make up a sort of callee chain.
			 *
			 * A link is created every time a context switch happens
			 * (ie: moving from one API to another).  Consider the following:
			 *
			 * page('1').tags().tag('content').render('#content');
			 *
			 * Accomplishing the above chain of execution will involve 3
			 * different chainable APIs, and 2 different API switches: a page
			 * API flows into a tags collection API, which in turn flows to a
			 * tag API.  This method is invoked each time that the exposed API
			 * mutates in this way.
			 *
			 * @private
			 * @param {Chainback} ctor The Chainback class we want to continue
			 *                         with.
			 * @param {number|string|Array.<number|string>|object} settings
			 *      If this argument is not defined, a random hash will be
			 *      generated as the object's hash.
			 *      An object can be provided instead of an id to directly
			 *      instantiate it from JSON data received from the server.
			 * @param {function} success
			 * @param {function} error
			 * @return {Chainback}
			 * @throws UNKNOWN_ARGUMENT If `settings' is not a number, string,
			 *                          array or object.
			 */
			_continue: function (ctor, settings, success, error) {
				// Is this a fully realized Chainback, or is it a Chainback
				// which has yet to determine which id it is bound to, from its
				// parent?
				var isFetus = false;
				var ids;
				var hashInputs = [];

				switch (jQuery.type(settings)) {
				case 'undefined':
				case 'null':
					isFetus = true;
					break;
				case 'array':
					ids = settings.sort().join(',');
					break;
				case 'number':
				case 'string':
					ids = settings;
					break;
				case 'object':
					ids = settings.id;
					break;
				default:
					GCN.error('UNKNOWN_ARGUMENT',
						'Don\'t know what to do with the object ' + settings);
					return;
				}

				var hash;
				if (isFetus) {
					hash = null;
				} else {
					var channel = getChannel(this);
					hash = ctor._needsChainedHash
					     ? ctor._makeHash(this.__gcnhash__, channel + '/' + ids)
					     : ctor._makeHash(channel + '/' + ids);
				}

				var chainback = GCN.getChainback(ctor, hash, this,
					[settings, success, error, {}]);

				return chainback;
			},

			/**
			 * Terminates any further exection of the functions that remain in
			 * the call queue.
			 *
			 * TODO: Kill all ajax calls.
			 *
			 * @private
			 * @return {Array.<function>} A list of functions that we in this
			 *                            Chainback's call queue when an abort
			 *                            happend.
			 */
			_abort: function () {
				this._clearCache();
				var callchain =
						this.__gcncallchain__.concat(this.__gcncallqueue__);
				this.__gcnmutex__ = true;
				this.__gcncallchain__ = [];
				this.__gcncallqueue__ = [];
				return callchain;
			},

			/**
			 * Gets the chainback from which this object was `_continue'd()
			 * from.
			 *
			 * @private
			 * @param {Chainback}
			 * @return {Chainback} This Chainback's ancestor.
			 */
			_ancestor: function () {
				return this._chain;
			},

			/**
			 * Locks the semaphore.
			 *
			 * @private
			 * @return {Chainback} This Chainback.
			 */
			_procure: function () {
				this.__gcnmutex__ = false;
				return this;
			},

			/**
			 * Unlocks the semaphore.
			 *
			 * @private
			 * @return {Chainback} This Chainback.
			 */
			_vacate: function () {
				this.__gcnmutex__ = true;
				this.__release__();
				return this;
			},

			/**
			 * Halts and forks the main call chain of this chainback object.
			 * Creates a derivitive object that will be used to accomplish
			 * operations that need to complete before the main chain is
			 * permitted to proceed.  Before execution on the main chainback
			 * object is restarted, the forked derivitive object is merged into
			 * the original chainback instance.
			 *
			 * @private
			 * @return {Chainback} A derivitive Chainback object forked from
			 *                     this Chainback instance.
			 */
			_fork: function () {
				var that = this;
				this._procure();
				var Fork = function ChainbackFork() {
					var prop;
					for (prop in that) {
						if (that.hasOwnProperty(prop) && !DONT_MERGE[prop]) {
							this[prop] = that[prop];
						}
					}
					this.__gcnorigin__    = that;
					this.__gcnmutex__     = true;
					this.__gcncallchain__ = [];
					this.__gcncallqueue__ = [];
				};
				Fork.prototype = new this._constructor();
				return new Fork();
			},

			/**
			 * Transfers the state of this derivitive into its origin.
			 *
			 * @private
			 * @param {Boolean} release Whether or not to vacate the lock on
			 *                          this fork's origin object after merging.
			 *                          Defaults to true.
			 */
			_merge: function (release) {
				if (!this.__gcnorigin__) {
					return;
				}
				var origin = this.__gcnorigin__;
				var prop;
				for (prop in this) {
					if (this.hasOwnProperty(prop) && !DONT_MERGE[prop]) {
						origin[prop] = this[prop];
					}
				}
				if (false !== release) {
					origin._vacate();
				}
				return origin;
			},

			/**
			 * Wraps jQuery's `ajax' method.  Queues the callbacks in the chain
			 * call so that they can be invoked synchonously.  Without blocking
			 * the browser thread.
			 * @TODO: The onError callbacks should always return a value.
			 *        'undefined' should be treated like false.
			 *
			 * @private
			 * @param {object} settings
			 */
			_queueAjax: function (settings) {
				if (settings.json) {
					settings.data = JSON.stringify(settings.json);
					delete settings.json;
				}

				settings.dataType = 'json';
				settings.contentType = 'application/json; charset=utf-8';
				settings.error = (function (onError) {
					return function (xhr, status, error) {
						// Check if the error message and the response headers
						// are empty. This means that the ajax request was aborted.
						// This usually happens when another page is loaded.
						// jQuery doesn't provide a meaningful error message in this case.
						if (!error
								&& typeof xhr === 'object'
								&& typeof xhr.getAllResponseHeaders === 'function'
								&& !xhr.getAllResponseHeaders()) {
							return;
						}

						var throwException = true;
						if (onError) {
							throwException = onError(GCN.createError('HTTP_ERROR', error, xhr));
						}
						if (throwException !== false) {
							GCN.error('AJAX_ERROR', error, xhr);
						}
					};
				}(settings.error));

				// Duck-type the complete callback, or add one if not provided.
				// We use complete to forward the continuation because it is
				// the last callback to be executed the jQuery ajax callback
				// sequence.
				settings.complete = (function (chainback, onComplete, opts) {
					return function () {
						--chainback.__gcnajaxcount__;
						onComplete.apply(chainback, arguments);
						callNext(chainback);
					};
				}(this, settings.complete || function () {}, settings));

				++this.__gcnajaxcount__;

				GCN.ajax(settings);
			},

			/**
			 * Clears the cache for this individual object.
			 *
			 * @private
			 * @return {Chainback} This Chainback.
			 */
			_clearCache: function () {
				if (chainback.__gcncache__[this.__gcnhash__]) {
					delete chainback.__gcncache__[this.__gcnhash__];
				}
				return this;
			},

			/**
			 * Add this object to the cache, using its hash as the key.
			 *
			 * @private
			 * @return {Chainback} This Chainback.
			 */
			_addToCache: function () {
				this._constructor.__gcncache__[this.__gcnhash__] = this;
				return this;
			},

			/**
			 * Removes the given chainback instance from the temporary cache,
			 * usually after the chainback instance has matured from a "fetus"
			 * into a fully realized chainback object.
			 *
			 * @param {Chainback} instance The chainback instance to remove.
			 * @return {boolean} True if this chainback instance was found and
			 *                   removed, false if it could not be found.
			 */
			_removeFromTempCache: function (instance) {
				var hash;
				var cache = instance._ancestor().__gcntempcache__;
				for (hash in cache) {
					if (cache.hasOwnProperty(hash) && instance === cache[hash]) {
						delete cache[hash];
						return true;
					}
				}
				return false;
			},

			/**
			 * @private
			 * @param {string|number} str
			 * @return {Chainback} This Chainback.
			 */
			_setHash: function (str) {
				var ctor = this._constructor;
				var addAncestorHash = ctor._needsChainedHash && this._chain;
				var channel = getChannel(this);
				var hash = addAncestorHash
				         ? ctor._makeHash(this._chain.__gcnhash__, channel + '/' + str)
				         : ctor._makeHash(channel + '/' + str);
				this.__gcnhash__ = hash;
				return this;
			},

			/**
			 * Invokes the given callback function while ensuring that any
			 * exceptions that occur during the invocation of the callback,
			 * will be caught and allow the Chainback object to complete its
			 * all remaining queued calls.
			 *
			 * @param {function} callback The function to invoke.
			 * @param {Array.<*>=} args A list of object that will be passed as
			 *                          arguments into the callback function.
			 */
			_invoke: function (callback, args) {
				if (typeof callback !== 'function') {
					return;
				}
				try {
					if (args && args.length) {
						callback.apply(null, args);
					} else {
						callback();
					}
				} catch (ex) {
					setTimeout(function () {
						throw ex;
					}, 1);
				}
			}

		});

		/**
		 * Causes the chainback call queue to start running again once a lock
		 * has been released.  This function is defined here because it needs
		 * to be made chainable.
		 *
		 * @private
		 */
		props.__release__ = function () {};

		var propName;
		var propValue;

		// Generates the chainable callback methods.  Transforms all functions
		// whose names do not start with the "!" character into chainable
		// callback prototype methods.
		for (propName in props) {
			if (props.hasOwnProperty(propName)) {
				propValue = props[propName];
				if (jQuery.type(propValue) === 'function' &&
						propName.charAt(0) !== '!') {
					chainback.prototype[propName] =
							makeMethodChainable(propValue, propName);
				} else {
					if (propName.charAt(0) === '!') {
						propName = propName.substring(1, propName.length);
					}
					chainback.prototype[propName] = propValue;
				}
			}
		}

		return chainback;
	};

}(GCN));
