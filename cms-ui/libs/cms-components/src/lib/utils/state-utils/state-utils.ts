import { Action } from '@ngxs/store';

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
interface ConstructorOf<T> { new(...args: any[]): T; }

interface AppStateActionClass extends ConstructorOf<any>, Function {
    type: string;
}

/**
 * Decorates a class as the declaration of an action for the ngxs store.
 *
 * This decorator adds the static `type` property required by ngxs, based on the `appStateBranch` and the class name.
 */
export function ActionDeclaration(appStateBranch: string): (target: ConstructorOf<any>) => void {
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
