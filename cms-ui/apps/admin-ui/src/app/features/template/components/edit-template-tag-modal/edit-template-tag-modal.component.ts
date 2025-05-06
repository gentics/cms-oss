import { I18nNotificationService } from '@admin-ui/core';
import { TemplateTagDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { BaseModal } from '@gentics/ui-core';
import { TemplateTag } from '@gentics/cms-models';
import { TemplateTagPropertiesMode } from '../template-tag-properties/template-tag-properties.component';

@Component({
    selector: 'gtx-edit-template-tag-modal',
    templateUrl: './edit-template-tag-modal.component.html',
    styleUrls: ['./edit-template-tag-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditTemplateTagModalComponent extends BaseModal<TemplateTag> implements OnInit {

    readonly TemplateTagPropertiesMode = TemplateTagPropertiesMode;

    @Input()
    public nodeId: number;

    @Input()
    public templateId: string | number;

    @Input()
    public tag: TemplateTag;

    public form: UntypedFormControl;
    public loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private entityData: TemplateTagDataService,
        private notification: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new UntypedFormControl({
            name: this.tag.name,
            constructId: this.tag.constructId,
            active: this.tag.active ?? true,
            editableInPage: this.tag.editableInPage ?? true,
            mandatory: this.tag.mandatory ?? false,
            properties: this.tag.properties ?? {},
        }, Validators.required);
    }

    okayClicked(): void {
        if (this.loading || this.form.invalid || this.form.pristine) {
            return;
        }

        this.loading = true;
        this.form.disable({ emitEvent: false });
        const tag: TemplateTag = {
            ...this.form.value,
        };

        // Since the control is disabled, the name isn't included in the form-value
        tag.name = this.tag.name;

        this.entityData.updateEntity(this.templateId, {
            template: {
                templateTags: {
                    [tag.name]: tag,
                },
            },
        }).subscribe(() => {
            this.notification.show({
                message: 'templateTag.tag_saved',
                translationParams: {
                    name: tag.name,
                },
                type: 'success',
            });
            this.closeFn(tag);
        }, err => {
            this.notification.show({
                message: 'templateTag.tag_save_error',
                translationParams: {
                    name: tag.name,
                },
                type: 'alert',
            });
            this.loading = false;
            this.form.enable({ emitEvent: false });
            this.changeDetector.markForCheck();
            this.errorFn(err);
        });
    }
}
