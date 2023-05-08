import { ContentItem, ContentItemBO, ContentItemTypes } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { ContentItemTrableLoaderOptions, ContentItemTrableLoaderService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { TableAction, TableColumn } from '@gentics/ui-core';
import { BaseEntityTrableComponent } from '../base-entity-trable/base-entity-trable.component';

@Component({
    selector: 'gtx-content-item-trable',
    templateUrl: './content-item-trable.component.html',
    styleUrls: ['./content-item-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentItemTrableComponent extends BaseEntityTrableComponent<ContentItem, ContentItemBO, ContentItemTrableLoaderOptions> {

    @Input()
    public selectableItems: ContentItemTypes[] = [];

    @Input()
    public listableItems: Exclude<ContentItemTypes, 'folder' | 'node' | 'channel'>[] = [];

    @Input()
    public rootId: number;

    @Input()
    public includeRoot: boolean;

    @Input()
    public inlineExpansion = false;

    @Input()
    public inlineSelection = false;

    @Input()
    public actions: TableAction<ContentItemBO>[] = [];

    public rawColumns: TableColumn<ContentItemBO>[] = [
        {
            id: 'name',
            label: 'shared.element',
            fieldPath: 'name',
        },
    ];

    constructor(
        changeDetector: ChangeDetectorRef,
        i18n: I18nService,
        loader: ContentItemTrableLoaderService,
    ) {
        super(changeDetector, i18n, loader);
    }

    protected override createAdditionalLoadOptions(): ContentItemTrableLoaderOptions {
        return {
            rootId: this.rootId,
            includeRoot: this.includeRoot,
            selectable: this.selectableItems,
            listable: this.listableItems,
        };
    }
}
