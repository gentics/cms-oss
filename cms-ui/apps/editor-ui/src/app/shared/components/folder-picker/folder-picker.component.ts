import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { Folder } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { RepositoryBrowserClient } from '../../providers';

@Component({
    selector: 'gtx-folder-picker',
    templateUrl: './folder-picker.component.html',
    styleUrls: ['./folder-picker.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(FolderPickerComponent)],
    standalone: false
})
export class FolderPickerComponent extends BaseFormElementComponent<number> {

    @Input()
    public placeholder = '';

    @Input()
    public clearable = true;

    public loadedFolder: Folder | null = null;
    public loading = false;

    private loader: Subscription;

    constructor(
        changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private repo: RepositoryBrowserClient,
    ) {
        super(changeDetector);
    }

    public pickNewFolder(): void {
        this.repo.openRepositoryBrowser({
            allowedSelection: 'folder',
            selectMultiple: false,
            startNode: this.loadedFolder?.nodeId,
            startFolder: this.loadedFolder?.motherId,
        }).then(folder => {
            this.loadedFolder = folder;
            this.triggerChange(this.loadedFolder?.id ?? 0);
            this.changeDetector.markForCheck();
        });
    }

    public clearValue(): void {
        this.loadedFolder = null;
        this.triggerChange(0);
    }

    protected loadFolderFromValue(): void {
        if (this.loader != null) {
            this.loader.unsubscribe();
        }

        if (this.value != null && this.value > 0) {
            this.loading = true;
            this.loader = this.client.folder.get(this.value).subscribe(res => {
                this.loadedFolder = res.folder;
                this.loading = false;
            });
            this.subscriptions.push(this.loader);
        } else {
            this.loadedFolder = null;
        }
    }

    protected onValueChange(): void {
        this.loadFolderFromValue();
    }
}
