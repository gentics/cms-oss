import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { BaseModal } from '@gentics/ui-core';
import { RichContent, RichContentLinkType, RichContentType } from '../../../common/models';

@Component({
    selector: 'gtx-rich-content-modal',
    templateUrl: './rich-content-modal.component.html',
    styleUrls: ['./rich-content-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RichContentModal extends BaseModal<RichContent> implements OnInit {

    public readonly RichContentType = RichContentType;

    @Input()
    public type: RichContentType;

    @Input()
    public content: RichContent;

    public control: FormControl<RichContent>;

    public ngOnInit(): void {
        this.control = new FormControl(this.content, Validators.required);
    }

    public closeWithValue(): void {
        this.closeFn({
            ...this.content,
            type: this.type,
            linkType: RichContentLinkType.PAGE,
            nodeId: 1337,
            itemId: 420,
        });
        // this.closeFn(this.control.value);
    }
}
