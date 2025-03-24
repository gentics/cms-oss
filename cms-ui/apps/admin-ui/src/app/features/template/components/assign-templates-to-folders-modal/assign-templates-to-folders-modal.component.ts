import { discard, TemplateBO } from '@admin-ui/common';
import { FolderOperations, I18nNotificationService, TemplateOperations } from '@admin-ui/core';
import { FolderLinkEvent, FolderTrableLoaderService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { EntityIdType, Folder } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { intersection } from 'lodash-es';
import { combineLatest, Observable, Subscription, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-templates-to-folders-modal',
    templateUrl: './assign-templates-to-folders-modal.component.html',
    styleUrls: ['./assign-templates-to-folders-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignTemplatesToFoldersModalComponent extends BaseModal<void> implements OnInit, OnDestroy {

    @Input()
    public nodeId: number;

    @Input()
    public rootFolderId: number;

    @Input()
    public templates: TemplateBO[] = [];

    public selectedIds = new Set<EntityIdType>();

    protected subscriptions: Subscription[] = [];

    constructor(
        protected templateOperations: TemplateOperations,
        protected folderOperations: FolderOperations,
        protected notification: I18nNotificationService,
        protected loader: FolderTrableLoaderService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(combineLatest(
            this.templates.map(template => {
                return this.templateOperations.getLinkedFolders(template.id).pipe(
                    map(folders => folders.map(folder => folder.id)),
                );
            }),
        ).subscribe(selectedFolders => {
            this.selectedIds = new Set(intersection(...selectedFolders));
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    onLinkEvent(event: FolderLinkEvent): void {
        this.subscriptions.push(this.linkFolder(event.folder, event.recursive).subscribe(() => {
            if (event.recursive) {
                this.notification.show({
                    type: 'success',
                    message: 'template.recursive_assign_multiple_to_folder_success',
                    translationParams: {
                        folderName: event.folder.name,
                    },
                });

                this.loader.reload();
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'template.assign_multiple_to_folder_success',
                    translationParams: {
                        folderName: event.folder.name,
                    },
                });
            }
        }));
    }

    onUnlinkEvent(event: FolderLinkEvent): void {
        this.subscriptions.push(this.unlinkFolder(event.folder, event.recursive).subscribe(() => {
            if (event.recursive) {
                this.notification.show({
                    type: 'success',
                    message: 'template.recursive_unassign_multiple_from_folder_success',
                    translationParams: {
                        folderName: event.folder.name,
                    },
                });

                this.loader.reload();
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'template.unassign_multiple_from_folder_success',
                    translationParams: {
                        folderName: event.folder.name,
                    },
                });
            }
        }));
    }

    linkFolder(folder: Folder, recursive: boolean): Observable<void> {
        return combineLatest(this.templates.map(template => {
            return this.templateOperations.linkFolders(template.id, {
                folderIds: [folder.id],
                nodeId: this.nodeId,
                recursive,
                delete: true,
            }).pipe(
                catchError(err => {
                    console.log('assign error', template, err);

                    this.notification.show({
                        type: 'alert',
                        message: 'template.assign_to_folder_error',
                        delay: 10_000,
                        translationParams: {
                            templateName: template.name,
                            folderName: folder.name,
                            errorMessage: err.message,
                        },
                    });

                    return throwError(err);
                }),
            );
        })).pipe(
            discard(() => {
                this.selectedIds.add(folder.id);
                this.selectedIds.add(folder.globalId);
            }),
        );
    }

    unlinkFolder(folder: Folder, recursive: boolean): Observable<void> {
        return combineLatest(this.templates.map(template => {
            return this.templateOperations.unlinkFolders(template.id, {
                folderIds: [folder.id],
                nodeId: this.nodeId,
                recursive,
                delete: true,
            }).pipe(
                catchError(err => {
                    console.log('unassign error', template, err);

                    this.notification.show({
                        type: 'alert',
                        message: 'template.unassign_from_folder_error',
                        delay: 10_000,
                        translationParams: {
                            templateName: template.name,
                            folderName: folder.name,
                            errorMessage: err.message,
                        },
                    });

                    return throwError(err);
                }),
            );
        })).pipe(
            discard(() => {
                this.selectedIds.delete(folder.id);
                this.selectedIds.delete(folder.globalId);
            }),
        );
    }
}
