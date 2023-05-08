import { EventEmitter } from '@angular/core';
import * as _ from 'lodash';

export interface ObjectWithEvents<T extends object> {
    events$: EventEmitter<T> | undefined;
}

/**
 * Creates a sealed Proxy object from existing object with event emitting feature (supports also IE11 polyfill for Proxy)
 *
 * @param obj The object to be proxied
 * @param customHandler Custom handler for the proxy
 * @param eventEmitter An EventEmitter to emit on changes. This will be also returned as "events$" from the object.
 */
export function getSealedProxyObject<T extends {}, H extends {}>(obj: T, customHandler?: H, eventEmitter?: EventEmitter<T>): T & ObjectWithEvents<T> {

    const eventEmitter$ = eventEmitter;
    const handler = {
        get: (target, name) => {
            if (eventEmitter$ && name === 'events$') {
                return eventEmitter$;
            }

            return target[name];
        },
        set: (target, name, value) => {
            target[name] = value;

            if (eventEmitter$) {
                eventEmitter$.emit({ ...target });
            }

            return target[name] === value;
        }
    };

    /** Undefined "events$"" added for support Proxy polyfill */
    const sealedObject = Object.seal({ events$: undefined, ..._.cloneDeep(obj) });
    if (eventEmitter$) {
        eventEmitter$.emit(sealedObject);
    }

    return new Proxy(
        sealedObject,
        (customHandler || handler)
    );
}
