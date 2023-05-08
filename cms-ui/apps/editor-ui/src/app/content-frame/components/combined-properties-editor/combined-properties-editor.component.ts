import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    QueryList,
    SimpleChange,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import { Api } from '@editor-ui/app/core/providers/api/api.service';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
import { ResourceUrlBuilder } from '@editor-ui/app/core/providers/resource-url-builder/resource-url-builder';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import {
    AddExpandedTabGroupAction,
    ApplicationStateService,
    ChangeTabAction,
    EditorActionsService,
    FolderActionsService,
    MarkContentAsModifiedAction,
    MarkObjectPropertiesAsModifiedAction,
    PostUpdateBehavior,
    RemoveExpandedTabGroupAction,
    SaveErrorAction,
    SaveSuccessAction,
    SetOpenObjectPropertyGroupsAction,
    StartSavingAction,
} from '@editor-ui/app/state';
import {
    TagEditorHostComponent,
    TagEditorService,
} from '@editor-ui/app/tag-editor';
import { EditableObjectTag } from '@editor-ui/app/tag-editor/common';
import {
    EditableFileProps,
    EditableFolderProps,
    EditableFormProps,
    EditableImageProps,
    EditableNodeProps,
    EditablePageProps,
    EditableTag,
    Folder,
    FolderItemOrTemplateType,
    FolderSaveRequestOptions,
    Form, ItemPermissions,
    ItemType,
    ItemWithContentTags,
    ItemWithObjectTags,
    Language,
    Node,
    NodeFeature,
    ObjectTag,
    Page,
    Tag,
    TagPropertyType,
    Tags,
    Template,
    TemplateListRequest,
    noItemPermissions,
} from '@gentics/cms-models';
import {
    GroupedTabsComponent,
    ModalService,
    TooltipComponent,
} from '@gentics/ui-core';
import { cloneDeep, isEqual } from 'lodash-es';
import {
    BehaviorSubject,
    Observable,
    ReplaySubject,
    Subscription,
    combineLatest,
    from,
    of as observableOf,
} from 'rxjs';
import {
    delay,
    distinctUntilChanged,
    filter,
    map,
    publishReplay,
    refCount,
    startWith,
    switchMap,
    tap,
} from 'rxjs/operators';
import { ITEM_PROPERTIES_TAB, ITEM_REPORTS_TAB, ITEM_TAG_LIST_TAB, PropertiesTab } from '@editor-ui/app/common/models';
import { CustomScriptHostService } from '../../providers/custom-script-host/custom-script-host.service';
import { PropertiesEditor } from '../properties-editor/properties-editor.component';


/** Allows to define additional options for saving. */
export interface SaveChangesOptions {
    applyToSubfolders?: boolean;
    applyToLanguageVariants?: number[];
}

export interface ObjectPropertiesCategory {
    name: string;
    objProperties: EditableObjectTag[];
}

const OBJ_PROP_CATEGORY_OTHERS = 'editor.object_properties_category_others_label';

/**
 * Displays vertical tabs, which contain one tab for the item's properties (PropertiesEditor component)
 * and one tab for each of the item's object properties.
 */
