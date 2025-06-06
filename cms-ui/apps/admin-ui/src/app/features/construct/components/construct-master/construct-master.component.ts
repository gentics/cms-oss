import { BO_PERMISSIONS, ConstructBO, EditableEntity, EntityTableActionClickEvent } from '@admin-ui/common';
import { ConstructHandlerService, ConstructTableLoaderService, ErrorHandler, I18nNotificationService, I18nService } from '@admin-ui/core';
import { ASSIGN_CONSTRUCT_TO_CATEGORY_ACTION, ASSIGN_CONSTRUCT_TO_NODES_ACTION, COPY_CONSTRUCT_ACTION } from '@admin-ui/shared';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GcmsPermission, TagType } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { combineLatest, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import { AssignConstructsToCategoryModalComponent } from '../assign-constructs-to-category-modal/assign-constructs-to-category-modal.component';
import { AssignConstructsToNodesModalComponent } from '../assign-constructs-to-nodes-modal/assign-constructs-to-nodes-modal.component';
import { CopyConstructModalComponent } from '../copy-construct-modal/copy-construct-modal.component';
import { CreateConstructModalComponent } from '../create-construct-modal/create-construct-modal.component';

@Component({
    selector: 'gtx-construct-master',
    templateUrl: './construct-master.component.html',
    styleUrls: ['./construct-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructMasterComponent extends BaseTableMasterComponent<TagType, ConstructBO> {

    public selection: string[] = [];

    protected entityIdentifier = EditableEntity.CONSTRUCT;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected i18n: I18nService,
        protected loader: ConstructTableLoaderService,
        protected handler: ConstructHandlerService,
        protected modalService: ModalService,
        protected notification: I18nNotificationService,
        protected errorHandler: ErrorHandler,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    async handleCreate(): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(CreateConstructModalComponent, {
                closeOnEscape: false,
                closeOnOverlayClick: false,
                width: '80%',
            });
            const created = await dialog.open();

            if (!created) {
                return;
            }

            this.loader.reload();
        } catch (err) {
            if (wasClosedByUser(err)) {
                return;
            }
            throw err;
        }
    }

    public handleAction(event: EntityTableActionClickEvent<ConstructBO>): void {
        const entities = event.selection ? event.selectedItems : [event.item];

        switch (event.actionId) {
            case ASSIGN_CONSTRUCT_TO_NODES_ACTION:
                this.assignConstructsToNodes(entities);
                break;

            case ASSIGN_CONSTRUCT_TO_CATEGORY_ACTION:
                this.assignConstructsToCategories(entities);
                break;

            case COPY_CONSTRUCT_ACTION:
                this.copyConstruct(event.item);
                break;
        }
    }

    async copyConstruct(construct: ConstructBO): Promise<void> {
        if (construct == null) {
            return;
        }

        const dialog = await this.modalService.fromComponent(CopyConstructModalComponent, {
            width: '80%',
        }, {
            // Pass in a clone
            construct: structuredClone(construct),
        });
        const response: boolean = await dialog.open();

        if (!response) {
            return;
        }

        this.loader.reload();
    }

    protected verifyConstructs(
        constructs: ConstructBO[],
        perm: GcmsPermission,
        message: string,
    ): { valid: boolean; filtered: ConstructBO[] } {
        const res = { valid: false, filtered: constructs };

        if (!Array.isArray(constructs)) {
            return res;
        }

        res.valid = true;
        res.filtered = constructs.filter(con => con);

        res.filtered.forEach(construct => {
            if (construct[BO_PERMISSIONS].includes(perm)) {
                return;
            }

            res.valid = false;
            this.notification.show({
                type: 'alert',
                message: message,
                translationParams: {
                    constructName: construct.name,
                },
            });
        });

        res.valid = res.valid && constructs.length > 0;

        return res;
    }

    protected async assignConstructsToNodes(constructs: ConstructBO[]): Promise<void> {
        const { valid, filtered } = this.verifyConstructs(constructs, GcmsPermission.EDIT, 'construct.assign_construct_to_nodes_permission_required');
        if (!valid) {
            return;
        }
        constructs = filtered;

        try {
            const dialog = await this.modalService.fromComponent(AssignConstructsToNodesModalComponent, {
                closeOnEscape: false,
                closeOnOverlayClick: false,
            }, {
                constructs,
            });
            const didChange = await dialog.open();

            if (didChange) {
                this.loader.reload();
            }
        } catch (err) {
            this.errorHandler.catch(err);
        }
    }

    protected async assignConstructsToCategories(constructs: ConstructBO[]): Promise<void> {
        const { valid, filtered } = this.verifyConstructs(constructs, GcmsPermission.EDIT, 'construct.assign_construct_to_category_permission_required');
        if (!valid) {
            return;
        }
        constructs = filtered;

        const dialog = await this.modalService.fromComponent(AssignConstructsToCategoryModalComponent, {}, {});
        const response: false | number = await dialog.open();

        if (!response) {
            return;
        }

        this.subscriptions.push(
            combineLatest(constructs.map(con => this.handler.updateMapped(con.id, { categoryId: response }).pipe(
                map(cat => cat.id),
                catchError(err => {
                    this.notification.show({
                        type: 'alert',
                        delay: 10_000,
                        message: 'construct.assign_error',
                        translationParams: {
                            constructName: con.name,
                            errorMessage: err.message,
                        },
                    });
                    console.error(err);
                    return of(null);
                }),
            ))).subscribe(ids => {
                // If at least one has been successfully deleted, then we reload the page
                if (ids.some(id => id)) {
                    this.loader.reload();
                    this.changeDetector.markForCheck();
                }
            }),
        );
    }
}
