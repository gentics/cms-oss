import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { ContentRepository, ContentRepositoryType, Node, NodePageLanguageCode, NodeUrlMode, Raw } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';

/**
 * Defines the data editable by the `NodePublishingPropertiesComponent`.
 */
export type NodePublishingPropertiesFormData = Pick<Node, 'disablePublish' | 'publishFs' | 'publishFsPages' | 'publishDir' |
'publishFsFiles' | 'binaryPublishDir' | 'contentRepositoryId' | 'publishContentMap' | 'publishContentMapPages' | 'publishContentMapFiles' |
'publishContentMapFolders' | 'urlRenderWayPages' | 'urlRenderWayFiles' | 'omitPageExtension' | 'pageLanguageCode'>;

const FS_CONTROLS: (keyof NodePublishingPropertiesFormData)[] = [
    'publishFsPages',
    'publishFsFiles',
];

const CR_CONTROLS: (keyof NodePublishingPropertiesFormData)[] = [
    'publishContentMap',
    'publishContentMapFiles',
    'publishContentMapFolders',
    'publishContentMapPages',
];

const PUBLISH_MAP_CONTROLS: (keyof NodePublishingPropertiesFormData)[] = [
    'publishContentMapFiles',
    'publishContentMapFolders',
    'publishContentMapPages',
];

@Component({
    selector: 'gtx-node-publishing-properties',
    templateUrl: './node-publishing-properties.component.html',
    styleUrls: ['./node-publishing-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(NodePublishingPropertiesComponent),
        generateValidatorProvider(NodePublishingPropertiesComponent),
    ],
    standalone: false
})
export class NodePublishingPropertiesComponent extends BasePropertiesComponent<NodePublishingPropertiesFormData> implements OnInit {

    public readonly NodePageLanguageCode = NodePageLanguageCode;

    public readonly URL_MODES: Record<NodeUrlMode, string> = {
        [NodeUrlMode.AUTOMATIC]: 'node.url_mode_automatic',
        [NodeUrlMode.PORTAL_LINK]: 'node.url_mode_plink',
        [NodeUrlMode.DYNAMIC]: 'node.url_mode_dynamic',
        [NodeUrlMode.WITH_DOMAIN]: 'node.url_mode_w_domain',
        [NodeUrlMode.WITHOUT_DOMAIN]: 'node.url_mode_wo_domain',
    };

    public contentRepositories: ContentRepository<Raw>[] = [];

    public publishDirsLinked: boolean = null;
    public linkButtonDisabled = false;

    private previousPublishCr = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.client.contentRepository.list().subscribe(res => {
            this.contentRepositories = res.items;
            if (this.form) {
                this.configureForm(this.form.value as any, false);
            }
            this.changeDetector.markForCheck();
        }));
    }

    protected createForm(): FormGroup<FormProperties<NodePublishingPropertiesFormData>> {
        this.previousPublishCr = this.value?.publishContentMap ?? false;

        this.initLinkedDir();

        return new FormGroup<FormProperties<NodePublishingPropertiesFormData>>({
            disablePublish: new FormControl(this.safeValue('disablePublish')),

            publishFs: new FormControl(this.safeValue('publishFs')),
            publishFsPages: new FormControl(this.safeValue('publishFsPages')),
            publishDir: new FormControl(this.safeValue('publishDir')),
            publishFsFiles: new FormControl(this.safeValue('publishFsFiles')),
            binaryPublishDir: new FormControl(this.safeValue('binaryPublishDir')),

            publishContentMap: new FormControl(this.safeValue('publishContentMap')),
            publishContentMapPages: new FormControl(this.safeValue('publishContentMapPages')),
            publishContentMapFiles: new FormControl(this.safeValue('publishContentMapFiles')),
            publishContentMapFolders: new FormControl(this.safeValue('publishContentMapFolders')),
            contentRepositoryId: new FormControl(this.safeValue('contentRepositoryId')),

            urlRenderWayFiles: new FormControl(this.safeValue('urlRenderWayPages')),
            urlRenderWayPages: new FormControl(this.safeValue('urlRenderWayFiles')),

            omitPageExtension: new FormControl(this.safeValue('omitPageExtension')),
            pageLanguageCode: new FormControl(this.safeValue('pageLanguageCode')),
        });
    }

    protected configureForm(value: NodePublishingPropertiesFormData, loud?: boolean): void {
        loud = !!loud;
        const options = { emitEvent: loud, onlySelf: true };

        setControlsEnabled(this.form, CR_CONTROLS, value?.contentRepositoryId > 0, options);
        setControlsEnabled(this.form, PUBLISH_MAP_CONTROLS, value?.publishContentMap, options);
        setControlsEnabled(this.form, FS_CONTROLS, value?.publishFs, options);

        let cr: ContentRepository | null = null;
        if (this.form.value.contentRepositoryId > 0) {
            cr = this.contentRepositories.find(cr => cr.id === this.form.value.contentRepositoryId);
        }
        const isMeshCr = cr?.crType === ContentRepositoryType.MESH;
        const isProjectPerNode = cr?.projectPerNode;
        this.linkButtonDisabled = cr != null && isMeshCr;
        if (this.linkButtonDisabled) {
            this.publishDirsLinked = true;
        }

        setControlsEnabled(this.form, ['publishDir'], cr == null || !isMeshCr || isProjectPerNode, options);
        setControlsEnabled(this.form, ['binaryPublishDir'], cr == null || !isMeshCr || isProjectPerNode, options);

        // We have to use the current/up to date form-value here, as the controls might have been disabled before and therefore are always undefined.
        this.form.updateValueAndValidity();
        const tmpValue = this.form.value;

        // When the `publishContentMap` changes to `true`, check if all other `publishXXX` fields are `false`.
        // If so, then set these to `true`, to enable them by default.
        if (tmpValue?.publishContentMap
            && !this.previousPublishCr
            && !tmpValue?.publishContentMapFiles
            && !tmpValue?.publishContentMapFolders
            && !tmpValue?.publishContentMapPages
        ) {
            this.form.patchValue({
                publishContentMapFiles: true,
                publishContentMapFolders: true,
                publishContentMapPages: true,
            }, options);
        }
        this.previousPublishCr = tmpValue?.publishContentMap ?? false;

        this.checkPublishDirectories(loud);
    }

    protected override onValueChange(): void {
        this.initLinkedDir();

        super.onValueChange();
    }

    protected assembleValue(value: NodePublishingPropertiesFormData): NodePublishingPropertiesFormData {
        return {
            ...value,
            publishDir: value?.publishDir || '',
            binaryPublishDir: value?.binaryPublishDir || '',
        };
    }

    protected initLinkedDir(): void {
        if (
            this.value != null
            && this.value.publishDir
            && this.value.binaryPublishDir
            && this.publishDirsLinked == null
        ) {
            this.publishDirsLinked = this.value.publishDir === this.value.binaryPublishDir;
        }
    }

    protected checkPublishDirectories(loud: boolean = false): void {
        // Nothing to do if they are not linked
        if (!this.publishDirsLinked) {
            return;
        }

        const value = this.form.value;

        if (value.publishDir !== value.binaryPublishDir) {
            this.form.controls.binaryPublishDir.setValue(value.publishDir, {
                emitEvent: loud,
            });
            this.triggerChange(this.form.value as any);
        }
    }

    togglePublishDirLink(): void {
        this.publishDirsLinked = !this.publishDirsLinked;
        this.checkPublishDirectories(true);
    }
}
