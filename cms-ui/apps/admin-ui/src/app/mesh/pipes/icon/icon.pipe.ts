import { MBO_TYPE, MeshBusinessObject, MeshType } from '@admin-ui/mesh/common';
import { Pipe, PipeTransform } from '@angular/core';
import { Permission } from '@gentics/mesh-models';

@Pipe({
    name: 'gtxMeshIcon',
    standalone: false
})
export class MeshIconPipe implements PipeTransform {

    transform(value: MeshBusinessObject | MeshType | Permission): string {
        if (value == null) {
            return '';
        }

        if (typeof value === 'object') {
            value = value[MBO_TYPE];
        }

        switch (value) {
            case MeshType.USER:
                return 'person';
            case MeshType.GROUP:
                return 'group';
            case MeshType.ROLE:
                return 'fact_check';
            case MeshType.PROJECT:
                return 'topic';
            case MeshType.SCHEMA:
                return 'view_compact';
            case MeshType.MICROSCHEMA:
                return 'view_module';
            case MeshType.TAG:
                return 'local_offer';
            case MeshType.TAG_FAMILY:
                return  'bookmarks';
            case MeshType.NODE:
                return 'book';

            case Permission.CREATE:
                return 'add';
            case Permission.READ:
                return 'visibility';
            case Permission.UPDATE:
                return 'edit';
            case Permission.DELETE:
                return 'delete';
            case Permission.PUBLISH:
                return 'public';
            case Permission.READ_PUBLISHED:
                return 'face';
        }
    }
}
