import { Pipe, PipeTransform } from '@angular/core';
import { User } from '@gentics/cms-models';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { I18nService } from '@gentics/cms-components';

/**
 * Transforms a `User` object or a user ID into the full name of the user.
 */
@Pipe({
    name: 'userFullName',
    standalone: false,
})
export class UserFullNamePipe implements PipeTransform {

    constructor(
        private entityResolver: EntityResolver,
        private i18n: I18nService,
    ) { }

    transform(user: number | User, skipUnknown?: false): string;
    transform(user: number | User, skipUnknown: true): string | null;
    transform(user: number | User, skipUnknown: boolean = false): string | null {
        let userObj: User;
        if (typeof user === 'number') {
            userObj = this.entityResolver.getUser(user);
        } else if (user && typeof user === 'object') {
            userObj = (user);
        }

        if (userObj == null) {
            return skipUnknown ? null : this.i18n.instant('editor.user_unknown');
        }

        if (userObj.firstName && userObj.lastName) {
            return `${userObj.firstName} ${userObj.lastName}`;
        }

        return userObj.login;
    }

}
