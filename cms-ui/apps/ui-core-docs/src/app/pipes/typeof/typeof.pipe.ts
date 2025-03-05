import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'gtxTypeof',
})
export class TypeOfPipe implements PipeTransform {

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    transform(value: any): string {
        return typeof value;
    }
}
