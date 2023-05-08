import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'values',
})
export class ValuesPipe implements PipeTransform {
    transform(value: any, ...args: any[]): any {
        if (value != null && typeof value === 'object') {

            // If the value is a set, then we return a new array of the values
            if (value.__proto__ === Set.prototype) {
                return Array.from(value);
            }

            return Object.values(value);
        }
        return value;
    }
}
