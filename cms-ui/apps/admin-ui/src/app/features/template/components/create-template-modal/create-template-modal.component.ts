import { I18nNotificationService } from '@admin-ui/core';
import { TemplateDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { Node, Raw, TemplateBO, TemplateCreateRequest } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-template-modal',
    templateUrl: './create-template-modal.component.html',
    styleUrls: ['./create-template-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateTemplateModalComponent extends BaseModal<TemplateBO<Raw>> implements OnInit {

    constructor(
        private changeDetector: ChangeDetectorRef,
        private entityData: TemplateDataService,
        private notification: I18nNotificationService,
    ) {
        super();
    }

    @Input()
    public node: Node<Raw>;

    public form: UntypedFormControl;
    public loading = false;

    ngOnInit(): void {
        this.form = new UntypedFormControl({});
    }

    okayClicked(): void {
        if (this.loading || this.form.disabled || this.form.invalid) {
            return;
        }

        this.loading = true;
        this.form.disable({ emitEvent: false });
        this.changeDetector.markForCheck();

        const req: TemplateCreateRequest = {
            folderIds: [
                this.node.folderId,
            ],
            nodeId: this.node.id,
            template: this.form.value,
        };

        this.entityData.create(req).subscribe(template => {
            this.closeFn(template);
        }, err => {
            this.notification.show({
                message: 'template.create_error',
                translationParams: {
                    name: req.template?.name,
                },
                type: 'alert',
            });
            this.errorFn(err);
        }, () => {
            this.loading = false;
            this.form.enable({ emitEvent: false });
            this.changeDetector.markForCheck();
        });
    }
}
