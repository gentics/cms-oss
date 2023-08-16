import { TagFamilyHandlerService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { TagFamily } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';
import { TagFamilyPropertiesMode } from '../tag-family-properties/tag-family-properties.component';

@Component({
    selector: 'gtx-mesh-tag-family-modal',
    templateUrl: './tag-family-modal.component.html',
    styleUrls: ['./tag-family-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagFamilyModal extends BaseModal<TagFamily> implements OnInit {

    public readonly TagFamilyPropertiesMode = TagFamilyPropertiesMode;

    @Input()
    public mode: TagFamilyPropertiesMode;

    @Input()
    public project: string;

    @Input()
    public family: TagFamily;

    public form: FormControl;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: TagFamilyHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormControl(this.family || {}, createNestedControlValidator());
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === TagFamilyPropertiesMode.CREATE
            ? this.handler.create(this.project, val)
            : this.handler.update(this.project, this.family.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}
