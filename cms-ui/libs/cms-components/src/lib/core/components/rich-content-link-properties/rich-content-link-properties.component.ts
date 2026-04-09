import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ItemInNode } from '@gentics/cms-models';
import { BaseFormPropertiesComponent, FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { LINK_DEFAULT_DISPLAY_VALUE, RichContentLink, RichContentLinkType, RichContentType } from '../../../common/models';

function getItemType(linkType: RichContentLinkType): 'page' | 'file' {
    return linkType === RichContentLinkType.PAGE ? 'page' : 'file';
}

function getLinkType(itemType: string): RichContentLinkType.PAGE | RichContentLinkType.FILE {
    return itemType === 'page' ? RichContentLinkType.PAGE : RichContentLinkType.FILE;
}

interface PickedItemRef {
    type: string;
    id: number;
    nodeId: number;
    name?: string;
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
    standalone: false,
})
export class RichContentLinkPropertiesComponent extends BaseFormPropertiesComponent<RichContentLink> {

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

    public linkItemType: string;

    public pickedItemRef: PickedItemRef;

    private previousLinkType: RichContentLinkType;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
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

        this.linkItemType = getItemType(value?.linkType);

        // If the item type changes, we have to clear the values from this instance and from the form
        if (this.previousLinkType != null && this.previousLinkType !== value?.linkType) {
            this.previousLinkType = value.linkType;
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

    public setPickedItem(picked: ItemInNode): void {
        this.pickedItemRef = picked;
        this.previousLinkType = getLinkType(picked.type);

        this.form.controls.itemId.markAsDirty();
        this.form.controls.nodeId.markAsDirty();

        this.form.patchValue({
            itemId: this.pickWithLocalIds ? picked.id : picked.globalId,
            nodeId: this.pickWithLocalIds ? picked.id : picked.globalId,
        });

        this.changeDetector.markForCheck();
    }
}
