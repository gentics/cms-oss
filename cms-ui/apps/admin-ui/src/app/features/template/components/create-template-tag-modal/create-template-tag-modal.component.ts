import { TemplateTagDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { I18nNotificationService } from '@gentics/cms-components';
import { TemplateTag } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { TemplateTagPropertiesMode } from '../template-tag-properties/template-tag-properties.component';

@Component({
    selector: 'gtx-create-template-tag-modal',
    templateUrl: './create-template-tag-modal.component.html',
    styleUrls: ['./create-template-tag-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CreateTemplateTagModalComponent extends BaseModal<TemplateTag> implements OnInit {

    constructor(
        private changeDetector: ChangeDetectorRef,
        private entityData: TemplateTagDataService,
        private notification: I18nNotificationService,
    ) {
        super();
    }

    readonly TemplateTagPropertiesMode = TemplateTagPropertiesMode;

    @Input()
    public nodeId: number;

    @Input()
    public templateId: string | number;

    @Input()
    public tag: TemplateTag;

    public form: UntypedFormControl;
    public loading = false;

    ngOnInit(): void {
        this.form = new UntypedFormControl({}, Validators.required);
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

        this.entityData.updateEntity(this.templateId, {
            template: {
                templateTags: {
                    [tag.name]: tag,
                },
            },
        }).subscribe(() => {
            this.notification.show({
                message: 'template_tag.tag_created',
                translationParams: {
                    name: tag.name,
                },
                type: 'success',
            });
            this.closeFn(tag);
        }, (err) => {
            this.notification.show({
                message: 'template_tag.tag_save_error',
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
