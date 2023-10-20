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
import { ITEM_PROPERTIES_TAB, ITEM_REPORTS_TAB, ITEM_TAG_LIST_TAB, PropertiesTab } from '@editor-ui/app/common/models';
import { Api } from '@editor-ui/app/core/providers/api/api.service';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
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
    ObjectTag,
    Page,
    Tag,
    Tags,
    Template,
    TemplateFolderListRequest,
    noItemPermissions,
} from '@gentics/cms-models';
import {
    GroupedTabsComponent,
    ModalService,
    TooltipComponent,
} from '@gentics/ui-core';
import {cloneDeep, isEqual, merge, startsWith} from 'lodash-es';
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

    @Input()
    isDisabled: boolean;

    @Input()
    useRouter = true;

    @Input()
    nodeId: number;

    pointObjProp: any;
    position: string;

    tagfillLightState$: Observable<boolean>;

    activeTabId$: Observable<string>;
    itemWithObjectProperties$: Observable<{ item: ItemWithObjectTags | Node, objProperties: EditableObjectTag[] }>;
    itemWithContentTags$: Observable<any>;
    objectPropertiesGrouped$: Observable<ObjectPropertiesCategory[]>;
    objectPropertiesGroupedDelayed$: Observable<ObjectPropertiesCategory[]>;
    activeTabObjectProperty$: Observable<{ item: ItemWithObjectTags, tag: EditableObjectTag }>;
    itemProperties$: Observable<{
        item: ItemWithObjectTags | Node,
        languages: Language[],
        templates: Template[]
    }>;
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
    private subscriptions: Subscription[] = [];
    private internalActiveTab = new BehaviorSubject<string>('item-properties');
    private activeTabObjectProperty: { item: ItemWithObjectTags, tag: EditableObjectTag };

    expandedState$: Observable<string[]>;

    itemPermissions: ItemPermissions = noItemPermissions;

    selectedContentTags$ = new BehaviorSubject<number[]>([]);

    constructor(
        private api: Api,
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

        this.activeTabId$ = combineLatest([
            this.itemWithObjectProperties$.pipe(
                filter(item => !!item.item),
                switchMap(() => editorState$),
                map(state => state.openPropertiesTab || ITEM_PROPERTIES_TAB || ITEM_REPORTS_TAB),
            ),
            this.internalActiveTab.asObservable().pipe(
                startWith('item-properties'),
            ),
        ]).pipe(
            map(([stateTab, internalTab]) => this.useRouter ? stateTab : internalTab),
            distinctUntilChanged(isEqual),
        );

        this.subscriptions.push(
            this.activeTabId$.subscribe(tabId => {
                if (tabId === ITEM_TAG_LIST_TAB) {
                    this.selectedContentTags$.next([]);
                }

                this.activeTabId = tabId;

                if (this.editedObjectProperty) {
                    debugger
                    (this.item as any).properties = this.editedObjectProperty.properties;
                }
                setTimeout(() => {
                    if (!this.propertiesEditor) {
                        return;
                    }

                    this.propertiesEditor.changes.subscribe(props => {

                        this.item = merge(this.item, props)
                        /*
                        this.item = {
                            ... this.item,
                            ... props,
                        } as any;

                         */
                    });
                }, 200);
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

        this.subscriptions.push(
            this.activeTabObjectProperty$.subscribe(prop => this.activeTabObjectProperty = prop),
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

        const currNodeSub = currNodeId$.subscribe(nodeId => {
            this.currentNode = this.entityResolver.getNode(nodeId)
        });
        this.subscriptions.push(currNodeSub);

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
            switchMap(objProp => combineLatest(
                observableOf(objProp),
                this.loadItemPermissions(objProp.item),
                tagEditorHost$,
            )),
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
