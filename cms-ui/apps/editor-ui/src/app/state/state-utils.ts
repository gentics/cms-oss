import { Action, State } from '@ngxs/store';
import { StateClass } from '@ngxs/store/internals';
import { StateOperator, StoreOptions } from '@ngxs/store/src/symbols';
import { Schema, normalize } from 'normalizr';
import {
    NormalizedResponse,
    Response as ResponseModel,
    fileSchema,
    folderSchema,
    formSchema,
    imageSchema,
    nodeSchema,
    pageSchema,
    templateSchema,
} from '../common/models';
import { AppState } from '../common/models/app-state';

export const NO_ACTION_CLASS_ERROR_MSG =
    'An AppState Action must have a static "type" property. Did you forget to apply the @ActionDeclaration decorator to your action class?';

/**
 * Used to declare a property, which has to be set to the class (i.e., the constructor) of a certain type.
 *
 * Example:
   ```
   // This needs to be set to a class, which implements the Resolve<BreadcrumbInfo> interface.
   breadcrumbResolver: ConstructorOf<Resolve<BreadcrumbInfo>>;
   ```
 */
export type ConstructorOf<T> = new(...args: any[]) => T;

export type RecursiveDiffOperations<T> = {
    [K in keyof T]?: T[K] | StateOperator<RecursiveDiffOperations<T[K]>>;
};

interface AppStateActionClass extends ConstructorOf<any>, Function {
    type: string;
}

/**
 * Extends the `StoreOptions` interface provided by ngxs with a restriction on the
 * `name` property to match one of the properties of the `AppState` interface.
 */
export interface AppStateStoreOptions<T> extends StoreOptions<T> {

    /** Name of the AppState branch. */
    name: keyof AppState;

}

/**
 * Decorates a class as an AppState branch in the ngxs store.
 */
// eslint-disable-next-line @typescript-eslint/naming-convention
export const AppStateBranch = <T>(options: AppStateStoreOptions<T>): (target: StateClass) => void => (target: StateClass) => {
    const origDecorator = State(options);
    origDecorator(target);
};

/**
 * Decorates a class as the declaration of an action for the ngxs store.
 *
 * This decorator adds the static `type` property required by ngxs, based on the `appStateBranch` and the class name.
 */
// eslint-disable-next-line @typescript-eslint/naming-convention
export const ActionDeclaration = (appStateBranch: keyof AppState): (target: ConstructorOf<any>) => void => (target: AppStateActionClass) => {
    if (!target.type) {
        const actionType = `[${appStateBranch}] ${target.name}`;
        target.type = actionType;
    }
};

/**
 * Decorates a method in an AppStateBranch as the one that executes the specified action.
 */
// eslint-disable-next-line @typescript-eslint/naming-convention
export function ActionDefinition(actionClass: ConstructorOf<any>): (target: any, methodKey: string, methodDescription: PropertyDescriptor) => void {
    if (typeof (actionClass as AppStateActionClass).type !== 'string') {
        throw new Error(NO_ACTION_CLASS_ERROR_MSG);
    }
    return (target: any, methodKey: string, methodDescriptor: PropertyDescriptor) => {
        const origDecorator = Action(actionClass as any);
        origDecorator(target, methodKey, methodDescriptor);
    };
}

/**
 * Concatenate two Lists/arrays without duplicates, maintaining array order.
 * Can be passed a hashing function or a comparator to handle objects by value.
 *
 * @example
 *   concatUnique([1, 2, 5, 7, 4], [2, 3, 9, 7])
 *   // result: [1, 2, 3, 5, 7, 9]
 *
 *   concatUnique([{id:5}, {id:7}], [{id:7}], obj => obj.id)
 *   // result: [{id: 5}, {id: 7}]
 */
