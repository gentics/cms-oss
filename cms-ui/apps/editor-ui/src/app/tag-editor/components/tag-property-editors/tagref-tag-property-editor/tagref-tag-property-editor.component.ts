/* eslint-disable no-underscore-dangle */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ObservableStopper } from '@editor-ui/app/common/utils/observable-stopper/observable-stopper';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { RepositoryBrowserOptions, TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    Folder,
    Page,
    PageTagTagPartProperty,
    Raw,
    Tag,
    TagInContainer,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
    TagType,
    Template,
    TemplateTagTagPartProperty,
} from '@gentics/cms-models';
import { BehaviorSubject, Observable, Subject, Subscription, merge, of } from 'rxjs';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { Api } from '../../../../core/providers/api/api.service';
import { I18nService } from '../../../../core/providers/i18n/i18n.service';

/**
 * Helper class for fetching a Page or a Template depending on the TagPropertyType
 * and accessing the tags of the page/template in a unified way.
 */
class ContainerWrapper {
    private container: Page<Raw> | Template<Raw>;

    constructor(private api: Api) {}

    /**
     * Gets the tags of the container.
     */
    get tags(): { [key: string]: Tag } {
        const contentTags = (<Page<Raw>> this.container).tags;
        const templateTags = (<Template<Raw>> this.container).templateTags;
        return contentTags || templateTags;
    }

    /**
     * Fetches the container referenced in the tagProperty, sets it as the current container
     * and returns an Observable for it.
     */
    fetchAndUpdateContainer(tagProperty: PageTagTagPartProperty | TemplateTagTagPartProperty): Observable<Page<Raw> | Template<Raw>> {
        let request: Observable<Page<Raw> | Template<Raw>>;
        if (tagProperty.type === TagPropertyType.PAGETAG) {
            request = this.getOrFetchPage(tagProperty.pageId);
        } else {
            request = this.getOrFetchTemplate(tagProperty.templateId);
        }
        return request.pipe(
            tap((container) => {
                this.updateContainer(container);
            }),
        );
    }

    /**
     * Sets the specified newContainer as the current container.
     */
    updateContainer(newContainer: Page<Raw> | Template<Raw>): void {
        this.container = newContainer;
    }

    /**
     * Finds the tag with the ID specified in the tagProperty in the container.
     * @returns The Tag object or null if no tag with the specified id could be found or no id was specified.
     */
    findTag(tagProperty: PageTagTagPartProperty | TemplateTagTagPartProperty): Tag {
        let id: number;
        if (tagProperty.type === TagPropertyType.PAGETAG) {
            id = tagProperty.contentTagId;
        } else {
            id = tagProperty.templateTagId;
        }
        if (!id) {
            return null;
        }

        const tags = this.tags;
        for (const key of Object.keys(tags)) {
            const tag = tags[key];
            if (tag.id === id) {
                return tag;
            }
        }
        return null;
    }

    private getOrFetchPage(pageId: number): Observable<Page<Raw>> {
        if (!pageId) {
            return of(null);
        }
        if (this.container && this.container.type === 'page' && this.container.id === pageId) {
            return of(this.container);
        } else {
            return this.api.folders
                .getItem(pageId, 'page', {folder: false, template: false}).pipe(
                    map(response => response.page),
                );
        }
    }

    private getOrFetchTemplate(templateId: number): Observable<Template<Raw>> {
        if (!templateId) {
            return of(null);
        }
        if (this.container && this.container.type === 'template' && this.container.id === templateId) {
            return of(this.container);
        } else {
            return this.api.folders.getItem(templateId, 'template').pipe(
                map(response => response.template),
            );
        }
    }

}

export interface TagSelection {
    container: Page<Raw> | Template<Raw>;
    tag: Tag;
}

interface AugmentedTagSelection extends TagSelection {
    tagType: TagType;
}

/**
 * Used to edit PAGETAG and TEMPLATETAG TagParts.
 */
