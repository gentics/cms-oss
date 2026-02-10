import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { PagePropertiesMode } from '@editor-ui/app/shared/components';
import { EditablePageProps, Language, Page, Raw, Template } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService, FolderActionsService } from '../../../state';

@Component({
    selector: 'create-page-modal',
    templateUrl: './create-page-modal.component.html',
    styleUrls: ['./create-page-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreatePageModalComponent extends BaseModal<Page<Raw>> implements OnInit, OnDestroy {

    public readonly PagePropertiesMode = PagePropertiesMode;

    // Will be loaded from the app-state in init
    public templates: Template[];
    public folderId: number;
    public nodeId: number;
    public languages: Language[];

    public control: FormControl<EditablePageProps>;
    public loading = false;

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
        const languages = this.appState.now.entities.language;

        this.control = new FormControl({
            language: languages[this.appState.now.folder.activeLanguage]?.code,
        });

        this.subscriptions.push(this.appState.select(state => state.folder.pages.creating).subscribe(loading => {
            this.loading = loading;
            this.changeDetector.markForCheck();
        }));

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

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    saveChanges(): void {
        const { name, ...value } = this.control.value;

        this.folderActions.createNewPage({
            ...value,
            pageName: name,

            folderId: this.folderId,
            nodeId: this.nodeId,
        } as any)
            .then(page => {
                if (page) {
                    this.closeFn(page);
                }
            });
    }
}