export function concatUnique(left: number[], right: number[]): number[];
export function concatUnique(left: string[], right: string[]): string[];
export function concatUnique<T>(left: T[], right: T[], compareFn?: (a: T, b?: T) => boolean | string | number | object): T[];
export function concatUnique<T, H>(left: any, right: any, fn?: Function): T[] {
    const both: T[] = [].concat(left, right);

    let result: T[];

    if (!fn) {
        // compare by identity
        const resultSet = new Set<T>(both);
        result = Array.from(resultSet);
    } else if (fn.length === 1) {
        // fn is a hashing function
        const calcHash = fn as (o: T) => H;

        const resultMap = new Map<H, T>();
        for (const value of both) {
            const hash = calcHash(value);
            if (!resultMap.has(hash)) {
                resultMap.set(hash, value);
            }
        }

        result = Array.from(resultMap.values());
    } else if (fn.length === 2) {
        // fn is a comparator function
        const compare = fn as (a: T, b: T) => boolean;

        const resultArray: T[] = [];
        for (const r of both) {
            let alreadyIncluded = false;
            for (const l of resultArray) {
                if (compare(l, r) === true) {
                    alreadyIncluded = true;
                    break;
                }
            }
            if (!alreadyIncluded) {
                resultArray.push(r);
            }
        }
        result = resultArray;
    } else {
        throw new Error('Function parameter is not a hashing or comparator function');
    }

    return result.length !== left.length ? result : left;
}

/**
 * Removes entries from a List/array , maintaining array order.
 * Can be passed a hashing function or a comparator to handle objects by value.
 *
 * @example
 *   removeEntries([1, 2, 5, 7, 4], [2, 5])
 *   // result: [1, 7, 4]
 *
 *   removeEntries([{id:5}, {id:7}], [{id:7}], obj => obj.id)
 *   // result: [{id: 5}]
 */
export function removeEntries(haystack: number[], needle: number[]): number[];
export function removeEntries(haystack: string[], needle: string[]): string[];
export function removeEntries<T>(haystack: T[], needle: T[], compareFn?: (a: T, b?: T) => boolean | string | number | object): T[];
export function removeEntries<T, H>(left: T[], right: T[], fn?: Function): T[] {
    let result: T[];

    if (left === undefined || !left.length) {
        return [];
    }

    if (!fn) {
        // compare by identity
        const removeSet = new Set<T>(right);
        result = left.filter(el => !removeSet.has(el));
    } else if (fn.length === 1) {
        // fn is a hashing function
        const calcHash = fn as (o: T) => H;

        const deleteHashes = new Set<H>(right.map(del => calcHash(del)));
        result = left.filter(el => !deleteHashes.has(calcHash(el)));
    } else if (fn.length === 2) {
        // fn is a comparator function
        const compare = fn as (a: T, b: T) => boolean;

        result = left.filter(l => right.every(r => compare(l, r) === false));
    } else {
        throw new Error('Function parameter is not a hashing or comparator function');
    }

    return result.length !== left.length ? result : left;
}

/**
 * Given a IResponse object and an entity or collection of entities, returns
 * a new object with an appended `_normalized` property, which contains the
 * result of the normalizr operation.
 */
export function normalizeEntities(res: ResponseModel, collection: any, schema: any): NormalizedResponse {
    let normalized = normalize(collection, schema);
    // eslint-disable-next-line @typescript-eslint/naming-convention
    return Object.assign({}, res, { _normalized: normalized });
}

/**
 * Given an item type, returns the corresponding Normalizr schema.
 */
export function getNormalizrSchema(type: string, silent: boolean = false): Schema {
    switch (type) {
        case 'folder':
        case 'folders':
            return folderSchema;
        case 'form':
        case 'forms':
            return formSchema;
        case 'page':
        case 'pages':
            return pageSchema;
        case 'file':
        case 'files':
            return fileSchema;
        case 'image':
        case 'images':
            return imageSchema;
        case 'template':
        case 'templates':
            return templateSchema;
        case 'node':
        case 'nodes':
            return nodeSchema;
        default:
            if (silent) {
                return null;
            }
            throw new Error(`Type "${type}" was not recognized`);
    }
}

const REDUX_DEVTOOLS_PROD_ENABLED_KEY = 'GCMS_EDITOR_UI-state-redux-devtools-prod';
const ENABLED_VALUE = 'true';

/**
 * Checks if the AppState Redux Devtools plugin should be enabled also in a PROD build.
 */
export function checkStateReduxDevtoolsEnabledForProd(): boolean {
    const settingValue = localStorage.getItem(REDUX_DEVTOOLS_PROD_ENABLED_KEY);
    return settingValue === ENABLED_VALUE;
}

export function enableStateReduxDevtoolsForProd(): void {
    localStorage.setItem(REDUX_DEVTOOLS_PROD_ENABLED_KEY, ENABLED_VALUE);
}

export function disableStateReduxDevtoolsForProd(): void {
    localStorage.removeItem(REDUX_DEVTOOLS_PROD_ENABLED_KEY);
}
