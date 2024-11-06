export interface ObservedObjectChange {
    path: string[];
    value: any;
}

type ObserveCallback = (change: ObservedObjectChange) => void;

const OBSERVER_FN = Symbol('gtx-observe-fn');
const OBSERVER_ROOT = Symbol('gtx-observe-root');

export type ObservbedObject = {
    [OBSERVER_FN]: ObserveCallback;
}

export type ObservedSubObject = {
    [OBSERVER_ROOT]: ObservbedObject;
}

export function observeObjectChanges<T, R = T>(
    changeFn: ObserveCallback,
    refObj: R,
    observeObj: T,
): T & ObservbedObject;

export function observeObjectChanges<T, R = T>(
    changeFn: ObserveCallback,
    refObj: R,
    observeObj: T,
    currentPath: string[],
    rootObj: R & ObservbedObject,
): T & ObservedSubObject;

/**
 * Observes an object by creating a copy of the provided object and using getter/setters to
 * access/write to the original contents.
 * Updating a property will trigger a change, which will be notified by calling the `changeFn`.
 *
 * @param changeFn The function to call whenever a change occurs
 * @param refObj The object which will actually hold the data
 * @param observeObj The object under the `currentPath`, i.E. sub-property. For root element, same as `refObj`.
 * @param currentPath The path of where `observeObj` currently resides in.
 * @param rootObj The root observe-object, which has the reference to the callback function set.
 *
 * @returns A copy of the object which will notfy any changes to the `refObj` by calling `changeFn` after it has been applied.
 */
export function observeObjectChanges<T, R = T>(
    changeFn: ObserveCallback,
    refObj: R,
    observeObj: T,
    currentPath: string[] = [],
    rootObj: R & ObservbedObject = null,
): (T & ObservbedObject) | (T & ObservedSubObject) {
    // Can't observe non-objects, and also skip already observed objects
    // Arrays are also skipped, as we don't patch the modification functions such as `push`, `pop`, etc.
    if (observeObj == null || typeof observeObj !== 'object' || Array.isArray(observeObj)) {
        return observeObj as any;
    }

    // We only need to actually change the FN in the reference
    if (observeObj[OBSERVER_FN]) {
        observeObj[OBSERVER_FN] = changeFn;
        return observeObj as any;
    }

    const output = {} as ((T & ObservbedObject) | (T & ObservedSubObject));

    if (rootObj == null) {
        // If no root-object is provided, then we assume this is the root
        output[OBSERVER_FN] = changeFn;
        rootObj = output as any;
    } else {
        output[OBSERVER_ROOT] = rootObj;
    }

    Object.keys(observeObj).forEach(propertyName => {
        const propPath = [...currentPath, propertyName];

        Object.defineProperty(output, propertyName, {
            configurable: true,
            enumerable: true,
            get: () => {
                let value = refObj;
                for (const part of propPath) {
                    value = value?.[part];
                }

                if (value == null || typeof value !== 'object' || Array.isArray(value)) {
                    return value;
                }

                // Sub properties have to be converted to a observed object first, to get notified when something changes.
                return observeObjectChanges(changeFn, refObj, value, propPath, rootObj);
            },
            set: (value) => {
                let target = refObj;
                for (const part of currentPath) {
                    target = target?.[part];
                }
                // Help?
                if (target == null) {
                    return;
                }

                // New object is getting assigned
                if (value != null && typeof value === 'object') {
                    // Observe the object if it isn't observed yet
                    if (!value[OBSERVER_ROOT]) {
                        value = observeObjectChanges(changeFn, refObj, value, propPath, rootObj);
                    }
                }

                target[propertyName] = value;
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                rootObj[OBSERVER_FN]({
                    path: propPath,
                    value: value,
                });
            },
        });
    });

    return output;
}
