import { Injectable } from '@angular/core';
import {
    AppState,
    EditorPermissions,
    getNoPermissions,
} from '@editor-ui/app/common/models';
import { ApplicationStateService } from '@editor-ui/app/state/providers/application-state/application-state.service';
import { ItemFetchingSuccessAction } from '@editor-ui/app/state/modules/folder/folder.actions';
import {
    DefaultPermissionsFactory,
    FolderInstancePermissions,
    InstancePermissions,
    TypePermissions,
    UniformInstancePermissions,
    UniformTypePermissions,
} from '@gentics/cms-components';
import {
    AccessControlledType,
    File,
    FilePermissions,
    Folder,
    FolderItemOrTemplateType,
    FolderItemType,
    FolderPermissions,
    Form,
    FormPermissions,
    GcmsPermission,
    GcmsRolePrivilege,
    GcmsRolePrivilegeMapCollection,
    Image,
    ImagePermissions,
    InheritableItem,
    Item,
    ItemPermissions,
    Language,
    Node,
    Page,
    PagePermissions,
    PermissionResponse,
    PermissionsMapCollection,
    PrivilegeMap,
    PrivilegeMapFromServer,
    RolePrivileges,
    Template,
} from '@gentics/cms-models';
import { cloneDeep, isEqual } from 'lodash-es';
import { Observable, combineLatest, forkJoin, of } from 'rxjs';
import {
    catchError,
    distinctUntilChanged,
    filter,
    map,
    mergeMap,
    publishReplay,
    refCount,
    switchMap,
    take,
    tap,
    withLatestFrom,
} from 'rxjs/operators';
import { deepEqual } from '../../../common/utils/deep-equal';
import { Api } from '../api/api.service';
import { EntityResolver } from '../entity-resolver/entity-resolver';

// Debugging purposes only
declare const window: Window & {
    gcmsui_getUserPermissions(): any;
};

/** A hash of observables that emit the permissions of an item type */
export interface ObservableItemPermissions {
    create$: Observable<boolean>;
    edit$: Observable<boolean>;
    delete$: Observable<boolean>;
    import$: Observable<boolean>;
    inherit$: Observable<boolean>;
    localize$: Observable<boolean>;
    publish$: Observable<boolean>;
    translate$: Observable<boolean>;
    view$: Observable<boolean>;
}

/**
 * A service that provides a central way to check for user permissions.
 */
@Injectable()
export class PermissionService {
    /** Emits a hash of all privileges when the privileges changed after switching folder. */
    all$: Observable<EditorPermissions>;

    /** Emits true if the current user can assign user permissions to a folder in the current folder and node. */
    assignPermissions$: Observable<boolean>;

    /** A hash containing observables for folder permissions */
    folder: ObservableItemPermissions;

    /** A hash containing observables for form permissions */
    form: Partial<ObservableItemPermissions>;

    /** A hash containing observables for page permissions */
    page: ObservableItemPermissions;

    /** A hash containing observables for file permissions */
    file: ObservableItemPermissions;

    /** A hash containing observables for image permissions */
    image: ObservableItemPermissions;

    /** A hash containing observables for template permissions */
    template: ObservableItemPermissions;

    /** Emits true when the user can link a template in the current folder and node. */
    linkTemplate$: Observable<boolean>;

    /** Emits true if the current user can synchronize changes to the parent node in the current folder and node. */
    synchronizeChannel$: Observable<boolean>;

    /** Emits true if the current user can view his message inbox. */
    viewInbox$: Observable<boolean>;

    /** Emits true if the current user can view the publish queue. */
    viewPublishQueue$: Observable<boolean>;

    /** Emits true if the current user can view the wastebin and restore items from it. */
    wastebin$: Observable<boolean>;

    /** Cache pending requests, so querying the same folder twice re-uses the API observable. */
    private pendingApiRequests: {
        [nodeAndFolderId: string]: Observable<PermissionsMapCollection>;
    } = {};

