import { Pipe, PipeTransform } from '@angular/core';
import { ObjectTag } from '@gentics/cms-models';

/**
 * Transforms an `ObjectTag` into the string that should be used for
 * the labeling its tab in the object properties list.
 * An '*' is added to this string if the ObjectTag is mandatory.
 */
@Pipe({ name: 'objTagName' })
export class ObjectTagNamePipe implements PipeTransform {

    transform(value: ObjectTag): string {
        if (!value) {
            return '';
        }

        const label = value.displayName;
        if (value.required) {
            return label + ' *';
        } else {
            return label;
        }
    }

}
