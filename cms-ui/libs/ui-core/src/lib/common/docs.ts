/**
 * Marker decorator that a simple property of an element should be included to the generated
 * documentation. If the decorator is not present, the property is not included.
 */
/* eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types, @typescript-eslint/naming-convention,@typescript-eslint/no-unused-vars, @typescript-eslint/ban-types */
export const IncludeToDocs: () => PropertyDecorator = () => (target: Object, propertyKey: string) => {}
