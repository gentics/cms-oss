import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FileCreateRequest, FileOrImage, NodeFeature } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { combineLatest, Subscription } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { UploadConflictService } from '../../../core/providers/upload-conflict/upload-conflict.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import {
    ExternalAssetsModalComponent,
    GtxExternalAssetManagementApiResponse,
    GtxExternalAssetManagementApiRootObject,
} from '../external-assets-modal/external-assets-modal.component';

interface DefaultProviderSettings {
    default: boolean;
}

interface AssetProviderSettings {
    [name: string]: AssetManagementSettings;
}

interface AssetManagementSettings {
    // eslint-disable-next-line @typescript-eslint/naming-convention
    label_i18n: { [key: string]: string }[];
    iframeSrcUrl: string;
}

interface DefaultProvider {
    default: true;
}

interface ExternalAssetManagementProvider {
    label: string;
    iframeSrcUrl: string;
}

type AssetManagementProvider = DefaultProvider | ExternalAssetManagementProvider;

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
})
export class UploadButtonComponent implements OnDestroy, OnInit {

    @Input()
    public disabled = false;
    /**
     * If TRUE, selected external assets get uploaded directly after selection and emitted via `assetsUploaded`.
     * If FALSE, selected external assets are _not_ uploaded and emitted via `assetsSelected`.
     */
    @Input()
    public instantUpload = false;

    @Input()
    public targetNodeId: number;

    @Input()
    public targetFolderId: number;

    @Input()
    public itemType: 'file' | 'image';

    @Input()
    public acceptUploads = 'image/*';

    @Input()
    public multiple: boolean;

    @Input()
    public btnSize = 'medium';

    @Input()
    public btnType = 'secondary';

    @Input()
    public btnLabel: string;
    /**
     * Default component action on user file selection:
     * If output has _no_ observers, selected files will be uploaded by this compoenent.
     * If output has at least one observer, selected files will _not_ be uploaded by this compoenent and instead file selection emitted.
     */
    @Output()
    public filesSelected = new EventEmitter<File[]>();

    /** Action emitted if node feature `asset_management` is configured on successful asset upload. */
    @Output()
    public assetsUploaded = new EventEmitter<FileOrImage[]>();

    /** Action emitted if node feature `asset_management` is configured on successful asset selection. */
    @Output()
    public assetsSelected = new EventEmitter<GtxExternalAssetManagementApiRootObject[]>();

    @Output()
    public uploadInProgress = new EventEmitter<boolean>();

    public assetManagementEnabled = false;
    public languageCode: string;
    public providers: AssetManagementProvider[] = [];

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private uploadConflictService: UploadConflictService,
        private modalService: ModalService,
        private folderActions: FolderActionsService,
        private appState: ApplicationStateService,
    ) { }

    ngOnInit(): void {
        // UI Language is constant and doesn't change
        this.languageCode = this.appState.now.ui.language;

        const activeNode$ = this.appState.select(state => state.folder.activeNode);

        this.subscriptions.push(combineLatest([
            activeNode$,
            this.appState.select(state => state.features.nodeFeatures),
        ]).pipe(
            map(([id, features]) => (features?.[id] || []).includes(NodeFeature.ASSET_MANAGEMENT)),
            distinctUntilChanged(),
        ).subscribe(enabled => {
            this.assetManagementEnabled = enabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            activeNode$,
            this.appState.select(state => state.nodeSettings),
        ]).pipe(
            map(([id, settings]) => settings.node?.[id]?.asset_management),
            distinctUntilChanged(isEqual),
        ).subscribe((entries: (AssetProviderSettings | DefaultProviderSettings)[]) => {
            this.providers = [];

            if (!Array.isArray(entries)) {
                console.error('Asset management has invalid settings!');
                return;
            }

            entries.forEach(providerSettings => {
                if (providerSettings.default != null && providerSettings.default) {
                    this.providers.push({
                        default: true,
                        label: '',
                    });
                    return;
                }

                const names = Object.getOwnPropertyNames(providerSettings) || [];

                // Invalid amount of properties defined
                if (names.length !== 1) {
                    return;
                }

                const key = names[0];
                const value = providerSettings[key];

                if (value == null || typeof value !== 'object') {
                    console.error(`The asset-management settings for "${key}" is invalid! Please correct them!\nSee the documentation for more information: ${DOCS_URL}`);
                    return;
                }

                if (value.label_i18n == null || typeof value.label_i18n[this.languageCode] !== 'string') {
                    console.error(`The asset-management settings for "${key}" do not have valid 'label_i18n' settings or is missing language "${this.languageCode}"!`);
                    return;
                }

                this.providers.push({
                    label: value.label_i18n[this.languageCode],
                    iframeSrcUrl: value.iframeSrcUrl,
                });
            });

            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
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
    customUpload(config: ExternalAssetManagementProvider): void {
        this.uploadInProgress.emit(true);

        let iframeUrl: string;

        // Try to parse the URL and append the query-param this way, to support query-params in the URL.
        // Otherwise it could mangle it and cause issues. Old way stays for compatibility/fallback reasons.
        try {
            const target = new URL(config.iframeSrcUrl, window.location as any);
            target.searchParams.set('locale', this.languageCode);
            iframeUrl = target.toString();
        } catch (err) {
            iframeUrl = `${config.iframeSrcUrl}?locale=${this.languageCode}`;
        }

        this.modalService.fromComponent(ExternalAssetsModalComponent, {}, { title: config.label, iframeSrcUrl: iframeUrl })
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
}
