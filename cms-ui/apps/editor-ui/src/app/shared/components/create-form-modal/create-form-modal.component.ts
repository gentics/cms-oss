import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { I18nNotificationService } from '@gentics/cms-components';
import { EditableFormProperties, Form, FormCreateRequest, IndexById, Language } from '@gentics/cms-models';
import { BaseModal, setEnabled } from '@gentics/ui-core';
import { Subscription, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { v4 as uuidV4 } from 'uuid';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { FormPropertiesMode, FormPropertiesData } from '../form-properties/form-properties.component';

@Component({
    selector: 'create-form-modal',
    templateUrl: './create-form-modal.component.html',
    styleUrls: ['./create-form-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CreateFormModalComponent
    extends BaseModal<Form>
    implements OnInit, OnDestroy {

    public readonly FormPropertiesMode = FormPropertiesMode;

    @Input()
    public defaultProps: Partial<EditableFormProperties> = {};

    // Will be loaded from state
    public folderId: number;
    public nodeId: number;

    public languages: Language[] = [];
    public activeLanguage: Language = null;

    public loading = false;
    public control: FormControl<FormPropertiesData>;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private appState: ApplicationStateService,
        private notification: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.control = new FormControl<FormPropertiesData>(this.defaultProps as any);

        const folderState = this.appState.now.folder;

        this.folderId = folderState.activeFolder;
        this.nodeId = folderState.activeNode;

        this.subscriptions.push(this.appState.select((state) => state.folder.forms.creating).subscribe((loading) => {
            this.loading = loading;
            setEnabled(this.control, !loading);
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.appState.select((state) => state.entities.language),
            this.appState.select((state) => state.folder.activeNodeLanguages.list),
        ]).pipe(
            map(([indexedLanguages, activeNodeLanguagesIds]: [IndexById<Language>, number[]]) => {
                return activeNodeLanguagesIds.map((id) => indexedLanguages[id]);
            }),
        ).subscribe((languages) => {
            this.languages = languages;
            this.activeLanguage = languages.find((lang) => lang.id === folderState.activeLanguage);
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }

    saveChanges(): void {
        if (!this.control.valid) {
            return;
        }

        const id = uuidV4();

        const form: FormCreateRequest = {
            ...this.control.value,
            nodeId: this.nodeId,
            folderId: this.folderId,
            languages: this.control.value.languages || [],
            schema: {
                key: id,
                version: '1.0',
                properties: {},
            },
            uiSchema: {
                key: id,
                version: '1.0',
                formGrid: {
                    // TODO: Define flows
                    flow: '',
                    width: 12,
                    widthOptimized: false,
                },
                pages: [{
                    pagename: {
                        de: 'Standart Seite',
                        en: 'Default Page',
                    },
                    elements: [],
                }],
            },
        };

        // Default to the active language if none was selected for some reason
        if (form.languages.length === 0 && this.activeLanguage != null) {
            form.languages.push(this.activeLanguage.code);
        }

        if (form.languages.length === 0) {
            this.notification.show({
                type: 'alert',
                message: 'message.form_missing_language',
            });
            return;
        }

        this.folderActions.createNewForm(form).then((form) => {
            if (form) {
                this.closeFn(form);
            }
        });
    }
}
