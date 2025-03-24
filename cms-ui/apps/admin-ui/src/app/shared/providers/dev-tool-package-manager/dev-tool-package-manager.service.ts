import { BO_ID, DevToolEntityHandler, EditableEntity } from '@admin-ui/common';
import { TemplateOperations } from '@admin-ui/core';
import { Injectable } from '@angular/core';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { map } from 'rxjs/operators';
import { ContentRepositoryFragmentOperations } from '../../../core/providers/operations/cr-fragment/cr-fragment.operations';
import { ConstructHandlerService } from '../construct-handler/construct-handler.service';
import { ContentRepositoryHandlerService } from '../content-repository-handler/content-repository-handler.service';
import { DataSourceHandlerService } from '../data-source-handler/data-source-handler.service';
import { DevToolPackageHandlerService } from '../dev-tool-package-handler/dev-tool-package-handler.service';
import { ObjectPropertyHandlerService } from '../object-property-handler/object-property-handler.service';

/**
 * Simple Service to manage the selections of dev-tool package elements.
 */
@Injectable()
export class DevToolPackageManagerService {

    constructor(
        protected devToolPackages: DevToolPackageHandlerService,
        protected constructs: ConstructHandlerService,
        protected contentRepositories: ContentRepositoryHandlerService,
        protected crFragments: ContentRepositoryFragmentOperations,
        protected dataSources: DataSourceHandlerService,
        protected objProps: ObjectPropertyHandlerService,
        protected templates: TemplateOperations,
    ) {}

    public getSelectedEntityIds(
        devToolPackage: string,
        entityType: EditableEntity,
    ): Observable<string[]> {
        const handler = this.getHandler(entityType);
        if (handler == null) {
            return throwError(new Error(`No Handler found for type ${entityType}!`));
        }

        return handler.listFromDevToolMapped(devToolPackage).pipe(
            map(res => res.items.map(item => item[BO_ID])),
        );
    }

    public manageNodeAssignment(
        nodeId: number | string,
        assignedPackages: string[],
        newAssignment: string[],
    ): Observable<boolean> {
        const toAdd = newAssignment.filter(id => !assignedPackages.includes(id));
        const toRemove = assignedPackages.filter(id => !newAssignment.includes(id));

        const operations = [
            ...toAdd.map(id => this.devToolPackages.assignToNode(nodeId, id)),
            ...toRemove.map(id => this.devToolPackages.unassignFromNode(nodeId, id)),
        ];

        if (operations.length === 0) {
            return of(false);
        }

        return forkJoin(operations).pipe(
            map(() => true),
        );
    }

    public manageSelection(
        devToolPackage: string,
        entityType: EditableEntity,
        assignedElements: (number | string)[],
        newAssignment: (number | string)[],
    ): Observable<boolean> {
        const handler = this.getHandler(entityType);
        if (handler == null) {
            return throwError(new Error(`No Handler found for type ${entityType}!`));
        }

        const toAdd = newAssignment.filter(id => !assignedElements.includes(id));
        const toRemove = assignedElements.filter(id => !newAssignment.includes(id));

        const operations = [
            ...toAdd.map(id => handler.addToDevTool(devToolPackage, id)),
            ...toRemove.map(id => handler.removeFromDevTool(devToolPackage, id)),
        ];

        if (operations.length === 0) {
            return of(false);
        }

        return forkJoin(operations).pipe(
            map(() => true),
        );
    }

    protected getHandler<K extends EditableEntity>(entityType: K): DevToolEntityHandler<K> {
        switch (entityType) {
            case EditableEntity.CONSTRUCT:
                return this.constructs as any;
            case EditableEntity.CONTENT_REPOSITORY:
                return this.contentRepositories as any;
            case EditableEntity.CR_FRAGMENT:
                return this.crFragments as any;
            case EditableEntity.DATA_SOURCE:
                return this.dataSources as any;
            case EditableEntity.OBJECT_PROPERTY:
                return this.objProps as any;
            case EditableEntity.TEMPLATE:
                return this.templates as any;
            default:
                return null;
        }
    }
}
