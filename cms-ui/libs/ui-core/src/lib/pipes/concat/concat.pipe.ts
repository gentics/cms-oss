import { Pipe, PipeTransform } from '@angular/core';
import { IncludeToDocs } from '@gentics/ui-core';

/**
 * Simple helper pipe to concatinate all arguments into one array.
 */
@Pipe({
    name: 'gtxConcat',
    pure: true,
})
export class ConcatPipe implements PipeTransform {

    /**
     * All values can be mixed, and will be joined together.
     * Contains `null` checks to skip `null` elements completely.
     * If a value is not an array yet, it'll be converted to one if possible.
     * No value will be modified by this pipe.
     *
     * @param value Initial value
     * @param args Additional elements to concatinate
     * @returns A array which will contain all provided elements.
     */
    @IncludeToDocs()
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
