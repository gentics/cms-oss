import { ConstructHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormGroup, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { Raw, TagEditorChange, TagType, TemplateTag } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { environment } from '../../../../../environments/environment';

export enum TemplateTagPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

@Component({
    selector: 'gtx-template-tag-properties',
    templateUrl: './template-tag-properties.component.html',
    styleUrls: ['./template-tag-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(TemplateTagPropertiesComponent)],
})
export class TemplateTagPropertiesComponent extends BasePropertiesComponent<TemplateTag> implements OnInit {

    readonly TemplateTagPropertiesMode = TemplateTagPropertiesMode;

    @Input()
    public mode: TemplateTagPropertiesMode = TemplateTagPropertiesMode.UPDATE;

    @Input()
    public nodeId: number;

    @Input()
    public templateId: string | number;

    @Input()
    public tagName: string;

    public form: UntypedFormGroup;
    public constructs: TagType<Raw>[] = [];
    public tagEditorBaseUrl: URL;

    private propertiesAreValid = true;

    constructor(
        changeDectector: ChangeDetectorRef,
        private constructHandler: ConstructHandlerService,
    ) {
        super(changeDectector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.constructHandler.listMapped().subscribe(constructs => {
            this.constructs = constructs.items;
        }));

        if (!environment.production) {
            this.tagEditorBaseUrl = new URL(environment.editorUrl, window.location.toString());
        }
    }

    protected createForm(): FormGroup<any> {
        return new UntypedFormGroup({
            name: new UntypedFormControl('', Validators.required),
            constructId: new UntypedFormControl(null, Validators.required),
            active: new UntypedFormControl(true),
            editableInPage: new UntypedFormControl(true),
            mandatory: new UntypedFormControl(false),
            properties: new UntypedFormControl({}, () => {
                if (this.propertiesAreValid) {
                    return null;
                }
                return { properties: 'invalid' };
            }),
        });
    }

    protected configureForm(value: TemplateTag, loud?: boolean): void {
        if (this.mode !== TemplateTagPropertiesMode.CREATE) {
            this.form.get('name').disable({ emitEvent: false });
        }
    }

    protected assembleValue(value: TemplateTag): TemplateTag {
        return value;
    }

    onTagEditorChange(change: TagEditorChange): void {
        // Seems to be a change for a different tag
        if (change.tagName !== this.tagName
            || change.entityId !== this.templateId
            || change.entityType !== 'template'
            || change.nodeId !== this.nodeId
        ) {
            return;
        }

        const ctrl = this.form.get('properties');
        ctrl.setValue(change.tag.properties ?? {});
        this.propertiesAreValid = change.valid;
        if (change.modified) {
            ctrl.markAsDirty();
        } else {
            ctrl.markAsPristine();
        }
    }
}
