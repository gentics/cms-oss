import {
    BO_ID,
    createFormSaveDisabledTracker,
    detailLoading,
    discard,
    FormGroupTabHandle,
    FormTabHandle,
    LanguageBO,
    NodeDetailTabs,
    NULL_FORM_TAB_HANDLE,
    sortEntityRow,
    TableLoadEndEvent,
    TableSortEvent,
} from '@admin-ui/common';
import {
    BREADCRUMB_RESOLVER,
    EditorTabTrackerService,
    FeatureOperations,
    FolderOperations,
    LanguageHandlerService,
    LanguageTableLoaderService,
    NodeOperations,
    NodeTableLoaderService,
    PermissionsService,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { FolderDataService, NodeDataService } from '@admin-ui/shared';
import { BaseDetailComponent } from '@admin-ui/shared/components';
import { AppStateService, SelectState, UIStateModel } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    Type,
} from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
    Feature,
    Folder,
    IndexById,
    Language,
    Node,
    NodeFeature,
    NodeFeatureModel,
    NodeHostnameType,
    NodePreviewurlType,
    NodeSaveRequest,
    NormalizableEntityType,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import { ModalService, TableRow } from '@gentics/ui-core';
import { NGXLogger } from 'ngx-logger';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import {
    delay,
    filter,
    map,
    startWith,
    switchMap,
    take,
    takeUntil,
    tap,
} from 'rxjs/operators';
import { AssignLanguagesToNodeModal } from '../assign-languages-to-node-modal/assign-languages-to-node-modal.component';
import { NodeFeaturesFormData } from '../node-features/node-features.component';
import { NodePropertiesFormData } from '../node-properties/node-properties.component';
import { NodePublishingPropertiesFormData } from '../node-publishing-properties/node-publishing-properties.component';

/**
 * # NodeDetailComponent
 */
