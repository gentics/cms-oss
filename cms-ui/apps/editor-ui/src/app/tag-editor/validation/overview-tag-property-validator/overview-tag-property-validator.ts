import { TagPropertyValidator, ValidationResult } from '@gentics/cms-integration-api-models';
import {
    ListType,
    OrderBy,
    OrderDirection,
    Overview,
    OverviewSetting,
    OverviewTagPartProperty,
    SelectType,
    TagPart,
    TagPartProperty,
} from '@gentics/cms-models';

/**
 * Validator for tag properties of type TagPropertyType.OVERVIEW.
 */
export class OverviewTagPropertyValidator implements TagPropertyValidator<OverviewTagPartProperty> {

    validate(editedProperty: TagPartProperty, tagPart: TagPart): ValidationResult {
        const overviewProperty = editedProperty as OverviewTagPartProperty;
        const isSet = this.checkIfSet(overviewProperty.overview, tagPart.overviewSettings);
        return {
            isSet: isSet,
            success: isSet || !tagPart.mandatory
        };
    }

    private checkIfSet(overview: Overview, settings: OverviewSetting): boolean {
        if (!overview) {
            return false;
        }

        let isSet = true;
        isSet = isSet && !!overview.listType && overview.listType !== ListType.UNDEFINED;
        isSet = isSet && !!overview.selectType && overview.selectType !== SelectType.UNDEFINED;
        if (!settings.hideSortOptions) {
            isSet = isSet && !!overview.orderDirection && overview.orderDirection !== OrderDirection.UNDEFINED;
            isSet = isSet && !!overview.orderBy && overview.orderBy !== OrderBy.UNDEFINED;
        }
        if (overview.selectType !== SelectType.AUTO) {
            if (settings.stickyChannel) {
                isSet = isSet && Array.isArray(overview.selectedNodeItemIds) && overview.selectedNodeItemIds.length > 0;
            }  else {
                isSet = isSet && Array.isArray(overview.selectedItemIds) && overview.selectedItemIds.length > 0;
            }
        }
        return isSet;
    }

}
