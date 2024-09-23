import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { BaseModal } from '@gentics/ui-core';
import { RichContent, RichContentType } from '../../../common/models';

@Component({
    selector: 'gtx-rich-content-modal',
    templateUrl: './rich-content-modal.component.html',
    styleUrls: ['./rich-content-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RichContentModal extends BaseModal<RichContent> implements OnInit {

    public readonly RichContentType = RichContentType;

    @Input({ required: true })
    public type: RichContentType;

    @Input({ required: true})
    public content: RichContent;

    @Input()
    public enterLinkDisplayText = false;

    public control: FormControl<RichContent>;

    public ngOnInit(): void {
        this.control = new FormControl(this.content, Validators.required);
    }

    public closeWithValue(): void {
        if (!this.control?.valid) {
            return;
        }

        this.closeFn(this.control.value);
    }
}
