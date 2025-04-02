import {
    BO_ID,
    discard,
    EditableEntity,
    FormGroupTabHandle,
    LanguageBO,
    NULL_FORM_TAB_HANDLE,
    sortEntityRow,
    TableLoadEndEvent,
    TableSortEvent,
} from '@admin-ui/common';
import { ErrorHandler, LanguageHandlerService, LanguageTableLoaderService, NodeHandlerService, NodeTableLoaderService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import { Feature, Folder, Language, NodeFeatureModel, NodeHostnameType, NodePreviewurlType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ModalService, TableRow } from '@gentics/ui-core';
import { finalize } from 'rxjs/operators';
import { AssignLanguagesToNodeModal } from '../assign-languages-to-node-modal/assign-languages-to-node-modal.component';
import { NodeFeaturesFormData } from '../node-features/node-features.component';
import { NodePropertiesFormData, NodePropertiesMode } from '../node-properties/node-properties.component';
import { NodePublishingPropertiesFormData } from '../node-publishing-properties/node-publishing-properties.component';

@Component({
    selector: 'gtx-node-editor',
    templateUrl: './node-editor.component.html',
    styleUrls: ['./node-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeEditorComponent extends BaseEntityEditorComponent<EditableEntity.NODE> implements OnInit {

    public readonly NodePropertiesMode = NodePropertiesMode;

    /** Form data of tab 'Properties' */
    public fgProperties: FormControl<NodePropertiesFormData>;

    /** form of tab 'Publishing' */
    public fgPublishing: FormControl<NodePublishingPropertiesFormData>;

    /** form of tab 'Node Features' */
    public fgNodeFeatures: FormControl<NodeFeaturesFormData>;

    public isChildNode = false;
    public rootFolder: Folder;

    public features: NodeFeatureModel[] = null;
    public currentFeatures: NodeFeaturesFormData;

    public languageRows: TableRow<LanguageBO>[] = [];
    public isLanguagesChanged = false;

    public devtoolsEnabled = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: NodeHandlerService,
        protected tableLoader: NodeTableLoaderService,
        protected client: GCMSRestClientService,
        protected modalService: ModalService,
        protected languageHandler: LanguageHandlerService,
        protected languageTableLoader: LanguageTableLoaderService,
        protected errorHandler: ErrorHandler,
    ) {
        super(
            EditableEntity.NODE,
            changeDetector,
            route,
            router,
            appState,
            handler,
        )
    }

    override ngOnInit(): void {
        this.isChildNode = this.entity?.type === 'channel';

        this.subcriptions.push(this.appState.select(state => state.features.global[Feature.DEVTOOLS]).subscribe(enabled => {
            this.devtoolsEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        Promise.all([
            this.loadFeatureData(),
            this.loadRootFolder(),
        ]).then(([nodeFeatures]) => {
            this.currentFeatures = nodeFeatures;
            super.ngOnInit();
        });
    }

    override onEntityUpdate(): void {
        this.tableLoader.reload();
    }

    protected initializeTabHandles(): void {
        this.fgProperties = new FormControl(this.getPropertiesData());
        this.tabHandles[this.Tabs.PROPERTIES] = new FormGroupTabHandle(this.fgProperties, {
            save: () => {
                const { description, ...value } = this.fgProperties.value;
                this.fgProperties.disable();

                return this.handler.updateMapped(this.entityId, {
                    description,
                    node: value,
                }).pipe(
                    discard(entity => {
                        this.handleEntityLoad(entity);
                        this.onEntityUpdate();
                    }),
                    finalize(() => this.fgProperties.enable()),
                ).toPromise();
            },
            reset: () => {
                this.fgProperties.reset(this.getPropertiesData());
                return Promise.resolve();
            },
        });

        this.fgPublishing = new FormControl(this.getPublishData());
        this.tabHandles[this.Tabs.PUBLISHING] = new FormGroupTabHandle(this.fgPublishing, {
            save: () => {
                const value = this.fgPublishing.value;
                this.fgPublishing.disable();

                return this.handler.updateMapped(this.entityId, {
                    node: value,
                }).pipe(
                    discard(entity => {
                        this.handleEntityLoad(entity);
                        this.onEntityUpdate();
                    }),
                    finalize(() => this.fgPublishing.enable()),
                ).toPromise();
            },
            reset: () => {
                this.fgPublishing.reset(this.getPublishData());
                return Promise.resolve();
            },
        });

        this.fgNodeFeatures = new FormControl(this.currentFeatures);
        this.tabHandles[this.Tabs.FEATURES] = new FormGroupTabHandle(this.fgNodeFeatures, {
            save: () => {
                return (this.handler as NodeHandlerService).updateFeatures(this.entityId, this.fgNodeFeatures.value).pipe(
                    discard(),
                ).toPromise();
            },
            reset: () => {
                this.fgNodeFeatures.reset(this.currentFeatures);
                return Promise.resolve();
            },
        });

        this.tabHandles[this.Tabs.LANGUAGES] = {
            save: () => this.updateLanguages(),
            isDirty: () => this.isLanguagesChanged,
            isValid: () => true,
        };

        this.tabHandles[this.Tabs.PACKAGES] = NULL_FORM_TAB_HANDLE;
    }

    protected onEntityChange(): void {
        this.isChildNode = this.entity?.type === 'channel';

        Promise.all([
            this.loadFeatureData(),
            this.loadRootFolder(),
        ]).then(([nodeFeatures]) => {
            this.currentFeatures = nodeFeatures;
            this.isLanguagesChanged = false;
            this.resetTabs();
        });
    }

    protected loadRootFolder(): Promise<void> {
        if (this.entity == null) {
            return;
        }

        return new Promise((resolve, reject) => {
            this.subcriptions.push(this.client.folder.get(this.entity.folderId).subscribe(res => {
                this.rootFolder = res.folder;

                resolve();

                this.changeDetector.markForCheck();
            }, reject));
        });
    }

    protected loadFeatureData(): Promise<NodeFeaturesFormData> {
        return Promise.all([
            (this.handler as NodeHandlerService).getFeatures(this.entityId).toPromise(),
            this.loadAvailableFeatures(),
        ]).then(([nodeFeatures]) => {
            return this.features.reduce((acc, feat) => {
                acc[feat.id] = nodeFeatures[feat.id] ?? false;
                return acc;
            }, {});
        });
    }

    protected loadAvailableFeatures(): Promise<void> {
        if (this.features) {
            return Promise.resolve();
        }

        return new Promise((resolve, reject) => {
            this.subcriptions.push((this.handler as NodeHandlerService).listFeatures().subscribe(features => {
                this.features = features;
                this.changeDetector.markForCheck();
                resolve();
            }, reject));
        });
    }

    protected getPropertiesData(): NodePropertiesFormData {
        return {
            host: this.entity?.host,
            hostProperty: this.entity?.hostProperty,
            hostType: this.entity?.hostProperty
                ? NodeHostnameType.PROPERTY
                : NodeHostnameType.VALUE,
            https: this.entity?.https,
            inheritedFromId: this.entity?.inheritedFromId,
            insecurePreviewUrl: this.entity?.insecurePreviewUrl,
            meshPreviewUrl: this.entity?.meshPreviewUrl,
            meshPreviewUrlProperty: this.entity?.meshPreviewUrlProperty,
            previewType: this.entity?.meshPreviewUrlProperty
                ? NodePreviewurlType.PROPERTY
                : NodePreviewurlType.VALUE,
            name: this.entity?.name,
            publishImageVariants: this.entity?.publishImageVariants,
            defaultFileFolderId: this.entity?.defaultFileFolderId,
            defaultImageFolderId: this.entity?.defaultImageFolderId,
            pubDirSegment: this.entity?.pubDirSegment,
            description: this.rootFolder?.description,
        };
    }

    protected getPublishData(): NodePublishingPropertiesFormData {
        return {
            binaryPublishDir: this.entity?.binaryPublishDir,
            contentRepositoryId: this.entity?.contentRepositoryId,
            disablePublish: this.entity?.disablePublish,
            omitPageExtension: this.entity?.omitPageExtension,
            pageLanguageCode: this.entity?.pageLanguageCode,
            publishContentMap: this.entity?.publishContentMap,
            publishContentMapFiles: this.entity?.publishContentMapFiles,
            publishContentMapFolders: this.entity?.publishContentMapFolders,
            publishContentMapPages: this.entity?.publishContentMapPages,
            publishDir: this.entity?.publishDir,
            publishFs: this.entity?.publishFs,
            publishFsFiles: this.entity?.publishFsFiles,
            publishFsPages: this.entity?.publishFsPages,
            urlRenderWayFiles: this.entity?.urlRenderWayFiles,
            urlRenderWayPages: this.entity?.urlRenderWayPages,
        };
    }

    languagesLoaded(event: TableLoadEndEvent<LanguageBO>): void {
        this.languageRows = event.rows;
        this.isLanguagesChanged = false;
    }

    updateLanguages(): Promise<void> {
        const nodeLanguages: Language[] = this.languageRows.map(row => row.item);

        return (this.handler as NodeHandlerService).updateLanguages(this.entityId, nodeLanguages).pipe(
            discard(() => {
                // Force languages to reload
                this.languageTableLoader.reload();
                this.isLanguagesChanged = false;
                this.changeDetector.markForCheck();
            }),
        ).toPromise();
    }

    sortLanguages(event: TableSortEvent<LanguageBO>): void {
        this.languageRows = sortEntityRow(this.languageRows, event.from, event.to);
        this.isLanguagesChanged = true;
        this.changeDetector.markForCheck();
    }

    async assignLanguagesToNode(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            AssignLanguagesToNodeModal,
            { closeOnOverlayClick: false , width: '50%' },
            {
                nodeId: this.entityId,
                nodeName: this.entity.name,
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
                this.changeDetector.markForCheck();
            }
        } catch (err) {
            if (wasClosedByUser(err)) {
                return;
            }
            this.errorHandler.catch(err);
        }
    }
}
