/* eslint-disable @typescript-eslint/no-unsafe-call */
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    SimpleChanges,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { EditableNodeProps } from '@editor-ui/app/common/models';
import { FolderActionsService } from '@editor-ui/app/state';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { RepositoryBrowserOptions } from '@gentics/cms-integration-api-models';
import { Folder, Node, NODE_HOSTNAME_PROPERTY_PREFIX, NodeHostnameType, Raw } from '@gentics/cms-models';
import {
    createPropertyPatternValidator,
    FormProperties,
    generateFormProvider,
    generateValidatorProvider,
    setControlsEnabled,
} from '@gentics/ui-core';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { RepositoryBrowserClient } from '../../../shared/providers';

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
})
export class NodePropertiesComponent
    extends BasePropertiesComponent<EditableNodeProps>
    implements OnInit, OnChanges, OnDestroy {

    public readonly NodeHostnameType = NodeHostnameType;

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

    public defaultFileFolder: Folder | undefined;
    public defaultImageFolder: Folder | undefined;

    public linkInputs = true;
    public hostnameType: NodeHostnameType;

    get publishingDisabled(): boolean {
        return this.form?.controls?.disablePublish?.value;
    }

    get fileSystemDisabled(): boolean {
        return this.publishingDisabled || !this.form?.controls?.publishFs?.value;
    }

    get contentRepositoryDisabled(): boolean {
        return this.publishingDisabled || !this.form?.controls?.publishContentMap?.value;
    }

    constructor(
        changeDetector: ChangeDetectorRef,
        private errorHandler: ErrorHandler,
        private folderActions: FolderActionsService,
        private repositoryBrowserClient: RepositoryBrowserClient,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        if (this.value) {
            this.updateHostnameType(this.value.hostProperty ? NodeHostnameType.PROPERTY : NodeHostnameType.VALUE);
            this.linkInputs = this.value.publishDir === this.value.binaryPublishDir;
        }

        const fileFolderPromise = this.value?.defaultFileFolderId == null ?
            null : this.folderActions.getFolder(this.value.defaultFileFolderId);

        const imageFolderPromise = this.value?.defaultImageFolderId == null ?
            null : this.folderActions.getFolder(this.value.defaultImageFolderId);

        Promise.all([fileFolderPromise, imageFolderPromise])
            .then(([fileFolder, imageFolder]) => {
                this.defaultFileFolder = fileFolder;
                this.defaultImageFolder = imageFolder;
                this.changeDetector.markForCheck();
            });
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    protected createForm(): FormGroup {
        return new FormGroup<FormProperties<EditableNodeProps>>({
            name: new FormControl(this.value?.name || '', Validators.required),
            https: new FormControl(this.value?.https ?? false),
            host: new FormControl(this.value?.host || '', Validators.required),
            hostProperty: new FormControl(this.value?.hostProperty || '', [
                Validators.required,
                createPropertyPatternValidator(NODE_HOSTNAME_PROPERTY_PREFIX),
            ]),
            utf8: new FormControl(this.value?.utf8 ?? false),
            defaultFileFolderId: new FormControl(this.value?.defaultFileFolderId),
            defaultImageFolderId: new FormControl(this.value?.defaultImageFolderId),
            disablePublish: new FormControl(this.value?.disablePublish ?? false),
            publishFs: new FormControl(this.value?.publishFs ?? false),
            publishFsPages: new FormControl(this.value?.publishFsPages ?? false),
            publishFsFiles: new FormControl(this.value?.publishFsFiles ?? false),
            publishDir: new FormControl(this.value?.publishDir || ''),
            binaryPublishDir: new FormControl(this.value?.binaryPublishDir || ''),
            publishContentMap: new FormControl(this.value?.publishContentMap ?? false),
            publishContentMapPages: new FormControl(this.value?.publishContentMapPages ?? false),
            publishContentMapFiles: new FormControl(this.value?.publishContentMapFiles ?? false),
            publishContentMapFolders: new FormControl(this.value?.publishContentMapFolders ?? false),
            urlRenderWayPages: new FormControl(this.value?.urlRenderWayPages || 0),
            urlRenderWayFiles: new FormControl(this.value?.urlRenderWayFiles || 0),
        });
    }

    protected configureForm(value: EditableNodeProps, loud?: boolean): void {
        const options = { onlySelf: loud, emitEvent: loud };
        setControlsEnabled(this.form, [
            'publishFs',
            'publishContentMap',
            'urlRenderWayFiles',
            'urlRenderWayPages',
        ], !this.publishingDisabled, options);
        setControlsEnabled(this.form, [
            'publishFsPages',
            'publishDir',
            'binaryPublishDir',
            'publishFsFiles',
        ], !this.fileSystemDisabled, options);
        setControlsEnabled(this.form, [
            'publishContentMapPages',
            'publishContentMapFiles',
            'publishContentMapFolders',
        ], !this.contentRepositoryDisabled, options);
    }

    protected assembleValue(value: EditableNodeProps): EditableNodeProps {
        return value;
    }

    public updateHostnameType(value: NodeHostnameType): void {
        this.hostnameType = value;
        if (!this.form) {
            return;
        }

        if (this.hostnameType === NodeHostnameType.PROPERTY) {
            // this.form.get('host').setValue('');
            this.form.controls.host.disable();
            this.form.controls.hostProperty.enable();
        } else if (this.hostnameType === NodeHostnameType.VALUE) {
            // this.form.get('hostnameProperty').setValue('');
            this.form.controls.host.enable();
            this.form.controls.hostProperty.disable();
        }
        this.form.updateValueAndValidity();
    }

    selectDefaultFolder(type: 'file' | 'image'): void {
        const options: RepositoryBrowserOptions = {
            allowedSelection: 'folder',
            selectMultiple: false,
        };

        this.repositoryBrowserClient.openRepositoryBrowser(options)
            .then((selected: Folder<Raw>) => {
                if (type === 'file') {
                    this.defaultFileFolder = selected;
                    this.form.controls.defaultFileFolderId.setValue(selected.id);
                } else {
                    this.defaultImageFolder = selected;
                    this.form.controls.defaultImageFolderId.setValue(selected.id);
                }
            })
            .catch(error => this.errorHandler.catch(error));
    }

    clearDefaultFolder(type: 'file' | 'image'): void {
        if (type === 'file') {
            this.defaultFileFolder = undefined;
            this.form.controls.defaultFileFolderId.setValue(undefined);
        } else {
            this.defaultImageFolder = undefined;
            this.form.controls.defaultImageFolderId.setValue(undefined);
        }
    }

    fileSystemDirChange(value: string): void {
        if (this.linkInputs) {
            this.form.controls.publishDir.setValue(value, { emitEvent: false });
            this.form.controls.binaryPublishDir.setValue(value, { emitEvent: false });
        }
    }

    toggleLinkInputs(): void {
        if (this.fileSystemDisabled) {
            return;
        }

        if (!this.linkInputs) {
            this.linkInputs = true;
            const pageDir = this.form.controls.publishDir.value;
            if (pageDir !== this.form.controls.binaryPublishDir.value) {
                this.form.controls.binaryPublishDir.setValue(pageDir);
            }
        } else {
            this.linkInputs = false;
        }
    }

    protected override onValueChange(): void {
        super.onValueChange();

        if (!this.hostnameType) {
            this.updateHostnameType(this.value?.hostProperty ? NodeHostnameType.PROPERTY : NodeHostnameType.VALUE);
        }
    }
}
