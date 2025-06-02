import { Pipe, PipeTransform } from '@angular/core';
import { PermissionsAndRoles } from '@gentics/cms-models';

/**
 * Counts the number of roles in a given `PermissionsAndRoles` object.
 */
@Pipe({
    name: 'countRoles',
    standalone: false
})
export class CountRolesPipe implements PipeTransform {

    transform(value: PermissionsAndRoles): number {
        return value && value.roles ? value.roles.filter(role => role.value).length : 0;
    }

}
