import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { EditableFormProps, Form, FormCreateRequest, IndexById, Language } from '@gentics/cms-models';
import { BaseModal, setEnabled } from '@gentics/ui-core';
import { Subscription, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { FormPropertiesMode } from '../../../shared/components/form-properties/form-properties.component';
import { ApplicationStateService, FolderActionsService } from '../../../state';

@Component({
    selector: 'create-form-modal',
    templateUrl: './create-form-modal.component.html',
    styleUrls: ['./create-form-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateFormModalComponent
    extends BaseModal<Form>
    implements OnInit, OnDestroy {

    public readonly FormPropertiesMode = FormPropertiesMode;

    @Input()
    public defaultProps: EditableFormProps = {};

    // Will be loaded from state
    public folderId: number;
    public nodeId: number;

    public languages: Language[] = [];
    public activeLanguage: Language = null;

    public loading = false;
    public control: FormControl<EditableFormProps>;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private appState: ApplicationStateService,
        private notification: I18nNotification,
    ) {
        super();
    }

    ngOnInit(): void {
        this.control = new FormControl(this.defaultProps, Validators.required);

        const folderState = this.appState.now.folder;

        this.folderId = folderState.activeFolder;
        this.nodeId = folderState.activeNode;

        this.subscriptions.push(this.appState.select(state => state.folder.forms.creating).subscribe(loading => {
            this.loading = loading;
            setEnabled(this.control, !loading);
            this.changeDetector.markForCheck();
        }))

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.entities.language),
            this.appState.select(state => state.folder.activeNodeLanguages.list),
        ]).pipe(
            map(([indexedLanguages, activeNodeLanguagesIds]: [IndexById<Language>, number[]]) => {
                return activeNodeLanguagesIds.map(id => indexedLanguages[id]);
            }),
        ).subscribe(languages => {
            this.languages = languages;
            this.activeLanguage = languages.find(lang => lang.id ===  folderState.activeLanguage);
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    saveChanges(): void {
        if (!this.control.valid) {
            return;
        }

        if (this.languages?.length === 0) {
            this.notification.show({
                type: 'alert',
                message: 'message.form_missing_language',
            });
            return;
        }

        const form: FormCreateRequest = {
            ...this.control.value,
            folderId: this.folderId,
            languages: this.control.value.languages || [],
        };

        if (this.languages?.length > 0) {
            form.languages = [this.activeLanguage.code];
        }

        this.folderActions.createNewForm(form).then(form => {
            if (form) {
                this.closeFn(form);
            }
        });

    }
}
