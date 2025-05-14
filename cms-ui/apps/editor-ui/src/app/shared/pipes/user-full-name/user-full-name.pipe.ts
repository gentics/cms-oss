import { Pipe, PipeTransform } from '@angular/core';
import { User } from '@gentics/cms-models';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';

/**
 * Transforms a `User` object or a user ID into the full name of the user.
 */
@Pipe({
    name: 'userFullName',
    standalone: false
})
export class UserFullNamePipe implements PipeTransform {

    constructor(private entityResolver: EntityResolver) { }

    transform(user: number | User) {
        let userObj: User;
        if (typeof user === 'number') {
            userObj = this.entityResolver.getUser(user);
        } else if (user && typeof user === 'object') {
            userObj = (user as User);
        }
        return userObj ? userObj.firstName + ' ' + userObj.lastName : '';
    }

}
