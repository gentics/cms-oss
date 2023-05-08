import { Pipe, PipeTransform } from '@angular/core';
import { InheritableItem } from '@gentics/cms-models';

export enum InheritanceType {
    /** the object is inherited without restrictions */
    Inherited = 'inherited',

    /** the object is inherited with some restrictions */
    Disinherited = 'disinherited',

    /** the object is excluded from multichannelling */
    Excluded = 'excluded',

    /** the object is not inherited into a higher channel */
    ExcludedByParent = 'excluded_by_parent',
}

@Pipe({
  name: 'getInheritance'
})
export class GetInheritancePipe implements PipeTransform {

  transform(value: InheritableItem, args?: any): InheritanceType {
    if (value && value.excluded === true) {
        return InheritanceType.Excluded;
    }

    if (value && value.disinherited === true) {
        return InheritanceType.Disinherited;
    }

    return InheritanceType.Inherited;
  }

}