@Component({
    selector: 'tagref-tag-property-editor',
    templateUrl: './tagref-tag-property-editor.component.html',
    styleUrls: ['./tagref-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TagRefTagPropertyEditor implements TagPropertyEditor, OnInit, OnDestroy {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: PageTagTagPartProperty | TemplateTagTagPartProperty;

    /** The string that should be displayed in the input field. */
    tagDisplayValue$: Observable<string>;

    /** The currently selected container. */
    selectedContainer: Page<Raw> | Template<Raw>;

    selectedFullPath: string;

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    /** Message to be displayed in tag property input */
    private loadingError$ = new Subject<{ error: any, item: { itemId: number, nodeId?: number } }>();

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    uploadDestination: Folder<Raw>;

    /** Page this edited tag belongs to */
    private page?: Page<Raw>;

    /** The objects representing the selected page/template and the selected tag within it. */
    private selectionSubject = new BehaviorSubject<TagSelection>({ container: null, tag: null });
    private selection$ = this.selectionSubject.asObservable().pipe(
        switchMap(selection => this.addTagTypeToSelection(selection)),
    );
    private selectedContainerWrapper: ContainerWrapper;

    private subscriptions = new Subscription();

    private stopper = new ObservableStopper();

    constructor(
        private api: Api,
        private changeDetector: ChangeDetectorRef,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private i18n: I18nService,
    ) {
        this.selectedContainerWrapper = new ContainerWrapper(api);
    }

    ngOnInit(): void {
        this.tagDisplayValue$ = merge(
            this.selection$.pipe(
                tap(selection => {
                    this.selectedContainer = selection.container;
                }),
                map((selection: AugmentedTagSelection) => {
                    if (selection) {
                        this.selectedFullPath = this.generateBreadcrumbsPath(selection.container);
                    }
                    if (selection.container && selection.tag && selection.tagType) {
                        return `${selection.tag.name} - ${selection.tagType.name}`;
                    } else if (selection.container && (!selection.tag || !selection.tagType)) {
                        /**
                         * The tag or tagType is missing, if the container exists but the contained tag was removed.
                         * This means, that there was a previous selection, but now the tag is gone.
                         */
                        if (this.tagProperty) {
                            if (this.tagProperty.type === TagPropertyType.PAGETAG) {
                                return this.i18n.translate(
                                    'editor.tag_not_found_in_page',
                                    { id: this.tagProperty.contentTagId, pageId: this.tagProperty.pageId},
                                );
                            } else {
                                return this.i18n.translate(
                                    'editor.tag_not_found_in_template',
                                    { id: this.tagProperty.templateTagId , templateId: this.tagProperty.templateId },
                                );
                            }
                        } else {
                            return '';
                        }
                    } else {
                        /**
                         * null is emitted, when nothing is selected.
                         * Also, null is emitted in case a referenced page or template got deleted and the tag property data was refetched.
                         * (Since the pageId or templateId in tagProperty gets removed)
                         */
                        return this.i18n.translate('editor.tag_no_selection');
                    }
                }),
            ),
            this.loadingError$.pipe(
                map((error: { error: any, item: { itemId: number, nodeId?: number } }) => {
                    /**
                     * When a page or a template that contained a chosen tag and is referenced gets deleted, the pageId or templateId is kept in tagProperty.
                     * When we try to fetch the page or template information we get an error message.
                     * In that case we want to inform the user that the page or template got deleted
                     * (and thus avoid suggesting that a valid page or template is still selected).
                     */
                    if (this.tagProperty) {
                        if (this.tagProperty.type === TagPropertyType.PAGETAG) {
                            return this.i18n.translate(
                                'editor.tag_not_found_in_page',
                                { id: this.tagProperty.contentTagId, pageId: this.tagProperty.pageId},
                            );
                        } else {
                            return this.i18n.translate(
                                'editor.tag_not_found_in_template',
                                { id: this.tagProperty.templateTagId , templateId: this.tagProperty.templateId },
                            );
                        }
                    } else {
                        return '';
                    }
                }),
            ),
        );
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
        this.stopper.stop();
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.tagPart = tagPart;
        this.readOnly = context.readOnly;
        this.page = context.page;
        this.updateTagProperty(tagProperty);

        this.selection$
            .pipe(
                switchMap((selection) => {
                    if (selection.container) {
                        return this.api.folders.getItem(selection.container.folderId, 'folder')
                            .pipe(
                                map(response => response.folder),
                                catchError(err => of(err)),
                                tap((folder: Folder<Raw>) => {
                                    this.uploadDestination = folder;
                                    this.changeDetector.markForCheck();
                                }),
                            )
                    }

                    return this.api.folders.getItem(this.page.folderId, 'folder')
                        .pipe(
                            map(response => response.folder),
                            catchError(err => of(err)),
                            tap((folder: Folder<Raw>) => {
                                this.uploadDestination = folder;
                                this.changeDetector.markForCheck();
                            }),
                        );
                }),
                takeUntil(this.stopper.stopper$),
            )
            .subscribe();
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
     * Changes the values of this.tagProperty and this.selection$ according
     * to the newSelection. This method must only be called in response to
     * user input.
     */
    changeSelection(newSelection: TagSelection): void {
        let containerId = 0;
        let tagId = 0;
        if (newSelection.container && newSelection.tag) {
            containerId = newSelection.container.id;
            tagId = newSelection.tag.id;
        }

        if (this.tagProperty.type === TagPropertyType.PAGETAG) {
            this.tagProperty.pageId = containerId;
            this.tagProperty.contentTagId = tagId;
        } else {
            this.tagProperty.templateId = containerId;
            this.tagProperty.templateTagId = tagId;
        }

        if (this.onChangeFn) {
            const changes: Partial<TagPropertyMap> = {};
            changes[this.tagPart.keyword] = this.tagProperty;
            this.onChangeFn(changes);
        }
        this.selectionSubject.next(newSelection);
    }

    /**
     * Opens the repository browser to allow the user to select a tag.
     */
    browseForTag(): void {
        let contentLanguage: string;
        if (this.page) {
            contentLanguage = this.page.language;
        }
        const options: RepositoryBrowserOptions = {
            allowedSelection: this.tagProperty.type === TagPropertyType.PAGETAG ? 'contenttag' : 'templatetag',
            selectMultiple: false,
            contentLanguage,
            startFolder: this.uploadDestination ? this.uploadDestination.id : undefined,
        };

        this.repositoryBrowserClient.openRepositoryBrowser(options)
            .then((selectedTag: TagInContainer) => {
                const selection: TagSelection = {
                    container: selectedTag.__parent__,
                    tag: selectedTag,
                };
                delete selectedTag.__parent__;
                this.changeSelection(selection);
            });
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.PAGETAG && newValue.type !== TagPropertyType.TEMPLATETAG) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by TagRefTagPropertyEditor.`);
        }
        this.tagProperty = newValue ;

        const sub = this.selectedContainerWrapper.fetchAndUpdateContainer(this.tagProperty)
            .subscribe(
                (container: Page<Raw> | Template<Raw>) => {
                    this.selectionSubject.next({
                        container: container,
                        tag: container ? this.selectedContainerWrapper.findTag(this.tagProperty) : null,
                    });
                    this.changeDetector.markForCheck();
                },
                (error) => {
                    this.loadingError$.next(error);
                },
            );
        this.subscriptions.add(sub);
    }

    /**
     * Loads the TagType for the selected Tag.
     */
    private addTagTypeToSelection(selection: TagSelection): Observable<AugmentedTagSelection> {
        let tagType$: Observable<TagType>;
        if (selection.container && selection.tag) {
            tagType$ = this.api.tagType.getTagType(selection.tag.constructId).pipe(
                map((response) => response.construct),
            );
        } else {
            tagType$ = of(null);
        }

        return tagType$.pipe(
            map(tagType => ({
                ...selection,
                tagType: tagType,
            })),
            catchError(error => of({
                ...selection,
                tagType: null,
            })),
        );
    }

    /**
     * @returns A string with the breadcrumbs path of the specified Page or Template.
     */
    private generateBreadcrumbsPath(selectedContainer: Page<Raw> | Template<Raw>): string {
        let breadcrumbsPath = '';
        if (selectedContainer) {
            breadcrumbsPath = selectedContainer.path + selectedContainer.name;
            breadcrumbsPath = breadcrumbsPath.replace('/', '');
            breadcrumbsPath = breadcrumbsPath.split('/').join(' > ');
        }
        return breadcrumbsPath;
    }
}
