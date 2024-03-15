import { AfterViewInit, ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChange } from '@angular/core';
import { EditableProperties } from '@editor-ui/app/common/models';
import {
    EditableFileProps,
    EditableFolderProps,
    EditableFormProps,
    EditableNodeProps,
    EditablePageProps,
    FileOrImage,
    Folder,
    Form,
    InheritableItem,
    Language,
    Node,
    Page,
    Template,
} from '@gentics/cms-models';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { PermissionService } from '../../../core/providers/permissions/permission.service';

@Component({
    selector: 'properties-editor',
    templateUrl: './properties-editor.component.html',
    styleUrls: ['./properties-editor.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PropertiesEditor implements OnInit, OnChanges, AfterViewInit {

    @Input()
    item: InheritableItem | Node;

    @Input()
    nodeId: number;

    @Input()
    templates: Template[];

    @Input()
    languages: Language[];

    @Output()
    valueChange = new EventEmitter<EditableProperties>();

    /**
     * All changes from the child forms are merged into this single stream.
     */
    changes: BehaviorSubject<EditableProperties> = new BehaviorSubject(undefined);

    /** Behaviour to call whenever a new permission check needs to occur */
    permissionCheck = new BehaviorSubject<void>(undefined);

    /** Observable which streams the permission to edit the properties */
    editPermission$: Observable<boolean>;

    /** The properties of the editor/form */
    properties: EditableProperties;

    /**
     * Gets the current `item` as a folder to allow accessing folder-specific properties in the template.
     * Note that this getter does not validate if the item is actually a folder.
     */
    get itemAsFolder(): Folder {
        return this.item as Folder;
    }

    /**
     * Gets the current `item` as a page to allow accessing page-specific properties in the template.
     * Note that this getter does not validate if the item is actually a page.
     */
    get itemAsPage(): Page {
        return this.item as Page;
    }

    private viewInitialized = false;

    constructor(
        public permissions: PermissionService,
    ) { }

    ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        if (changes.item || changes.nodeId) {
            this.properties = this.getItemProperties(this.item);
            this.permissionCheck.next();
        }
    }

    ngOnInit(): void {
        this.editPermission$ = this.permissionCheck.pipe(
            switchMap(() => {
                if (this.item.type === 'folder') {
                    return this.permissions.forFolder(this.item.id, this.nodeId).pipe(
                        map(permission => {
                            return permission.folder.edit;
                        }),
                    );
                }

                return this.permissions.forItem(this.item, this.nodeId).pipe(
                    map(permission => {
                        return permission.edit;
                    }),
                );
            }),
        );
    }

    ngAfterViewInit(): void {
        // As the child forms are populated, they will emit change events which we want to ignore since we are
        // only interested in user-created changes, which only occur after the view has initialized.
        this.viewInitialized = true;
    }

    simplePropertiesChanged(changes: EditableProperties): void {
        if (this.viewInitialized) {
            this.valueChange.emit(changes);
            this.changes.next(changes);
        }
    }

    private getItemProperties(item: InheritableItem | Node): EditableProperties {
        // an item with type "node" or "channel" may be the base folder of a node. If it has
        // a folder-only property, then we can assume it is the base folder.
        if ((item.type === 'node' || item.type === 'channel') && item.hasOwnProperty('hasSubfolders')) {
            (item as any).type = 'folder';
        }

        switch (item.type) {
            case 'folder':
                return {
                    name: item.name,
                    description: (item as Folder).description,
                    directory: (item as Folder).publishDir,
                    descriptionI18n: (item as Folder).descriptionI18n,
                    nameI18n: (item as Folder).nameI18n,
                    publishDirI18n: (item as Folder).publishDirI18n,
                } as EditableFolderProps;

            case 'form':{
                let dataProperties: Partial<EditableFormProps> = {};
                if ((item as Form).data) {
                    dataProperties = {
                        email: (item as Form).data.email,
                        successurl_i18n: (item as Form).data.successurl_i18n,
                        successurl: (item as Form).data.successurl,
                        mailsubject_i18n: (item as Form).data.mailsubject_i18n,
                        mailtemp_i18n: (item as Form).data.mailtemp_i18n,
                        mailsource_pageid: (item as Form).data.mailsource_pageid,
                        mailsource_nodeid: (item as Form).data.mailsource_nodeid,
                        templateContext: (item as Form).data.templateContext,
                        type: (item as Form).data.type,
                        elements: (item as Form).data.elements,
                    }
                }

                return {
                    name: item.name,
                    description: (item as Form).description,
                    languages: (item as Form).languages,
                    successPageId: (item as Form).successPageId,
                    successNodeId: (item as Form).successNodeId,
                    ...dataProperties,
                } as EditableFormProps;
            }

            case 'page':
                return {
                    pageName: item.name,
                    fileName: (item as Page).fileName,
                    description: (item as Page).description,
                    niceUrl: (item as Page).niceUrl,
                    alternateUrls: (item as Page).alternateUrls,
                    templateId: (item as Page).templateId,
                    language: (item as Page).language,
                    customCdate: (item as Page).customCdate,
                    customEdate: (item as Page).customEdate,
                    priority: (item as Page).priority,
                } as EditablePageProps;

            case 'file':
            case 'image':
                return {
                    name: item.name,
                    description: (item as FileOrImage).description,
                    forceOnline: (item as FileOrImage).forceOnline,
                    niceUrl: (item as FileOrImage).niceUrl,
                    alternateUrls: (item as FileOrImage).alternateUrls,
                } as EditableFileProps;

            case 'node':
            case 'channel':
                return {
                    contentRepository: item.publishContentMap,
                    contentRepositoryFiles: item.publishContentMapFiles,
                    contentRepositoryFolders: item.publishContentMapFolders,
                    contentRepositoryPages: item.publishContentMapPages,
                    defaultFileFolderId: item.defaultFileFolderId,
                    defaultImageFolderId: item.defaultImageFolderId,
                    disablePublish: item.disablePublish,
                    fileSystem: item.publishFs,
                    fileSystemBinaryDir: item.binaryPublishDir,
                    fileSystemFiles: item.publishFsFiles,
                    fileSystemPageDir: item.publishDir,
                    fileSystemPages: item.publishFsPages,
                    host: item.host,
                    https: item.https,
                    nodeName: item.name,
                    urlRenderingFiles: item.urlRenderWayFiles,
                    urlRenderingPages: item.urlRenderWayPages,
                    utf8: item.utf8,
                } as EditableNodeProps;

            default:
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                throw new Error(`getItemProperties: ${(item as any).type} is not handled.`);
        }
    }

}
