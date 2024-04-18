import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { ContentRepository, ContentRepositoryType, Node, NodePageLanguageCode, Raw } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';

/**
 * Defines the data editable by the `NodePublishingPropertiesComponent`.
 */
export type NodePublishingPropertiesFormData = Pick<Node, 'disablePublish' | 'publishFs' | 'publishFsPages' | 'publishDir' |
'publishFsFiles' | 'binaryPublishDir' | 'contentRepositoryId' | 'publishContentMapPages' | 'publishContentMapFiles' |
'publishContentMapFolders' | 'urlRenderWayPages' | 'urlRenderWayFiles' | 'omitPageExtension' | 'pageLanguageCode'>;

const FILE_SYSTEM_CONTROLS: (keyof NodePublishingPropertiesFormData)[] = [
    'publishFsPages',
    'publishFsFiles',
];

const PUBLISH_DIR_CONTROLS: (keyof NodePublishingPropertiesFormData)[] = [
    'publishDir',
    'binaryPublishDir',
];

const CR_CONTROLS: (keyof NodePublishingPropertiesFormData)[] = [
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
})
export class NodePublishingPropertiesComponent extends BasePropertiesComponent<NodePublishingPropertiesFormData> implements OnInit {

    public readonly NodePageLanguageCode = NodePageLanguageCode;

    public readonly URL_MODES = {
        0: 'node.url_mode_automatic',
        1: 'node.url_mode_plink',
        2: 'node.url_mode_dynamic',
        3: 'node.url_mode_w_domain',
        4: 'node.url_mode_wo_domain',
    };

    public contentRepositories: ContentRepository<Raw>[] = [];

    public publishDirsLinked: boolean;

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
        return new FormGroup<FormProperties<NodePublishingPropertiesFormData>>({
            disablePublish: new FormControl(this.value?.disablePublish),

            publishFs: new FormControl(this.value?.publishFs),
            publishFsPages: new FormControl(this.value?.publishFsPages),
            publishDir: new FormControl(this.value?.publishDir),
            publishFsFiles: new FormControl(this.value?.publishFsFiles),
            binaryPublishDir: new FormControl(this.value?.binaryPublishDir),

            publishContentMapPages: new FormControl(this.value?.publishContentMapPages),
            publishContentMapFiles: new FormControl(this.value?.publishContentMapFiles),
            publishContentMapFolders: new FormControl(this.value?.publishContentMapFolders),
            contentRepositoryId: new FormControl(this.value?.contentRepositoryId),

            urlRenderWayFiles: new FormControl(this.value?.urlRenderWayPages),
            urlRenderWayPages: new FormControl(this.value?.urlRenderWayFiles),

            omitPageExtension: new FormControl(this.value?.omitPageExtension),
            pageLanguageCode: new FormControl(this.value?.pageLanguageCode),
        });
    }

    protected configureForm(value: NodePublishingPropertiesFormData, loud?: boolean): void {
        loud = !!loud;

        setControlsEnabled(this.form, FILE_SYSTEM_CONTROLS, value?.publishFs, { emitEvent: loud, onlySelf: true });
        setControlsEnabled(this.form, CR_CONTROLS, value?.contentRepositoryId > 0, { emitEvent: loud, onlySelf: true });
        const crAllowsDirs = this.checkContentRepository();

        setControlsEnabled(this.form, ['publishDir'], !!value?.publishFs && value?.publishFsPages && crAllowsDirs, { emitEvent: loud, onlySelf: true });
        setControlsEnabled(this.form, ['binaryPublishDir'], !!value?.publishFs && value?.publishFsFiles && crAllowsDirs, { emitEvent: loud, onlySelf: true });

        this.checkPublishDirectories(loud);
    }

    protected assembleValue(value: NodePublishingPropertiesFormData): NodePublishingPropertiesFormData {
        const tmp: NodePublishingPropertiesFormData = {...value};

        /* Defaulting is done in the parent before sending it to the API */

        // // Default the controls to false (If they are disabled, the value is `null`)
        // [...CR_CONTROLS, ...FILE_SYSTEM_CONTROLS].forEach(field => {
        //     (tmp as any)[field] = tmp[field] ?? false;
        // });
        // PUBLISH_DIR_CONTROLS.forEach(field => {
        //     (tmp  as any)[field] = tmp[field] || '';
        // });

        return tmp;
    }

    protected checkContentRepository(): boolean {
        let enabled = true;

        if (this.form.value.contentRepositoryId > 0) {
            const found = this.contentRepositories.find(cr => cr.id === this.form.value.contentRepositoryId);
            if (found && found.crType === ContentRepositoryType.MESH && found.projectPerNode) {
                enabled = false;
            }
        }

        return enabled;
    }

    protected checkPublishDirectories(loud: boolean = false): void {
        // Nothing to do if they are not linked
        if (!this.publishDirsLinked) {
            return;
        }

        const dir = this.form.controls.publishDir.value;
        const binDirCtl = this.form.controls.binaryPublishDir;

        if (dir !== binDirCtl.value) {
            binDirCtl.setValue(dir, { emitEvent: loud });
        }
    }

    togglePublishDirLink(): void {
        this.publishDirsLinked = !this.publishDirsLinked;
        this.checkPublishDirectories();
    }
}
