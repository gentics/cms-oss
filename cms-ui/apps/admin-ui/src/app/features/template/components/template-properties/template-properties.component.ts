import { MarkupLanguageDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { TagEditorChange } from '@gentics/cms-integration-api-models';
import { IndexById, MarkupLanguage, Node, Raw, TemplateBO } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

export enum TemplatePropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

@Component({
    selector: 'gtx-template-properties',
    templateUrl: './template-properties.component.html',
    styleUrls: ['./template-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(TemplatePropertiesComponent),
        generateValidatorProvider(TemplatePropertiesComponent),
    ],
})
export class TemplatePropertiesComponent extends BasePropertiesComponent<TemplateBO> implements OnInit {

    public readonly GENERAL_TAB = 'general';
    public readonly TemplatePropertiesMode = TemplatePropertiesMode;

    @Input()
    public mode: TemplatePropertiesMode = TemplatePropertiesMode.UPDATE;

    @Input()
    public node: Node;

    public markupLanguages: IndexById<MarkupLanguage<Raw>> = {};
    public activePropertiesTab: string = this.GENERAL_TAB;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected markupData: MarkupLanguageDataService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.markupData.watchAllEntities().subscribe(values => {
            this.markupLanguages = {};
            for (const ml of values) {
                this.markupLanguages[`${ml.id}`] = ml;
            }
            this.changeDetector.markForCheck();
        }));
    }

    public tagsChanged(event: TagEditorChange): void {
        // Ignore all events which are not for the current template
        if (!event
            || event.entityType !== 'template'
            || `${event.entityId}` !== `${this.value?.id || ''}`
            || event.nodeId !== this.node?.id
        ) {
            return;
        }

        let fieldName: string;

        switch (event.tag.type) {
            case 'OBJECTTAG':
                fieldName = 'objectTags';
                break;
            case 'TEMPLATETAG':
                fieldName = 'templateTags';
                break;
            default:
                // Can't have content-tags, and other fields are invalid
                return;
        }

        const ctl = this.form.get(fieldName);
        ctl.setValue({
            ...ctl.value,
            [event.tagName]: event.tag,
        }, { emitEvent: false, onlySelf: true });

        if (event.modified) {
            ctl.markAsDirty({ onlySelf: false });
        } else {
            ctl.markAsPristine({ onlySelf: false });
        }

        ctl.setValidators(() => event.valid ? null : { 'tag-invalid': event.tagName });
        ctl.updateValueAndValidity();
    }

    protected createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            id: new UntypedFormControl(this.safeValue('id') || null),
            name: new UntypedFormControl(this.safeValue('name') || '', [Validators.required, Validators.maxLength(255)]),
            description: new UntypedFormControl(this.safeValue('description') || ''),
            markupLanguage: new UntypedFormControl(`${(this.safeValue('markupLanguage')?.id || '')}`, Validators.required),
            source: new UntypedFormControl(this.safeValue('source') || ''),
            objectTags: new UntypedFormControl(this.safeValue('objectTags') || {}),
            templateTags: new UntypedFormControl(this.safeValue('templateTags') || {}),
        });
    }

    protected configureForm(value: any, loud?: boolean): void {
        // Nothing to do
    }

    protected assembleValue(value: any): TemplateBO {
        return {
            ...value,
            markupLanguage: this.markupLanguages[value.markupLanguage],
        };
    }

    protected onValueChange(): void {
        if (this.form) {
            this.form.patchValue({
                id: this.value?.id || null,
                name: this.value?.name || '',
                description: this.value?.description || '',
                markupLanguage: `${(this.value?.markupLanguage?.id || '')}` as any,
                source: this.value?.source || '',
                objectTags: this.value?.objectTags || {},
                templateTags: this.value?.templateTags || {},
            });
        }
    }

}
