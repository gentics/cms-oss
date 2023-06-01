import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, QueryList, ViewChildren } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { fileSchema, folderSchema, imageSchema, nodeSchema, pageSchema, templateSchema } from '@editor-ui/app/common/models';
import { MarkObjectPropertiesAsModifiedAction, SetHideExtrasAction, addNormalizedEntities } from '@editor-ui/app/state';
import { WindowRef, coerceToBoolean } from '@gentics/cms-components';
import {
    EditableTag,
    EntityType,
    ItemPermissions,
    ItemWithObjectTags,
    Node,
    ObjectTag,
    Raw,
    Tag,
    TagEditorChangeMessage,
    Template,
    noItemPermissions,
} from '@gentics/cms-models';
import { cloneDeep, isEqual } from 'lodash';
import { Schema, normalize } from 'normalizr';
import { Observable, Subscription, combineLatest, from, of, throwError } from 'rxjs';
import { distinctUntilChanged, filter, map, publishReplay, refCount, startWith, switchMap, tap } from 'rxjs/operators';
import { ApplicationStateService, EditorActionsService, FolderActionsService, SetNodeFeaturesAction } from '../../../state';
import { TagEditorHostComponent, TagEditorService } from '../../../tag-editor';
import { Api } from '../../providers/api';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { PermissionService } from '../../providers/permissions/permission.service';
import { UsersnapService } from '../../providers/usersnap/usersnap.service';

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
    tagfillLightEnabled$: Observable<boolean>;
    showTitle$: Observable<boolean>;

    itemPermissions: ItemPermissions = noItemPermissions;
    tagName: string;
    editedTag: EditableTag;
    savedItem: EditableEntity;

    get canSave(): boolean {
        return this.hasUpdatePermission !== false;
    }

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
        private windowRef: WindowRef,
        private usersnap: UsersnapService,
    ) { }

    ngOnInit(): void {
        this.appState.dispatch(new SetHideExtrasAction(true));
        this.usersnap.destroy();
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
        // This emits the current TagEditorHost
        const tagEditorHost$ = combineLatest([
            this.tagEditorHost.changes.pipe(
                map(hosts => hosts.first),
                startWith(null),
            ),
        ]).pipe(
            map(([editor]) => editor),
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

            this.editTag(itemAndTag.tag, itemAndTag.item, this.entityResolver.getNode(nodeId), host);
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
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

    private checkIfReadOnly(tag: Tag): boolean {
        return !this.itemPermissions.edit || (tag.type === 'CONTENTTAG' && (tag as ObjectTag).readOnly);
    }

    markObjectPropertiesAsModifiedInState(modified: boolean, valid: boolean): void {
        this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(modified, valid));
    }
}
