import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RoleReference } from '@gentics/mesh-models';

@Component({
    selector: 'gtx-mesh-role-permissions-trable',
    templateUrl: './mesh-role-permissions-trable.component.html',
    styleUrls: ['./mesh-role-permissions-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshRolePermissionsTrableComponent {

    @Input()
    public role: RoleReference;
}
