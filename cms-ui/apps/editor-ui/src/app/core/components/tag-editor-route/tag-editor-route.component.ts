import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, QueryList, ViewChildren } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { fileSchema, folderSchema, imageSchema, nodeSchema, pageSchema, templateSchema } from '@editor-ui/app/common/models';
import { MarkObjectPropertiesAsModifiedAction, SetHideExtrasAction, addNormalizedEntities } from '@editor-ui/app/state';
import { WindowRef, coerceToBoolean } from '@gentics/cms-components';
import {
    EditableObjectTag,
    EditableTag,
    EntityType,
    FolderSaveRequestOptions,
    ItemPermissions,
    ItemWithObjectTags,
    Node,
    NodeFeature,
    ObjectTag,
    Raw,
    Tag,
    TagEditorChangeMessage,
    TagPropertyType,
    Tags,
    Template,
    noItemPermissions,
} from '@gentics/cms-models';
import { NotificationService } from '@gentics/ui-core';
import { cloneDeep, isEqual } from 'lodash';
import { Schema, normalize } from 'normalizr';
import { Observable, Subscription, combineLatest, from, of, throwError } from 'rxjs';
import { distinctUntilChanged, filter, map, publishReplay, refCount, startWith, switchMap, tap } from 'rxjs/operators';
import { SaveChangesOptions } from '../../../content-frame/components/combined-properties-editor/combined-properties-editor.component';
import { ApplicationStateService, EditorActionsService, FolderActionsService, SetNodeFeaturesAction } from '../../../state';
import { TagEditorHostComponent, TagEditorService } from '../../../tag-editor';
import { Api } from '../../providers/api';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../providers/error-handler/error-handler.service';
import { PermissionService } from '../../providers/permissions/permission.service';
import { ResourceUrlBuilder } from '../../providers/resource-url-builder/resource-url-builder';

type EditableEntity = ItemWithObjectTags | Template<Raw>;

