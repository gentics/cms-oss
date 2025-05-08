import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { EditablePageProps, Language, NodeFeature, Page, Raw, ResponseCode, Template } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService, FolderActionsService, TranslateRequestFunction } from '../../../state';
import { TranslationActionsService } from '../../providers';

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
    standalone: false
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

    public autoTranslationEnabled = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private translationService: TranslationActionsService,
        private notificationService: I18nNotification,
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

        this.subscriptions.push(this.appState.select(state => state.features.nodeFeatures).subscribe(nodeFeatures => {
            this.autoTranslationEnabled = ((nodeFeatures || {})[this.nodeId] || []).includes(NodeFeature.AUTOMATIC_TRANSLATION);
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

    createAutomaticallyTranslatedPage(): void {
        this.createTranslationWithFunction((pageId, options) =>
            this.translationService.translatePage(pageId, options).then(result => {
                if (result?.page) {
                    return result.page
                }
                if(result?.responseInfo.responseCode === ResponseCode.OK) {
                    this.notificationService.show({message: result.messages[0]?.message});
                    this.cancelFn();
                    return;
                }
            }),
        ).then((newPage: Page<Raw>) => {
            if (newPage) {
                this.closeFn({ newPage, action: TranslatePageModalActions.EDIT_PAGE })
            }
        }).catch(error=> {
            console.log(error)
        })
    }

    /**
     * Create page translation with the provided translation function.
     */
    private async createPageTranslation(): Promise<Page<Raw> | void> {
        return this.createTranslationWithFunction((pageId, options) => this.folderActions.createPageTranslation(pageId, options));
    }

    private createTranslationWithFunction(translationFunction: TranslateRequestFunction): Promise<Page<Raw> | void> {
        return this.folderActions.executePageTranslationFunction(this.nodeId, this.pageId, this.defaultProps.language, translationFunction)
            .then(page => {
                if (page) {
                    this.folderActions.updatePageProperties(page.id, this.control.value, { showNotification: true, fetchForUpdate: false });
                    this.folderActions.refreshList(page.type);

                    return page;
                }
            }).catch(err => this.cancelFn(err));
    }
}
