import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { EditablePageProps, Language, Page, Raw, Template } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService, FolderActionsService } from '../../../state';

export enum TranslatePageModalActions {
    EDIT_PAGE = 'editPage',
    EDIT_PAGE_COMPARE_WITH_LANGUAGE = 'editPageCompareWithLanguage',
}

export interface TranslateResult {
    newPage: Page,
    action: TranslatePageModalActions,
}

@Component({
    selector: 'translate-page-modal',
    templateUrl: './translate-page-modal.tpl.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TranslatePageModal extends BaseModal<TranslateResult> implements OnInit, OnDestroy {

    public readonly TranslatePageModalActions = TranslatePageModalActions;

    // The following fields should be provided by the call to ModalService.fromComponent()
    @Input()
    public defaultProps: EditablePageProps = {};

    @Input()
    public languageName: string;

    @Input()
    public pageId: number;

    @Input()
    public nodeId: number;

    @Input()
    public folderId: number;

    public languages: Language[] = [];
    public templates: Template[] = [];

    public loading = false;
    public control: FormControl<EditablePageProps>;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private entityResolver: EntityResolver,
        private appState: ApplicationStateService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.control = new FormControl(this.defaultProps);

        this.subscriptions.push(this.appState.select(state => state.folder.templates.list).subscribe(templateIds => {
            this.templates = templateIds.map(id => this.entityResolver.getTemplate(id));
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.folder.activeNodeLanguages.list).subscribe(languageIds => {
            this.languages = languageIds.map(id => this.entityResolver.getLanguage(id));
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.folder.folders.creating).subscribe(loading => {
            this.loading = loading;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    handleConfirm(action: TranslatePageModalActions): void {
        this.createPageTranslation()
            .then((newPage: Page<Raw>) => this.closeFn({ newPage, action }));
    }

    /**
     * Create page translation
     */
    private async createPageTranslation(): Promise<Page<Raw> | void> {
        const value = this.control.value;
        const languageCode = this.defaultProps.language;
        const newPageProps: EditablePageProps = {
            name: value.name,
            fileName: value.fileName,
            description: value.description,
            templateId: value.templateId,
            priority: value.priority,
        };

        return this.folderActions.createPageTranslation(this.nodeId, this.pageId, languageCode)
            .then(page => {
                if (page) {
                    this.folderActions.updatePageProperties(page.id, newPageProps, { showNotification: true, fetchForUpdate: false });
                    this.folderActions.refreshList(page.type);
                }
                return page;
            })
            .catch(err => this.cancelFn(err));
    }
}
