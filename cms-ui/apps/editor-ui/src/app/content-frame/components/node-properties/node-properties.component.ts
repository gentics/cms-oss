/* eslint-disable @typescript-eslint/no-unsafe-call */
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { EditableNodeProps } from '@editor-ui/app/common/models';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { ContentRepository, ContentRepositoryType, Node, NODE_HOSTNAME_PROPERTY_PREFIX, NodeHostnameType, NodeUrlMode } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import {
    createPropertyPatternValidator,
    FormProperties,
    generateFormProvider,
    generateValidatorProvider,
    setControlsEnabled,
} from '@gentics/ui-core';

const FS_CONTROLS: (keyof EditableNodeProps)[] = [
    'publishFsPages',
    'publishFsFiles',
];

const CR_CONTROLS: (keyof EditableNodeProps)[] = [
    'publishContentMap',
    'publishContentMapFiles',
    'publishContentMapFolders',
    'publishContentMapPages',
];

const PUBLISH_MAP_CONTROLS: (keyof EditableNodeProps)[] = [
    'publishContentMapFiles',
    'publishContentMapFolders',
    'publishContentMapPages',
];

export enum NodePropertiesMode {
    CREATE = 'create',
    EDIT = 'edit'
}

/**
 * A form for creating or editing the properties of a Node or Channel
 */
@Component({
    selector: 'gtx-node-properties',
    templateUrl: './node-properties.component.html',
    styleUrls: ['./node-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(NodePropertiesComponent),
        generateValidatorProvider(NodePropertiesComponent),
    ],
    standalone: false
})
export class NodePropertiesComponent
    extends BasePropertiesComponent<EditableNodeProps>
    implements OnInit, OnDestroy {

    public readonly NodePropertiesMode = NodePropertiesMode;
    public readonly NodeHostnameType = NodeHostnameType;
    public readonly NodeUrlMode = NodeUrlMode;

    /** selectable options for node input hostnameType */
    public readonly HOSTNAME_TYPES: { id: NodeHostnameType; label: string; }[] = [
        {
            id: NodeHostnameType.VALUE,
            label: 'editor.node_hostname_type_value',
        },
        {
            id: NodeHostnameType.PROPERTY,
            label: 'editor.node_hostname_type_property',
        },
    ];

    @Input()
    public mode: NodePropertiesMode = NodePropertiesMode.EDIT;

    @Input()
    public item: Node | null;

    public publishDirsLinked = true;
    public linkButtonDisabled = false;

    public hostnameType: NodeHostnameType;

    protected previousPublishCr = false;
    protected contentRepositories: ContentRepository[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        if (this.valueIsSet()) {
            this.updateHostnameType(this.value.hostProperty ? NodeHostnameType.PROPERTY : NodeHostnameType.VALUE);
            this.initLinkedDir();
        }

        this.subscriptions.push(this.client.contentRepository.list().subscribe(res => {
            this.contentRepositories = res.items;
            if (this.form) {
                this.configureForm(this.form.value as any, false);
            }
            this.changeDetector.markForCheck();
        }));
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    protected createForm(): FormGroup {
        this.previousPublishCr = this.value?.publishContentMap ?? false;

        return new FormGroup<FormProperties<EditableNodeProps>>({
            name: new FormControl(this.safeValue('name') || '', Validators.required),
            https: new FormControl(this.safeValue('https') ?? false),
            host: new FormControl(this.safeValue('host') || '', Validators.required),
            hostProperty: new FormControl(this.value?.hostProperty || '', [
                Validators.required,
                createPropertyPatternValidator(NODE_HOSTNAME_PROPERTY_PREFIX),
            ]),
            defaultFileFolderId: new FormControl(this.safeValue('defaultFileFolderId')),
            defaultImageFolderId: new FormControl(this.safeValue('defaultImageFolderId')),
            disablePublish: new FormControl(this.safeValue('disablePublish') ?? false),
            publishFs: new FormControl(this.safeValue('publishFs') ?? false),
            publishFsPages: new FormControl(this.safeValue('publishFsPages') ?? false),
            publishFsFiles: new FormControl(this.safeValue('publishFsFiles') ?? false),
            publishDir: new FormControl(this.safeValue('publishDir') || ''),
            binaryPublishDir: new FormControl(this.safeValue('binaryPublishDir') || ''),
            publishContentMap: new FormControl(this.safeValue('publishContentMap') ?? false),
            publishContentMapPages: new FormControl(this.safeValue('publishContentMapPages') ?? false),
            publishContentMapFiles: new FormControl(this.safeValue('publishContentMapFiles') ?? false),
            publishContentMapFolders: new FormControl(this.safeValue('publishContentMapFolders') ?? false),
            urlRenderWayPages: new FormControl(this.safeValue('urlRenderWayPages') || 0),
            urlRenderWayFiles: new FormControl(this.safeValue('urlRenderWayFiles') || 0),
            contentRepositoryId: new FormControl(this.safeValue('contentRepositoryId')),
        });
    }

    protected configureForm(value: EditableNodeProps, loud?: boolean): void {
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
    }

    protected assembleValue(value: EditableNodeProps): EditableNodeProps {
        return {
            ...value,
            host: value.host || '',
            hostProperty: value?.hostProperty || '',
            publishDir: value?.publishDir || '',
            binaryPublishDir: (this.publishDirsLinked ? value?.publishDir : value?.binaryPublishDir) || '',
        };
    }

    public updateHostnameType(value: NodeHostnameType): void {
        this.hostnameType = value;
        if (!this.form) {
            return;
        }

        if (this.hostnameType === NodeHostnameType.PROPERTY) {
            this.form.controls.host.disable();
            this.form.controls.hostProperty.enable();
        } else if (this.hostnameType === NodeHostnameType.VALUE) {
            this.form.controls.host.enable();
            this.form.controls.hostProperty.disable();
        }
        this.form.updateValueAndValidity({ emitEvent: true });
        this.changeDetector.markForCheck();
    }

    protected initLinkedDir(): void {
        if (
            this.value != null
            && this.value.publishDir
            && this.value.binaryPublishDir
            && this.publishDirsLinked == null
        ) {
            this.publishDirsLinked = this.value.publishDir === this.value.binaryPublishDir;
            this.checkPublishDirectories(true);
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

    protected override onValueChange(): void {
        super.onValueChange();

        if (!this.hostnameType) {
            this.updateHostnameType(this.value?.hostProperty ? NodeHostnameType.PROPERTY : NodeHostnameType.VALUE);
        }

        this.initLinkedDir();
    }

    protected override onDisabledChange(): void {
        super.onDisabledChange();

        if (!this.disabled) {
            this.updateHostnameType(this.value?.hostProperty ? NodeHostnameType.PROPERTY : NodeHostnameType.VALUE);
        }
    }
}
