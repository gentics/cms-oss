import { BO_ID, ObservableStopper } from '@admin-ui/common';
import {
    ConstructHandlerService,
    ContentRepositoryFragmentOperations,
    ContentRepositoryHandlerService,
    DataSourceHandlerService,
    ObjectPropertyHandlerService,
    PackageOperations,
    TemplateOperations,
} from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { NormalizableEntityType } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-entity-to-package-modal',
    templateUrl: './assign-entity-to-package-modal.component.html',
    changeDetection: ChangeDetectionStrategy.Default,
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
        private packageOperations: PackageOperations,
        private constructHandler: ConstructHandlerService,
        private crHandler: ContentRepositoryHandlerService,
        private crFragmentOperations: ContentRepositoryFragmentOperations,
        private dataSourceHandler: DataSourceHandlerService,
        private objectPropertyHandler: ObjectPropertyHandlerService,
        private templateOperations: TemplateOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        switch (this.entityIdentifier) {
            case 'construct':
                this.packageChildEntityIds$ = this.constructHandler.listFromDevtoolMapped(this.packageId).pipe(
                    map(res => res.items.map(entity => entity.id[BO_ID])),
                );
                break;
            case 'contentRepository':
                this.packageChildEntityIds$ = this.crHandler.listFromDevtoolMapped(this.packageId).pipe(
                    map(res => res.items.map(entity => entity[BO_ID])),
                );
                break;
            case 'contentRepositoryFragment':
                this.packageChildEntityIds$ = this.crFragmentOperations.getAllFromPackage(this.packageId).pipe(
                    map(entities => entities.map(entity => String(entity.id))),
                );
                break;
            case 'dataSource':
                this.packageChildEntityIds$ = this.dataSourceHandler.listFromDevtoolMapped(this.packageId).pipe(
                    map(entities => entities.items.map(entity => entity[BO_ID])),
                );
                break;
            case 'objectProperty':
                this.packageChildEntityIds$ = this.objectPropertyHandler.listFromDevtoolMapped(this.packageId).pipe(
                    map(res => res.items.map(entity => entity.id[BO_ID])),
                );
                break;
            case 'template':
                this.packageChildEntityIds$ = this.templateOperations.getAllFromPackage(this.packageId).pipe(
                    map(entities => entities.map(entity => entity.id)),
                );
                break;

            default:
                break;
        }

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

    private changeConstructsOfPackage(): Promise<void> {
        switch (this.entityIdentifier) {
            case 'construct':
                return this.packageOperations.changeConstructOfPackage(
                    this.packageId,
                    this.packageChildEntitySelectedIdsCurrent,
                    this.packageChildEntitySelectedIdsInitial,
                ).toPromise();
            case 'contentRepository':
                return this.packageOperations.changeContentRepositoryOfPackage(
                    this.packageId,
                    this.packageChildEntitySelectedIdsCurrent,
                    this.packageChildEntitySelectedIdsInitial,
                ).toPromise();
            case 'contentRepositoryFragment':
                return this.packageOperations.changeContentRepositoryFragmentOfPackage(
                    this.packageId,
                    this.packageChildEntitySelectedIdsCurrent,
                    this.packageChildEntitySelectedIdsInitial,
                ).toPromise();
            case 'dataSource':
                return this.packageOperations.changeDataSourceOfPackage(
                    this.packageId,
                    this.packageChildEntitySelectedIdsCurrent,
                    this.packageChildEntitySelectedIdsInitial,
                ).toPromise();
            case 'objectProperty':
                return this.packageOperations.changeObjectPropertyOfPackage(
                    this.packageId,
                    this.packageChildEntitySelectedIdsCurrent,
                    this.packageChildEntitySelectedIdsInitial,
                ).toPromise();
            case 'template':
                return this.packageOperations.changeTemplateOfPackage(
                    this.packageId,
                    this.packageChildEntitySelectedIdsCurrent,
                    this.packageChildEntitySelectedIdsInitial,
                ).toPromise();
            default:
                throw new Error(`Unknown entityIdentifier: ${this.entityIdentifier}`);
        }
    }
}
