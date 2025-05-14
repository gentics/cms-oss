import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'gtxConcat',
    pure: true,
    standalone: false
})
export class ConcatPipe implements PipeTransform {

    transform(value: any[], ...args: any[]): any[] {
        if (value == null) {
            value = [];
        } else if (!Array.isArray(value)) {
            value = [value];
        }

        let out = value;
        if (args?.length > 0) {
            for (const toAdd of args) {
                if (toAdd == null) {
                    continue;
                }
                out = out.concat(toAdd);
            }
        }

        return out;
    }
}
