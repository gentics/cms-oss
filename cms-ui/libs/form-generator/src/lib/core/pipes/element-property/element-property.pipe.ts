import { Pipe, PipeTransform } from '@angular/core';
import { CmsFormElementBO, CmsFormElementI18nValue, CmsFormElementPropertyType } from '@gentics/cms-models';

/**
 * # Gentics Form Generator element property pipe
 * Fetches the value of the property of type string set in label_property_ui
 * @example
 * ```html
 * <div class="form-element-label">
 *     {{ element | property:'label' | async }}
 * </div>
 * ```
 */
@Pipe({
    name: 'property',
    pure: true,
    standalone: false
})
export class ElementPropertyPipe implements PipeTransform {

    constructor() { }

    transform(element: CmsFormElementBO, content: 'label' | 'value' = 'value'): CmsFormElementI18nValue<string> {
        if (!element.label_property_ui) {
            return {};
        }

        for (const property of element.properties) {
            if (
                property.type !== CmsFormElementPropertyType.STRING
                || element.label_property_ui !== property.name
            ) {
                continue;
            }

            if (content === 'label') {
                return property.label_i18n_ui;
            } else {
                // can only be of type string, however the value is not typed precise enough
                return property.value_i18n as CmsFormElementI18nValue<string>;
            }
        }

        return {};
    }

}
