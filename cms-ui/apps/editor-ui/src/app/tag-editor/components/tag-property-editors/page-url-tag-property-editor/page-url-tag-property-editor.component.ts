import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    Folder,
    ItemInNode,
    Page,
    PageTagPartProperty,
    Raw,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { isEqual } from 'lodash-es';
import { Subject, Subscription, merge, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ObservableStopper } from '../../../../common/utils/observable-stopper/observable-stopper';
import { SelectedItemHelper } from '../../../../shared/util/selected-item-helper/selected-item-helper';

/**
 * Used to edit the following  UrlPage TagParts.
 */

// enum CHANGE_CASE {
//     ALL_TO_EXTERN
//     EXTERN_TO_INTERN
// }
@Component({
    selector: 'page-url-tag-property-editor',
    templateUrl: './page-url-tag-property-editor.component.html',
    styleUrls: ['./page-url-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PageUrlTagPropertyEditor implements TagPropertyEditor, OnInit, OnDestroy {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: PageTagPartProperty;

    /** Dermines whether the page is interal or external. */
    isInternalPage = true;

    /** The URL of the external page. */
    externalUrl: string;

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    uploadDestination: Folder<Raw>;

    /** Page this edited tag belongs to */
    private page?: Page<Raw>;

    /** Used to debounce the input/textarea changes. */
    externalUrlChange = new Subject<string>();
    externalUrlBlur = new Subject<string>();

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    /** The helper for managing and loading the selected internal page. */
    public selectedInternalPage: SelectedItemHelper<ItemInNode<Page<Raw>>>;

    private subscriptions = new Subscription();

    private stopper = new ObservableStopper();

    constructor(
        private client: GCMSRestClientService,
        private changeDetector: ChangeDetectorRef,
    ) { }

    ngOnInit(): void {
        const debouncer = this.externalUrlChange.pipe(debounceTime(100));
        const blurOrDebouncedChange = merge(this.externalUrlBlur, debouncer).pipe(
            distinctUntilChanged(isEqual),
        );
        this.subscriptions.add(
            blurOrDebouncedChange.subscribe((newValue) => this.changeSelectedPage(newValue)),
        );
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
        this.stopper.stop();
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.selectedInternalPage = new SelectedItemHelper('page', context.node.id, this.client);
        this.tagPart = tagPart;
        this.readOnly = context.readOnly;
        this.page = context.page;
        this.updateTagProperty(tagProperty);

        this.selectedInternalPage.selectedItem$.pipe(
            switchMap((selectedInternalPage) => {
                if (selectedInternalPage) {
                    return this.client.folder.get(selectedInternalPage.folderId)
                        .pipe(
                            map((response) => response.folder),
                            catchError((err) => of(err)),
                            tap((folder: Folder<Raw>) => {
                                this.uploadDestination = folder;
                                this.changeDetector.markForCheck();
                            }),
                        );
                }

                // If no page is available, fall back to the context folder
                if (this.page == null) {
                    this.uploadDestination = context.folder;
                    this.changeDetector.markForCheck();
                    return of(context.folder);
                }

                return this.client.folder.get(this.page.folderId)
                    .pipe(
                        map((response) => response.folder),
                        catchError((err) => of(err)),
                        tap((folder: Folder<Raw>) => {
                            this.uploadDestination = folder;
                            this.changeDetector.markForCheck();
                        }),
                    );
            }),
            takeUntil(this.stopper.stopper$),
        ).subscribe();
    }

    registerOnChange(fn: TagPropertiesChangedFn): void {
        this.onChangeFn = fn;
    }

    writeChangedValues(values: Partial<TagPropertyMap>): void {
        // We only care about changes to the TagProperty that this control is responsible for.
        const tagProp = values[this.tagPart.keyword];
        if (tagProp) {
            this.updateTagProperty(tagProp);
        }
    }

    /**
     * Changes the values of this.tagProperty and this.selectedInternalPage$ according
     * to newSelectedPage. This method must only be called in response to
     * user input.
     *
     * The API expects three scenarios:
     * a) setting an external value:
     *   tagProperty.pageId should be not set / null
     *   tagProperty.nodeId should be not set / null
     *   stringValue should be set to the desired value
     *
     * b) setting an internal page as a reference
     *   tagProperty.pageId should be set to the ID of the referenced page
     *   tagProperty.nodeId should be set to the nodeId of the node where the referenced page is located
     *   stringValue should be an empty string
     *
     * c) unsetting / clearing the input
     *   tagProperty.pageId should be set to 0
     *   tagProperty.nodeId should be set to 0
     *   stringValue should be an empty string
     */
    changeSelectedPage(newSelectedPage: ItemInNode<Page<Raw>> | string): void {
        let selectedInternalPage: ItemInNode<Page<Raw>> = null;
        let externalUrl: string;

        const newIsExternalValue = typeof newSelectedPage === 'string'
          && newSelectedPage.length > 0;
        const newIsInternalValue = newSelectedPage instanceof Object
          && (
              Number.isInteger((newSelectedPage as unknown as ItemInNode<Page<Raw>>).id)
              && (newSelectedPage as unknown as ItemInNode<Page<Raw>>).id > 0
              && Number.isInteger((newSelectedPage as unknown as ItemInNode<Page<Raw>>).nodeId)
              && (newSelectedPage as unknown as ItemInNode<Page<Raw>>).nodeId > 0
          );
        const newIsNoValue = !newIsExternalValue && !newIsInternalValue;

        this.tagProperty.pageId = null;
        this.tagProperty.nodeId = null;
        this.tagProperty.stringValue = '';

        if (newIsExternalValue) {
            this.tagProperty.stringValue = newSelectedPage;
            externalUrl = newSelectedPage;

        } else if (newIsInternalValue) {
            this.tagProperty.pageId = (newSelectedPage).id;
            this.tagProperty.nodeId = (newSelectedPage).nodeId;
            selectedInternalPage = newSelectedPage;

        } else if (newIsNoValue) {
            this.tagProperty.pageId = 0;
            this.tagProperty.nodeId = 0;
            externalUrl = '';
        } else {
            throw new Error('Unexpected value combination.');
        }

        // notify about results
        if (this.onChangeFn) {
            const changes: Partial<TagPropertyMap> = {};
            changes[this.tagPart.keyword] = this.tagProperty;
            this.onChangeFn(changes);
        }
        // update view property
        this.externalUrl = externalUrl;
        // select item using helper service
        this.selectedInternalPage.setSelectedItem(selectedInternalPage);
    }

    onRadioButtonsChange(): void {
        // This seems to be necessary to make the radio buttons react correctly.
        this.changeDetector.detectChanges();
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.PAGE) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by PageUrlTagPropertyEditor.`);
        }
        this.tagProperty = newValue;

        this.isInternalPage = !this.tagProperty.stringValue;
        if (this.isInternalPage) {
            this.externalUrl = '';
            this.selectedInternalPage.setSelectedItem(this.tagProperty.pageId || null, this.tagProperty.nodeId);
        } else {
            this.externalUrl = this.tagProperty.stringValue;
        }

        // This seems to be necessary to make the radio buttons react correctly.
        this.changeDetector.detectChanges();
        setTimeout(() => this.changeDetector.detectChanges());
    }

    /**
     * @returns A string with the breadcrumbs path of the specified Page.
     */
    private generateBreadcrumbsPath(selectedItem: Page): string {
        let breadcrumbsPath = '';
        if (selectedItem) {
            breadcrumbsPath = selectedItem.path.replace('/', '');
            if (breadcrumbsPath.length > 0 && breadcrumbsPath.charAt(breadcrumbsPath.length - 1) === '/') {
                breadcrumbsPath = breadcrumbsPath.substring(0, breadcrumbsPath.length - 1);
            }
            breadcrumbsPath = breadcrumbsPath.split('/').join(' > ');
        }
        return breadcrumbsPath;
    }

}
