/* eslint-disable @typescript-eslint/no-unsafe-call */
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { EditableNodeProps } from '@editor-ui/app/common/models';
import { ApplicationStateService, FolderActionsService, MarkObjectPropertiesAsModifiedAction } from '@editor-ui/app/state';
import { RepositoryBrowserOptions } from '@gentics/cms-integration-api-models';
import { Folder, NODE_HOSTNAME_PROPERTY_PREFIX, Node, NodeHostnameType, Raw } from '@gentics/cms-models';
import { FormProperties, createPropertyPatternValidator } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { deepEqual } from '../../../common/utils/deep-equal';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { RepositoryBrowserClient } from '../../../shared/providers';

/**
 * A form for creating or editing the properties of a Node or Channel
 */
@Component({
    selector: 'node-properties-form',
    templateUrl: './node-properties-form.tpl.html',
    styleUrls: ['./node-properties-form.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodePropertiesFormComponent implements OnInit, OnChanges, OnDestroy {

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
    public properties: EditableNodeProps = {} as any;

    @Input()
    public disabled = false;

    @Input()
    public node: Node;

    @Input()
    public mode: 'create' | 'edit' = 'edit';

    @Output()
    public changes = new EventEmitter<EditableNodeProps>();

    form: FormGroup<FormProperties<EditableNodeProps>>;

    defaultFileFolder: Folder | undefined;
    defaultImageFolder: Folder | undefined;
    linkInputs: boolean;
    hostnameType: NodeHostnameType;

    get publishingDisabled(): boolean {
        return this.form.controls.disablePublish.value;
    }

    get fileSystemDisabled(): boolean {
        return this.publishingDisabled || !this.form.controls.publishFs.value;
    }

    get contentRepositoryDisabled(): boolean {
        return this.publishingDisabled || !this.form.controls.publishContentMap.value;
    }

    get hostnameDisabled(): boolean {
        return this.hostnameType !== NodeHostnameType.VALUE;
    }

    get hostnamePropertyDisabled(): boolean {
        return this.hostnameType !== NodeHostnameType.PROPERTY;
    }

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private state: ApplicationStateService,
        private errorHandler: ErrorHandler,
        private folderActions: FolderActionsService,
        private repositoryBrowserClient: RepositoryBrowserClient,
    ) {}

    public ngOnInit(): void {
        this.form = new FormGroup<FormProperties<EditableNodeProps>>({
            name: new FormControl(this.properties.name || '', Validators.required),
            https: new FormControl(this.properties.https ?? false),
            host: new FormControl(this.properties.host || '', Validators.required),
            hostProperty: new FormControl(this.properties.hostProperty || '', [
                Validators.required,
                createPropertyPatternValidator(NODE_HOSTNAME_PROPERTY_PREFIX),
            ]),
            utf8: new FormControl(this.properties.utf8 ?? false),
            defaultFileFolderId: new FormControl(this.properties.defaultFileFolderId),
            defaultImageFolderId: new FormControl(this.properties.defaultImageFolderId),
            disablePublish: new FormControl(this.properties.disablePublish ?? false),
            publishFs: new FormControl(this.properties.publishFs ?? false),
            publishFsPages: new FormControl(this.properties.publishFsPages ?? false),
            publishFsFiles: new FormControl(this.properties.publishFsFiles ?? false),
            publishDir: new FormControl(this.properties.publishDir || ''),
            binaryPublishDir: new FormControl(this.properties.binaryPublishDir || ''),
            publishContentMap: new FormControl(this.properties.publishContentMap ?? false),
            publishContentMapPages: new FormControl(this.properties.publishContentMapPages ?? false),
            publishContentMapFiles: new FormControl(this.properties.publishContentMapFiles ?? false),
            publishContentMapFolders: new FormControl(this.properties.publishContentMapFolders ?? false),
            urlRenderWayPages: new FormControl(this.properties.urlRenderWayPages || 0),
            urlRenderWayFiles: new FormControl(this.properties.urlRenderWayFiles || 0),
        });

        if (this.properties) {
            this.updateHostnameType(this.properties.hostProperty ? NodeHostnameType.PROPERTY : NodeHostnameType.VALUE);
        }

        this.subscriptions.push(this.form.valueChanges.subscribe(changes => {
            // notify state about entity properties validity -> relevant for `ContentFrame.modifiedObjectPropertyValid`
            const isModified = !deepEqual(this.properties, changes);
            this.state.dispatch(new MarkObjectPropertiesAsModifiedAction(isModified, this.form.valid));

            this.changes.emit(changes as any);
        }));

        this.linkInputs = this.properties.publishDir === this.properties.binaryPublishDir;

        const fileFolderPromise = this.properties.defaultFileFolderId === undefined ?
            undefined : this.folderActions.getFolder(this.properties.defaultFileFolderId);

        const imageFolderPromise = this.properties.defaultImageFolderId === undefined ?
            undefined : this.folderActions.getFolder(this.properties.defaultImageFolderId);

        Promise.all([fileFolderPromise, imageFolderPromise])
            .then(([fileFolder, imageFolder]) => {
                this.defaultFileFolder = fileFolder;
                this.defaultImageFolder = imageFolder;
                this.changeDetector.markForCheck();
            });
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes['properties']) {
            this.updateForm(this.properties);
        }
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public updateHostnameType(value: NodeHostnameType): void {
        this.hostnameType = value;
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

    private updateForm(properties: EditableNodeProps): void {
        if (!this.form) {
            return;
        }

        if (!this.hostnameType) {
            this.updateHostnameType(properties.hostProperty ? NodeHostnameType.PROPERTY : NodeHostnameType.VALUE);
        }

        this.form.patchValue(properties, { emitEvent: false });
    }
}
