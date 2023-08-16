import { TagHandlerService } from '@admin-ui/mesh/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { Tag } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';
import { TagPropertiesMode } from '../tag-properties/tag-properties.component';

@Component({
    selector: 'gtx-mesh-tag-modal',
    templateUrl: './tag-modal.component.html',
    styleUrls: ['./tag-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagModal extends BaseModal<Tag> implements OnInit {

    public readonly TagPropertiesMode = TagPropertiesMode;

    @Input()
    public mode: TagPropertiesMode;

    @Input()
    public project: string;

    @Input()
    public family: string;

    @Input()
    public tag: Tag;

    public form: FormControl;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: TagHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormControl(this.tag || {}, createNestedControlValidator());
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === TagPropertiesMode.CREATE
            ? this.handler.create(this.project, this.family, val)
            : this.handler.update(this.project, this.family, this.tag.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}
