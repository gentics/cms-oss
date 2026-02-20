import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { File, ItemInNode, Node, Page } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { map, switchMap } from 'rxjs/operators';
import { LINK_DEFAULT_DISPLAY_VALUE, RichContentLink, RichContentLinkType, RichContentType } from '../../../common/models';
import { GCMS_UI_SERVICES_PROVIDER, GcmsUiServices } from '../../providers/gcms-ui-services/gcms-ui-services';
import { BasePropertiesComponent } from '../base-properties/base-properties.component';

function getItemType(linkType: RichContentLinkType): 'page' | 'file' {
    return linkType === RichContentLinkType.PAGE ? 'page' : 'file';
}

// eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
function getLinkType(itemType: string): RichContentLinkType.PAGE | RichContentLinkType.FILE {
    return itemType === 'page' ? RichContentLinkType.PAGE : RichContentLinkType.FILE;
}

@Component({
    selector: 'gtx-rich-content-link-properties',
    templateUrl: './rich-content-link-properties.component.html',
    styleUrls: ['./rich-content-link-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(RichContentLinkPropertiesComponent),
        generateValidatorProvider(RichContentLinkPropertiesComponent),
    ],
    standalone: false
})
export class RichContentLinkPropertiesComponent extends BasePropertiesComponent<RichContentLink> implements OnInit {

    public readonly RichContentLinkType = RichContentLinkType;
    public readonly AVAILABLE_TARGETS = [
        { label: 'richContent.link_target_current_tab', value: '_top' },
        { label: 'richContent.link_target_new_tab', value: '_blank' },
    ];

    /**
     * If the item-picker is available, and a new item is picked, if it should
     * save the reference to that item with the local ids (i.E. integers), or
     * by default/`false`, with the global ids.
     */
    @Input()
    public pickWithLocalIds = false;

    /**
     * If the `displayText` property should actually be editable.
     */
    @Input()
    public enterDisplayText = false;

    /**
     * If the initial value has an item picked and was successfully loaded.
     */
    public hasLoadedItem = false;
    /**
     * The display name of the currently picked item.
     */
    public itemDisplayValue: string;
    /**
     * If it is currently loading the item.
     */
    public loadingItem = false;

    /**
     * The loaded item.
     */
    protected loadedItem: File | Page;
    protected loadedNode: Node;
    // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
    protected loadedItemType: RichContentLinkType.FILE | RichContentLinkType.PAGE;

    constructor(
        changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        @Inject(GCMS_UI_SERVICES_PROVIDER) private gcmsUiServices: GcmsUiServices,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.loadInitialItem();
    }

    protected createForm(): FormGroup {
        return new FormGroup<FormProperties<RichContentLink>>({
            type: new FormControl(RichContentType.LINK),
            linkType: new FormControl(this.safeValue('linkType') ?? RichContentLinkType.URL, Validators.required),
            displayText: new FormControl(this.safeValue('displayText') || LINK_DEFAULT_DISPLAY_VALUE, Validators.required),
            url: new FormControl(this.safeValue('url') || '', Validators.required),
            nodeId: new FormControl(this.safeValue('nodeId'), Validators.required),
            itemId: new FormControl(this.safeValue('itemId'), Validators.required),
            target: new FormControl(this.safeValue('target') || '_top'),
        });
    }

    protected configureForm(value: RichContentLink, loud?: boolean): void {
        const options = { emitEvent: loud, onlySelf: loud };

        // If the item type changes, we have to clear the values from this instance and from the form
        if (value?.linkType != null && this.loadedItemType != null && value.linkType !== this.loadedItemType) {
            this.loadedItem = null;
            this.loadedNode = null;
            this.loadedItemType = null;
            this.itemDisplayValue = null;
            this.form.patchValue({
                itemId: null,
                nodeId: null,
            }, options);
        }

        setControlsEnabled(this.form, ['url'], !this.disabled && value?.linkType === RichContentLinkType.URL, options);
        setControlsEnabled(this.form, ['nodeId', 'itemId'], !this.disabled && value?.linkType !== RichContentLinkType.URL);
    }

    protected assembleValue(value: RichContentLink): RichContentLink {
        return value;
    }

    protected override onValueChange(): void {
        super.onValueChange();

        if (!this.hasLoadedItem) {
            this.loadInitialItem();
        }
    }

    protected loadInitialItem(): void {
        // If an item is already selected, then we need to reload it, so we get the display-value for it.
        if (
            this.value?.linkType == null
            || this.value.linkType === RichContentLinkType.URL
            || this.value.nodeId == null
            || this.value.itemId == null
        ) {
            return;
        }

        this.loadingItem = true;
        this.changeDetector.markForCheck();

        this.subscriptions.push(this.client.node.get(this.value.nodeId).pipe(
            switchMap(nodeRes => {
                if (this.value.linkType === RichContentLinkType.PAGE) {
                    return this.client.page.get(this.value.itemId, {
                        nodeId: nodeRes.node.id,
                        langvars: true,
                    }).pipe(
                        map(res => [nodeRes.node, res.page]),
                    );
                } else {
                    return this.client.file.get(this.value.itemId, {
                        nodeId: nodeRes.node.id,
                    }).pipe(
                        map(res => [nodeRes.node, res.file]),
                    );
                }
            }),
        ).subscribe(([node, item]) => {
            this.hasLoadedItem = true;
            this.loadingItem = false;
            this.loadedNode = node as Node;
            this.loadedItem = item as Page | File;
            this.itemDisplayValue = item.name;
            this.loadedItemType = getLinkType(item.type);
            this.changeDetector.markForCheck();
        }));
    }

    public async pickItem(): Promise<void> {
        const type = getItemType(this.form?.value?.linkType);

        try {
            const picked = await this.gcmsUiServices.openRepositoryBrowser({
                allowedSelection: type,
                selectMultiple: false,
            }) as ItemInNode;

            if (picked == null) {
                return;
            }

            this.loadingItem = false;
            this.hasLoadedItem = true;
            this.loadedNode = (await this.client.node.get(picked.nodeId).toPromise()).node;
            this.loadedItem = picked as any;
            this.itemDisplayValue = this.loadedItem.name;
            this.loadedItemType = getLinkType(picked.type);

            this.form.controls.itemId.markAsDirty();
            this.form.controls.nodeId.markAsDirty();

            this.form.patchValue({
                itemId: this.pickWithLocalIds ? this.loadedItem.id : this.loadedItem.globalId,
                nodeId: this.pickWithLocalIds ? this.loadedNode.id : this.loadedNode.globalId,
            });

            this.changeDetector.markForCheck();
        } catch (err) {
            // Ignore errors
        }
    }
}
