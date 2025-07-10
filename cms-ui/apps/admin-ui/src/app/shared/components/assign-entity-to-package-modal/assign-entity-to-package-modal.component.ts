import { EditableEntity, ObservableStopper } from '@admin-ui/common';
import { DevToolPackageManagerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { NormalizableEntityType } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';

function entityIdentifierToEditableEntity(identifier: NormalizableEntityType): EditableEntity {
    switch (identifier) {
        case 'construct':
            return EditableEntity.CONSTRUCT;
        case 'constructCategory':
            return EditableEntity.CONSTRUCT_CATEGORY;
        case 'contentPackage':
            return EditableEntity.CONTENT_PACKAGE;
        case 'contentRepository':
            return EditableEntity.CONTENT_REPOSITORY;
        case 'contentRepositoryFragment':
            return EditableEntity.CR_FRAGMENT;
        case 'dataSource':
            return EditableEntity.DATA_SOURCE;
        case 'folder':
            return EditableEntity.FOLDER;
        case 'language':
            return EditableEntity.LANGUAGE;
        case 'node':
            return EditableEntity.NODE;
        case 'objectProperty':
            return EditableEntity.OBJECT_PROPERTY;
        case 'objectPropertyCategory':
            return EditableEntity.OBJECT_PROPERTY_CATEGORY;
        case 'role':
            return EditableEntity.ROLE;
        case 'schedule':
            return EditableEntity.SCHEDULE;
        case 'scheduleTask':
            return EditableEntity.SCHEDULE_TASK;
        case 'package':
            return EditableEntity.DEV_TOOL_PACKAGE;
        case 'template':
            return EditableEntity.TEMPLATE;
        case 'user':
            return EditableEntity.USER;
        default:
            return null;
    }
}

@Component({
    selector: 'gtx-assign-entity-to-package-modal',
    templateUrl: './assign-entity-to-package-modal.component.html',
    changeDetection: ChangeDetectionStrategy.Default,
    standalone: false
})
export class AssignEntityToPackageModalComponent extends BaseModal<void> implements OnDestroy, OnInit {

    /** ID of group to be (un)assigned to/from constructs */
    packageId: string;

    /** Name of the entity */
    entityIdentifier: NormalizableEntityType;

    /** IDs of constructs to be (un)assigned to/from groups */
    packageChildEntityIds$: Observable<string[]>;
    packageChildEntitySelectedIdsInitial: string[] = [];
    packageChildEntitySelectedIdsCurrent: string[] = [];

    private stopper = new ObservableStopper();

    constructor(
        private manager: DevToolPackageManagerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.packageChildEntityIds$ = this.manager.getSelectedEntityIds(this.packageId, entityIdentifierToEditableEntity(this.entityIdentifier));

        this.packageChildEntityIds$.toPromise().then(ids => {
            this.packageChildEntitySelectedIdsInitial = ids;
            this.packageChildEntitySelectedIdsCurrent = [...ids];
        });
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    /** Get form validity state */
    allIsValid(): boolean {
        return this.packageChildEntitySelectedIdsCurrent && this.packageChildEntitySelectedIdsCurrent.length > 0;
    }

    /**
     * If user clicks "assign"
     */
    buttonAssignPackageChildEntityToPackageClicked(): void {
        this.changeConstructsOfPackage()
            .then(() => this.closeFn());
    }

    private changeConstructsOfPackage(): Promise<boolean> {
        return this.manager.manageSelection(
            this.packageId,
            entityIdentifierToEditableEntity(this.entityIdentifier),
            this.packageChildEntitySelectedIdsInitial,
            this.packageChildEntitySelectedIdsCurrent,
        ).toPromise();
    }
}
