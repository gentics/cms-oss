import * as data from '../../../docs.output.json';

// eslint-disable-next-line @typescript-eslint/naming-convention, prefer-arrow/prefer-arrow-functions
export function InjectDocumentation(id: string): PropertyDecorator {
    // eslint-disable-next-line @typescript-eslint/ban-types
    return function(target: Object, propertyKey: string): void {
        Object.defineProperty(target, propertyKey, {
            get: () => data[id],
            set: () => {},
        });
    }
}
