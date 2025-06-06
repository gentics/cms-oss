import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    QueryList,
    SimpleChange,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import {
    EditableNodeProps,
    EditableProperties,
    ITEM_PROPERTIES_TAB,
    ITEM_REPORTS_TAB,
    ITEM_TAG_LIST_TAB,
    PropertiesTab,
    noItemPermissions,
} from '@editor-ui/app/common/models';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import {
    AddExpandedTabGroupAction,
    ApplicationStateService,
    ChangeTabAction,
    FolderActionsService,
    MarkContentAsModifiedAction,
    MarkObjectPropertiesAsModifiedAction,
    PostUpdateBehavior,
    RemoveExpandedTabGroupAction,
    SaveErrorAction,
    SaveSuccessAction,
    StartSavingAction,
} from '@editor-ui/app/state';
import {
    TagEditorHostComponent,
    TagEditorService,
} from '@editor-ui/app/tag-editor';
import { EditMode } from '@gentics/cms-integration-api-models';
import {
    EditableFileProps,
    EditableFolderProps,
    EditableFormProps,
    EditableImageProps,
    EditableObjectTag,
    EditablePageProps,
    EditableTag,
    Feature,
    Folder,
    FolderItemOrTemplateType,
    FolderSaveRequestOptions,
    Form,
    ItemPermissions,
    ItemType,
    ItemWithContentTags,
    ItemWithObjectTags,
    Language,
    Node,
    ObjectTag,
    Page,
    Tag,
    TagPropertyMap,
    Tags,
    Template,
    TemplateFolderListRequest,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import {
    GroupedTabsComponent,
    ModalService,
    TableAction,
    TableActionClickEvent,
    TableColumn,
    TableRow,
    TableSelectAllType,
    TooltipComponent,
} from '@gentics/ui-core';
import { cloneDeep, isEqual, merge } from 'lodash-es';
import {
    BehaviorSubject,
    Observable,
    Subscription,
    combineLatest,
    forkJoin,
    from,
    of,
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
import { generateContentTagList, getItemProperties } from '../../utils';

/** Allows to define additional options for saving. */
export interface SaveChangesOptions {
    applyToSubfolders?: boolean;
    applyToLanguageVariants?: number[];
}

export interface ObjectPropertiesCategory {
    id: string;
    name: string;
    objProperties: EditableObjectTag[];
}

function isObjectPropertyTag(tag: Tag): tag is ObjectTag {
    return tag.type === 'OBJECTTAG';
}

export const ID_OBJ_PROP_CATEGORY_OTHERS = '_others_';
export const NAME_OBJ_PROP_CATEGORY_OTHERS = 'editor.object_properties_category_others_label';

export function groupObjectPropertiesByCategory(objectProperties: EditableObjectTag[]): ObjectPropertiesCategory[] {
    const categories: ObjectPropertiesCategory[] = [];
    const categoriesMap = new Map<string, ObjectPropertiesCategory>();
    const othersCategory: ObjectPropertiesCategory = {
        id: ID_OBJ_PROP_CATEGORY_OTHERS,
        name: NAME_OBJ_PROP_CATEGORY_OTHERS,
        objProperties: [],
    };
    categoriesMap.set(ID_OBJ_PROP_CATEGORY_OTHERS, othersCategory);

    objectProperties.forEach(objProp => {
        const categoryId = `${objProp.categoryId || ID_OBJ_PROP_CATEGORY_OTHERS}`;
        const categoryName = objProp.categoryName || NAME_OBJ_PROP_CATEGORY_OTHERS;

        let category = categoriesMap.get(categoryId);
        if (!category) {
            category = {
                id: categoryId,
                name: categoryName,
                objProperties: [],
            };
            categories.push(category);
            categoriesMap.set(categoryId, category);
        }

        category.objProperties.push(objProp);
    });

    if (othersCategory.objProperties.length) {
        categories.push(othersCategory);
    }

    return categories;
}

const ACTION_DELETE = 'delete';
const ACTION_ACTIVATE = 'activate';
const ACTION_DEACTIVATE = 'deactivate';

/**
 * Displays vertical tabs, which contain one tab for the item's properties (PropertiesEditor component)
 * and one tab for each of the item's object properties.
 */
@Component({
    selector: 'combined-properties-editor',
    templateUrl: './combined-properties-editor.component.html',
    styleUrls: ['./combined-properties-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CombinedPropertiesEditorComponent implements OnInit, AfterViewInit, OnDestroy, OnChanges {

    /* Constants for the Template */
    public readonly ITEM_PROPERTIES_TAB = ITEM_PROPERTIES_TAB;
    public readonly ITEM_REPORTS_TAB = ITEM_REPORTS_TAB;
    public readonly ITEM_TAG_LIST_TAB = ITEM_TAG_LIST_TAB;
    public readonly TableSelectAllType = TableSelectAllType;

    /** The item, whose properties should be edited. */
    @Input()
    item: ItemWithObjectTags | Form | Node;

    @Input()
    isDisabled: boolean;

    @Input()
    useRouter = true;

    @Input()
    itemClean: boolean;

    @Input()
    nodeId: number;

    @Output()
    itemCleanChange = new EventEmitter<boolean>();

    @Output()
    itemChange = new EventEmitter<ItemWithObjectTags | Form | Node>();

    @ViewChild(GroupedTabsComponent, { static: false })
    propertiesTabs: GroupedTabsComponent;

    @ViewChildren(TagEditorHostComponent)
    tagEditorHostList: QueryList<TagEditorHostComponent>;

    pointObjProp: any;
    position: string;

    public contentTagRows: TableRow<Tag>[] = [];
    public contentTagColumns: TableColumn<Tag>[] = [];
    public contentTagActions: TableAction<Tag>[] = [];
    public contentTagSelection: string[] = [];

    /** The current properties of the item which are being edited. */
    public editingProperties: EditableProperties;

    activeTabId$: Observable<string>;
    itemWithObjectProperties$: Observable<{ item: ItemWithObjectTags | Node, objProperties: EditableObjectTag[] }>;
    activeTabObjectProperty$: Observable<{ item: ItemWithObjectTags, tag: EditableObjectTag }>;
    itemProperties$: Observable<{
        item: ItemWithObjectTags | Node,
        languages: Language[],
        templates: Template[]
    }>;
    currentNode: Node;
    objectPropertiesGrouped: ObjectPropertiesCategory[] = [];
    expandedObjectPropertyCategories: string[] = [];

    get canSave(): boolean {
        return this.hasUpdatePermission !== false;
    }

    private hasUpdatePermission = false;

    private item$ = new BehaviorSubject<ItemWithObjectTags | Node>(null);
    editedObjectProperty: EditableObjectTag;
    private activeTabId: PropertiesTab;
    private subscriptions: Subscription[] = [];
    private internalActiveTab = new BehaviorSubject<string>(ITEM_PROPERTIES_TAB);
    public tagFillLightEnabled = true;

    itemPermissions: ItemPermissions = noItemPermissions;

    selectedContentTags$ = new BehaviorSubject<number[]>([]);

    constructor(
        private client: GCMSRestClientService,
        private appState: ApplicationStateService,
        private changeDetector: ChangeDetectorRef,
        private entityResolver: EntityResolver,
        private errorHandler: ErrorHandler,
        private folderActions: FolderActionsService,
        private navigationService: NavigationService,
        private permissionService: PermissionService,
        private tagEditorService: TagEditorService,
        private elementRef: ElementRef<HTMLElement>,
        private modalService: ModalService,
        private i18n: I18nService,
        private userSettings: UserSettingsService,
    ) {}

    ngOnInit(): void {
        const editorState$ = this.appState.select(state => state.editor);

        this.contentTagColumns = [
            {
                id: 'name',
                fieldPath: 'name',
                label: this.i18n.translate('editor.tagname_label'),
                clickable: true,
            },
            {
                id: 'type',
                fieldPath: 'construct.name',
                label: this.i18n.translate('editor.tagtype_label'),
                clickable: true,
            },
            {
                id: 'active',
                fieldPath: 'active',
                label: this.i18n.translate('editor.obj_prop_active_label'),
                align: 'center',
                clickable: true,
            },
        ];
        this.rebuildContentTagActions();

        this.subscriptions.push(editorState$.subscribe(state => {
            this.expandedObjectPropertyCategories = state.openObjectPropertyGroups.slice(0);
            this.changeDetector.markForCheck();
        }));

        const currNodeId$ = editorState$.pipe(
            map(state => state.nodeId),
            filter(nodeId => !!nodeId),
            distinctUntilChanged(isEqual),
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

        this.subscriptions.push(this.item$.subscribe(item => {
            const tags = generateContentTagList(item as Page);
            this.contentTagRows = tags.map(tag => {
                return {
                    id: `${tag.id}`,
                    item: tag,
                };
            });
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.itemWithObjectProperties$.pipe(
            map(itemWithObjProps => itemWithObjProps.objProperties),
            distinctUntilChanged(isEqual),
            map(objectProperties => groupObjectPropertiesByCategory(objectProperties)),
        ).subscribe(data => {
            this.objectPropertiesGrouped = data;
            this.changeDetector.markForCheck();
        }));

        this.activeTabId$ = combineLatest([
            this.itemWithObjectProperties$.pipe(
                filter(item => !!item.item),
                switchMap(() => editorState$),
                map(state => state.openPropertiesTab || ITEM_PROPERTIES_TAB || ITEM_REPORTS_TAB),
            ),
            this.internalActiveTab.asObservable().pipe(
                startWith(ITEM_PROPERTIES_TAB),
            ),
        ]).pipe(
            map(([stateTab, internalTab]) => this.useRouter ? stateTab : internalTab),
            distinctUntilChanged(isEqual),
        );

        this.subscriptions.push(this.activeTabId$.subscribe(tabId => {
            if (tabId === ITEM_TAG_LIST_TAB) {
                this.selectedContentTags$.next([]);
            }

            this.activeTabId = tabId;
            this.changeDetector.markForCheck();
        }));

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

        this.itemProperties$ = this.item$.pipe(
            filter(item => item != null),
            distinctUntilChanged(isEqual),
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

        this.subscriptions.push(this.appState.select(state => state.features[Feature.TAGFILL_LIGHT]).subscribe(enabled => {
            this.tagFillLightEnabled = enabled;
            this.rebuildContentTagActions();
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(currNodeId$.subscribe(nodeId => {
            this.currentNode = this.entityResolver.getNode(nodeId);
            this.changeDetector.markForCheck();
        }));

        if (this.nodeId) {
            this.currentNode = this.entityResolver.getNode(this.nodeId);
        }
    }

    ngAfterViewInit(): void {
        // This emits the current TagEditorHost instance
        const tagEditorHost$ = combineLatest([
            this.tagEditorHostList.changes.pipe(startWith(this.tagEditorHostList)),
        ]).pipe(
            map(([queryList]) => queryList.first as TagEditorHostComponent),
            distinctUntilChanged(isEqual),
        );

        // This emits with all the infos necessary for editing an object property, whenever the object property changes.
        const objPropAndTagEditor$ = this.activeTabObjectProperty$.pipe(
            filter(objProp => !!objProp && !!objProp.tag.tagType),
            switchMap(objProp => combineLatest([
                of(objProp),
                this.loadItemPermissions(objProp.item),
                tagEditorHost$,
            ])),
            filter(([,,host]) => host != null),
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
            this.editObjectProperty(objProp.tag, objProp.item, tagEditorHost);
            this.changeDetector.markForCheck();
        });

        this.subscriptions.push(editObjPropSub);
        this.tagEditorHostList.notifyOnChanges();
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());

        // Reset the currently opened tab. This is necessary, because if an object property was opened and this would remain in the state,
        // then the next time the CombinedPropertiesEditor is opened, object property editing would be started for just an instant.
        // This could lead to objectPropertyModified = true in the state.
        this.appState.dispatch(new ChangeTabAction(this.appState.now.editor.openTab, ITEM_PROPERTIES_TAB));
    }

    ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        if (changes.item) {
            this.editingProperties = getItemProperties(this.item);
            if (!this.appState.now.editor.objectPropertiesModified) {
                this.item$.next(this.item as any);
            }
        }
    }

    handlePropChanges(changes: EditableProperties): void {
        this.editingProperties = changes;
    }

    onTabChange(newTabId: string, readOnly: boolean = false): void {
        if (!this.useRouter) {
            this.item$.next(this.item as any);
            this.internalActiveTab.next(newTabId);
            return;
        }

        this.navigationService
            .detailOrModal(this.currentNode.id, this.item.type, this.item.id, EditMode.EDIT_PROPERTIES, {
                openTab: 'properties',
                propertiesTab: newTabId,
                readOnly,
            })
            .navigate();

        of(null).pipe(
            delay(0),
        ).subscribe(() => this.scrollToRight());
    }

    openContentTag(item: ItemWithContentTags, tagElement: Tag): void {
        this.tagEditorService.openTagEditor(tagElement, tagElement.construct, item).then(
            (result) => {
                this.saveObjectProperty(result.tag as any, {}, true).then(() => {
                    this.item$.next(this.item as ItemWithContentTags);
                }).catch(() => {
                    // silence
                });
            },
        ).catch(() => {
            // silence
        });
    }

    rebuildContentTagActions(): void {
        this.contentTagActions = [
            {
                id: ACTION_DELETE,
                enabled: true,
                icon: 'delete',
                label: this.i18n.translate('editor.tagtype_delete_label'),
                type: 'alert',
                single: true,
                multiple: true,
            },
        ];

        if (!this.tagFillLightEnabled) {
            this.contentTagActions.unshift({
                id: ACTION_ACTIVATE,
                enabled: (item) => item == null || !item.active,
                icon: 'check_circle',
                label: this.i18n.translate('editor.tagtype_activate_label'),
                type: 'success',
                single: true,
                multiple: true,
            }, {
                id: ACTION_DEACTIVATE,
                enabled: (item) => item == null || item.active,
                icon: 'cancel',
                label: this.i18n.translate('editor.tagtype_deactivate_label'),
                type: 'warning',
                single: true,
                multiple: true,
            });
        }
    }

    handleContentTagClick(row: TableRow<Tag>): void {
        this.openContentTag(this.item as ItemWithContentTags, row.item);
    }

    handleContentTagAction(event: TableActionClickEvent<Tag>): void {
        let names: string[] = [];
        if (!event.selection) {
            names = [event.item.name];
        } else {
            names = this.contentTagSelection
                .map(id => this.contentTagRows.find(row => row.id === id)?.item?.name)
                .filter(name => name != null);
        }

        switch (event.actionId) {
            case ACTION_DELETE:
                this.deleteContentTag(this.item as ItemWithContentTags, names);
                break;

            case ACTION_ACTIVATE:
                this.setContentTagActiveState(this.item as ItemWithContentTags, names, true);
                break;

            case ACTION_DEACTIVATE:
                this.setContentTagActiveState(this.item as ItemWithContentTags, names, false);
                break;
        }
    }

    async setContentTagActiveState(item: ItemWithContentTags, tagNames: string[], active: boolean): Promise<void> {
        const tags = tagNames.reduce((acc, name) => {
            acc[name] = {
                name: name,
                active,
            } as any;
            return acc;
        }, {} as Tags);

        try {
            const updatedItem = await this.folderActions.updateItemObjectProperties(
                item.type,
                item.id,
                tags,
                { showNotification: true, fetchForUpdate: this.itemPermissions.edit, fetchForConstruct: true },
                {},
            );

            this.item = updatedItem;
            this.item$.next(this.item);
            this.itemChange.emit(this.item);
            this.contentTagSelection = [];
            this.changeDetector.markForCheck();
        } catch (err) {
            // Nothing to do, error is already properly handled in update method
        }
    }

    deleteContentTag(item: ItemWithContentTags, tagNames: string[]): void {
        const options = {
            delete: tagNames,
        };

        const count = tagNames.length;
        const translateType = count > 1 ? 'plural' : 'singular';

        this.modalService.dialog({
            title: this.i18n.translate(`modal.confirmation_tag_delete_${translateType}_title`),
            body: this.i18n.translate(`modal.delete_tag_confirm_${translateType}` , {
                count: count,
                names: `<ul class="browser-default"><li>${tagNames.join('</li><li>')}</li></ul>`,
                name: tagNames[0],
            }),
            buttons: [
                {
                    label: this.i18n.translate('common.cancel_button'),
                    type: 'secondary',
                    flat: true,
                    returnValue: false,
                    shouldReject: true,
                },
                {
                    label: this.i18n.translate('common.delete_button'),
                    type: 'alert',
                    returnValue: true,
                },
            ],
        })
            .then(dialog => dialog.open())
            .then(() => this.folderActions.updateItemObjectProperties(
                item.type,
                item.id,
                {},
                { showNotification: true, fetchForUpdate: this.itemPermissions.edit, fetchForConstruct: true },
                options,
            ).then(updatedItem => {
                this.item = updatedItem;
                this.item$.next(this.item);
                this.itemChange.emit(this.item);
                this.contentTagSelection = [];
                this.changeDetector.markForCheck();
            }));
    }

    forwardItemCleanChange(value: boolean): void {
        this.itemCleanChange.emit(value);
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
            updatePromise = Promise.resolve();
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

    markContentAsModifiedInState(modified: boolean): void {
        this.appState.dispatch(new MarkContentAsModifiedAction(modified));
    }

    markObjectPropertiesAsModifiedInState(modified: boolean, valid: boolean): void {
        this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(modified, valid));
    }

    private scrollToRight(): void {
        const container = this.elementRef.nativeElement;
        container.scrollBy({ left: container.offsetWidth, behavior: 'smooth' });
    }

    async tabGroupToggled(event: { id: string, expand: boolean }, name: string): Promise<void> {
        if (event.expand) {
            await this.appState.dispatch(new AddExpandedTabGroupAction(name)).toPromise();
        } else {
            await this.appState.dispatch(new RemoveExpandedTabGroupAction(name)).toPromise();
        }
        this.userSettings.setOpenObjectPropertyGroups(this.appState.now.editor.openObjectPropertyGroups);
    }

    private generateObjectPropertiesList(item: ItemWithObjectTags | Node): ObjectTag[] {
        return Object.values((item as ItemWithObjectTags)?.tags || {})
            .filter(isObjectPropertyTag)
            .sort((a, b) => a.sortOrder - b.sortOrder);
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
        return of(editableTags);
    }

    public saveItemProperties(
        postUpdateBehavior: PostUpdateBehavior = { showNotification: true, fetchForUpdate: true, fetchForConstruct: true },
    ): Promise<ItemWithObjectTags | Form | Node | void> {
        const formValue = this.editingProperties;
        if (!formValue) {
            return Promise.resolve(null);
        }

        const itemId = this.item.id;
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
                updatePromise = this.folderActions.updateNodeProperties(itemId, formValue as EditableNodeProps, postUpdateBehavior);
                break;
            default:
                throw new Error(`Type not recognized: ${JSON.stringify(this.item, null, 2)}`);
        }

        return updatePromise;
    }

    private saveCurrentObjectProperty(options: SaveChangesOptions): Promise<void> {
        return this.saveObjectProperty(this.editedObjectProperty, options, true);
    }

    /**
     * Saves the specified object property and then set the current item to the updated item.
     */
    private saveObjectProperty(objectProperty: EditableObjectTag | EditableTag, options: SaveChangesOptions, showNotification: boolean): Promise<void> {
        const objProp = structuredClone(objectProperty);
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
                    this.item$.next(this.item);
                    this.itemChange.emit(this.item);
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
                this.item$.next(this.item);
                this.itemChange.emit(this.item);
                this.changeDetector.markForCheck();
            });
    }

    public saveAllObjectProperties(
        postUpdateBehavior: PostUpdateBehavior & Required<Pick<PostUpdateBehavior, 'fetchForUpdate'>>
        = { showNotification: true, fetchForUpdate: true, fetchForConstruct: true },
    ): Promise<void> {
        if (this.item == null) {
            return Promise.reject(new Error('No Item present'));
        }

        if (this.item.type === 'node' || this.item.type === 'channel') {
            return Promise.reject(new Error('Cannot save Node Object-Properties'));
        }

        return this.folderActions.updateItemObjectProperties(
            (this.item as ItemWithObjectTags).type,
            (this.item as ItemWithObjectTags).id,
            (this.item as ItemWithObjectTags).tags,
            postUpdateBehavior,
        )
            .then(updatedItem => {
                this.item = updatedItem;
                this.item$.next(updatedItem);
                this.itemChange.emit(this.item);
                this.changeDetector.markForCheck();
            });
    }

    private loadFolderWithTags(item: ItemWithObjectTags | Node): Observable<ItemWithObjectTags | Node> {
        if (item && item.type === 'folder' && !item.tags) {
            return from(this.folderActions.getFolder(item.id, { construct: true, update: true }));
        }

        return of(item);
    }

    private loadItemFolder(item: ItemWithObjectTags | Node): Observable<{ item: ItemWithObjectTags | Node, folder: Folder }> {
        if (item.type === 'folder') {
            return of({ item: item, folder: item });
        }

        const existingFolderEntity = this.entityResolver.getFolder(item.folderId);
        if (existingFolderEntity) {
            return of({ item: item, folder: existingFolderEntity });
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
        return forkJoin([
            this.loadLanguages(folder),
            this.loadTemplates(folder),
        ]).pipe(
            map(([languages, templates]) => ({ item, languages, templates })),
        );
    }

    private loadLanguages(folder: Folder): Observable<Language[]> {
        const node = this.entityResolver.getNode(folder.nodeId);
        const folderState = this.appState.now.folder;

        if (node.id === folderState.activeNode && !folderState.activeNodeLanguages.fetching && folderState.activeNodeLanguages.total) {
            const languageIds = folderState.activeNodeLanguages.list;
            const languages = languageIds.map(id => this.entityResolver.getLanguage(id));
            return of(languages);
        }

        return this.client.node.listLanguages(node.id).pipe(
            map(response => response.languages),
        );
    }

    private loadTemplates(folder: Folder): Observable<Template[]> {
        const folderState = this.appState.now.folder;
        if (folder.id === folderState.activeFolder && !folderState.templates.fetching && folderState.templates.total) {
            const templateIds = folderState.templates.list;
            const templates = templateIds.map(id => this.entityResolver.getTemplate(id));
            return of(templates);
        }

        const options: TemplateFolderListRequest = {
            nodeId: folder.nodeId,
            maxItems: -1,
            recursive: false,
        };

        return this.client.folder.templates(folder.id, options).pipe(
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
        if (this.tagFillLightEnabled) {
            objProp.active = true;
        }
        this.editedObjectProperty = objProp;

        const tagEditorContext = this.tagEditorService.createTagEditorContext({
            tag: objProp,
            node: this.currentNode,
            tagOwner: item,
            tagType: objProp.tagType,
            readOnly: isReadOnly,
            withDelete: false,
        });

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        const isValid = tagEditorContext.validator.validateAllTagProperties(objProp.properties).allPropertiesValid;
        this.markObjectPropertiesAsModifiedInState(false, isValid && !isReadOnly);

        tagEditorHost.editTagLive(objProp, tagEditorContext, (tagProperties) => {
            this.handleObjectPropertyChange(tagProperties);
        });
    }

    public handleObjectPropertyChange(tagProperties: TagPropertyMap): void {
        if (!tagProperties) {
            this.markObjectPropertiesAsModifiedInState(true, false);
            return;
        }

        this.editedObjectProperty.properties = tagProperties;
        this.markObjectPropertiesAsModifiedInState(true, true);

        (this.item as any).properties = merge((this.item as any).properties || {}, this.editedObjectProperty.properties);
        (this.item as any).tags = {
            ... (this.item as any).tags,
            [this.editedObjectProperty.name]: this.editedObjectProperty,
        }

        this.itemChange.emit(this.item);
    }

    private checkIfReadOnly(objProp: ObjectTag): boolean {
        return !this.itemPermissions.edit || objProp.readOnly;
    }
}