@Component({
    selector: 'gtx-tag-editor-route',
    templateUrl: './tag-editor-route.component.html',
    styleUrls: ['./tag-editor-route.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    })
export class TagEditorRouteComponent implements OnInit, AfterViewInit, OnDestroy {

    @ViewChildren(TagEditorHostComponent)
    tagEditorHost: QueryList<TagEditorHostComponent>;

    nodeId$: Observable<number>;
    item$: Observable<EditableEntity>;
    tagName$: Observable<string>;
    tagToEdit$: Observable<{ item: EditableEntity, tag: EditableTag }>;
    useNewTagEditor$: Observable<boolean>;
    tagfillLightEnabled$: Observable<boolean>;
    showTitle$: Observable<boolean>;

    oldTagfillUrl: string;
    oldTagFillIFrameState = false;
    showOldTagFill = false;

    itemPermissions: ItemPermissions = noItemPermissions;
    tagName: string;
    editedTag: EditableTag;
    savedItem: EditableEntity;

    get canSave(): boolean {
        return this.hasUpdatePermission !== false;
    }

    private oldTagFillStateFallbackTimeout: any;
    private subscription = new Subscription();
    private hasUpdatePermission = false;
    private currentEntityType: EntityType;
    private currentEntityId: string | number;
    private currentNodeId: number;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private route: ActivatedRoute,
        private appState: ApplicationStateService,
        private api: Api,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
        private editorActions: EditorActionsService,
        private permissionService: PermissionService,
        private tagEditorService: TagEditorService,
        private urlBuilder: ResourceUrlBuilder,
        private notification: NotificationService,
        private errorHandler: ErrorHandler,
        private windowRef: WindowRef,
    ) { }

    ngOnInit(): void {
        this.appState.dispatch(new SetHideExtrasAction(true));
        this.tagfillLightEnabled$ = this.appState.select(state => state.features.tagfill_light);

        this.tagName$ = this.route.paramMap.pipe(
            map(params => params.get('tagName')),
            distinctUntilChanged(isEqual),
            tap(value => this.tagName = value),
        );

        this.nodeId$ = this.route.paramMap.pipe(
            map(params => Number(params.get('nodeId'))),
            filter(nodeId => !!nodeId || !Number.isInteger(nodeId)),
            distinctUntilChanged(isEqual),
            switchMap(nodeId => combineLatest([
                from(this.folderActions.getNode(nodeId) as Promise<Node>),
                this.api.folders.getNodeFeatures(nodeId),
            ])),
            map(([node, featureResponse]) => ({ node, features: featureResponse.features })),
            tap(({ node, features }) => {
                if (node) {
                    this.entityResolver.entities = addNormalizedEntities(this.entityResolver.entities, normalize(node, nodeSchema));
                    this.appState.dispatch(new SetNodeFeaturesAction(node.id, features));
                    this.currentNodeId = node.id;
                }
            }),
            map(({ node }) => node?.id),
            distinctUntilChanged(isEqual),
        );

        const entityInfo$ = this.route.paramMap.pipe(
            map(params => {
                const type = params.get('entityType');
                const id = params.get('entityId');
                return { type, id };
            }),
            distinctUntilChanged(isEqual),
            tap(({ type, id }) => {
                this.currentEntityType = type;
                this.currentEntityId = id;
            }),
        );

        this.item$ = combineLatest([
            entityInfo$,
            this.nodeId$,
        ]).pipe(
            map(([{ type, id }, nodeId]) => ({ type, id, nodeId })),
            switchMap(({ type, id, nodeId }) => {
                let schema: Schema<any>;
                let options: any = {};

                switch (type) {
                    case 'page':
                        schema = pageSchema;
                        options = { nodeId };
                        break;

                    case 'folder':
                        schema = folderSchema;
                        options = { nodeId };
                        break;

                    case 'image':
                        schema = imageSchema;
                        options = { nodeId };
                        break;

                    case 'file':
                        schema = fileSchema;
                        options = { nodeId };
                        break;

                    case 'template':
                        schema = templateSchema;
                        options = { nodeId, construct: true, update: true };
                        break;

                    default:
                        // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                        return throwError(new Error(`The type "${type}" is invalid!`));
                }

                return from(this.folderActions.getItem(id, type, options)).pipe(
                    map(entity => ({ type, id, entity, schema })),
                );
            }),
            filter(({ entity }) => !!entity),
            tap(({ entity, schema }) => {
                this.entityResolver.entities = addNormalizedEntities(this.entityResolver.entities, normalize(entity, schema));
            }),
            map(({ entity }) => entity as any),
        );

        this.tagToEdit$ = combineLatest([
            this.item$.pipe(switchMap(item => this.loadFolderWithTags(item))),
            this.tagName$,
        ]).pipe(
            filter(([item, tagName]) => !!item && !!tagName),
            map(([item, tagName]) => ({
                item: item ,
                tag: this.getTagFromItem(item, tagName),
            })),
            tap(elem => {
                this.hasUpdatePermission = elem?.tag != null && elem.tag.type === 'OBJECTTAG'
                    ? !(elem.tag as any as ObjectTag).readOnly
                    : true;
            }),
            publishReplay(1),
            refCount(),
        );

        const newTagEditorEnabledForCurrentNode$ = this.nodeId$.pipe(
            switchMap(nodeId => this.appState.select(state => state.features.nodeFeatures[nodeId])),
            filter(nodeFeatures => !!nodeFeatures),
            map(nodeFeatures => nodeFeatures.indexOf(NodeFeature.newTagEditor) !== -1),
        );

        this.useNewTagEditor$ = newTagEditorEnabledForCurrentNode$.pipe(
            switchMap(newTagEditorEnabled => combineLatest([
                this.tagToEdit$.pipe(
                    filter(objProp => !!(objProp?.tag?.tagType)),
                ),
                of(newTagEditorEnabled),
            ])),
            map(([objProp, newTagEditorEnabled]) =>
                newTagEditorEnabled && objProp.tag.tagType.newEditor,
            ),
            distinctUntilChanged(isEqual),
        );

        this.subscription.add(combineLatest([
            this.item$,
            this.nodeId$,
        ]).subscribe(([item, nodeId]) => {
            if (item && nodeId) {
                this.editorActions.editProperties(item.id, item.type as any, nodeId);
            }
        }));

        this.showTitle$ = this.route.queryParamMap.pipe(
            map(params => coerceToBoolean(params.get('title'))),
        );

        this.subscription.add(this.route.queryParamMap.pipe(
            map(params => coerceToBoolean(params.get('transparent'))),
            distinctUntilChanged(isEqual),
        ).subscribe(transparentBg => {
            const bodyClasses = this.windowRef.nativeWindow.document.body.classList;
            bodyClasses.remove('transparent-bg');
            if (transparentBg) {
                bodyClasses.add('transparent-bg');
            }
        }));
    }

    ngAfterViewInit(): void {
        // This emits the current newTagEditorHost instance if useNewTagEditor is true or null if useNewTagEditor is false.
        const tagEditorHost$ = combineLatest([
            this.useNewTagEditor$,
            this.tagEditorHost.changes.pipe(
                map(hosts => hosts.first),
                startWith(null),
            ),
        ]).pipe(
            map(([useNewTagEditor, editor]) => useNewTagEditor ? editor : null),
            distinctUntilChanged(isEqual),
        );

        this.subscription.add(combineLatest([
            tagEditorHost$,
            this.tagToEdit$,
            this.nodeId$,
            combineLatest([this.item$, this.nodeId$]).pipe(
                switchMap(([item, nodeId]) => this.loadItemPermissions(item, nodeId)),
            ),
        ]).subscribe(([host, itemAndTag, nodeId, permissions]) => {
            // Update item permissions
            this.itemPermissions = permissions;

            // Store the item for other references
            if (this.savedItem == null) {
                this.savedItem = itemAndTag.item;
            }

            if (host) {
                this.oldTagfillUrl = null;
                this.editTag(itemAndTag.tag, itemAndTag.item, this.entityResolver.getNode(nodeId), host);
            } else {
                this.loadOldTagfill(itemAndTag.tag, itemAndTag.item, nodeId);
            }
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    onOldTagEditorIFrameLoad(state: boolean): void {
        // If no permission to edit, remove focus from the iframe
        if (this.canSave === false && document.activeElement && (document.activeElement as any).blur) {
            (document.activeElement as HTMLIFrameElement).blur();
        }

        clearTimeout(this.oldTagFillStateFallbackTimeout);

        // Has unloaded - means a navigation occurred. Usually these lead to an error page
        // which do not make sense, therefore briefly hide the Iframe, and then show it again.
        // This resets the Iframe and everything is well.
        // if (this.oldTagFillIFrameState && !state) {
        //     this.showOldTagFill = false;
        // }

        this.oldTagFillIFrameState = state;
        this.changeDetector.markForCheck();

        // If connection is slow, show the iframe after timeout
        if (!this.oldTagFillIFrameState) {
            this.oldTagFillStateFallbackTimeout = setTimeout(() => {
                this.oldTagFillIFrameState = true;
                this.showOldTagFill = true;
                this.changeDetector.markForCheck();
            }, 1000);
        }
    }

    private loadFolderWithTags(item: EditableEntity): Observable<EditableEntity> {
        if (item && item.type === 'folder' && !item.tags) {
            return from(this.folderActions.getFolder(item.id, { construct: true, update: true }));
        }

        return of(item);
    }

    private getTagFromItem(item: EditableEntity, tagName: string): EditableTag {
        if (item) {
            if ((item as ItemWithObjectTags).tags) {
                const tag = (item as ItemWithObjectTags).tags[tagName];
                if (tag != null && tag.type === 'OBJECTTAG') {
                    return this.augmentTagWithTagType(tag);
                }
            }

            if ((item as Template).objectTags) {
                const tag = (item as Template).objectTags[tagName];
                if (tag != null && tag.type === 'OBJECTTAG') {
                    return this.augmentTagWithTagType(tag);
                }
            }

            if ((item as Template).templateTags) {
                const tag = (item as Template).templateTags[tagName];
                if (tag != null && tag.type === 'TEMPLATETAG') {
                    return this.augmentTagWithTagType(tag);
                }
            }
        }

        return null;
    }

    private augmentTagWithTagType(tag: Tag): EditableTag {
        // Originally we had to load the TagTypes manually for each Tag.
        // Since this was only possible through a REST endpoint that requires admin permissions,
        // the `Tag.construct` property was added to the REST model.
        // The TagEditor relies on the EditableTag.tagType property, so we just copy the reference here.

        const clone: EditableTag = cloneDeep(tag) as any;
        if (clone.construct) {
            clone.tagType = clone.construct;
        }

        return clone;
    }

    private loadItemPermissions(item: EditableEntity, nodeId: number): Observable<ItemPermissions> {
        if (item == null) {
            return of(null);
        }

        return this.permissionService.forItem(item.id, item.type, nodeId);
    }

    private editTag(
        tag: EditableTag,
        item: EditableEntity,
        node: Node,
        tagEditorHost: TagEditorHostComponent,
    ): void {
        tag = cloneDeep(tag);
        const isReadOnly = this.checkIfReadOnly(tag);
        if (this.appState.now.features.tagfill_light) {
            tag.active = true;
        }
        this.editedTag = tag;

        const tagEditorContext = this.tagEditorService.createTagEditorContext({
            tag: tag,
            node: node,
            tagOwner: item,
            tagType: tag.tagType,
            readOnly: isReadOnly,
        });

        const isValid = tagEditorContext.validator.validateAllTagProperties(tag.properties).allPropertiesValid;
        this.markObjectPropertiesAsModifiedInState(false, isValid && !isReadOnly);

        tagEditorHost.editTagLive(tag, tagEditorContext, (tagProperties) => {
            if (tagProperties) {
                this.editedTag.properties = tagProperties;
                this.markObjectPropertiesAsModifiedInState(true, true);
            } else {
                this.markObjectPropertiesAsModifiedInState(true, false);
            }

            const msg: TagEditorChangeMessage = {
                type: 'tag-editor-change',
                modified: true,
                valid: !!tagProperties,
                tagName: this.tagName,
                tag: cloneDeep(this.editedTag),
                entityType: this.currentEntityType,
                entityId: this.currentEntityId,
                nodeId: this.currentNodeId,
            };

            let targetWindow: Window = this.windowRef.nativeWindow;

            // If this route is embedded in an iframe (which is usually is),
            // then we need to get the parent window and post a message there instead.
            if (targetWindow.parent) {
                targetWindow = targetWindow.parent;
            } else if (targetWindow.opener) {
                targetWindow = targetWindow.opener;
            }

            targetWindow.postMessage(msg, '*');
        });
    }

    private loadOldTagfill(tag: EditableTag, item: EditableEntity, nodeId: number): void {
        const isReadOnly = this.checkIfReadOnly(tag);

        // If the tag does not have an ID yet, the tag was defined after the item was created (the tag is not in the DB yet).
        // This is no problem when using the new tag editor, but for the old tagfill the ID is needed.
        // Thus we save the item with the tag once (like the old UI) to store the tag in the DB to get its ID.
        let tagLoaded: Promise<void>;

        if (typeof tag.id === 'number') {
            tagLoaded = Promise.resolve();
        } else if (isReadOnly) {
            tagLoaded = Promise.reject();
        } else {
            tagLoaded = this.saveTag(tag, {}, false)
                .then(() => {
                    item = this.savedItem ;
                    if (tag.type === 'CONTENTTAG') {
                        tag = {
                            ...tag,
                            ...(item as ItemWithObjectTags).tags?.[tag.name],
                        };
                    } else if (tag.type === 'TEMPLATETAG') {
                        tag = {
                            ...tag,
                            ...(item as Template).templateTags?.[tag.name],
                        };
                    } else if (tag.type === 'OBJECTTAG') {
                        tag = {
                            ...tag,
                            ...(item as Template).objectTags?.[tag.name],
                        };
                    }
                });
        }

        tagLoaded.then(() => {
            this.editedTag = cloneDeep(tag);
            const wasActive = tag.active;
            const folderId = item.type !== 'folder' ? item.folderId : item.motherId;
            const tagContainsOverview = tag.tagType.parts.some(tagPart => tagPart.type === TagPropertyType.OVERVIEW);
            this.showOldTagFill = true;
            this.oldTagfillUrl = this.urlBuilder.objectPropertyTagfill(
                tag.id,
                item.id,
                folderId,
                nodeId,
                item.type,
                tagContainsOverview,
            );
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

    /**
     * Saves the specified object property and then set the current item to the updated item.
     */
    private saveTag(
        originalTag: EditableObjectTag | EditableTag,
        options: SaveChangesOptions,
        showNotification: boolean,
    ): Promise<void> {
        if (!this.savedItem) {
            return Promise.resolve();
        }

        const tag = cloneDeep(originalTag);
        delete tag.tagType;
        const update: Tags = {};
        update[tag.name] = tag;

        // For folders it is possible to apply the object property to all subfolders as well.
        let requestOptions: FolderSaveRequestOptions;
        if (this.savedItem.type === 'folder' && options.applyToSubfolders) {
            requestOptions = {
                tagsToSubfolders: [tag.name],
            };
        }

        if (this.savedItem.type === 'page' && options.applyToLanguageVariants) {
            const languageVariantsUpdate = options.applyToLanguageVariants.map(languageVariantId => ({
                itemId: languageVariantId,
                updatedObjProps: update,
                requestOptions,
            }));

            return this.folderActions.updateItemsObjectProperties(
                (this.savedItem ).type,
                languageVariantsUpdate,
                { showNotification, fetchForUpdate: this.itemPermissions.edit, fetchForConstruct: true },
            )
                .then(updatedItems => {
                    this.savedItem = updatedItems.find(item => item.id === this.savedItem.id);
                    this.changeDetector.markForCheck();
                });
        }

        if (this.savedItem.type === 'template') {
            const body: Partial<Template> = {};
            if (tag.type === 'TEMPLATETAG') {
                body.templateTags = update as any;
            } else if (tag.type === 'CONTENTTAG') {
                body.objectTags = update as any;
            }

            return this.folderActions.updateItem(
                'template',
                this.savedItem.id,
                body,
                null as never,
                { showNotification, fetchForUpdate: this.itemPermissions.edit, fetchForConstruct: true },
            ).then(updatedItem => {
                this.savedItem = updatedItem as Template;
                this.changeDetector.markForCheck();
            });
        }

        return this.folderActions.updateItemObjectProperties(
            (this.savedItem ).type,
            this.savedItem.id,
            update,
            { showNotification, fetchForUpdate: this.itemPermissions.edit, fetchForConstruct: true },
            requestOptions,
        )
            .then(updatedItem => {
                this.savedItem = updatedItem;
                this.changeDetector.markForCheck();
            });
    }

    private checkIfReadOnly(tag: Tag): boolean {
        return !this.itemPermissions.edit || (tag.type === 'CONTENTTAG' && (tag as ObjectTag).readOnly);
    }

    markObjectPropertiesAsModifiedInState(modified: boolean, valid: boolean): void {
        this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(modified, valid));
    }
}
