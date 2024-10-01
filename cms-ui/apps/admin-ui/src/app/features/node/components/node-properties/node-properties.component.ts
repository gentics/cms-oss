import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import {
    EditableNodeProps,
    Feature,
    NODE_HOSTNAME_PROPERTY_PREFIX,
    NODE_PREVIEW_URL_PROPERTY_PREFIX,
    Node,
    NodeHostnameType,
    NodePreviewurlType,
    Raw,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import {
    FormProperties,
    generateFormProvider,
    setControlsEnabled,
    createPropertyPatternValidator,
    VALIDATOR_REGEX_ERROR_PROPERTY,
    generateValidatorProvider,
} from '@gentics/ui-core';

export type NodePropertiesFormData = Pick<Node, 'name' | 'inheritedFromId' | 'https' | 'host' | 'hostProperty' |
'meshPreviewUrl' | 'meshPreviewUrlProperty' | 'insecurePreviewUrl' | 'meshProjectName' | 'defaultFileFolderId' | 'defaultImageFolderId' |
'pubDirSegment' | 'publishImageVariants'> & {
    description?: string;
    previewType: NodePreviewurlType;
    hostType: NodeHostnameType;
};

export enum NodePropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

@Component({
    selector: 'gtx-node-properties',
    templateUrl: './node-properties.component.html',
    styleUrls: ['./node-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(NodePropertiesComponent),
        generateValidatorProvider(NodePropertiesComponent),
    ],
})
export class NodePropertiesComponent extends BasePropertiesComponent<EditableNodeProps> implements OnInit, OnChanges {

    public readonly NodePropertiesMode = NodePropertiesMode;
    public readonly VALIDATOR_REGEX_ERROR_PROPERTY = VALIDATOR_REGEX_ERROR_PROPERTY;

    /** selectable options for node input hostnameType */
    public readonly HOSTNAME_TYPES: { id: NodeHostnameType; label: string; }[] = [
        {
            id: NodeHostnameType.VALUE,
            label: 'node.hostnameType_value',
        },
        {
            id: NodeHostnameType.PROPERTY,
            label: 'node.hostnameType_property',
        },
    ];

    /** selectable options for node input meshPreviewUrlType */
    public readonly MESH_PREVIEWURL_TYPES: { id: NodePreviewurlType; label: string; }[] = [
        {
            id: NodePreviewurlType.VALUE,
            label: 'node.mesh_preview_url_type_value',
        },
        {
            id: NodePreviewurlType.PROPERTY,
            label: 'node.mesh_preview_url_type_property',
        },
    ];

    @Input()
    public mode: NodePropertiesMode = NodePropertiesMode.CREATE;

    public nodes: Node<Raw>[];
    protected nodesLoading = false;
    protected nodesLoaded = false;

    /**
     * If global feature "pub_dir_segment" is activated, node will have this property.
     *
     * @see https://www.gentics.com/Content.Node/cmp8/guides/feature_pub_dir_segment.html
     */
    public pubDirSegmentActivated: boolean;
    public multiChannelingEnabled = false;
    public meshCrEnabled = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
        private client: GCMSRestClientService,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.appState.select(state => state.features.global[Feature.PUB_DIR_SEGMENT]).subscribe(featureEnabled => {
            this.pubDirSegmentActivated = featureEnabled;
            if (this.form) {
                this.form.controls.pubDirSegment.enable();
            }
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.features.global[Feature.MULTICHANNELLING]).subscribe(featureEnabled => {
            this.multiChannelingEnabled = featureEnabled;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.features.global[Feature.MESH_CR]).subscribe(featureEnabled => {
            this.meshCrEnabled = featureEnabled;
            this.changeDetector.markForCheck();
        }));
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.mode) {
            this.loadNodesIfNeeded();
        }
    }

    protected loadNodesIfNeeded(): void {
        if (this.mode !== NodePropertiesMode.CREATE || this.nodesLoaded || this.nodesLoading) {
            return;
        }

        this.nodesLoading = true;
        this.subscriptions.push(this.client.node.list().subscribe(nodes => {
            this.nodes = nodes.items;
            this.nodesLoading = false;
            this.nodesLoaded = true;
            this.changeDetector.markForCheck();
        }));
    }

    protected createForm(): FormGroup<FormProperties<EditableNodeProperties>> {
        return new FormGroup<FormProperties<EditableNodeProps>>({
            name: new FormControl(this.value?.name, [
                Validators.required,
                Validators.maxLength(50),
            ]),
            description: new FormControl(this.value?.description, Validators.maxLength(200)),
            inheritedFromId: new FormControl({
                value: this.value?.inheritedFromId,
                disabled: this.mode !== NodePropertiesMode.CREATE,
            }),

            previewType: new FormControl<NodePreviewurlType>(this.value?.previewType ?? this.value?.meshPreviewUrlProperty
                ? NodePreviewurlType.PROPERTY
                : NodePreviewurlType.VALUE,
            ),
            meshProjectName: new FormControl(this.value?.meshProjectName, Validators.maxLength(255)),
            meshPreviewUrl: new FormControl(this.value?.meshPreviewUrl, Validators.maxLength(255)),
            meshPreviewUrlProperty: new FormControl(this.value?.meshPreviewUrlProperty, [
                Validators.maxLength(255),
                createPropertyPatternValidator(NODE_PREVIEW_URL_PROPERTY_PREFIX),
            ]),

            https: new FormControl(this.value?.https),
            insecurePreviewUrl: new FormControl(this.value?.insecurePreviewUrl),
            publishImageVariants: new FormControl(this.value?.publishImageVariants),

            hostType: new FormControl<NodeHostnameType>(this.value?.hostType ?? this.value?.hostProperty
                ? NodeHostnameType.PROPERTY
                : NodeHostnameType.VALUE,
            ),
            host: new FormControl(this.value?.host, Validators.maxLength(255)),
            hostProperty: new FormControl(this.value?.hostProperty, [
                Validators.required,
                Validators.maxLength(255),
                createPropertyPatternValidator(NODE_HOSTNAME_PROPERTY_PREFIX),
            ]),

            pubDirSegment: new FormControl({
                value: this.value?.pubDirSegment,
                disabled: !this.pubDirSegmentActivated,
            }),
            defaultFileFolderId: new FormControl(this.value?.defaultFileFolderId),
            defaultImageFolderId: new FormControl(this.value?.defaultImageFolderId),
        });
    }

    protected configureForm(value: Partial<EditableNodeProps>, loud?: boolean): void {
        if (!value) {
            return;
        }

        if (value.previewType) {
            setControlsEnabled(this.form, ['meshPreviewUrl'], value.previewType !== NodePreviewurlType.PROPERTY, {
                emitEvent: loud,
            });
            setControlsEnabled(this.form, ['meshPreviewUrlProperty'], value.previewType === NodePreviewurlType.PROPERTY, {
                emitEvent: loud,
            });
        }

        if (value.hostType) {
            setControlsEnabled(this.form, ['host'], value.hostType !== NodeHostnameType.PROPERTY, {
                emitEvent: loud,
            });
            setControlsEnabled(this.form, ['hostProperty'], value.hostType === NodeHostnameType.PROPERTY, {
                emitEvent: loud,
            });
        }
    }

    protected assembleValue(value: EditableNodeProps): EditableNodeProps {
        return {
            ...value,
            hostProperty: value.hostProperty || '',
            meshPreviewUrlProperty: value.meshPreviewUrlProperty || '',
        };
    }
}
