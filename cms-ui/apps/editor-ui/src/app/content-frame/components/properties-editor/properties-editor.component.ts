/* eslint-disable @typescript-eslint/naming-convention */
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChange,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { EditableProperties } from '@editor-ui/app/common/models';
import { ApplicationStateService } from '@editor-ui/app/state';
import {
    CmsFormData,
    EditableFileProps,
    EditableFolderProps,
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
import { setEnabled } from '@gentics/ui-core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { PermissionService } from '../../../core/providers/permissions/permission.service';

@Component({
    selector: 'properties-editor',
    templateUrl: './properties-editor.component.html',
    styleUrls: ['./properties-editor.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PropertiesEditorComponent implements OnInit, OnChanges, OnDestroy, AfterViewInit {

    @Input()
    item: InheritableItem | Node;

    @Input()
    nodeId: number;

    @Input()
    templates: Template[];

    @Input()
    languages: Language[];

    @Input()
    itemClean: boolean;

    @Output()
    valueChange = new EventEmitter<EditableProperties>();

    @Output()
    itemCleanChange = new EventEmitter<boolean>();

    /** Behaviour to call whenever a new permission check needs to occur */
    permissionCheck = new BehaviorSubject<void>(undefined);

    /** Observable which streams the permission to edit the properties */
    editPermission$: Observable<boolean>;

    /** The properties of the editor/form */
    properties: EditableProperties;

    propertiesCtl: FormControl<EditableProperties>;

    private viewInitialized = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        public permissions: PermissionService,
    ) { }

    ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        if (changes.item || changes.nodeId) {
            this.properties = this.getItemProperties(this.item);
            if (this.propertiesCtl != null) {
                this.propertiesCtl.setValue(this.properties);
            }
            this.permissionCheck.next();
        }
    }

    ngOnInit(): void {
        this.propertiesCtl = new FormControl(this.properties);

        /*
         * TODO: When all forms have been migrated to the base-properties, we can move the modified marker here,
         * to prevent code duplication and have it solved cleanly in one place, rather then 5 or 6.
         */

        // this.subscriptions.push(combineLatest([
        //     this.propertiesCtl.valueChanges,
        //     this.propertiesCtl.statusChanges,
        // ]).subscribe(() => {
        //     this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(this.propertiesCtl.dirty, this.propertiesCtl.valid));
        // }));

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
            tap(enabled => {
                setEnabled(this.propertiesCtl, enabled);
                this.changeDetector.markForCheck();
            }),
        );
    }

    ngAfterViewInit(): void {
        // As the child forms are populated, they will emit change events which we want to ignore since we are
        // only interested in user-created changes, which only occur after the view has initialized.
        this.viewInitialized = true;
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    simplePropertiesChanged(changes: EditableProperties): void {
        if (this.viewInitialized) {
            this.valueChange.emit(changes);
        }
    }

    forwardItemCleanChange(value: boolean): void {
        this.itemCleanChange.emit(value);
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
                    directory: (item as any).directory ?? (item as Folder).publishDir,
                    descriptionI18n: (item as Folder).descriptionI18n,
                    nameI18n: (item as Folder).nameI18n,
                    publishDirI18n: (item as Folder).publishDirI18n,
                } as EditableFolderProps;

            case 'form':{
                let dataProperties: Partial<CmsFormData> = {};
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
                    data: dataProperties,
                };
            }

            case 'page':
                return {
                    pageName: (item as any).pageName ?? item.name,
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
                return item;

            default:
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                throw new Error(`getItemProperties: ${(item as any).type} is not handled.`);
        }
    }

}
