/* eslint-disable no-underscore-dangle */
import { discard } from '@admin-ui/common';
import { FolderOperations, I18nNotificationService, TemplateOperations } from '@admin-ui/core';
import { FolderLinkEvent } from '@admin-ui/shared';
import { SelectState } from '@admin-ui/state';
import { ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { EntityIdType, Folder, NormalizableEntityType } from '@gentics/cms-models';
import { isEqual } from'lodash-es'
import { BehaviorSubject, combineLatest, Observable, Subscription } from 'rxjs';
import { distinctUntilChanged, filter, switchMap, tap } from 'rxjs/operators';

@Component({
    selector: 'gtx-template-folder-link-master',
    templateUrl: './template-folder-link-master.component.html',
    styleUrls: ['./template-folder-link-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TemplateFolderLinkMasterComponent implements OnInit, OnDestroy {

    @Input()
    public disabled: boolean;

    @Input()
    public rootFolderId: number;

    @Input()
    public templateName: string;

    @SelectState(state => state.ui.focusEntityType)
    focusedEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusedEntityId$: Observable<EntityIdType>;

    @SelectState(state => state.ui.focusEntityNodeId)
    focusedEntityNodeId$: Observable<number>;

    loaded$: Observable<boolean>;

    selectedIds = new Set<EntityIdType>();

    protected currentTemplateId: EntityIdType;
    protected currentNodeId: number;

    protected rootId = new BehaviorSubject<number>(null);
    protected loadComplete = new BehaviorSubject<boolean>(false);
    protected _reload = new BehaviorSubject<void>(null);

    protected subscription = new Subscription();

    constructor(
        protected entityOperations: TemplateOperations,
        protected folderOperations: FolderOperations,
        protected notification: I18nNotificationService,
    ) { }

    ngOnInit(): void {
        this.loaded$ = this.loadComplete.asObservable();

        this.subscription.add(this.focusedEntityNodeId$.pipe(
            distinctUntilChanged(isEqual),
        ).subscribe(nodeId => {
            this.currentNodeId = nodeId;
        }));

        this.subscription.add(combineLatest([
            this.focusedEntityType$,
            this.focusedEntityId$,
        ]).pipe(
            filter(([type, id]) => type === 'template'),
            distinctUntilChanged(isEqual),
        ).subscribe(([type, id]) => {
            this.currentTemplateId = id;
            this.reload();
        }));

        this.subscription.add(this._reload.asObservable().pipe(
            tap(() => this.loadComplete.next(false)),
            switchMap(() => this.entityOperations.getLinkedFolders(this.currentTemplateId)),
        ).subscribe(selectedFolders => {
            this.selectedIds = new Set(selectedFolders.map(folder =>String(folder.id)));
            this.loadComplete.next(true);
        }));
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    reload(): void {
        this._reload.next(null);
    }

    onLinkEvent(event: FolderLinkEvent): void {
        if (this.disabled) {
            return;
        }

        this.subscription.add(this.linkFolder(event.folder, event.recursive).subscribe(() => {
            if (event.recursive) {
                this.notification.show({
                    type: 'success',
                    message: 'template.recursive_assign_to_folder_success',
                    translationParams: {
                        templateName: this.templateName,
                        folderName: event.folder.name,
                    },
                });

                this.reload();
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'template.assign_to_folder_success',
                    translationParams: {
                        templateName: this.templateName,
                        folderName: event.folder.name,
                    },
                });
            }
        }, err => {
            console.error(err);

            this.notification.show({
                type: 'alert',
                message: 'template.assign_to_folder_error',
                translationParams: {
                    templateName: this.templateName,
                    folderName: event.folder.name,
                },
            });
        }));
    }

    onUnlinkEvent(event: FolderLinkEvent): void {
        if (this.disabled) {
            return;
        }

        this.subscription.add(this.unlinkFolder(event.folder, event.recursive).subscribe(() => {
            if (event.recursive) {
                this.notification.show({
                    type: 'success',
                    message: 'template.recursive_unassign_from_folder_success',
                    translationParams: {
                        templateName: this.templateName,
                        folderName: event.folder.name,
                    },
                });

                this.reload();
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'template.unassign_from_folder_success',
                    translationParams: {
                        templateName: this.templateName,
                        folderName: event.folder.name,
                    },
                });
            }
        }, err => {
            console.error(err);

            this.notification.show({
                type: 'alert',
                message: 'template.unassign_from_folder_error',
                translationParams: {
                    templateName: this.templateName,
                    folderName: event.folder.name,
                },
            });
        }));
    }

    linkFolder(folder: Folder, recursive: boolean): Observable<void> {
        return this.entityOperations.linkFolders(this.currentTemplateId, {
            folderIds: [folder.id],
            nodeId: this.currentNodeId,
            recursive,
        }).pipe(
            discard(() => {
                this.selectedIds.add(folder.id);
                this.selectedIds.add(folder.globalId);
            }),
        );
    }

    unlinkFolder(folder: Folder, recursive: boolean): Observable<void> {
        return this.entityOperations.unlinkFolders(this.currentTemplateId, {
            folderIds: [folder.id],
            nodeId: this.currentNodeId,
            recursive,
            delete: true,
        }).pipe(
            discard(() => {
                this.selectedIds.delete(folder.id);
                this.selectedIds.delete(folder.globalId);
            }),
        );
    }
}
