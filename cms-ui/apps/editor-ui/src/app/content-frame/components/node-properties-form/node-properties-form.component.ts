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
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ApplicationStateService, FolderActionsService, MarkObjectPropertiesAsModifiedAction } from '@editor-ui/app/state';
import { EditableNodeProps, Folder, Node, Raw, RepositoryBrowserOptions } from '@gentics/cms-models';
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
export class NodePropertiesForm implements OnInit, OnChanges, OnDestroy {

    @Input() properties: EditableNodeProps = {};
    @Input() disabled = false;
    @Input() node: Node;
    @Input() mode: 'create' | 'edit' = 'edit';
    @Output() changes = new EventEmitter<EditableNodeProps>();

    form: UntypedFormGroup;
    changeSub: Subscription;
    defaultFileFolder: Folder | undefined;
    defaultImageFolder: Folder | undefined;
    masterNode: Node | undefined;
    linkInputs: boolean;

    get publishingDisabled(): boolean {
        return this.form.get('disablePublish').value;
    }

    get fileSystemDisabled(): boolean {
        return this.publishingDisabled || !this.form.get('fileSystem').value;
    }

    get contentRepositoryDisabled(): boolean {
        return this.publishingDisabled || !this.form.get('contentRepository').value;
    }

    constructor(
        private changeDetector: ChangeDetectorRef,
        private state: ApplicationStateService,
        private errorHandler: ErrorHandler,
        private folderActions: FolderActionsService,
        private repositoryBrowserClient: RepositoryBrowserClient,
    ) {}

    public ngOnInit(): void {
        this.form = new UntypedFormGroup({
            nodeName: new UntypedFormControl(this.properties.nodeName || '', Validators.required),
            https: new UntypedFormControl(this.properties.https || false),
            host: new UntypedFormControl(this.properties.host || ''),
            utf8: new UntypedFormControl(this.properties.utf8 || false),
            defaultFileFolderId: new UntypedFormControl(this.properties.defaultFileFolderId),
            defaultImageFolderId: new UntypedFormControl(this.properties.defaultImageFolderId),
            disablePublish: new UntypedFormControl(this.properties.disablePublish || false),
            fileSystem: new UntypedFormControl(this.properties.fileSystem || false),
            fileSystemPages: new UntypedFormControl(this.properties.fileSystemPages || false),
            fileSystemFiles: new UntypedFormControl(this.properties.fileSystemFiles || false),
            fileSystemPageDir: new UntypedFormControl(this.properties.fileSystemPageDir || false),
            fileSystemBinaryDir: new UntypedFormControl(this.properties.fileSystemBinaryDir || false),
            contentRepository: new UntypedFormControl(this.properties.contentRepository || false),
            contentRepositoryPages: new UntypedFormControl(this.properties.contentRepositoryPages || false),
            contentRepositoryFiles: new UntypedFormControl(this.properties.contentRepositoryFiles || false),
            contentRepositoryFolders: new UntypedFormControl(this.properties.contentRepositoryFolders || false),
            urlRenderingPages: new UntypedFormControl(this.properties.urlRenderingPages || 0),
            urlRenderingFiles: new UntypedFormControl(this.properties.urlRenderingFiles || 0),
        });

        this.changeSub = this.form.valueChanges.subscribe(changes => {
            // notify state about entity properties validity -> relevant for `ContentFrame.modifiedObjectPropertyValid`
            const isModified = !deepEqual(this.properties, changes);
            this.state.dispatch(new MarkObjectPropertiesAsModifiedAction(isModified, this.form.valid));

            this.changes.emit(changes);
        });

        this.linkInputs = this.properties.fileSystemPageDir === this.properties.fileSystemBinaryDir;

        const fileFolderPromise = this.properties.defaultFileFolderId === undefined ?
            undefined : this.folderActions.getFolder(this.properties.defaultFileFolderId);

        const imageFolderPromise = this.properties.defaultImageFolderId === undefined ?
            undefined : this.folderActions.getFolder(this.properties.defaultImageFolderId);

        this.state.select(state => state.entities.node)
            .map(nodeMap => nodeMap[this.node.inheritedFromId])
            .filter(node => !!node)
            .take(1)
            .subscribe(node => {
                this.masterNode = node;
                this.changeDetector.markForCheck();
            });

        Promise.all([fileFolderPromise, imageFolderPromise])
            .then(([fileFolder, imageFolder]) => {
                this.defaultFileFolder = fileFolder;
                this.defaultImageFolder = imageFolder;
                this.changeDetector.markForCheck();
            });

        // TODO: Looks like a bug in the gtx-select component, where the starting value is not correctly
        // rendered upon creation of the form.
        // See https://jira.gentics.com/browse/GUIC-129
        setTimeout(() => {
            this.form.get('urlRenderingPages').setValue(this.properties.urlRenderingPages, { emitEvent: false });
            this.form.get('urlRenderingFiles').setValue(this.properties.urlRenderingFiles, { emitEvent: false });
            this.changeDetector.markForCheck();
        });

    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes['properties']) {
            this.updateForm(this.properties);
        }
    }

    public ngOnDestroy(): void {
        if (this.changeSub) {
            this.changeSub.unsubscribe();
        }
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
                    this.form.get('defaultFileFolderId').setValue(selected.id);
                } else {
                    this.defaultImageFolder = selected;
                    this.form.get('defaultImageFolderId').setValue(selected.id);
                }
            },
            error => this.errorHandler.catch(error));
    }

    clearDefaultFolder(type: 'file' | 'image'): void {
        if (type === 'file') {
            this.defaultFileFolder = undefined;
            this.form.get('defaultFileFolderId').setValue(undefined);
        } else {
            this.defaultImageFolder = undefined;
            this.form.get('defaultImageFolderId').setValue(undefined);
        }
    }

    fileSystemDirChange(value: string): void {
        if (this.linkInputs) {
            this.form.get('fileSystemPageDir').setValue(value, { emitEvent: false });
            this.form.get('fileSystemBinaryDir').setValue(value, { emitEvent: false });
        }
    }

    toggleLinkInputs(): void {
        if (this.fileSystemDisabled) {
            return;
        }
        if (!this.linkInputs) {
            this.linkInputs = true;
            const pageDir = this.form.get('fileSystemPageDir').value;
            if (pageDir !== this.form.get('fileSystemBinaryDir').value) {
                this.form.get('fileSystemBinaryDir').setValue(pageDir);
            }
        } else {
            this.linkInputs = false;
        }
    }

    private updateForm(properties: EditableNodeProps): void {
        if (!this.form) {
            return;
        }

        this.form.setValue({
            nodeName: properties.nodeName,
            https: properties.https,
            host: properties.host,
            utf8: properties.utf8,
            defaultFileFolderId: properties.defaultFileFolderId,
            defaultImageFolderId: properties.defaultImageFolderId,
            disablePublish: properties.disablePublish,
            fileSystem: properties.fileSystem,
            fileSystemPages: properties.fileSystemPages,
            fileSystemFiles: properties.fileSystemFiles,
            fileSystemPageDir: properties.fileSystemPageDir,
            fileSystemBinaryDir: properties.fileSystemBinaryDir,
            contentRepository: properties.contentRepository,
            contentRepositoryPages: properties.contentRepositoryPages,
            contentRepositoryFiles: properties.contentRepositoryFiles,
            contentRepositoryFolders: properties.contentRepositoryFolders,
            urlRenderingPages: properties.urlRenderingPages,
            urlRenderingFiles: properties.urlRenderingFiles,
        }, { emitEvent: false });
    }
}
