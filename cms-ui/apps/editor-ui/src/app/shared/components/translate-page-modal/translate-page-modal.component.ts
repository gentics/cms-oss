import { AfterViewInit, Component, OnDestroy, ViewChild } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { EditablePageProps, Language, NodeFeature, Page, Raw, ResponseCode, Template } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { PagePropertiesForm } from '../../../shared/components/page-properties-form/page-properties-form.component';
import { ApplicationStateService, FolderActionsService, TranslateRequestFunction } from '../../../state';
import { TranslationActionsService } from '../../providers';

export enum TranslatePageModalActions {
    EditPage = 'editPage',
    EditPageCompareWithLanguage = 'editPageCompareWithLanguage',
}

export interface TranslationForm {
    pageName: string;
    fileName: string;
    description: string;
    templateId: number | null;
    language: string | null;
    priority: number;
}


@Component({
    selector: 'translate-page-modal',
    templateUrl: './translate-page-modal.tpl.html',
})
export class TranslatePageModal implements IModalDialog, AfterViewInit, OnDestroy {

    // The following fields should be provided by the call to ModalService.fromComponent()
    defaultProps: EditablePageProps = {};
    languageName: string;
    pageId: number;
    nodeId: number;
    folderId: number;

    @ViewChild(PagePropertiesForm, { static: true }) pagePropertiesForm: PagePropertiesForm;
    creating$: Observable<boolean>;
    languages$: Observable<Language[]>;
    templates$: Observable<Template[]>;
    form: UntypedFormGroup;

    translationEnabled = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private folderActions: FolderActionsService,
        private translationService: TranslationActionsService,
        private notificationService: I18nNotification,
        entityResolver: EntityResolver,
        appState: ApplicationStateService,
    ) {

        this.templates$ = appState.select(
            state => state.folder.templates.list.map(
                templateId => entityResolver.getTemplate(templateId)));

        this.languages$ = appState.select(
            state => state.folder.activeNodeLanguages.list.map(
                langId => entityResolver.getLanguage(langId)));

        this.creating$ = appState.select(state => state.folder.folders.creating);

        const nodeFeatures$ = appState.select(state => state.features.nodeFeatures);

        this.subscriptions.push(nodeFeatures$.subscribe(nodeFeatures => {
            this.translationEnabled = this.isTranslationFeatureEnabled(nodeFeatures)
        }));
    }

    ngAfterViewInit(): void {
        setTimeout(() => this.form = this.pagePropertiesForm.form);
    }


    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    closeFn(val: { newPage: Page<Raw>, action: string }): void { }
    cancelFn(val?: any): void {}

    registerCloseFn(close: (val: { newPage: Page<Raw>, action: string }) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

    /**
     * User chooses to translate and do nothing afterwards.
     */
    createTranslation(): void {
        this.createTranslationWithFunction((pageId, options)  => this.folderActions.createPageTranslation(pageId, options) )
            .then((newPage: Page<Raw>) => this.closeFn({ newPage, action: 'editPage' }));
    }

    /**
     * User chooses to translate and open translated page afterwards
     */
    createTranslationAndEditTranslatedPage(): void {
        this.createTranslationWithFunction((pageId, options) => this.folderActions.createPageTranslation(pageId, options ))
            .then((newPage: Page<Raw>) => this.closeFn({ newPage, action: 'editPageCompareWithLanguage' }));
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
                this.closeFn({ newPage, action: 'editPage' })
            }
        }).catch(error=> {
            console.log(error)
        })
    }


    /**
     * Create page translation with the provided translation function.
     */
    private createTranslationWithFunction(translationFunction: TranslateRequestFunction): Promise<Page<Raw> | void> {
        const languageCode = this.defaultProps.language;
        const newPageProps = this.getNewPageProperties();

        return this.folderActions.executePageTranslationFunction(this.nodeId, this.pageId, languageCode, translationFunction)
            .then(page => {
                if (page) {
                    this.folderActions.updatePageProperties(page.id, newPageProps, { showNotification: true, fetchForUpdate: false });
                    this.folderActions.refreshList(page.type);

                    return page;
                }
            }).catch(err => this.cancelFn(err));
    }

    private getNewPageProperties(): EditablePageProps {
        const formValue: TranslationForm = this.form.value;
        const newPageProps: EditablePageProps = {
            pageName: formValue.pageName,
            fileName: formValue.fileName,
            description: formValue.description,
            templateId: formValue.templateId,
            priority: formValue.priority,
        };

        return newPageProps;
    }

    private isTranslationFeatureEnabled(features: {[id: number]: NodeFeature[]}): boolean {
        for (const key in features) {
            if (features[key].includes(NodeFeature.AUTOMATIC_TRANSLATION)  ) {
                return true;
            }
        }
        return false;
    }

}
