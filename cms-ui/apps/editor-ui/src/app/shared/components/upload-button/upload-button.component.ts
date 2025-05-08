import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FileCreateRequest, FileOrImage, NodeFeature } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { Observable, Subject } from 'rxjs';
import { filter, map, mergeMap, takeUntil, withLatestFrom } from 'rxjs/operators';
import { UploadConflictService } from '../../../core/providers/upload-conflict/upload-conflict.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import {
    ExternalAssetsModalComponent,
    GtxExternalAssetManagementApiResponse,
    GtxExternalAssetManagementApiRootObject,
} from '../external-assets-modal/external-assets-modal.component';

type GtxNodeSettingsAssetManagementConfigEnvelope = { [key: string]: GtxNodeSettingsAssetManagementConfig; }

interface GtxNodeSettingsAssetManagementConfig {
    label_i18n: { [key: string]: string }[];
    iframeSrcUrl: string;
}

interface GtxNodeSettingsAssetManagementConfigBO extends GtxNodeSettingsAssetManagementConfig {
    key: string;
}

const DOCS_URL = 'https://gentics.com/Content.Node/cmp8/guides/feature_asset_management.html';

/**
 * An upload component enabling local file system upload by default.
 * If feature `asset_management`is true and configured, external asset stores can be configured.
 */
