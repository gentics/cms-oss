import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { EditablePageProps, Language, Page, Raw, Template } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { PagePropertiesForm } from '../../../shared/components/page-properties-form/page-properties-form.component';
import { ApplicationStateService, FolderActionsService } from '../../../state';

export enum TranslatePageModalActions {
    EditPage = 'editPage',
    EditPageCompareWithLanguage = 'editPageCompareWithLanguage',
}

@Component({
    selector: 'translate-page-modal',
    templateUrl: './translate-page-modal.tpl.html'
    })
export class TranslatePageModal implements IModalDialog, AfterViewInit {

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

    constructor(
        private folderActions: FolderActionsService,
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
    }

    ngAfterViewInit(): void {
        setTimeout(() => this.form = this.pagePropertiesForm.form);
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
        this._createTranslation()
            .then((newPage: Page<Raw>) => this.closeFn({ newPage, action: 'editPage' }));
    }

    /**
     * User chooses to translate and open translated page afterwards
     */
    createTranslationAndEditTranslatedPage(): void {
        this._createTranslation()
            .then((newPage: Page<Raw>) => this.closeFn({ newPage, action: 'editPageCompareWithLanguage' }));
    }

    /**
     * Create page translation
     */
    private async _createTranslation(): Promise<Page<Raw> | void> {
        const formValue = this.form.value as {
            pageName: string,
            fileName: string,
            description: string,
            templateId: number | null,
            language: string | null,
            priority: number
        };
        const languageCode = this.defaultProps.language;
        const newPageProps: EditablePageProps = {
            pageName: formValue.pageName,
            fileName: formValue.fileName,
            description: formValue.description,
            templateId: formValue.templateId,
            priority: formValue.priority,
        };
        return this.folderActions.createPageTranslation(this.nodeId, this.pageId, languageCode)
            .then(page => {
                if (page) {
                    this.folderActions.updatePageProperties(page.id, newPageProps, { showNotification: true, fetchForUpdate: false });
                    this.folderActions.refreshList(page.type);
                }
                return page;
            });
    }
}
