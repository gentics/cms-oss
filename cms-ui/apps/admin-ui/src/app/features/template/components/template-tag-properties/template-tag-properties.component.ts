import { ConstructDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { Raw, TagTypeBO, TagEditorChange } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
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
export class TemplateTagPropertiesComponent implements OnInit, OnDestroy, ControlValueAccessor {

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
    public constructs$: Observable<TagTypeBO<Raw>[]>;
    public tagEditorBaseUrl: URL;

    private subscriptions = new Subscription();
    private propertiesAreValid = true;

    constructor(
        private constructData: ConstructDataService,
    ) { }

    ngOnInit(): void {
        this.constructs$ = this.constructData.watchAllEntities();
        if (!environment.production) {
            this.tagEditorBaseUrl = new URL(environment.editorUrl, window.location.toString());
        }

        this.form = new UntypedFormGroup({
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

        if (this.mode !== TemplateTagPropertiesMode.CREATE) {
            this.form.get('name').disable({ emitEvent: false });
        }

        this.subscriptions.add(this.form.valueChanges.subscribe(value => {
            this.changeFn(this.form.valid ? value : null);
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    writeValue(obj: any): void {
        this.form.patchValue(obj || {});
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

    // Noop functions
    changeFn: (value?: any) => void = () => { };
    touchFn: () => void = () => { };

    registerOnChange(fn: any): void {
        this.changeFn = fn;
    }

    registerOnTouched(fn: any): void {
        this.touchFn = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            if (!this.form.disabled) {
                this.form.disable({ emitEvent: false });
            }
        } else if (this.form.disabled) {
            this.form.enable({ emitEvent: false });
        }
    }

}