    protected permFactory = new DefaultPermissionsFactory();

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
        private entityResolver: EntityResolver,
    ) {
        // Debug - provide a global method for viewing the current permissions from the console.
        window.gcmsui_getUserPermissions = () => {
            this.all$
                .pipe(take(1))
                .subscribe((privs) => {
                    // eslint-disable-next-line no-console
                    console.info('Privileges:\n' + JSON.stringify(privs, null, 2));
                });
        };

        // An observable that emits the current folder and nodeId when the folder and its permissions were fetched from the server.
        // TODO: (GCU-180) Re-fetch permissions after logging in with a different user.
        //  After GCU-180, this is only one additional distinctUntilChanged()
        const current$: Observable<{
            folder: Folder;
            node: number;
            user: number;
        }> = appState.select((state) => state).pipe(
            distinctUntilChanged((a, b) =>
                a.folder.activeFolder === b.folder.activeFolder &&
                a.folder.activeNode === b.folder.activeNode &&
                a.auth.currentUserId === b.auth.currentUserId &&
                a.folder.activeLanguage === b.folder.activeLanguage,
            ),
            filter((state) => state.auth.isLoggedIn),
            switchMap((state) => appState.select((state) => state.entities.folder[state.folder.activeFolder]).pipe(
                filter((folder) => folder != null && folder.privilegeMap != null),
                distinctUntilChanged(
                    isEqual,
                    (folder) => folder.privilegeMap,
                ),
                map(cloneDeep),
                map((folder) => ({
                    folder,
                    node: state.folder.activeNode,
                    user: state.auth.currentUserId,
                })),
            )),
            switchMap((current) => {
                return this.api.permissions.getFolderPermissions(
                    current.folder.id,
                    current.folder.nodeId,
                ).pipe(
                    map((folderPermissions) => {
                        current.folder.permissionsMap = folderPermissions.permissionsMap;
                        return current;
                    }),
                );
            }),
            // TODO: This workaround of state logic is bad architecture and should be removed as soon as REST API allows for it.
            tap((current) => {
                const rawFolderWithPermissionData = this.entityResolver.denormalizeEntity(
                    'folder',
                    current.folder,
                );
                this.appState.dispatch(new ItemFetchingSuccessAction('folder', rawFolderWithPermissionData));
            }),
            publishReplay(1),
            refCount(),
        );

        const currentLanguage$ = appState.select((state) => state.folder).pipe(
            map((folder) =>folder.activeNodeLanguages.total
                ? folder.activeLanguage
                : undefined,
            ),
            distinctUntilChanged(isEqual),
        );

        // To abstract the relationship of permissions on the server side, we map the binary flags
        // to permissions grouped by item type for ease of use.
        this.all$ = current$.pipe(
            withLatestFrom(currentLanguage$),
            map(([current, languageId]) => {
                const folder = current.folder;
                return this.mapToPermissions(
                    folder.privilegeMap,
                    folder.permissionsMap,
                    languageId,
                );
            }),
            distinctUntilChanged(deepEqual),
            publishReplay(1),
            refCount(),
        );

        const perm = (fn: (p: EditorPermissions) => boolean) =>
            this.all$.pipe(
                map(fn),
                distinctUntilChanged(isEqual),
            );

        this.assignPermissions$ = perm((p) => p.assignPermissions);
        this.linkTemplate$ = perm((p) => p.template.link);
        this.synchronizeChannel$ = perm((p) => p.synchronizeChannel);
        this.wastebin$ = perm((p) => p.wastebin);

        this.folder = {
            create$: perm((p) => p.folder.create),
            delete$: perm((p) => p.folder.delete),
            edit$: perm((p) => p.folder.edit),
            import$: of(false),
            inherit$: perm((p) => p.folder.inherit),
            localize$: perm((p) => p.folder.localize),
            publish$: of(false),
            translate$: of(false),
            view$: perm((p) => p.folder.view),
        };

        this.form = {
            create$: perm((p) => p.form.create),
            delete$: perm((p) => p.form.delete),
            edit$: perm((p) => p.form.edit),
            publish$: perm((p) => p.form.publish),
            view$: perm((p) => p.form.view),
        };

        this.page = {
            create$: perm((p) => p.page.create),
            delete$: perm((p) => p.page.delete),
            edit$: perm((p) => p.page.edit),
            import$: perm((p) => p.page.import),
            inherit$: perm((p) => p.page.inherit),
            localize$: perm((p) => p.page.localize),
            publish$: perm((p) => p.page.publish),
            translate$: perm((p) => p.page.translate),
            view$: perm((p) => p.page.view),
        };

        this.file = {
            create$: perm((p) => p.file.create),
            delete$: perm((p) => p.file.delete),
            edit$: perm((p) => p.file.edit),
            import$: perm((p) => p.file.import),
            inherit$: perm((p) => p.file.inherit),
            localize$: perm((p) => p.file.localize),
            publish$: of(false),
            translate$: of(false),
            view$: perm((p) => p.file.view),
        };

        this.image = {
            create$: perm((p) => p.image.create),
            delete$: perm((p) => p.image.delete),
            edit$: perm((p) => p.image.edit),
            import$: perm((p) => p.image.import),
            inherit$: perm((p) => p.image.inherit),
            localize$: perm((p) => p.image.localize),
            publish$: of(false),
            translate$: of(false),
            view$: perm((p) => p.image.view),
        };

        this.template = {
            create$: perm((p) => p.template.create),
            delete$: perm((p) => p.template.delete),
            edit$: perm((p) => p.template.edit),
            import$: of(false),
            inherit$: perm((p) => p.template.inherit),
            localize$: perm((p) => p.template.localize),
            publish$: of(false),
            translate$: of(false),
            view$: perm((p) => p.template.view),
        };

        const user$ = this.appState
            .select((state) => state.auth.currentUserId)
            .pipe(filter((userId) => userId != null));

        this.viewInbox$ = user$.pipe(
            switchMap(() => this.api.permissions.getInboxPermissions()),
            map((perm) => perm.view),
            distinctUntilChanged(isEqual),
            publishReplay(1),
            refCount(),
        );

        this.viewPublishQueue$ = user$.pipe(
            switchMap(() => this.api.permissions.getPublishQueuePermissions()),
            map((perm) => perm.view),
            distinctUntilChanged(isEqual),
            publishReplay(1),
            refCount(),
        );
    }

    /**
     * Returns an observable that emits the permissionsMap on a specific folder.
     * Set the permissionsMap to a specific folder in the app state.
     */
    getFolderPermissionMap(folderId: number): Observable<PermissionsMapCollection> {
        return this.appState.select((state: AppState) => state.entities.folder[folderId]).pipe(
            switchMap((folder: Folder) => {
                return this.api.permissions.getFolderPermissions(folder.id, folder.nodeId).pipe(
                    map((folderPermissions: PermissionResponse) => {
                        return folder.permissionsMap = folderPermissions.permissionsMap;
                    }),
                );
            }),
        );
    }

    /**
     * Returns an observable that fetches and emits the permissions for a specific folder.
     * The permissions are emitted from the app state or fetched via the API.
     * Role privileges for the current language are merged and re-emitted when the language changes.
     * To receive the permissions only once, chain the return value with `.take(1)`.
     */
    forFolder(folderId: number, nodeId: number): Observable<EditorPermissions> {
        return this.appState.select((state) => state.folder).pipe(
            map((state) => state.activeNodeLanguages.total
                ? state.activeLanguage
                : undefined,
            ),
            distinctUntilChanged(isEqual),
            switchMap((language) =>
                this.forFolderInLanguage(folderId, nodeId, language),
            ),
            distinctUntilChanged(deepEqual),
        );
    }

    /**
     * Returns an observable that fetches and emits the permissions for the parent folder of a specific item.
     * The permissions are returned from the app state or fetched via the API.
     * Role privileges for the current language are merged and re-emitted when the language changes.
     * To receive the permissions only once, chain the return value with `.take(1)`.
     */
    forItem(item: Folder, nodeId: number): Observable<FolderPermissions>;
    forItem(item: Form, nodeId: number): Observable<FormPermissions>;
    forItem(item: Page, nodeId: number): Observable<PagePermissions>;
    forItem(item: File, nodeId: number): Observable<FilePermissions>;
    forItem(item: Image, nodeId: number): Observable<ImagePermissions>;
    forItem(item: Item, nodeId: number): Observable<ItemPermissions>;
    forItem(
        id: number,
        type: 'folder',
        nodeId: number
    ): Observable<FolderPermissions>;
    forItem(
        id: number,
        type: 'form',
        nodeId: number
    ): Observable<FormPermissions>;
    forItem(
        id: number,
        type: 'page',
        nodeId: number
    ): Observable<PagePermissions>;
    forItem(
        id: number,
        type: 'file',
        nodeId: number
    ): Observable<FilePermissions>;
    forItem(
        id: number,
        type: 'image',
        nodeId: number
    ): Observable<ImagePermissions>;
    forItem(
        id: number,
        type: FolderItemOrTemplateType,
        nodeId: number
    ): Observable<ItemPermissions>;

    forItem(
        idOrItem: any,
        typeOrNodeId: FolderItemOrTemplateType | number,
        nodeId?: number,
    ): Observable<ItemPermissions> {
        let item$: Observable<File | Folder | Form | Image | Page | Template>;
        let itemType: FolderItemOrTemplateType;
        let itemId: number;

        if (typeof idOrItem === 'number') {
            itemId = idOrItem ;
            itemType = typeOrNodeId as FolderItemOrTemplateType;

            const appState = this.appState.now;
            const item = (appState.entities[itemType] || [])[itemId];

            if (item) {
                item$ = of(item);
            } else {
                item$ = this.api.folders.getItem(itemId, itemType, { nodeId }).pipe(
                    map((response) => (<any>response)[itemType]),
                );
            }
        } else {
            item$ = of(
                idOrItem as File | Folder | Form | Image | Page | Template,
            );
            nodeId = typeOrNodeId as number;
        }

        if (itemType === 'template') {
            return forkJoin([
                this.api.permissions.getTemplateEditPermissions(itemId),
                this.api.permissions.getTemplateDeletePermissions(itemId),
            ]).pipe(
                map(multiResponses => multiResponses.map(res => res.granted)),
                map(([canEdit, canDelete]) => ({
                    view: canEdit,
                    edit: canEdit,
                    delete: canDelete,
                    // These are simply false on default
                    create: false,
                    inherit: false,
                    localize: false,
                    unlocalize: false,
                })),
            );
        }

        return item$.pipe(
            take(1),
            switchMap((item) => {
                const parentFolderId = this.getParentFolderId(item);
                itemType =
                    itemType ||
                    (item )
                        .type;
                itemId = itemId === undefined ? item.id : itemId;
                const parentPerms$ = this.forFolder(parentFolderId, nodeId);

                switch (itemType) {
                    case 'folder':
                    case 'form':
                    case 'template':
                    case 'page':
                    case 'file':
                    case 'image':
                        return parentPerms$.pipe(
                            map((privs) => privs[itemType]),
                        );
                    default:
                        throw new Error(
                            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                            `Invalid type "${itemType}" in PrivilegeService.forItem`,
                        );
                }
            }),
        );
    }

    /**
     * Returns an observable that emits the permissions on a specific folder.
     * Page, file and image permissions are augmented with group permissions of the passed language.
     */
    forFolderInLanguage(
        folderId: number,
        nodeId: number,
        languageId: number | null,
    ): Observable<EditorPermissions> {
        const sid = this.appState.now.auth.sid;

        // Cache pending requests to the API, so multiple calls to forFolderInLanguage only request once.
        const cache = this.pendingApiRequests;
        const cacheKey = `${nodeId}-${folderId}-${sid}`;

        if (!cache[cacheKey]) {
            cache[cacheKey] = this.api.permissions.getFolderPermissions(folderId, nodeId).pipe(
                map((response) => response.permissionsMap),
                // Remove from cache after 10 seconds
                tap(() => {
                    setTimeout(() => {
                        cache[cacheKey] = undefined;
                    }, 10000);
                }),
                publishReplay(1),
                refCount(),
            );
        }

        // Get the latest state of the folder, as there's a race condition where the
        // folder is being saved without the permission/privilage information.
        return combineLatest([
            cache[cacheKey],
            this.appState.select(state => (state.entities.folder || {})[folderId]).pipe(
                distinctUntilChanged(deepEqual),
            ),
        ]).pipe(
            map(([permMap, folder]) => {
                return this.mapToPermissions(
                    folder ? folder.privilegeMap : undefined,
                    folder && folder.permissionsMap && folder.nodeId === nodeId ? folder.permissionsMap : permMap,
                    languageId,
                );
            }),
        );
    }

    /**
     * Returns an observable that emits the permissions for a specific item type and a specific language.
     */
    public forItemInLanguage(
        type: 'folder',
        id: number,
        nodeId: number,
        lang: number | null
    ): Observable<FolderPermissions>;
    public forItemInLanguage(
        type: 'form',
        id: number,
        nodeId: number,
        lang: number | null
    ): Observable<FormPermissions>;
    public forItemInLanguage(
        type: 'page',
        id: number,
        nodeId: number,
        lang: number | null
    ): Observable<PagePermissions>;
    public forItemInLanguage(
        type: 'file',
        id: number,
        nodeId: number,
        lang: number | null
    ): Observable<FilePermissions>;
    public forItemInLanguage(
        type: 'image',
        id: number,
        nodeId: number,
        lang: number | null
    ): Observable<ImagePermissions>;
    public forItemInLanguage(
        type: FolderItemType,
        id: number,
        nodeId: number,
        lang: number | null
    ): Observable<ItemPermissions>;

    public forItemInLanguage(
        type: FolderItemType,
        id: number,
        nodeId: number,
        lang: number | null,
    ): Observable<ItemPermissions> {
        const entityState = this.appState.now.entities;
        const entityPath = entityState[type];
        const entity = entityPath && entityPath[id];
        let parentFolder$: Observable<number>;
        if (entity) {
            parentFolder$ = of(this.getParentFolderId(entity));
        } else {
            parentFolder$ = this.api.folders.getItem(
                id,
                type as 'folder' | 'form' | 'page' | 'file' | 'image',
                { nodeId },
            ).pipe(
                map((res) => {
                    const item = (<any>res)[type] as
                        | Folder
                        | Form
                        | Page
                        | File
                        | Image;
                    return item.type === 'folder'
                        ? (item ).motherId
                        : (item as Page | Form).folderId;
                }),
            );
        }

        return parentFolder$.pipe(
            mergeMap((parentFolderId) =>
                this.forFolderInLanguage(parentFolderId, nodeId, lang),
            ),
            map((permissions) => (permissions as any)[type]),
        );
    }

    /**
     * Gets the `TypePermissions` for the specified `type`.
     */
    getPermissions(type: AccessControlledType): Observable<TypePermissions>;
    /**
     * Gets the `InstancePermissions` for the specified `type`, `instanceId`, and (optionally) `nodeId`.
     */
    getPermissions(
        type: AccessControlledType,
        instanceId: number | string,
        nodeId?: number
    ): Observable<InstancePermissions>;
    /**
     * Gets the `FolderInstancePermissions` for the specified `instanceId`, and (optionally) `nodeId`.
     */
    getPermissions(
        type: AccessControlledType.FOLDER,
        instanceId: number | string,
        nodeId?: number
    ): Observable<FolderInstancePermissions>;
    /**
     * Gets the `TypePermissions` for the specified `type` or,
     * if `instanceId` is set, the `InstancePermissions` for the specified `type`, `instanceId`, and (optionally) `nodeId`.
     *
     * This is a convenience method to avoid having to select between `getTypePermissions()` and `getInstancePermissions()` manually.
     */
    getPermissions(
        type: AccessControlledType,
        instanceId?: number | string,
        nodeId?: number,
    ): Observable<TypePermissions> {
        if (typeof instanceId === 'number' || typeof instanceId === 'string') {
            return this.getInstancePermissions(type, instanceId, nodeId);
        } else {
            return this.getTypePermissions(type);
        }
    }

    /**
     * Gets the `TypePermissions` for the specified `type`.
     *
     * If the permissions are not yet in the AppState or if `forceRefresh` is true,
     * they are fetched from the CMS and then stored in the AppState.
     * Whenever the permissions in the AppState change, the returned observable emits again.
     */
    getTypePermissions(
        type: AccessControlledType,
        forceRefresh: boolean = false,
    ): Observable<TypePermissions> {
        // Very basic implementation without integration to the state
        // TODO: Integrate into the state, just like instance-permissions?
        return this.api.permissions.getPermissionsForType(type).pipe(
            map(response => {
                const permissionsMap = response.permissionsMap;

                if (permissionsMap && (permissionsMap.permissions || permissionsMap.rolePermissions)) {
                    return this.permFactory.createPermissionsFromMaps(type, permissionsMap);
                } else {
                    // If permissionsMap is not set, there was an error fetching the permissions.
                    return new UniformTypePermissions(type, false);
                }
            }),
        )
    }

    /**
     * Gets the `InstancePermissions` for the specified `type`, `instanceId`, and (optionally) `nodeId`.
     */
    getInstancePermissions(
        type: AccessControlledType,
        instanceId: number | string,
        nodeId?: number
    ): Observable<InstancePermissions>;
    getInstancePermissions(
        type: AccessControlledType.FOLDER,
        instanceId: number | string,
        nodeId?: number
    ): Observable<FolderInstancePermissions>;
    getInstancePermissions(
        type: AccessControlledType,
        instanceId: number | string,
        nodeId?: number,
    ): Observable<InstancePermissions> {
        return this.api.permissions.getPermissionsForInstance(type, instanceId, nodeId).pipe(
            map((response) => response.permissionsMap),
            map((permissionsMap) =>
                this.permFactory.createPermissionsFromMaps(
                    type,
                    permissionsMap,
                    instanceId,
                    nodeId,
                ),
            ),
            catchError((error) => {
                return of(
                    new UniformInstancePermissions(
                        type,
                        false,
                        instanceId,
                        nodeId,
                    ),
                );
            }),
        );
    }

    /**
     * The various entity types have different ways of representing the parent folder id. This
     * method figures out the correct property to read from.
     */
    private getParentFolderId(entity: InheritableItem | Node | Template): number {
        switch (entity.type) {
            case 'folder':
                // a "folder" does not have a motherId if it is the base folder of a node.
                // In this case, use the folder's own id.
                return (entity as Folder).motherId || entity.id;
            case 'page':
            case 'file':
            case 'form':
            case 'image':
            case 'template':
            // Pages, files, templates and images all have a "folderId" property which indicates the parent folder.
            // eslint-disable-next-line no-fallthrough
            case 'node':
            case 'channel':
                // 1. Nodes and channels do not have parents, but derive their permissions from the base
                // folder, which is also given as "folderId".
                // 2. Additionally, the base folders of nodes and channels also have the type "node" and "channel" respoectively,
                // rather than "folder" which is what might reasonably be expected.
                // In this case case, this will be no "folderId" property, but we can just use the folder's own id, since
                // the parent is the node itself, and the permissions of a node are that of the base folder.
                return (entity as Page).folderId || (entity as Folder).id;
            default:
                // TODO: Use an "assertNever()" once we enable strictNullChecks
                return (entity as any).folderId;
        }
    }

    /**
     * Creates a consumable HashMap from the raw permission data returned by the server.
     * When a language is passed, the group permissions of that language are merged in.
     */
    protected mapToPermissions(
        priv: PrivilegeMap,
        map: PermissionsMapCollection,
        languageId: number | null,
    ): EditorPermissions {
        let perm;
        let role;

        if (map) {
            perm = map.permissions;
            role = map.rolePermissions;
        }

        const language: Language = this.appState.now.entities.language[
            languageId
        ];
        const languageString: string = language && language.code;

        const result: EditorPermissions = Object.assign({}, getNoPermissions());

        // permissions only available in legacy property
        if (priv) {
            result.folder.create = priv.privileges.createfolder;
            result.folder.inherit = priv.privileges.inheritance;
            result.form.inherit = priv.privileges.inheritance;
            result.page.inherit = priv.privileges.inheritance;
            result.file.inherit = priv.privileges.inheritance;
            result.image.inherit = priv.privileges.inheritance;
            result.wastebin = priv.privileges.wastebin;
            result.template.inherit = priv.privileges.inheritance;
        }

        if (perm) {
            result.assignPermissions = perm.setperm;
            result.folder = Object.assign(result.folder, {
                delete: perm.deletefolder,
                edit: perm.updatefolder,
                localize: perm.createitems,
                unlocalize: perm.deletefolder,
                view: perm.readitems, // priv.privileges.viewfolder ?
            });
            result.form = Object.assign(result.form, {
                create: perm.createform,
                delete: perm.deleteform,
                edit: perm.updateform,
                publish: perm.publishform,
                localize: perm.create,
                unlocalize: perm.create,
                view: perm.viewform,
            });

            result.image = Object.assign(result.image, {
                create: perm.createitems,
                delete: perm.deleteitems,
                edit: perm.updateitems,
                import: perm.importitems,
                localize: perm.createitems,
                upload: perm.createitems,
                unlocalize: perm.createitems,
                view: perm.readitems,
            });

            /*
             * TODO: / FIXME:
             * The permission `channelsync` is a node-permission and would need to be loaded via `/perm/10001/<folderId>`.
             * Otherwise this permission is never sent from the folder directly.
             *
             * Best way would be to load the node-permission additionally on each folder request as well, but
             * would make an additional request per folder navigation.
             * Loading the permission at the beginning (folder-actions#getNode), would reduce request amount by a lot,
             * but the permissions might become stale.
             *
             * This same issue is also present for the `wastebin` checks.
             *
             * This workaround uses the deprecated privilage, as this is already present.
             */
            result.synchronizeChannel = perm.channelsync || (priv && priv.privileges.synchronizechannel);

            result.template = Object.assign(result.template, {
                create: perm.createtemplates,
                delete: perm.deletetemplates,
                edit: perm.updatetemplates,
                link: perm.linktemplates,
                localize: perm.createtemplates,
                unlocalize: perm.deletetemplates,
                view: perm.readtemplates,
            });
            result.tagType = {
                create: perm.createitems,
                delete: perm.deleteitems,
                edit: perm.updateitems,
                view: perm.readitems,
            };
        }

        result.page = Object.assign(result.page, {
            create: this.checkGroupAndRolePagePermission(
                perm,
                role,
                GcmsPermission.CREATE_ITEMS,
                languageString,
            ),

            delete: this.checkGroupAndRolePagePermission(
                perm,
                role,
                GcmsPermission.DELETE_ITEMS,
                languageString,
            ),

            edit: this.checkGroupAndRolePagePermission(
                perm,
                role,
                GcmsPermission.UPDATE_ITEMS,
                languageString,
            ),

            // there is no role specific permission for import
            import: !!perm && perm.importitems,

            // there is no role specific permission for linkTemplate
            linkTemplate: !!perm && perm.linktemplates,

            localize: this.checkGroupAndRolePagePermission(
                perm,
                role,
                GcmsPermission.CREATE_ITEMS,
                languageString,
            ),

            publish: this.checkGroupAndRolePagePermission(
                perm,
                role,
                GcmsPermission.PUBLISH_PAGES,
                languageString,
            ),

            unlocalize: this.checkGroupAndRolePagePermission(
                perm,
                role,
                GcmsPermission.CREATE_ITEMS,
                languageString,
            ),

            // the group permission applicable here is called createitems
            // the role permission  applicable here is called translatepages
            translate:
                (!!perm && perm.createitems) ||
                (!!role &&
                    (role.page.translatepages ||
                        (!!languageString &&
                            role.pageLanguages[languageString]
                                .translatepages))),

            view: this.checkGroupAndRolePagePermission(
                perm,
                role,
                GcmsPermission.READ_ITEMS,
                languageString,
            ),
        });

        result.file = Object.assign(result.file, {
            create:
                (!!perm && perm.createitems) ||
                (!!role && role.file.createitems),

            delete:
                (!!perm && perm.deleteitems) ||
                (!!role && role.file.deleteitems),

            edit:
                (!!perm && perm.updateitems) ||
                (!!role && role.file.updateitems),

            import: !!perm && perm.importitems,
            localize:
                (!!perm && perm.createitems) ||
                (!!role && role.file.createitems),

            upload:
                (!!perm && perm.createitems) ||
                (!!role && role.file.createitems),

            unlocalize:
                (!!perm && perm.createitems) ||
                (!!role && role.file.createitems),

            view: (!!perm && perm.readitems) || (!!role && role.file.readitems),
        });

        return result;
    }

    /**
     *
     *
     * @param perm permissions of the users (cumulative of all group permissions) returned by the API
     * @param role role specific permissions of the user
     * @param permissionName permissions property name to check
     * @param languageString language code
     */
    private checkGroupAndRolePagePermission(
        perm: Partial<Record<GcmsPermission, boolean>> | undefined,
        role: GcmsRolePrivilegeMapCollection | undefined,
        permissionName: GcmsPermission | GcmsRolePrivilege,
        languageString?: string,
    ): boolean {
        return !!(
            perm?.[permissionName] ||
            role?.page?.[permissionName] ||
            // undefined is a valid key for a javascript object - so we need to check if languageString is not undefined
            (!!languageString && role?.pageLanguages?.[languageString]?.[permissionName])
        );
    }

    /**
     * The permission API endpoint returns the data in a redundant way:
     * ```js
     *     languages: [
     *         { language: { id: 1, code: "de", ... }, privileges: { ..A.. } }
     *         { language: { id: 2, code: "en", ... }, privileges: { ..B.. } }
     *     ]
     * ```
     * This method converts the response data into a more usable format:
     * ```js
     *     languages: {
     *         1: { ..A.. },
     *         2: { ..B.. }
     *     }
     * ```
     */
    public normalizeAPIResponse(map: PrivilegeMapFromServer): PrivilegeMap {
        if (map.languages === undefined) {
            return map as any;
        }

        const { languages, privileges } = map;
        const languageHash = {} as { [lang: number]: RolePrivileges };
        if (languages && languages.length) {
            for (const languagePriv of languages) {
                languageHash[languagePriv.language.id] =
                    languagePriv.privileges;
            }
        }

        return {
            languages: languageHash,
            privileges,
        };
    }
}
