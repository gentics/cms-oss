import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Inject, Input, Output } from '@angular/core';
import { GcmsUiServices } from '@gentics/cms-integration-api-models';
import { AllowedItemSelectionType, Item, ItemInNode, ItemRef, ItemRequestOptions, MarkupLanguageType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseFormElementComponent, cancelEvent } from '@gentics/ui-core';
import { map } from 'rxjs';
import { GCMS_UI_SERVICES_PROVIDER } from '../../providers/gcms-ui-services/gcms-ui-services';

/**
 * Component which wrapps the repository-browser API and allows for items to be picked.
 * Will load the items if necessary in order to display them correctly.
 */
@Component({
    selector: 'gtx-browse-box',
    templateUrl: './browse-box.component.html',
    styleUrls: ['./browse-box.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class BrowseBoxComponent extends BaseFormElementComponent<ItemInNode | ItemInNode[]> {

    /**
     * Which items are allowed to be selected
     */
    @Input()
    public allowedSelection: AllowedItemSelectionType | AllowedItemSelectionType[];

    /**
     * In which language the content has to be
     */
    @Input()
    public contentLanguage?: string;

    /**
     * If only items from the current node are to be selected
     */
    @Input()
    public onlyInCurrentNode?: boolean;

    /**
     * If an editor is able to select multiple items
     */
    @Input()
    public multiple: boolean;

    /**
     * When opening the repository-browser, which node should be opened initially
     */
    @Input()
    public startNode?: number;

    /**
     * When opening the repository-browser, which folder should be opened initially
     */
    @Input()
    public startFolder?: number;

    /**
     * IDs of Markup-Languages which a template has to have for it to be displayed.
     * Pages need to have such a valid template to be displayed as well.
     */
    @Input()
    public includeMlId?: MarkupLanguageType[];

    /**
     * The title which will be displayed in the repository-browser.
     */
    @Input()
    public browseTitle?: string;

    /**
     * If true, a clear button is displayed.
     * Defaults to `!required`.
     */
    @Input()
    public clearable: boolean | null = null;

    /**
     * If true, the user can upload new files/images.
     */
    @Input()
    public canUpload = false;

    /**
     * If the breadcrumb should be displayed.
     * Defaults to `!multiple`.
     */
    @Input()
    public showBreadcrumb: boolean | null = null;

    /**
     * Placeholder value in case nothing is selected yet
     */
    @Input()
    public placeholder: string;

    /**
     * Optional tooltip for the clear button (if this is not set, a default tooltip is used).
     */
    @Input()
    public clearTooltip: string;

    /**
     * Optional tooltip for the browse button (if this is not set, a default tooltip is used).
     */
    @Input()
    public browseTooltip: string;

    /**
     * Optional tooltip for the upload button (if this is not set, a default tooltip is used).
     */
    @Input()
    public uploadTooltip: string;

    /**
     * The title for the breadcrumb
     */
    @Input()
    public breadcrumbTitle: string;

    @Output()
    public upload = new EventEmitter<void>();

    /**
     * The string which is displayed in the input.
     * Should contain the names of all selected items.
     */
    public displayValue: string;

    /** The breadrcrumbs/path of the item(s) */
    public breadcrumbs: string[] = [];

    private cachedItems: Record<string, ItemRef> = {};

    constructor(
        changeDetector: ChangeDetectorRef,
        @Inject(GCMS_UI_SERVICES_PROVIDER) private gcmsUiServices: GcmsUiServices,
        private client: GCMSRestClientService,
    ) {
        super(changeDetector);
    }

    public clearItems(event?: MouseEvent): void {
        cancelEvent(event);

        this.triggerChange(this.multiple ? [] : null);
    }

    public browseItems(event?: MouseEvent): void {
        cancelEvent(event);

        this.gcmsUiServices.openRepositoryBrowser({
            allowedSelection: this.allowedSelection,
            selectMultiple: this.multiple,
            contentLanguage: this.contentLanguage,
            includeMlId: this.includeMlId,
            onlyInCurrentNode: this.onlyInCurrentNode,
            startFolder: this.startFolder,
            startNode: this.startNode,
            title: this.browseTitle,
        }).then((result) => {
            // If it's null, then the user canceled the modal
            if (result == null) {
                return;
            }

            if (Array.isArray(result) && !this.multiple) {
                result = result[0];
            } else if (this.multiple && !Array.isArray(result)) {
                result = [result];
            }

            if (Array.isArray(result)) {
                for (const item of (result as any[])) {
                    this.cachedItems[this.getItemCacheKey(item)] = item;
                }
            } else {
                this.cachedItems[this.getItemCacheKey(result as any)] = result as any;
            }

            this.triggerChange(result as any);
            this.updateDisplayName();
        }).catch((error) => {
            console.error('Error while picking item from repo-browser', error);
        });
    }

    public uploadItems(event?: MouseEvent): void {
        cancelEvent(event);
        this.upload.emit();
    }

    protected onValueChange(): void {
        if (this.value == null) {
            this.updateDisplayName();
            return;
        }

        const arr = Array.isArray(this.value) ? this.value : [this.value];
        for (const item of arr) {
            if (!item) {
                continue;
            }

            const hash = this.getItemCacheKey(item);
            if (!this.cachedItems[hash]) {
                this.loadItemIntoCache(item, hash);
            }
        }

        this.updateDisplayName();
        this.changeDetector.markForCheck();
    }

    protected getItemCacheKey(item: ItemInNode): string {
        return `${item.type}_${item.nodeId}_${item.id}_${item.edate}`;
    }

    protected loadItemIntoCache(item: ItemInNode, hash: string): void {
        // If the item is already "loaded", or at least the name is available, then we store it as it is.
        if (item.name) {
            this.cachedItems[hash] = item;
            return;
        }

        let loader: Promise<Item> | null = null;
        const options: ItemRequestOptions = {};
        if (Number.isInteger(item.nodeId) && item.nodeId !== 0) {
            options.nodeId = item.nodeId;
        }

        switch (item.type) {
            case 'folder':
                loader = this.client.folder.get(item.id, options).pipe(
                    map((res) => res.folder),
                ).toPromise();
                break;
            case 'file':
                loader = this.client.file.get(item.id, options).pipe(
                    map((res) => res.file),
                ).toPromise();
                break;
            case 'image':
                loader = this.client.image.get(item.id, options).pipe(
                    map((res) => res.file),
                ).toPromise();
                break;
            case 'page':
                loader = this.client.page.get(item.id, options).pipe(
                    map((res) => res.page),
                ).toPromise();
                break;
            case 'form':
                loader = this.client.form.get(item.id, options).pipe(
                    map((res) => res.item),
                ).toPromise();
                break;
            case 'template':
                loader = this.client.template.get(item.id, options).pipe(
                    map((res) => res.template),
                ).toPromise();
                break;
        }

        if (loader == null) {
            return;
        }

        loader.then((loadedItem) => {
            this.cachedItems[hash] = {
                id: item.id,
                nodeId: item.nodeId,
                type: item.type,
                name: loadedItem.name,
            };
            this.updateDisplayName();
            this.changeDetector.markForCheck();
        });
    }

    protected updateDisplayName(): void {
        if (this.value == null) {
            this.displayValue = '';
            return;
        }

        const arr = Array.isArray(this.value) ? this.value : [this.value];
        this.displayValue = arr
            .filter((item) => item != null)
            .map((item) => {
                if (item?.name) {
                    return item.name;
                }
                const hash = this.getItemCacheKey(item);
                if (!this.cachedItems[hash]) {
                    return 'todo: loading?';
                }

                return this.cachedItems[hash].name;
            })
            .join(', ');

        this.breadcrumbs = Array.from(new Set(arr
            .filter((item) => item != null)
            .map((item: any) => {
                if (item.path) {
                    return item;
                }
                const hash = this.getItemCacheKey(item);
                if (!this.cachedItems[hash]) {
                    return null;
                }

                return this.cachedItems[hash];
            })
            .filter((item) => item != null && item.path)
            .map((item) => this.generateBreadcrumbsPath(item)),
        ));
    }

    /**
     * @returns A string with the breadcrumbs path of the specified Page.
     */
    private generateBreadcrumbsPath(item: ItemInNode & { path: string }): string {
        let breadcrumbsPath = '';
        breadcrumbsPath = item.path.replace('/', '');
        if (breadcrumbsPath.length > 0 && breadcrumbsPath.charAt(breadcrumbsPath.length - 1) === '/') {
            breadcrumbsPath = breadcrumbsPath.substring(0, breadcrumbsPath.length - 1);
        }
        return breadcrumbsPath.split('/').join(' > ');
    }
}
