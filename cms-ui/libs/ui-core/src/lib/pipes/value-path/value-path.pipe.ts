import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'gtxValuePath',
})
export class ValuePathPipe implements PipeTransform {

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    transform(value: any, path: symbol | string | (string | symbol)[]): any {
        if (value == null || path == null) {
            return value;
        }

        if (typeof path === 'string') {
            path = path.includes('.') ? path.split('.') : [path];
        } else if (typeof path === 'symbol') {
            path = [path];
        }

        for (const segment of path) {
            value = value?.[segment];
        }

        return value;
    }
}