@Component({
    selector: 'gtx-node-detail',
    templateUrl: './node-detail.component.html',
    styleUrls: [ './node-detail.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeDetailComponent extends BaseDetailComponent<'node', NodeOperations> implements OnInit {

    public readonly NodeDetailTabs = NodeDetailTabs;

    @SelectState(state => state.features.global[Feature.DEVTOOLS])
    featureDevtoolsIsEnabled$: Observable<boolean>;

    activeTabId$: Observable<string>;

    entityIdentifier: NormalizableEntityType = 'node';

    currentEntity: Node<Raw>;

    isChildNode: boolean;

    /** If the user is allowed to update the node */
    public allowedToUpdate = false;

    /** Form data of tab 'Properties' */
    fgProperties: UntypedFormGroup;

    /** form of tab 'Publishing' */
    fgPublishing: UntypedFormGroup;

    /** form of tab 'Node Features' */
    fgNodeFeatures: UntypedFormGroup;

    fgPropertiesSaveDisabled$: Observable<boolean>;
    fgPublishingSaveDisabled$: Observable<boolean>;
    fgNodeFeaturesSaveDisabled$: Observable<boolean>;

    updateEntity$ = new BehaviorSubject<boolean>(null);

    nodeFeatures$: Observable<NodeFeatureModel[]>;

    languageRows: TableRow<LanguageBO>[] = [];
    isLanguagesChanged = false;
    currentNodeId: number;

    currentRootFolder: Folder<Normalized>;

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.name || this.currentEntity.name === '';
    }

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab];
    }

    private tabHandles: Record<NodeDetailTabs, FormTabHandle>;

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        nodeData: NodeDataService,
        changeDetectorRef: ChangeDetectorRef,
        private folderData: FolderDataService,
        private nodeOperations: NodeOperations,
        private featureOperations: FeatureOperations,
        private folderOperations: FolderOperations,
        private editorTabTracker: EditorTabTrackerService,
        private languageHandler: LanguageHandlerService,
        private languageTableLoader: LanguageTableLoaderService,
        private modalService: ModalService,
        private nodeLoader: NodeTableLoaderService,
        private permissions: PermissionsService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            nodeData,
            changeDetectorRef,
        );
    }

    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        const entity = appState.now.entity.node[Number(route.params.id)];
        return of(entity ? { title: entity.name, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        // init forms
        this.initForms();

        // Get Node Features
        this.nodeFeatures$ = this.appState.select(state => state.ui.language).pipe(
            startWith(of(true)),
            delay(500), // Race condition tmp fix (ToDo: change after proper changing of server language setting has been implemented)
            switchMap(() => this.nodeOperations.getAvailableFeatures({ sort: [ { attribute: 'id' } ] })),
        );

        // get current entity
        this.currentEntity$ = combineLatest([this.userState$, this.updateEntity$]).pipe(
            map(([userState]) => userState),
            map((userState: UIStateModel) => this.entityData.getEntity(userState.focusEntityId)),
            filter((entity: Node<Raw>) => entity instanceof Object),
            tap(entity => {
                this.isChildNode = false;
                if (entity.id !== entity.masterNodeId) {
                    this.isChildNode = true;
                }
                this.setCurrentNode(entity);
            }),
        );

        // get current root folder
        const currentRootFolder$ = this.currentEntity$.pipe(
            switchMap(entity => this.folderData.getEntityFromState(entity.folderId)),
            filter((entity: Folder<Normalized>) => entity instanceof Object),
            tap(entity => this.currentRootFolder = entity),
        );

        const currentNodeFeatures$ = this.currentEntity$.pipe(
            switchMap(entity => this.featureOperations.getNodeFeatures(entity.id)),
        );

        combineLatest([
            currentNodeFeatures$,
            this.nodeFeatures$.pipe(take(1)),
        ]).pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(([nodeFeatures, nodeAvailableFeatures]) => {
            this.fgNodeFeaturesUpdate(nodeFeatures, nodeAvailableFeatures);
        });

        // assign values and validation of current entity
        currentRootFolder$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currRootFolder) => {
            // fill form with entity property values
            this.onNodeChange(this.currentEntity);
            this.onRootFolderChange(currRootFolder);
            this.changeDetectorRef.markForCheck();
        });

        const updatePerms = this.permissions.getUserActionPermsForId('node.updateNodeInstance');
        this.currentEntity$.pipe(
            switchMap(node => this.permissions.checkPermissions([
                ...updatePerms.typePermissions,
                {
                    ...updatePerms.instancePermissions,
                    instanceId: node.folderId,
                },
            ])),
            takeUntil(this.stopper.stopper$),
        ).subscribe(hasPerm => {
            this.allowedToUpdate = hasPerm;
            this.handleDisableState([
                this.fgProperties,
                this.fgPublishing,
                this.fgNodeFeatures,
            ], hasPerm);
            this.changeDetectorRef.markForCheck();
        });

        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);
    }

    private handleDisableState(controls: AbstractControl<any>[], enabled: boolean): void {
        if (enabled) {
            controls.forEach(c => c.enable({ emitEvent: false }));
        } else {
            controls.forEach(c => c.disable({ emitEvent: false }));
        }
    }

    /**
     * If user clicks to save input data in tab node properties
     */
    btnSavePropertiesOnClick(): void {
        this.updateNode();
    }

    /**
     * If user clicks to save input data in tab node publishing
     */
    btnSavePublishingOnClick(): void {
        this.updatePublishing();
    }

    /**
     * If user clicks to save input data in tab node features
     */
    btnSaveNodeFeaturesOnClick(): void {
        this.updateNodeFeatures();
    }

    /**
     * If user clicks to save input data in tab node languages
     */
    btnSaveLanguagesOnClick(): void {
        this.updateLanguages();
    }

    /**
     * Requests changes of node by id to CMS
     */
    updateNode(): Promise<Node<Raw>> {
        // assemble payload with conditional properties
        const nodeProperties: NodePropertiesFormData = this.fgProperties.value.data;
        const nodeSave: NodeSaveRequest = {
            node: {
                id: this.currentEntity.id,
                ...(nodeProperties.name && { name: nodeProperties.name }),
                https: nodeProperties.https,
                host: nodeProperties.hostnameType === NodeHostnameType.VALUE ? nodeProperties.hostname : '',
                hostProperty: nodeProperties.hostnameType === NodeHostnameType.PROPERTY ? nodeProperties.hostnameProperty : '',
                meshPreviewUrl: nodeProperties.meshPreviewUrlType === NodePreviewurlType.VALUE ? nodeProperties.meshPreviewUrl : '',
                meshPreviewUrlProperty: nodeProperties.meshPreviewUrlType === NodePreviewurlType.PROPERTY ? nodeProperties.meshPreviewUrlProperty : '',
                insecurePreviewUrl: nodeProperties.insecurePreviewUrl,
                pubDirSegment: nodeProperties.pubDirSegment,
                publishImageVariants: nodeProperties.publishImageVariants,
                // TODO: Need repositiry browser support for editing this property
                // ...(this.nodeProperties.defaultFileFolderId && { defaultFileFolderId: this.nodeProperties.defaultFileFolderId }),
                // ...(this.nodeProperties.defaultImageFolderId && { defaultImageFolderId: this.nodeProperties.defaultImageFolderId }),
            },
            description: nodeProperties.description,
        };

        return this.nodeOperations.update(nodeSave.node.id, nodeSave).pipe(
            tap(updatedNode => {
                this.setCurrentNode(updatedNode);
                this.nodeLoader.reload();
                this.fgProperties.patchValue(updatedNode);
                this.changeDetectorRef.markForCheck();
            }),
            switchMap(() => this.folderOperations.get(this.currentEntity.folderId)),
            map(() => this.currentEntity),
            detailLoading(this.appState),
            tap(() => this.fgProperties.markAsPristine()),
        ).toPromise();
    }

    /**
     * Requests changes of node by id to CMS
     */
    updatePublishing(): Promise<Node<Raw>> {
        // assemble payload with conditional properties
        const fgPublishingData: NodePublishingPropertiesFormData = this.fgPublishing.value.data;
        // Parsing the cr-repo ID to a number, as the API only supports the number ID and not the global ID.
        let crId = fgPublishingData.contentRepositoryId || null;
        if (crId != null) {
            if (typeof crId === 'string') {
                crId = parseInt(crId, 10);
            }
            if (isNaN(crId) || !isFinite(crId)) {
                crId = null;
            }
        }

        const nodeSave: NodeSaveRequest = {
            node: {
                id: this.currentEntity.id,
                contentRepositoryId: crId || 0,
                disablePublish: fgPublishingData.disableUpdates,
                publishFs: fgPublishingData.fileSystem,
                publishFsPages: fgPublishingData.fileSystemPages,
                publishDir: fgPublishingData.fileSystemPagesDir,
                publishFsFiles: fgPublishingData.fileSystemFiles,
                binaryPublishDir: fgPublishingData.fileSystemBinaryDir,
                publishContentMap: fgPublishingData.contentRepository,
                publishContentMapPages: fgPublishingData.contentRepositoryPages,
                publishContentMapFiles: fgPublishingData.contentRepositoryFiles,
                publishContentMapFolders: fgPublishingData.contentRepositoryFolders,
                urlRenderWayPages: fgPublishingData.urlRenderWayPages,
                urlRenderWayFiles: fgPublishingData.urlRenderWayFiles,
                omitPageExtension: fgPublishingData.omitPageExtension,
                pageLanguageCode: fgPublishingData.pageLanguageCode,
            } as Node<Raw>,
        };

        return this.nodeOperations.update(nodeSave.node.id, nodeSave).pipe(
            detailLoading(this.appState),
            tap(updatedNode => {
                this.setCurrentNode(updatedNode);
                this.nodeLoader.reload();
                this.fgPublishing.markAsPristine()
            }),
        ).toPromise();
    }

    updateNodeFeatures(): Promise<Node<Raw>> {
        const nodeFeatures: NodeFeaturesFormData = this.fgNodeFeatures.value.data;

        return this.nodeOperations.updateNodeFeatures(this.currentEntity.id, nodeFeatures).pipe(
            detailLoading(this.appState),
            map(() => {
                this.featureOperations.getNodeFeatures(this.currentEntity.id);
                this.updateEntity$.next(true)
                this.nodeLoader.reload();
                this.fgNodeFeatures.markAsPristine();
                return this.currentEntity;
            }),
        ).toPromise();
    }

    protected setCurrentNode(newNode: Node<Raw>): void {
        this.currentEntity = newNode;
        this.currentNodeId = Number(newNode.id);
        this.isLanguagesChanged = false;
    }

    languagesLoaded(event: TableLoadEndEvent<LanguageBO>): void {
        this.languageRows = event.rows;
        this.isLanguagesChanged = false;
    }

    updateLanguages(): Promise<unknown> {
        const nodeLanguages: Language[] = this.languageRows.map(row => row.item);

        return this.nodeOperations.updateNodeLanguages(this.currentEntity.id, nodeLanguages).pipe(
            detailLoading(this.appState),
            discard(() => {
                // Force languages to reload
                this.languageTableLoader.reload();
                this.isLanguagesChanged = false;
                this.changeDetectorRef.markForCheck();
            }),
        ).toPromise();
    }

    sortLanguages(event: TableSortEvent<LanguageBO>): void {
        this.languageRows = sortEntityRow(this.languageRows, event.from, event.to);
        this.isLanguagesChanged = true;
        this.changeDetectorRef.markForCheck();
    }

    async assignLanguagesToNode(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            AssignLanguagesToNodeModal,
            { closeOnOverlayClick: false , width: '50%' },
            {
                nodeId: this.currentNodeId,
                nodeName: this.currentEntity.name,
                selectedLanguages: this.languageRows.map(row => row.item[BO_ID]),
            },
        );
        try {
            const languages = await dialog.open();
            if (Array.isArray(languages)) {
                this.languageRows = languages
                    .map(lang => this.languageHandler.mapToBusinessObject(lang))
                    .map(bo => this.languageTableLoader.mapToTableRow(bo));
                this.isLanguagesChanged = false;
                this.changeDetectorRef.markForCheck();
            }
        } catch (err) {
            console.error(err);
        }
    }

    /**
     * Set new value of form 'Publishing'
     */
    private fgPublishingUpdate(node: Node): void {
        this.patchFormGroup<NodePublishingPropertiesFormData>(this.fgPublishing, {
            disableUpdates: node.disablePublish,
            fileSystem: node.publishFs,
            fileSystemPages: node.publishFsPages,
            fileSystemPagesDir: node.publishDir,
            fileSystemFiles: node.publishFsFiles,
            fileSystemBinaryDir: node.binaryPublishDir,
            contentRepository: node.publishContentMap,
            contentRepositoryPages: node.publishContentMapPages,
            contentRepositoryFiles: node.publishContentMapFiles,
            contentRepositoryFolders: node.publishContentMapFolders,
            contentRepositoryId: node.contentRepositoryId,
            urlRenderWayPages: node.urlRenderWayPages,
            urlRenderWayFiles: node.urlRenderWayFiles,
            omitPageExtension: node.omitPageExtension,
            pageLanguageCode: node.pageLanguageCode,
        });
    }

    /**
     * Set new value of form 'Node Features'
     */
    private fgNodeFeaturesUpdate(features: IndexById<Partial<Record<NodeFeature, boolean>>>, availFeatures: NodeFeatureModel[]): void {
        const nodeFeaturesGroup: any = {};

        availFeatures.forEach(feature => {
            nodeFeaturesGroup[feature.id] = false;
        });

        this.patchFormGroup(this.fgNodeFeatures, {
            ...nodeFeaturesGroup,
            ...features,
        });
    }

    private initForms(): void {
        this.fgProperties = this.createFormGroup(true);
        this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties);
        this.fgPublishing = this.createFormGroup(true);
        this.fgPublishingSaveDisabled$ = createFormSaveDisabledTracker(this.fgPublishing);
        this.fgNodeFeatures = this.createFormGroup(false);
        this.fgNodeFeaturesSaveDisabled$ = createFormSaveDisabledTracker(this.fgNodeFeatures);

        this.tabHandles = {
            [NodeDetailTabs.PROPERTIES]: new FormGroupTabHandle(this.fgProperties, {
                save: () => this.updateNode().then(() => {}),
            }),
            [NodeDetailTabs.PUBLISHING]: new FormGroupTabHandle(this.fgPublishing, {
                save: () => this.updatePublishing().then(() => {}),
            }),
            [NodeDetailTabs.FEATURES]: new FormGroupTabHandle(this.fgNodeFeatures, {
                save: () => this.updateNodeFeatures().then(() => {}),
            }),
            [NodeDetailTabs.LANGUAGES]: {
                isDirty: () => this.isLanguagesChanged,
                isValid: () => true,
                save: () => this.updateLanguages().then(() => {}),
            },
            [NodeDetailTabs.PACKAGES]: NULL_FORM_TAB_HANDLE,
        };
    }

    private onNodeChange(node: Node): void {
        this.fgPropertiesUpdate(node);
        this.fgPublishingUpdate(node);
    }

    /**
     * Set new value of form 'Properties' using the specified node's data.
     */
    private fgPropertiesUpdate(node: Node): void {
        if (!node) {
            this.fgProperties.reset();
            this.patchFormGroup<NodePropertiesFormData>(this.fgProperties, {
                hostnameType: NodeHostnameType.VALUE
            });
            this.fgProperties.markAsPristine();
            return;
        }

        this.patchFormGroup<NodePropertiesFormData>(this.fgProperties, {
            name: node.name,
            inheritedFromId: node.inheritedFromId !== node.id ? node.inheritedFromId : null,
            https: node.https,
            hostnameType: node.hostProperty ? NodeHostnameType.PROPERTY : NodeHostnameType.VALUE,
            hostname: node.host,
            hostnameProperty: node.hostProperty,
            meshPreviewUrlType: node.meshPreviewUrlProperty ? NodePreviewurlType.PROPERTY : NodePreviewurlType.VALUE,
            meshPreviewUrl: node.meshPreviewUrl,
            meshPreviewUrlProperty: node.meshPreviewUrlProperty,
            insecurePreviewUrl: node.insecurePreviewUrl,
            defaultFileFolderId: node.defaultFileFolderId || null,
            defaultImageFolderId: node.defaultImageFolderId || null,
            pubDirSegment: node.pubDirSegment,
            publishImageVariants: node.publishImageVariants,
        });
    }

    private onRootFolderChange(rootFolder: Folder): void {
        this.patchFormGroup<NodePropertiesFormData>(this.fgProperties, {
            description: rootFolder ? rootFolder.description : '',
        });
    }

    private patchFormGroup<T>(formGroup: UntypedFormGroup, changes: Partial<T>): void {
        const patched: T = {
            ...formGroup.value.data,
            ...changes,
        };

        formGroup.setValue({ data: patched }, { emitEvent: false });
        formGroup.markAsPristine();
        formGroup.updateValueAndValidity();
    }

    private createFormGroup(required: boolean): UntypedFormGroup {
        return new UntypedFormGroup({
            data: new UntypedFormControl(null, required ? [ Validators.required ] : undefined),
        });
    }

}
