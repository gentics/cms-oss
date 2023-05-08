import { Pipe, PipeTransform } from '@angular/core';
import { User } from '@gentics/cms-models';


/**
 * Transforms a `User` object or a user ID into the full name of the user.
 */
@Pipe({ name: 'userFullName' })
export class UserFullNamePipe implements PipeTransform {

    constructor() { }

    transform(user: User): string {
        let userObj: User;
        userObj = (user as User);
        return userObj ? userObj.firstName + ' ' + userObj.lastName : '';
    }

}
