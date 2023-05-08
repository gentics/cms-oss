import { CategoryInfo, PermissionsCategorizer, PermissionsUtils } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
    AccessControlledType,
    Group,
    GroupSetPermissionsRequest,
    IndexByKey,
    PermissionInfo,
    PermissionsSet,
    Raw,
    RoleAssignment
} from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { cloneDeep as _cloneDeep } from 'lodash';

export type EditPermissionsSuccessCallback = (changes: GroupSetPermissionsRequest) => void;
export type EditPermissionsCancelCallback = (val: void) => void;

@Component({
    selector: 'gtx-edit-permissions-modal',
    templateUrl: './edit-permissions-modal.component.html',
    styleUrls: ['./edit-permissions-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditPermissionsModalComponent implements OnInit, IModalDialog {

    /**
     * The `Group` for which the permissions should be set.
     *
     * This must be set before displaying the modal.
     */
    group: Group<Raw>;

    /**
     * The `PermissionsSet` that should be edited.
     *
     * This must be set before displaying the modal.
     */
    permSet: PermissionsSet;

    /**
     * If categories shoult grouped by ID.
     *
     * This must be set before displaying the modal.
     */
    groupPermsByCategory = true;

    closeFn: EditPermissionsSuccessCallback;
    cancelFn: EditPermissionsCancelCallback;

    allPermsGranted: boolean | 'indeterminate';
    applyToSubObjects = false;
    applyToSubGroups = false;

    permissionCategories: CategoryInfo[];
    categorizedPerms: IndexByKey<PermissionInfo[]>;

    AccessControlledType = AccessControlledType;

    private permissionsCategorizer: PermissionsCategorizer;

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) {}

    ngOnInit(): void {
        // Make sure that we can modify the PermissionsSet.
        this.permSet = _cloneDeep(this.permSet);
        this.permissionsCategorizer =
            this.groupPermsByCategory ?
                PermissionsUtils.createCategorizerByCategoryId() :
                PermissionsUtils.createCategorizerByPermType();
        this.updateAllPermsGranted();
        this.categorizePermissions();
    }

    registerCloseFn(close: EditPermissionsSuccessCallback): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: EditPermissionsCancelCallback): void {
        this.cancelFn = cancel;
    }

    onSaveClick(): void {
        const saveRequest = this.buildSetPermRequest();
        this.closeFn(saveRequest);
    }

    onAllPermsToggle(newValue: boolean): void {
        this.setAllPerms(newValue);
        this.updateAllPermsGranted();
    }

    onPermToggle(): void {
        this.updateAllPermsGranted();
        this.changeDetector.markForCheck();
    }

    hasEditPermissionRight(): boolean {
        if (this.permissionCategories) {
            return !!this.permissionCategories.find(category => {
                if (this.categorizedPerms[category.id]?.length > 0) {
                    return this.categorizedPerms[category.id].find(permission => permission.editable === true);
                }
                return false;
            });
        }
        return false;
    }

    private updateAllPermsGranted(): void {
        this.allPermsGranted = this.determineIfAllPermsGranted();
    }

    private setAllPerms(value: boolean): void {
        this.forEachEditablePermission(perm => perm.value = value);
        this.changeDetector.markForCheck();
    }

    private determineIfAllPermsGranted(): boolean | 'indeterminate' {
        let grantedCount = 0;
        let deniedCount = 0;
        this.forEachEditablePermission(perm => {
            if (perm.value) {
                ++grantedCount;
            } else {
                ++deniedCount;
            }
        });

        if (deniedCount === 0) {
            return true;
        }
        if (grantedCount === 0) {
            return false;
        }
        return 'indeterminate';
    }

    private buildSetPermRequest(): GroupSetPermissionsRequest {
        const req: GroupSetPermissionsRequest = {
            subGroups: this.applyToSubGroups,
            subObjects: this.applyToSubObjects,
            perms: [],
        };

        this.forEachEditablePermission(
            perm => req.perms.push({ type: perm.type, value: perm.value }),
        );

        if (this.permSet.roles) {
            req.roles = [];
            this.forEachEditableRole(
                role => req.roles.push({ id: role.id, value: role.value }),
            );
        }

        return req;
    }

    private forEachEditablePermission(action: (perm: PermissionInfo) => void): void {
        this.permSet.perms.forEach(perm => {
            if (perm.editable) {
                action(perm);
            }
        });
    }

    private forEachEditableRole(action: (role: RoleAssignment) => void): void {
        if (this.permSet.roles) {
            this.permSet.roles.forEach(role => {
                if (role.editable) {
                    action(role);
                }
            });
        }
    }

    private categorizePermissions(): void {
        this.categorizedPerms = this.permissionsCategorizer.categorizePermissions(this.permSet.perms);
        this.permissionCategories = this.permissionsCategorizer.getKnownCategories();
    }

}
