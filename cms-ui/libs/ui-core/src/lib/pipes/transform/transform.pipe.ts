import { Pipe, PipeTransform } from '@angular/core';
import { getValueByPath } from '../../utils';

type TransformFn = (value: any) => any;

@Pipe({
    name: 'gtxTransform',
})
export class TransformPipe implements PipeTransform {

    transform(value: any, pathOrFunction: string | symbol | (string | symbol)[] | TransformFn): any {
        if (pathOrFunction == null) {
            return value;
        }

        if (typeof pathOrFunction === 'function') {
            return pathOrFunction(value);
        }

        return getValueByPath(value, pathOrFunction);
    }
}
