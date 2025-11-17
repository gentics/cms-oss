import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { Folder, Node, Raw, Template } from '@gentics/cms-models';
import { IModalDialog, ModalService } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { LinkTemplateService } from '../../providers/link-template/link-template.service';

enum LinkMode {
    CANCEL,
    APPLY,
    APPEND,
}

@Component({
    selector: 'link-template-modal',
    templateUrl: './link-template-modal.component.html',
    styleUrls: ['./link-template-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class LinkTemplateModal implements IModalDialog, OnDestroy, OnInit {

    nodeId: number;
    node$: Observable<Node>;
    folderId: number;
    folder$: Observable<Folder>;
    currentFolderTemplates$: Observable<Template<Raw>[]>;
    currentFolderTemplates: Template<Raw>[] = [];

    /**
     * Emission from ViewChild
     * Those templates are the ones the user wants to be linked; all others shall be unlinked.
     */
    newFolderTemplates: Template<Raw>[] = [];

    /**
     * Emission from ViewChild
     * This search term limits the amount of total node templates and needs to
     * be provided in method LinkTemplateService.setTemplatesOfFolder!
     */
    searchTerm: string;

    /**
     * Emission from ViewChild
     * TRUE if child is loading ressources
     */
    isInProgressExternal$ = new BehaviorSubject<boolean>(true);
    /** TRUE if component is proceeding/waiting for responses */
    isInProgressInternal$ = new BehaviorSubject<boolean>(false);

    private destroy$ = new Subject<void>();

    constructor(
        private appState: ApplicationStateService,
        private linkTemplate: LinkTemplateService,
        private folderActions: FolderActionsService,
        private modals: ModalService,
        private i18n: I18nService,
    ) { }

    ngOnInit(): void {
        this.node$ = this.appState.select((state) => state.entities.node[this.nodeId]);
        this.folder$ = this.appState.select((state) => state.entities.folder[this.folderId]);

        this.currentFolderTemplates$ = this.folderActions.getTemplatesRaw(this.nodeId, this.folderId, true, '').pipe(
            filter((templates: Template<Raw>[]) => Array.isArray(templates)),
        );
        this.currentFolderTemplates$.pipe(
            takeUntil(this.destroy$),
        ).subscribe((currentFolderTemplates) => this.currentFolderTemplates = currentFolderTemplates);
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    closeFn = (newFolderTemplates: Template<Raw>[]) => {};
    cancelFn = () => {};

    registerCloseFn(close: (newFolderTemplates: Template<Raw>[]) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    async okBtnPressed(recursive: boolean): Promise<void> {
        const mode = await this.openModeSelectModal(recursive);

        switch (mode) {
            case LinkMode.APPEND:
                await this.appendTemplates(recursive);
                return;

            case LinkMode.APPLY:
                await this.applyTemplates(recursive);
                return;
        }
    }

    async openModeSelectModal(recursive: boolean): Promise<LinkMode> {
        const labelAppend = this.i18n.instant('template.link_mode_append');
        const labelApply = this.i18n.instant('template.link_mode_apply');

        const dialog = await this.modals.dialog({
            title: this.i18n.instant('modal.link_template_title'),
            body: this.i18n.instant('modal.link_template_body', {
                labelAppend,
                labelApply,
            }),
            buttons: [
                {
                    label: this.i18n.instant('common.cancel_button'),
                    returnValue: LinkMode.CANCEL,
                    type: 'secondary',
                },
                {
                    label: labelAppend,
                    returnValue: LinkMode.APPEND,
                },
                {
                    label: labelApply,
                    returnValue: LinkMode.APPLY,
                    type: 'warning',
                },
            ],
        });

        return await dialog.open();
    }

    private async appendTemplates(recursive: boolean): Promise<void> {
        this.isInProgressInternal$.next(true);

        const result = await this.linkTemplate.addTemplatesToFolder(
            this.nodeId,
            this.folderId,
            this.newFolderTemplates.map((template) => template.id),
            recursive,
        ).toPromise();

        // refresh templates in state
        await this.folderActions.getTemplates(this.folderId);

        this.isInProgressInternal$.next(false);

        if (result) {
            return this.closeFn(this.newFolderTemplates);
        }
    }

    private async applyTemplates(recursive: boolean): Promise<void> {
        this.isInProgressInternal$.next(true);

        const result = await this.linkTemplate.changeTemplatesOfFolder(
            this.nodeId,
            this.folderId,
            this.newFolderTemplates.map((template) => template.id),
            recursive,
            this.searchTerm,
        ).toPromise();

        this.isInProgressInternal$.next(false);

        if (result) {
            return this.closeFn(this.newFolderTemplates);
        }
    }

}
