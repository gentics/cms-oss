import { AfterViewInit, ChangeDetectionStrategy, Component, Input, ViewChild } from '@angular/core';
import { ModalCloseError, TagEditorContext, TagEditorResult } from '@gentics/cms-integration-api-models';
import { BaseModal } from '@gentics/ui-core';
import { TagEditorHostComponent } from '../tag-editor-host/tag-editor-host.component';

@Component({
    selector: 'gtx-tag-editor-modal',
    templateUrl: './tag-editor-modal.component.html',
    styleUrls: ['./tag-editor-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagEditorModal extends BaseModal<TagEditorResult> implements AfterViewInit {

    @Input()
    public context: TagEditorContext;

    @ViewChild(TagEditorHostComponent)
    public host: TagEditorHostComponent;

    ngAfterViewInit(): void {
        if (this.host == null) {
            return;
        }
        this.initEditor();
    }

    protected initEditor(): Promise<void> {
        return this.host.editTag(this.context.editedTag, this.context).then(result => {
            this.closeFn(result);
        }).catch(reason => {
            const err = new ModalCloseError(reason);
            this.cancelFn(null, err.reason);
        });
    }
}
