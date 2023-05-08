import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { EditablePageProps, Language, Page, Template } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { PagePropertiesForm } from '../../../shared/components/page-properties-form/page-properties-form.component';
import { ApplicationStateService, FolderActionsService } from '../../../state';

@Component({
    selector: 'create-page-modal',
    templateUrl: './create-page-modal.tpl.html',
    styleUrls: ['./create-page-modal.scss']
})
export class CreatePageModalComponent implements IModalDialog, OnInit, AfterViewInit {

    @ViewChild(PagePropertiesForm, { static: true })
    pagePropertiesForm: PagePropertiesForm;

    defaultProps: EditablePageProps = {};

    creating$: Observable<boolean>;
    languages: Language[];
    templates: Template[];
    form: UntypedFormGroup;
    folderId: number;
    nodeId: number;

    constructor(
        private folderActions: FolderActionsService,
        private entityResolver: EntityResolver,
        private appState: ApplicationStateService
    ) {
        this.creating$ = appState.select(state => state.folder.pages.creating);
    }

    ngOnInit(): void {
        const folderState = this.appState.now.folder;

        this.folderId = folderState.activeFolder;
        this.nodeId = folderState.activeNode;

        this.reloadTemplates();

        this.languages = folderState.activeNodeLanguages.list
            .map(langId => this.entityResolver.getLanguage(langId));
   }

    ngAfterViewInit(): void {
        this.form = this.pagePropertiesForm.form;
    }

    closeFn(page: Page): void { }
    cancelFn(): void { }

    registerCloseFn(close: (page: Page) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    saveChanges(): void {
        const formValue = this.form.value as {
            pageName: string,
            fileName: string,
            description: string,
            templateId: number | null,
            language: string | null,
            niceUrl: string,
            alternateUrls?: string[],
            priority: number,
        };

        const page = {
            pageName: formValue.pageName,
            fileName: formValue.fileName,
            description: formValue.description,
            language: formValue.language,
            priority: formValue.priority,
            niceUrl: formValue.niceUrl,
            alternateUrls: formValue.alternateUrls,
            nodeId: this.nodeId,
            folderId: this.folderId,
            templateId: formValue.templateId,
        };

        this.folderActions.createNewPage(page)
            .then(page => {
                if (page) {
                    this.closeFn(page);
                }
            });
    }

    reloadTemplates(): void {
        this.templates = this.appState.now.folder.templates.list
            .map(templateId => this.entityResolver.getTemplate(templateId));
    }
}
