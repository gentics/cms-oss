import { Pipe, PipeTransform } from '@angular/core';
import { TagType, ICON_MAPPING } from '@gentics/cms-models';

/**
 * Provides a material icon for a TagType.
 *
 * The input to this pipe must be a `TagType`.
 * The output will be a string indicating a material icon.
 */
@Pipe({ name: 'tagTypeIcon' })
export class TagTypeIconPipe implements PipeTransform {

    transform(value: TagType): string {
        let materialIcon: string;
        if (value && value.icon) {
            materialIcon = ICON_MAPPING[value.icon];
        }
        return materialIcon || '';
    }

}
