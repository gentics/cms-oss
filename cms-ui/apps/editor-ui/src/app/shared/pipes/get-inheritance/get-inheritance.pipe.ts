import { Pipe, PipeTransform } from '@angular/core';
import { InheritableItem } from '@gentics/cms-models';

export enum InheritanceType {
    /** the object is inherited without restrictions */
    INHERITED = 'inherited',

    /** the object is inherited with some restrictions */
    DISINHERITED = 'disinherited',

    /** the object is excluded from multichannelling */
    EXCLUDED = 'excluded',

    /** the object is not inherited into a higher channel */
    EXCLUDED_BY_PARENT = 'excluded_by_parent',

    /** The object is localized in the channel */
    LOCALIZED = 'localized',

    MASTER = 'master',
}

@Pipe({
    name: 'getInheritance',
    standalone: false
})
export class GetInheritancePipe implements PipeTransform {

    transform(value: InheritableItem, args?: any): InheritanceType {
        if (value && value.excluded === true) {
            return InheritanceType.EXCLUDED;
        }

        if (value && value.disinherited === true) {
            return InheritanceType.DISINHERITED;
        }

        if (value && value.inherited) {
            return InheritanceType.INHERITED;
        }

        if (value && value.inheritedFromId !== value.masterNodeId) {
            return InheritanceType.LOCALIZED;
        }

        return InheritanceType.MASTER;
    }

}