@Component({
    selector: 'upload-button',
    templateUrl: './upload-button.component.html',
    styleUrls: ['./upload-button.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class UploadButtonComponent implements OnDestroy, OnInit {

    @Input() disabled = false;
    /**
     * If TRUE, selected external assets get uploaded directly after selection and emitted via `assetsUploaded`.
     * If FALSE, selected external assets are _not_ uploaded and emitted via `assetsSelected`.
     */
    @Input() instantUpload = false;
    @Input() targetNodeId: number;
    @Input() targetFolderId: number;
    @Input() itemType: 'file' | 'image';
    @Input() acceptUploads = 'image/*';
    @Input() multiple: boolean;
    @Input() btnSize = 'medium';
    @Input() btnType = 'secondary';
    @Input() btnLabel: string;
    defaultIsActive: boolean;

    /**
     * Default component action on user file selection:
     * If output has _no_ observers, selected files will be uploaded by this compoenent.
     * If output has at least one observer, selected files will _not_ be uploaded by this compoenent and instead file selection emitted.
     */
    @Output() filesSelected = new EventEmitter<File[]>();
    /** Action emitted if node feature `asset_management` is configured on successful asset upload. */
    @Output() assetsUploaded = new EventEmitter<FileOrImage[]>();
    /** Action emitted if node feature `asset_management` is configured on successful asset selection. */
    @Output() assetsSelected = new EventEmitter<GtxExternalAssetManagementApiRootObject[]>();
    @Output() uploadInProgress = new EventEmitter<boolean>();

    featureAssetManagementIsActive$: Observable<boolean>;
    uilanguageCode: string;

    configs: ({ default: boolean; } | GtxNodeSettingsAssetManagementConfigBO)[] = [];

    private destroyed$ = new Subject<void>();

    constructor(
        private uploadConflictService: UploadConflictService,
        private modalService: ModalService,
        private folderActions: FolderActionsService,
        private appState: ApplicationStateService,
    ) { }

    ngOnInit(): void {
        const activeNode$ = this.appState.select(state => state.folder.activeNode);

        this.featureAssetManagementIsActive$ = activeNode$.pipe(
            mergeMap(nodeId => this.appState.select(state => state.features.nodeFeatures[nodeId])),
            filter(nodeFeatures => !!nodeFeatures),
            map(nodeFeatures => nodeFeatures.includes(NodeFeature.ASSET_MANAGEMENT)),
        );

        activeNode$.pipe(
            mergeMap(nodeId => this.appState.select(state => state.nodeSettings.node[nodeId])),
            filter(nodeSettings => nodeSettings && nodeSettings.asset_management),
            withLatestFrom(this.featureAssetManagementIsActive$),
            map(([nodeSettings, featureAssetManagementIsActive]) => {
                const configData = Object.values(nodeSettings.asset_management);
                // validate config data
                if (featureAssetManagementIsActive && (Array.isArray(configData) && configData.length === 0)) {
                    throw new Error(`Malformed config data configured in $NODE_SETTINGS -> "asset_management": expected config array. See ${DOCS_URL}`);
                }
                return configData;
            }),
            takeUntil(this.destroyed$),
        ).subscribe((data: (
            { default: boolean; } | GtxNodeSettingsAssetManagementConfigEnvelope
        )[]) => {
            // format config data
            const masterObj: { default: boolean; } | GtxNodeSettingsAssetManagementConfigEnvelope
                = data.reduce((prev, next) => Object.assign(prev, next), {});

            this.configs = Object.entries(masterObj).map(([key, value]) => {
                if (key === 'default') {
                    return { default: value };
                } else {
                    return this.unwrapConfig({ [key]: value });
                }
            });
        });

        this.appState.select(state => state.ui.language).pipe(
            takeUntil(this.destroyed$),
        ).subscribe(language => this.uilanguageCode = language);
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    isDefaultConfig(config: object): boolean {
        return config && Object.keys(config)[0] === 'default';
    }

    getI18nLabel(config: GtxNodeSettingsAssetManagementConfigBO): string {
        let i18nLabel: string;
        try {
            i18nLabel = config.label_i18n && config.label_i18n[this.uilanguageCode];
        } catch (error) {
            i18nLabel = `No data in config for language ${this.uilanguageCode}`;
        }
        return i18nLabel;
    }

    /**
     * Uploads file blobs selected by user via browser API.
     *
     * @param files File Blobs
     */
    uploadFiles(files: File[]): void {
        const isSet = files && this.filesSelected.observers.length > 0;
        if (isSet) {
            this.filesSelected.emit(files);
        } else {
            this.uploadConflictService.uploadFilesWithConflictsCheck(files, this.targetNodeId, this.targetFolderId)
                .toPromise();
        }
    }

    /**
     * Requests the CMS to download binary data provided by extternal asset management.
     *
     * @param config for external asset store
     */
    customUpload(config: GtxNodeSettingsAssetManagementConfigBO): void {
        this.uploadInProgress.emit(true);
        const iframeSrcUrl = `${config.iframeSrcUrl}?locale=${this.uilanguageCode}`;
        this.modalService.fromComponent(ExternalAssetsModalComponent, {}, { title: this.getI18nLabel(config), iframeSrcUrl })
            .then(modal => modal.open())
            .then((response: GtxExternalAssetManagementApiResponse) => {
                // if user aborted
                if (!response) {
                    this.uploadInProgress.emit(false);
                    return;
                }

                // do not upload but emit external results instead
                if (!this.instantUpload) {
                    this.assetsSelected.emit(response.data);
                }

                // upload external selection to backend
                if (!this.instantUpload) {
                    return;
                }

                const typeMap: { [key: string]: 'image' | 'file' } = {};
                const requestPayloads: FileCreateRequest[] = response.data.map(item => {
                    typeMap[item.name] = item.fileCategory === 'image' ? 'image' : 'file';
                    return {
                        overwriteExisting: false,
                        folderId: this.targetFolderId,
                        nodeId: this.targetNodeId,
                        name: item.name,
                        description: item.description,
                        sourceURL: item['@odata.mediaReadLink'],
                        niceURL: item.niceUrl,
                        alternateURLs: item.alternateUrls,
                        properties: item.properties,
                    };
                });

                return Promise.all(requestPayloads.map(payload => {
                    return this.folderActions.uploadFromSourceUrl(typeMap[payload.name], payload).toPromise();
                })).then(results => {
                    const files = results.map(r => r.file);
                    this.uploadInProgress.emit(false);
                    this.assetsUploaded.emit(files);
                });
            })
            .catch(() => this.uploadInProgress.emit(false));
    }

    private unwrapConfig(config: GtxNodeSettingsAssetManagementConfigEnvelope): GtxNodeSettingsAssetManagementConfigBO {
        let key: string;
        const configKeysAmount = Object.keys(config).length;
        if (configKeysAmount === 1) {
            key = Object.keys(config)[0];
        } else {
            throw new Error(`Malformed config data configured in $NODE_SETTINGS -> "asset_management": expected one key per config but got ${configKeysAmount}. See ${DOCS_URL}`);
        }
        return {
            key,
            ...config[key],
        };
    }

}
