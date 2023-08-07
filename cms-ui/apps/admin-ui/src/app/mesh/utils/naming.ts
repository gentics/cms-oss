import { User, UserReference } from '@gentics/mesh-models';

export function getUserName(user: User | UserReference): string {
    let out = '';

    if (user.firstname) {
        out = user.firstname;
    }

    if (user.lastname) {
        if (out !== '') {
            out += ' ';
        }
        out += user.lastname;
    }

    if ((user as User).username) {
        if (out !== '') {
            out += ` (${(user as User).username})`;
        } else {
            out = (user as User).username;
        }
    }

    if (out === '') {
        out = `${user.uuid}`;
    }

    return out;
}
