import { Pipe, PipeTransform } from '@angular/core';
import { getValueByPath } from '@gentics/ui-core';

@Pipe({
    name: 'gtxValuePath',
})
export class ValuePathPipe implements PipeTransform {

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    transform(value: any, path: symbol | string | (string | symbol)[]): any {
        return getValueByPath(value, path);
    }
}
