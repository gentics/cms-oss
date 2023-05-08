import { deepFreeze } from '@admin-ui/common/utils/deep-freeze/deep-freeze';
import { ConstructorOf } from '@admin-ui/common/utils/util-types/util-types';
import { Action, Select, State } from '@ngxs/store';
import { StateClass } from '@ngxs/store/internals';
import { StoreOptions } from '@ngxs/store/src/symbols';
import { AppState } from '../../app-state';

export const NO_ACTION_CLASS_ERROR_MSG =
    'An AppState Action must have a static "type" property. Did you forget to apply the @ActionDeclaration decorator to your action class?';

/**
 * Typed verion of the ngxs `@Select` decorator.
 * @param selector a function to select a part of the `AppState`
 */
export function SelectState<R>(selector: (state: AppState) => R): (target: any, name: string) => void {
    return (target: any, name: string) => {
        const origDecorator = Select(selector);
        origDecorator(target, name);
    };
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
export function AppStateBranch<T>(options: AppStateStoreOptions<T>): (target: StateClass) => void {
    return (target: StateClass) => {
        const origDecorator = State(options);
        origDecorator(target);
    };
}

/**
 * Used to define an initial version of a state and make it immutable.
 */
export function defineInitialState<T>(initialState: T): T {
    deepFreeze(initialState);
    return initialState;
}

interface AppStateActionClass extends ConstructorOf<any>, Function {
    type: string;
}

/**
 * Decorates a class as the declaration of an action for the ngxs store.
 *
 * This decorator adds the static `type` property required by ngxs, based on the `appStateBranch` and the class name.
 */
export function ActionDeclaration(appStateBranch: keyof AppState): (target: ConstructorOf<any>) => void {
    return (target: AppStateActionClass) => {
        if (!target.type) {
            const actionType = `[${appStateBranch}] ${target.name}`;
            target.type = actionType;
        }
    };
}

/**
 * Decorates a method in an AppStateBranch as the one that executes the specified action.
 */
export function ActionDefinition(actionClass: ConstructorOf<any>): (target: any, methodKey: string, methodDescription: PropertyDescriptor) => void {
    if (typeof (actionClass as AppStateActionClass).type !== 'string') {
        throw new Error(NO_ACTION_CLASS_ERROR_MSG);
    }
    return (target: any, methodKey: string, methodDescriptor: PropertyDescriptor) => {
        const origDecorator = Action(actionClass as any);
        origDecorator(target, methodKey, methodDescriptor);
    };
}
