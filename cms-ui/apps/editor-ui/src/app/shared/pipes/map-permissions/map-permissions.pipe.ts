import { Pipe, PipeTransform } from "@angular/core";
import { EditorPermissions, FolderPermissionData, getNoPermissions } from "../../../common/models";
import { PermissionService } from '../../../core/providers/permissions/permission.service';

@Pipe({
    name: 'gtxMapPermissions',
    standalone: false,
})
export class MapPermissionsPipe implements PipeTransform {

    constructor(
        private permissionService: PermissionService,
    ) {}

    transform(data: FolderPermissionData, language?: string | number): EditorPermissions {
        if (data == null) {
            return getNoPermissions();
        }

        return this.permissionService.mapToPermissions(data.privileges, data.permissions, language);
    }
}
