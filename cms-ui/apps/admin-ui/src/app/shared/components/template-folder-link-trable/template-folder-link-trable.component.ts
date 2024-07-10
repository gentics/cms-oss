import { BO_ID, FolderBO } from '@admin-ui/common';
import { FolderTrableLoaderService, I18nService } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Folder } from '@gentics/cms-models';
import { TableAction, TableActionClickEvent, TrableRow } from '@gentics/ui-core';

export interface FolderLinkEvent {
    folder: Folder;
    recursive: boolean;
}

const APPLY_RECURSIVE = 'applyRecusrive';

@Component({
    selector: 'gtx-template-folder-link-trable',
    templateUrl: './template-folder-link-trable.component.html',
    styleUrls: ['./template-folder-link-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TemplateFolderLinkTrableComponent implements OnInit {

    @Input()
    public disabled: boolean;

    @Input()
    public selected: string[] = [];

    @Output()
    public link = new EventEmitter<FolderLinkEvent>();

    @Output()
    public unlink = new EventEmitter<FolderLinkEvent>();

    @Output()
    public selectedChange = new EventEmitter<string[]>();

    public actions: TableAction<FolderBO>[] = [];

    constructor(
        protected i18n: I18nService,
        protected loader: FolderTrableLoaderService,
    ) { }

    ngOnInit(): void {
        this.actions = [
            {
                id: APPLY_RECURSIVE,
                label: this.i18n.instant('common.select_recursive'),
                icon: 'vertical_align_bottom',
                single: true,
                enabled: true,
            },
        ];
    }

    public handleAction(event: TableActionClickEvent<FolderBO>): void {
        switch (event.actionId) {
            case APPLY_RECURSIVE:
                this.applyFolderRecursive(event.item);
                break;
        }
    }

    applyFolderRecursive(folder: FolderBO): void {
        if (this.disabled) {
            return;
        }

        if (this.selected.includes(folder[BO_ID])) {
            this.link.emit({ folder, recursive: true });
        } else {
            this.unlink.emit({ folder, recursive: true });
        }
    }

    handleSelectEvent(row: TrableRow<FolderBO>): void {
        this.link.emit({ folder: row.item, recursive: false });
    }

    handleUnselectEvent(row: TrableRow<FolderBO>): void {
        this.unlink.emit({ folder: row.item, recursive: false });
    }

    updateSelection(event: string[]): void {
        this.selectedChange.emit(event);
    }
}