@Component({
    selector: 'combined-properties-editor',
    templateUrl: './combined-properties-editor.component.html',
    styleUrls: ['./combined-properties-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    })
export class CombinedPropertiesEditorComponent implements OnInit, AfterViewInit, OnDestroy, OnChanges {

    /** The item, whose properties should be edited. */
    @Input()
    item: ItemWithObjectTags | Form | Node;

    @Input() isDisabled: boolean;

    pointObjProp: any;
    position: string;

    tagfillLightState$: Observable<boolean>;

    activeTabId$: Observable<string>;
    itemWithObjectProperties$: Observable<{ item: ItemWithObjectTags | Node, objProperties: EditableObjectTag[] }>;
    itemWithContentTags$: Observable<any>;
    objectPropertiesGrouped$: Observable<ObjectPropertiesCategory[]>;
    objectPropertiesGroupedDelayed$: Observable<ObjectPropertiesCategory[]>;
    activeTabObjectProperty$: Observable<{ item: ItemWithObjectTags, tag: EditableObjectTag }>;
    useNewTagEditor$: Observable<boolean>;
    disableTagList$: Observable<boolean>;
    itemProperties$: Observable<{
        item: ItemWithObjectTags | Node,
        languages: Language[],
        templates: Template[]
    }>;
    oldTagfillUrl: string;
    oldTagFillIFrameState = false;
    currentNode: Node;

    /** The constant for the tab with the item's (non object-) properties. */
    readonly ITEM_PROPERTIES_TAB = ITEM_PROPERTIES_TAB;
    readonly ITEM_REPORTS_TAB = ITEM_REPORTS_TAB;
    readonly ITEM_TAG_LIST_TAB = ITEM_TAG_LIST_TAB;

    @ViewChild(GroupedTabsComponent, { static: false })
    propertiesTabs: GroupedTabsComponent;

    @ViewChild(PropertiesEditor, { static: false })
    propertiesEditor: PropertiesEditor;

    @ViewChildren(TagEditorHostComponent)
    tagEditorHostList: QueryList<TagEditorHostComponent>;

    get canSave(): boolean {
        return this.hasUpdatePermission !== false;
    }

    get expandedCategories(): string[] {
        return this.appState.now.editor.openObjectPropertyGroups;
    }

    private hasUpdatePermission = false;

    private item$ = new ReplaySubject<ItemWithObjectTags | Node>(1);
    editedObjectProperty: EditableObjectTag;
    private activeTabId: PropertiesTab;
    private useNewTagEditor: boolean;
    private subscriptions = new Subscription();

    expandedState$: Observable<string[]>;

    private oldTagFillStateFallbackTimeout: any;
    itemPermissions: ItemPermissions = noItemPermissions;

    selectedContentTags$ = new BehaviorSubject<number[]>([]);

    constructor(
        private api: Api,
        private appState: ApplicationStateService,
        private changeDetector: ChangeDetectorRef,
        private customScriptHostService: CustomScriptHostService,
        private editorActions: EditorActionsService,
        private entityResolver: EntityResolver,
        private errorHandler: ErrorHandler,
        private folderActions: FolderActionsService,
        private notification: I18nNotification,
        private navigationService: NavigationService,
        private permissionService: PermissionService,
        private urlBuilder: ResourceUrlBuilder,
        private tagEditorService: TagEditorService,
        private userSettings: UserSettingsService,
        private _element: ElementRef,
        private modalService: ModalService,
        private i18n: I18nService,
    ) {}

    ngOnInit(): void {

        const editorState$ = this.appState.select(state => state.editor);

        this.tagfillLightState$ = this.appState.select(state => state.features.tagfill_light);

        this.expandedState$ = editorState$.pipe(
            map(value => value.openObjectPropertyGroups),
            startWith([]),
        );

        const currNodeId$ = editorState$.pipe(
            map(state => state.nodeId),
            filter(nodeId => !!nodeId),
            distinctUntilChanged(isEqual),
        );

        const newTagEditorEnabledForCurrentNode$ = currNodeId$.pipe(
            switchMap(nodeId => this.appState.select(state => state.features.nodeFeatures[nodeId])),
            filter(nodeFeatures => !!nodeFeatures),
            map(nodeFeatures => nodeFeatures.indexOf(NodeFeature.newTagEditor) !== -1),
        );

        this.itemWithObjectProperties$ = this.item$.pipe(
            switchMap(item => this.loadFolderWithTags(item)),
            map(changedItem => ({ changedItem, objProperties: this.generateObjectPropertiesList(changedItem) })),
            switchMap(objProperties =>
                this.augmentObjPropertiesWithTagTypes(objProperties.objProperties).pipe(
                    map(augmentedObjProps => ({ item: objProperties.changedItem, objProperties: augmentedObjProps})),
                ),
            ),
            publishReplay(1),
            refCount(),
        );

        this.itemWithContentTags$ = this.item$.pipe(
            map(item => ({ item, properties: this.generateContentTagList(item as Page) })),
            publishReplay(1),
            refCount(),
        );

        this.objectPropertiesGrouped$ = this.itemWithObjectProperties$.pipe(
            map(itemWithObjProps => itemWithObjProps.objProperties),
            distinctUntilChanged(isEqual),
            map(objectProperties => this.groupObjectPropertiesByCategory(objectProperties)),
            publishReplay(1),
            refCount(),
        );

        this.objectPropertiesGroupedDelayed$ = this.objectPropertiesGrouped$.pipe(
            delay(0), // to make sure we don't get ExpressionChangedAfterItWasChecked
        );

        this.activeTabId$ = this.itemWithObjectProperties$.pipe(
            filter(item => !!item.item),
            switchMap(() =>
                editorState$.pipe(
                    map(state => state.openPropertiesTab || ITEM_PROPERTIES_TAB || ITEM_REPORTS_TAB),
                ),
            ),
        ).pipe(
            distinctUntilChanged(isEqual),
            publishReplay(1),
            refCount(),
        );

        this.subscriptions.add(
            this.activeTabId$.subscribe(tabId => {
                if (tabId === ITEM_TAG_LIST_TAB) {
                    this.selectedContentTags$.next([]);
                }

                this.activeTabId = tabId;
            }),
        );

        this.activeTabObjectProperty$ = this.activeTabId$.pipe(
            switchMap(activeTabId => this.itemWithObjectProperties$.pipe(
                map(itemWithObjProps => ({ activeTabId, itemWithObjProps }) ),
            )),
            map(({activeTabId, itemWithObjProps}) => {
                if (activeTabId && activeTabId !== ITEM_PROPERTIES_TAB) {
                    const objProp = itemWithObjProps.objProperties.find(objProp => objProp.name === activeTabId);
                    if (objProp) {
                        // Check if the tag is editable by the current user via the readOnly property
                        // readOnly: No Update Permission on the tag
                        this.hasUpdatePermission = !objProp.readOnly;
                        return { item: itemWithObjProps.item as ItemWithObjectTags, tag: objProp };
                    }
                }

                this.hasUpdatePermission = true;
                return null;
            }),
            publishReplay(1),
            refCount(),
        );

        this.useNewTagEditor$ = newTagEditorEnabledForCurrentNode$.pipe(
            switchMap(newTagEditorEnabled => combineLatest(
                this.activeTabObjectProperty$.pipe(
                    filter(objProp => !!objProp && !!objProp.tag.tagType),
                ),
                observableOf(newTagEditorEnabled),
            )),
            map(([objProp, newTagEditorEnabled]) =>
                newTagEditorEnabled && objProp.tag.tagType.newEditor,
            ),
            distinctUntilChanged(isEqual),
            publishReplay(1),
            refCount(),
        );

        this.disableTagList$ = newTagEditorEnabledForCurrentNode$.pipe(
            map(enabled => !enabled),
        );

        this.itemProperties$ = this.item$.pipe(
            switchMap(item => this.loadItemFolder(item)),
            switchMap(itemAndFolder => this.loadLanguagesAndTemplates(itemAndFolder.item, itemAndFolder.folder)),
            startWith({
                item: null,
                languages: [],
                templates: [],
            }),
            publishReplay(1),
            refCount(),
        );

        const currNodeSub = currNodeId$.subscribe(nodeId => this.currentNode = this.entityResolver.getNode(nodeId));
        const useNewTagEditorSub = this.useNewTagEditor$.subscribe(useNewTagEditor => this.useNewTagEditor = useNewTagEditor);
        this.subscriptions.add(currNodeSub);
        this.subscriptions.add(useNewTagEditorSub);
    }

    ngAfterViewInit(): void {
        // This emits the current newTagEditorHost instance if useNewTagEditor is true or null if useNewTagEditor is false.
        const tagEditorHost$ = combineLatest(
            this.useNewTagEditor$,
            this.tagEditorHostList.changes.pipe(startWith(this.tagEditorHostList)),
        ).pipe(
            filter(([useNewTagEditor, queryList]) => !useNewTagEditor || queryList.length > 0),
            map(([useNewTagEditor, queryList]) => useNewTagEditor ? queryList.first as TagEditorHostComponent : null),
            distinctUntilChanged(isEqual),
        );

        // This emits with all the infos necessary for editing an object property, whenever the object property changes.
        const objPropAndTagEditor$ = this.activeTabObjectProperty$.pipe(
            filter(objProp => !!objProp && !!objProp.tag.tagType),
            switchMap(objProp => combineLatest(
                observableOf(objProp),
                this.loadItemPermissions(objProp.item),
                tagEditorHost$,
            )),
            tap(() => {
                // Workaround: Trigger AfterContentInit Life-cycle hook to initialize active state of the tabs
                if (this.propertiesTabs) {
                    this.propertiesTabs.ngAfterContentInit();
                }
            }),
        );
        const editObjPropSub = objPropAndTagEditor$.subscribe(([objProp, itemPermissions, tagEditorHost]) => {
            // Update item permissions
            this.itemPermissions = itemPermissions;
            if (tagEditorHost) {
                this.oldTagfillUrl = null;
                this.editObjectProperty(objProp.tag, objProp.item, tagEditorHost);
            } else {
                this.loadOldTagfill(objProp.tag, objProp.item);
            }
            this.changeDetector.markForCheck();
        });
        this.subscriptions.add(editObjPropSub);
        this.tagEditorHostList.notifyOnChanges();
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();

        // Reset the currently opened tab. This is necessary, because if an object property was opened and this would remain in the state,
        // then the next time the CombinedPropertiesEditor is opened, object property editing would be started for just an instant.
        // This could lead to objectPropertyModified = true in the state.
        this.appState.dispatch(new ChangeTabAction(this.appState.now.editor.openTab, ITEM_PROPERTIES_TAB));
    }

    ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        if (changes.item && !this.appState.now.editor.objectPropertiesModified) {
            this.item$.next(changes.item.currentValue);
        }
    }

    onTabChange(newTabId: string, readOnly: boolean = false): void {
        clearTimeout(this.oldTagFillStateFallbackTimeout);

        this.navigationService
            .detailOrModal(this.currentNode.id, this.item.type, this.item.id, 'editProperties', {
                openTab: 'properties',
                propertiesTab: newTabId,
                readOnly,
            })
            .navigate();

        observableOf(null).pipe(
            delay(0),
        ).subscribe(() => this.scrollToRight());
    }

    onOldTagEditorIFrameLoad(state: boolean): void {
        // If no permission to edit, remove focus from the iframe
        if (this.canSave === false && document.activeElement && (document.activeElement as any).blur) {
            (document.activeElement as HTMLIFrameElement).blur();
        }

        clearTimeout(this.oldTagFillStateFallbackTimeout);

        this.oldTagFillIFrameState = state;

        // If connection is slow, show the iframe after timeout
        if (!this.oldTagFillIFrameState) {
            this.oldTagFillStateFallbackTimeout = setTimeout(() => {
                this.oldTagFillIFrameState = true;
                this.changeDetector.markForCheck();
            }, 1000);
        }
    }

    toggleAllContentTagSelected(tags: Tag[]): void {
        if (this.selectedContentTags$.value.length === tags.length) {
            this.selectedContentTags$.next([]);
        } else {
            this.selectedContentTags$.next(tags.map(tag => tag.id));
        }
    }

    isContentTagSelectedAll(tags: Tag[]): boolean {
        if (this.selectedContentTags$.value.length === tags.length) {
            return true;
        }
        return false;
    }

    isContentTagSelected(tagElement: Tag): boolean {
        return this.selectedContentTags$.value.includes(tagElement.id);
    }

    toggleSelectContentTag(tagElement: Tag, selected: boolean): void {
        let newSelection: number[];

        if (selected === true) {
            newSelection = this.selectedContentTags$.value.concat([tagElement.id]);
        } else {
            newSelection = this.selectedContentTags$.value.slice();
            newSelection.splice(newSelection.indexOf(tagElement.id), 1);
        }

        this.selectedContentTags$.next(newSelection);
    }

    openContentTag(item: ItemWithContentTags, tagElement: Tag): void {
        this.tagEditorService.openTagEditor(tagElement, tagElement.construct, item).then(
            (editedTag: EditableTag) => {
                this.saveObjectProperty(editedTag, {}, true).then(() => {
                    this.item$.next(this.item as ItemWithContentTags);
                }).catch(() => {
                    // silence
                });
            },
        ).catch(() => {
            // silence
        });
    }

    deleteSelectedContentTags(item: ItemWithContentTags, tagElements: Tag[]): void {
        const selectedTagElements = tagElements.filter(tagElement => this.selectedContentTags$.value.includes(tagElement.id));
        this.deleteContentTag(item, selectedTagElements);
    }

    deleteContentTag(item: ItemWithContentTags, tagElements: Tag[] | Tag): void {
        if (!Array.isArray(tagElements)) {
            tagElements = [tagElements];
        }

        const options = {
            delete: [ ...tagElements.map(tag => tag.name) ],
        };

        const count = tagElements.length;

        this.modalService.dialog({
            title: this.i18n.translate('modal.confirmation_tag_delete_title'),
            body: this.i18n.translate('modal.delete_tag_confirm_' + (count > 1 ? 'plural' : 'singular') , { count: count }),
            buttons: [
                { label: this.i18n.translate('common.cancel_button'), type: 'secondary' as const, flat: true, returnValue: false, shouldReject: true },
                { label: this.i18n.translate('common.delete_button'), type: 'alert' as const, returnValue: true },
            ],
        })
            .then(dialog => dialog.open())
            .then(result => this.folderActions.updateItemObjectProperties(
                item.type,
                item.id,
                {},
                { showNotification: true, fetchForUpdate: this.itemPermissions.edit, fetchForConstruct: true },
                options,
            ).then(updatedItem => {
                this.item = updatedItem;
                this.changeDetector.markForCheck();
                this.item$.next(this.item);
                this.selectedContentTags$.next([]);
            }));
    }

    toggleDisplayContent(): void {
        this.editedObjectProperty.active = !this.editedObjectProperty.active;
        this.markObjectPropertiesAsModifiedInState(true, this.appState.now.editor.modifiedObjectPropertiesValid);
    }

    toggleDescription(tooltip: TooltipComponent): void {
        tooltip.open();
        // Close tooltip after 3 sec automatically
        setTimeout(() => {
            tooltip.close();
        }, 3000);
    }

    /**
     * Saves the changes in the current tab.
     */
    saveChanges(options: SaveChangesOptions = { }): Promise<void> {
        let updatePromise: Promise<any>;
        if (this.activeTabId === ITEM_PROPERTIES_TAB) {
            updatePromise = this.saveItemProperties();
        } else if (this.activeTabId === ITEM_TAG_LIST_TAB) {
            // No-op, because its saved on changes
        } else {
            updatePromise = this.saveCurrentObjectProperty(options);
        }

        this.appState.dispatch(new StartSavingAction());
        return updatePromise
            .then(() => {
                this.appState.dispatch(new SaveSuccessAction());
                this.markContentAsModifiedInState(false);
                this.markObjectPropertiesAsModifiedInState(false, true);
            })
            .catch(error => {
                this.appState.dispatch(new SaveErrorAction(error.message));
                this.errorHandler.catch(error, { notification: true });
            });
    }

    /**
     * Returns if it's mobile device.
     */
    isMobile(): boolean {
        if ((/Android/i.exec(navigator.userAgent))
            || (/webOS/i.exec(navigator.userAgent))
            || (/iPhone/i.exec(navigator.userAgent))
            || (/iPad/i.exec(navigator.userAgent))
            || (/iPod/i.exec(navigator.userAgent))
            || (/BlackBerry/i.exec(navigator.userAgent))
            || (/Windows Phone/i.exec(navigator.userAgent))
        ) {
            return true;
        } else {
            return false;
        }
    }

    markContentAsModifiedInState(modified: boolean): void {
        this.appState.dispatch(new MarkContentAsModifiedAction(modified));
    }

    markObjectPropertiesAsModifiedInState(modified: boolean, valid: boolean): void {
        this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(modified, valid));
    }

    private scrollToRight(): void {
        const container = (this._element.nativeElement as HTMLElement);

        container.scrollBy({ left: container.offsetWidth, behavior: 'smooth' });
    }

    tabGroupToggled(event: any, name: string): void {
        if (event.expand) {
            this.appState.dispatch(new AddExpandedTabGroupAction(name));
        } else {
            this.appState.dispatch(new RemoveExpandedTabGroupAction(name));
        }
        this.appState.dispatch(new SetOpenObjectPropertyGroupsAction(this.expandedCategories));
    }

    private generateObjectPropertiesList(item: ItemWithObjectTags | Node): ObjectTag[] {
        const objectProperties: ObjectTag[] = [];
        if (item && item.type !== 'node' && item.type !== 'channel' && (item as ItemWithObjectTags).tags) {
            const itemWithTags = item as ItemWithObjectTags;
            for (let key of Object.keys(itemWithTags.tags)) {
                const tag = itemWithTags.tags[key];
                if (tag.type === 'OBJECTTAG') {
                    objectProperties.push(tag as ObjectTag);
                }
            }
        }
        objectProperties.sort((a, b) => a.sortOrder - b.sortOrder);
        return objectProperties;
    }

    private generateContentTagList(item: ItemWithContentTags | Node): Tag[] {
        const contentTagList: Tag[] = [];
        if (item && item.type === 'page' && (item ).tags) {
            const itemWithTags = item ;
            for (let key of Object.keys(itemWithTags.tags)) {
                const tag = itemWithTags.tags[key];
                if (tag.type === 'CONTENTTAG') {
                    contentTagList.push(tag );
                }
            }
        }

        return contentTagList;
    }

    private groupObjectPropertiesByCategory(objectProperties: EditableObjectTag[]): ObjectPropertiesCategory[] {
        const categories: ObjectPropertiesCategory[] = [];
        const categoriesMap = new Map<string, ObjectPropertiesCategory>();
        const othersCategory: ObjectPropertiesCategory = {
            name: OBJ_PROP_CATEGORY_OTHERS,
            objProperties: [],
        };
        categoriesMap.set(OBJ_PROP_CATEGORY_OTHERS, othersCategory);

        objectProperties.forEach(objProp => {
            const categoryName = objProp.categoryName || OBJ_PROP_CATEGORY_OTHERS;
            let category = categoriesMap.get(categoryName);
            if (!category) {
                category = { name: categoryName, objProperties: [] };
                categories.push(category);
                categoriesMap.set(categoryName, category);
            }
            category.objProperties.push(objProp);
        });

        if (othersCategory.objProperties.length) {
            categories.push(othersCategory);
        }
        return categories;
    }

    private augmentObjPropertiesWithTagTypes(objectProperties: ObjectTag[]): Observable<EditableObjectTag[]> {
        // Originally we had to load the TagTypes manually for each Tag.
        // Since this was only possible through a REST endpoint that requires admin permissions,
        // the `Tag.construct` property was added to the REST model.
        // The TagEditor relies on the EditableTag.tagType property, so we just copy the reference here.

        const editableTags: EditableObjectTag[] = objectProperties.map(objProp => ({
            ...objProp,
            tagType: objProp.construct,
        }));
        return observableOf(editableTags);
    }

    private saveItemProperties(): Promise<ItemWithObjectTags | Form | Node | void> {
        const formValue = this.propertiesEditor.changes.getValue();
        if (!formValue) {
            return Promise.resolve(null);
        }

        const itemId = this.item.id;
        const postUpdateBehavior: PostUpdateBehavior = { showNotification: true, fetchForUpdate: true, fetchForConstruct: true };
        let updatePromise: Promise<ItemWithObjectTags | Form | Node | void>;
        switch (this.item.type as ItemType) {
            case 'folder':
                updatePromise = this.folderActions.updateFolderProperties(itemId, formValue as EditableFolderProps, postUpdateBehavior);
                break;
            case 'form':
                updatePromise = this.folderActions.updateFormProperties(itemId, formValue as EditableFormProps, postUpdateBehavior);
                break;
            case 'page':
                updatePromise = this.folderActions.updatePageProperties(itemId, formValue as EditablePageProps, postUpdateBehavior);
                break;
            case 'file':
                updatePromise = this.folderActions.updateFileProperties(itemId, formValue as EditableFileProps, postUpdateBehavior);
                break;
            case 'image':
                updatePromise = this.folderActions.updateImageProperties(itemId, formValue as EditableImageProps, postUpdateBehavior);
                break;
            case 'node':
            case 'channel':
                updatePromise = this.folderActions.updateNodeProperties(itemId, formValue as EditableNodeProps);
                break;
            default:
                throw new Error(`Type not recognized: ${JSON.stringify(this.item, null, 2)}`);
        }

        return updatePromise;
    }

    private saveCurrentObjectProperty(options: SaveChangesOptions): Promise<void> {
        if (this.useNewTagEditor) {
            return this.saveObjectProperty(this.editedObjectProperty, options, true);
        } else {
            return this.saveCurrentObjectPropertyWithOldTagfill(options);
        }
    }

    /**
     * Saves the specified object property and then set the current item to the updated item.
     */
    private saveObjectProperty(objectProperty: EditableObjectTag | EditableTag, options: SaveChangesOptions, showNotification: boolean): Promise<void> {
        const objProp = {
            ...objectProperty,
        };
        delete objProp.tagType;
        const update: Tags = { };
        update[objProp.name] = objProp;

        // For folders it is possible to apply the object property to all subfolders as well.
        let requestOptions: FolderSaveRequestOptions;
        if (this.item.type === 'folder' && options.applyToSubfolders) {
            requestOptions = {
                nodeId: this.item.nodeId,
                tagsToSubfolders: [ objProp.name ],
            };
        }

        if (this.item.type === 'page' && options.applyToLanguageVariants) {
            const languageVariantsUpdate = options.applyToLanguageVariants.map(languageVariantId => ({
                itemId: languageVariantId,
                updatedObjProps: update,
                requestOptions,
            }));

            return this.folderActions.updateItemsObjectProperties(
                (this.item as ItemWithObjectTags).type,
                languageVariantsUpdate,
                { showNotification, fetchForUpdate: this.itemPermissions.edit, fetchForConstruct: true },
            )
                .then(updatedItems => {
                    this.item = updatedItems.find(item => item.id === this.item.id);
                    this.changeDetector.markForCheck();
                });
        }

        return this.folderActions.updateItemObjectProperties(
            (this.item as ItemWithObjectTags).type,
            this.item.id,
            update,
            { showNotification, fetchForUpdate: this.itemPermissions.edit, fetchForConstruct: true },
            requestOptions,
        )
            .then(updatedItem => {
                this.item = updatedItem;
                this.changeDetector.markForCheck();
            });
    }

    /**
     * Saves the currently edited object property using the old tagfill and then
     * fetches the updated item and sets it as the current item.
     */
    private saveCurrentObjectPropertyWithOldTagfill(options: SaveChangesOptions): Promise<void> {
        const itemType = this.item.type;
        const saveRequest = this.customScriptHostService.saveObjectProperty()
            .then(() => this.reloadItem(this.item as ItemWithObjectTags));
        let ret: Promise<void>;

        if (this.item.type === 'folder' && options.applyToSubfolders) {
            ret = saveRequest.then(updatedItem => {
                const updatedObjProp: EditableObjectTag = {
                    ...updatedItem.tags[this.editedObjectProperty.name] as ObjectTag,
                    tagType: this.editedObjectProperty.tagType,
                };
                return this.saveObjectProperty(updatedObjProp, options, true);
            });
        } else if (this.item.type === 'page' && options.applyToLanguageVariants) {
            ret = saveRequest.then(updatedItem => {
                const updatedObjProp: EditableObjectTag = {
                    ...updatedItem.tags[this.editedObjectProperty.name] as ObjectTag,
                    tagType: this.editedObjectProperty.tagType,
                };

                // Remove already updated page
                options.applyToLanguageVariants = options.applyToLanguageVariants.filter(id => id !== this.item.id);
                return this.saveObjectProperty(updatedObjProp, options, true);
            });
        } else {
            ret = saveRequest.then(updatedItem => {
                this.item = updatedItem;
                this.changeDetector.markForCheck();
                this.notification.show({
                    type: 'success',
                    message: 'message.updated_item',
                    translationParams: { _type: itemType },
                });
            });
        }

        return ret.catch(error => this.errorHandler.catch(error));
    }

    private loadFolderWithTags(item: ItemWithObjectTags | Node): Observable<ItemWithObjectTags | Node> {
        if (item && item.type === 'folder' && !item.tags) {
            return from(this.folderActions.getFolder(item.id, { construct: true, update: true }));
        }

        return observableOf(item);
    }

    private loadItemFolder(item: ItemWithObjectTags | Node): Observable<{ item: ItemWithObjectTags | Node, folder: Folder }> {
        if (item.type === 'folder') {
            return observableOf({ item: item, folder: item });
        }

        const existingFolderEntity = this.entityResolver.getFolder(item.folderId);
        if (existingFolderEntity) {
            return observableOf({ item: item, folder: existingFolderEntity });
        }

        // Somehow, a node loads after the folder when the node's folder properties are loaded on refresh
        // TODO: Get the root cause of this
        const itemId = item.type === 'node' && !item.folderId ? item.id : item.folderId;
        return from(this.folderActions.getFolder(itemId, { construct: true })).pipe(
            map(folder => ({ item, folder })),
        );
    }

    private loadLanguagesAndTemplates(item: ItemWithObjectTags | Node, folder: Folder): Observable<{
        item: ItemWithObjectTags | Node,
        languages: Language[],
        templates: Template[]
    }> {
        return combineLatest(
            this.loadLanguages(folder),
            this.loadTemplates(folder),
        ).pipe(
            map(([languages, templates]) => ({ item, languages, templates })),
        );
    }

    private loadLanguages(folder: Folder): Observable<Language[]> {
        const node = this.entityResolver.getNode(folder.nodeId);
        const folderState = this.appState.now.folder;

        if (node.id === folderState.activeNode && !folderState.activeNodeLanguages.fetching && folderState.activeNodeLanguages.total) {
            const languageIds = folderState.activeNodeLanguages.list;
            const languages = languageIds.map(id => this.entityResolver.getLanguage(id));
            return observableOf(languages);
        }

        return this.api.folders.getLanguagesOfNode(node.id).pipe(
            map(response => response.languages),
        );
    }

    private loadTemplates(folder: Folder): Observable<Template[]> {
        const folderState = this.appState.now.folder;
        if (folder.id === folderState.activeFolder && !folderState.templates.fetching && folderState.templates.total) {
            const templateIds = folderState.templates.list;
            const templates = templateIds.map(id => this.entityResolver.getTemplate(id));
            return observableOf(templates);
        }

        const options: TemplateListRequest = {
            nodeId: folder.nodeId,
            pageSize: -1,
            recursive: false,
        };

        return this.api.folders.getTemplates(folder.id, options).pipe(
            map(response => response.templates),
        );
    }

    private loadItemPermissions(item: ItemWithObjectTags | Node): Observable<ItemPermissions> {
        const isNode = item.type === 'node' || item.type === 'channel';
        const itemId = isNode ? (item ).folderId : item.id;
        const itemType = isNode ? 'folder' : item.type as FolderItemOrTemplateType;
        return this.permissionService.forItem(itemId, itemType, this.currentNode.id);
    }

    private editObjectProperty(objProp: EditableObjectTag, item: ItemWithObjectTags, tagEditorHost: TagEditorHostComponent): void {
        objProp = cloneDeep(objProp);
        const isReadOnly = this.checkIfReadOnly(objProp);
        if (this.appState.now.features.tagfill_light) {
            objProp.active = true;
        }
        this.editedObjectProperty = objProp;

        const tagEditorContext = this.tagEditorService.createTagEditorContext({
            tag: objProp,
            node: this.currentNode,
            tagOwner: item,
            tagType: objProp.tagType,
            readOnly: isReadOnly,
        });

        const isValid = tagEditorContext.validator.validateAllTagProperties(objProp.properties).allPropertiesValid;
        this.markObjectPropertiesAsModifiedInState(false, isValid && !isReadOnly);

        tagEditorHost.editTagLive(objProp, tagEditorContext, (tagProperties) => {
            if (tagProperties) {
                this.editedObjectProperty.properties = tagProperties;
                this.markObjectPropertiesAsModifiedInState(true, true);
            } else {
                this.markObjectPropertiesAsModifiedInState(true, false);
            }
        });
    }

    private loadOldTagfill(objProp: EditableObjectTag, item: ItemWithObjectTags): void {
        const isReadOnly = this.checkIfReadOnly(objProp);

        // If the objProp does not have an ID yet, the object property was defined after the item was created (the object tag is not in the DB yet).
        // This is no problem when using the new tag editor, but for the old tagfill the ID is needed.
        // Thus we save the item with the tag once (like the old UI) to store the tag in the DB to get its ID.
        let objPropIdLoaded: Promise<void>;
        if (typeof objProp.id === 'number') {
            objPropIdLoaded = Promise.resolve();
        } else {
            if (!isReadOnly) {
                objPropIdLoaded = this.saveObjectProperty(objProp, {}, false)
                    .then(() => {
                        item = this.item as ItemWithObjectTags;
                        objProp = {
                            ...objProp,
                            ...item.tags[objProp.name] as ObjectTag,
                        };
                    });
            } else {
                objPropIdLoaded = Promise.reject();
            }
        }

        objPropIdLoaded.then(() => {
            this.editedObjectProperty = cloneDeep(objProp);
            const wasActive = objProp.active;
            const folderId = item.type !== 'folder' ? item.folderId : item.motherId;
            const tagContainsOverview = objProp.tagType.parts.some(tagPart => tagPart.type === TagPropertyType.OVERVIEW);
            this.oldTagfillUrl = this.urlBuilder.objectPropertyTagfill(objProp.id, item.id, folderId, this.currentNode.id, item.type, tagContainsOverview);
            this.markObjectPropertiesAsModifiedInState(!wasActive && !isReadOnly, !isReadOnly);
        }).catch(error => {
            this.oldTagfillUrl = 'about:blank';
            if (isReadOnly) {
                this.notification.show({
                    type: 'alert',
                    message: 'editor.object_property_no_edit_permission',
                });
            } else {
                this.errorHandler.catch(error);
            }
            this.markObjectPropertiesAsModifiedInState(false, false);
        });
    }

    private reloadItem(item: ItemWithObjectTags): Promise<ItemWithObjectTags> {
        return this.folderActions.getItem(
            item.id,
            item.type,
            { nodeId: this.currentNode.id, construct: true, update: this.itemPermissions.edit },
        ) as Promise<ItemWithObjectTags>;
    }

    private checkIfReadOnly(objProp: ObjectTag): boolean {
        return !this.itemPermissions.edit || objProp.readOnly;
    }
}
