import { Pipe, PipeTransform } from '@angular/core';
import { GcmsUiLanguage, TagPart } from '@gentics/cms-models';

/**
 * Transforms a TagPart into the string that should be used for
 * the labeling the component that is used to edit the corresponding TagProperty.
 * An '*' is added to this string if the TagPart is mandatory.
 */
@Pipe({ name: 'tagPropLabel' })
export class TagPropertyLabelPipe implements PipeTransform {

    transform(value: TagPart, language?: GcmsUiLanguage): string {
        if (!value) {
            return '';
        }

        let label = value.keyword;
        if (value.name) {
            label = value.name;
        } else if (value.nameI18n && language && value.nameI18n[language]) {
            label = value.nameI18n[language];
        }

        return label + (value.mandatory ? ' *' : '');
    }

}
