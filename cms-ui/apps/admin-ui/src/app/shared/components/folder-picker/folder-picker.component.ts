import { BO_NODE_ID, PickableEntity } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { ContentItemTrableLoaderService } from '../../providers/content-item-trable-loader/content-item-trable-loader.service';

@Component({
    selector: 'gtx-folder-picker',
    templateUrl: './folder-picker.component.html',
    styleUrls: ['./folder-picker.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(FolderPickerComponent),
    ],
})
export class FolderPickerComponent extends BaseFormElementComponent<number> {

    @Input()
    public clearable = true;

    @Input()
    public allowNodes = true;

    public pickedFolder: PickableEntity;

    public loading = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected client: GCMSRestClientService,
        protected loader: ContentItemTrableLoaderService,
    ) {
        super(changeDetector);
    }

    protected onValueChange(): void {
        if (!this.value || this.value <= 0) {
            this.pickedFolder = null;
            return;
        }

        // Folder already loaded
        if (this.pickedFolder != null && this.pickedFolder.entity?.id === this.value) {
            return;
        }

        this.loading = true;
        this.changeDetector.markForCheck();

        this.subscriptions.push(this.client.folder.get(this.value).subscribe(res => {
            const folder = this.loader.mapToBusinessObject(res.folder, null);
            this.pickedFolder = {
                entity: folder,
                nodeId: folder[BO_NODE_ID],
                type: 'folder',
            };
            this.loading = false;
            this.changeDetector.markForCheck();
        }));
    }

    public handleItemChange(item: PickableEntity): void {
        this.pickedFolder = item;
        this.triggerChange(this.pickedFolder?.entity?.id ?? 0);
    }
}
