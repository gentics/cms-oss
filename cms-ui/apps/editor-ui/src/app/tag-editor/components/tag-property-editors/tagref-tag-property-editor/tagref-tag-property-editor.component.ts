/* eslint-disable no-underscore-dangle */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
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
import { BehaviorSubject, Observable, Subject, Subscription, of } from 'rxjs';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ObservableStopper } from '../../../../common/utils/observable-stopper/observable-stopper';
import { Api } from '../../../../core/providers/api/api.service';

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
                .getItem(pageId, 'page', { folder: false, template: false }).pipe(
                    map((response) => response.page),
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
                map((response) => response.template),
            );
        }
    }

}

export interface TagSelection {
    originalTag: TagInContainer;
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
    standalone: false,
})
export class TagRefTagPropertyEditor implements TagPropertyEditor, OnDestroy {

    public readonly TagPropertyType = TagPropertyType;

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
    private loadingError$ = new Subject<{ error: any; item: { itemId: number; nodeId?: number } }>();

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    uploadDestination: Folder<Raw>;

    /** Page this edited tag belongs to */
    public page?: Page<Raw>;

    /** The objects representing the selected page/template and the selected tag within it. */
    private selectionSubject = new BehaviorSubject<TagSelection>({ originalTag: null, container: null, tag: null });
    public selectedTag$ = this.selectionSubject.pipe(map((selection) => selection?.originalTag));
    private selection$ = this.selectionSubject.asObservable().pipe(
        switchMap((selection) => this.addTagTypeToSelection(selection)),
    );

    private selectedContainerWrapper: ContainerWrapper;

    private subscriptions = new Subscription();

    private stopper = new ObservableStopper();

    constructor(
        private api: Api,
        private changeDetector: ChangeDetectorRef,
    ) {
        this.selectedContainerWrapper = new ContainerWrapper(api);
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
                                map((response) => response.folder),
                                catchError((err) => of(err)),
                                tap((folder: Folder<Raw>) => {
                                    this.uploadDestination = folder;
                                    this.changeDetector.markForCheck();
                                }),
                            );
                    }

                    return this.api.folders.getItem(this.page.folderId, 'folder')
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
    changeSelection(tag: TagInContainer): void {
        let containerEl;
        let containerId = 0;
        let tagId = 0;
        let cleanTag: Tag;

        // Remap the tag from the ridicolus format
        if (tag && tag.__parent__) {
            const { __parent__: parentEl, ...tagContent } = tag;
            containerEl = parentEl;
            containerId = containerEl.id;
            cleanTag = tagContent as any;
            tagId = tag.id;
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

        this.selectionSubject.next(tagId && containerEl
            ? {
                originalTag: tag,
                container: containerEl,
                tag: cleanTag,
            }
            : null,
        );
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.PAGETAG && newValue.type !== TagPropertyType.TEMPLATETAG) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by TagRefTagPropertyEditor.`);
        }
        this.tagProperty = newValue;

        const sub = this.selectedContainerWrapper.fetchAndUpdateContainer(this.tagProperty)
            .subscribe(
                (container: Page<Raw> | Template<Raw>) => {
                    const tag = container ? this.selectedContainerWrapper.findTag(this.tagProperty) : null;
                    this.selectionSubject.next({
                        container: container,
                        tag: tag,
                        originalTag: tag ? { ...tag, __parent__: container } : null,
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
            map((tagType) => ({
                ...selection,
                tagType: tagType,
            })),
            catchError((error) => of({
                ...selection,
                tagType: null,
            })),
        );
    }
}
