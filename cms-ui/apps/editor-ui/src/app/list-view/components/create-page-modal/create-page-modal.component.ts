import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { EditablePageProps, Language, Page, Raw, Template } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { Observable, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { PagePropertiesForm } from '../../../shared/components/page-properties-form/page-properties-form.component';
import { ApplicationStateService, FolderActionsService } from '../../../state';

@Component({
    selector: 'create-page-modal',
    templateUrl: './create-page-modal.component.html',
    styleUrls: ['./create-page-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreatePageModalComponent extends BaseModal<Page<Raw>> implements OnInit, AfterViewInit, OnDestroy {

    // Why? FIXME: Use proper input/output or forms (BasePropertiesComponent) for this.
    @ViewChild(PagePropertiesForm, { static: true })
    pagePropertiesForm: PagePropertiesForm;

    creating$: Observable<boolean>;

    defaultProps: EditablePageProps = {};
    languages: Language[];
    templates: Template[];
    form: UntypedFormGroup;
    folderId: number;
    nodeId: number;

    protected subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private entityResolver: EntityResolver,
        private appState: ApplicationStateService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.creating$ = this.appState.select(state => state.folder.pages.creating);

        const folderState = this.appState.now.folder;

        this.folderId = folderState.activeFolder;
        this.nodeId = folderState.activeNode;

        this.languages = folderState.activeNodeLanguages.list
            .map(langId => this.entityResolver.getLanguage(langId));

        this.subscriptions.push(this.appState.select(state => state.folder.templates.list).pipe(
            distinctUntilChanged(isEqual),
            debounceTime(50),
            map((ids: number[]) => ids.map(id => this.entityResolver.getTemplate(id))),
        ).subscribe(templates => {
            this.templates = templates;
            this.changeDetector.markForCheck();
        }));
    }

    ngAfterViewInit(): void {
        this.form = this.pagePropertiesForm.form;
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
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
}
