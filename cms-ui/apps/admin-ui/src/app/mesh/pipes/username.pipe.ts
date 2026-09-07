import { Pipe, PipeTransform } from '@angular/core';
import { User } from '@gentics/mesh-models';

@Pipe({
    name: 'gtxMeshUsername',
    pure: true,
    standalone: false,
})
export class MeshUsernamePipe implements PipeTransform {
    public transform(user: User): string {
        if (user == null) {
            return '';
        }

        const parts = [(user.firstname || '').trim(), (user.lastname || '').trim()]
            .filter((name) => !!name);

        return parts.length === 2 ? parts.join(' ') : user.username;
    }
}
