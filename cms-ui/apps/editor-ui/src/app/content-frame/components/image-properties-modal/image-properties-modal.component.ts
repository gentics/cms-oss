import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { EditableProperties } from '@editor-ui/app/common/models';
import {
    CombinedPropertiesEditorComponent,
} from '@editor-ui/app/content-frame/components/combined-properties-editor/combined-properties-editor.component';
import {
    EditableFileProps,
    File,
    FileOrImage,
    InheritableItem,
    Node,
} from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';

@Component({
    selector: 'image-properties-modal',
    templateUrl: './image-properties-modal.component.html',
    styleUrls: ['./image-properties-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImagePropertiesModalComponent extends BaseModal<void> implements OnInit, OnDestroy {

    @Input()
    nodeId: number;

    @Input()
    file: File;

    @ViewChild(CombinedPropertiesEditorComponent, { static: true })
    public combinedPropertiesEditor: CombinedPropertiesEditorComponent;

    /** The properties of the editor/form */
    properties: EditableProperties;

    /** Observable which streams the permission to edit the properties */
    editPermission$: Observable<boolean>;

    ngOnInit(): void {
        this.properties = this.getItemProperties(this.file);
    }

    ngOnDestroy(): void {
    }

    async saveAndClose(): Promise<void> {
        await this.combinedPropertiesEditor.saveItemProperties({ showNotification: false, fetchForConstruct: false, fetchForUpdate: false });
        await this.combinedPropertiesEditor.saveAllObjectProperties();

        this.closeFn();
    }

    private getItemProperties(item: InheritableItem | Node): EditableProperties {
        // an item with type "node" or "channel" may be the base folder of a node. If it has
        // a folder-only property, then we can assume it is the base folder.
        if ((item.type === 'node' || item.type === 'channel') && item.hasOwnProperty('hasSubfolders')) {
            (item as any).type = 'folder';
        }

        switch (item.type) {
            case 'file':
            case 'image':
                return {
                    name: item.name,
                    description: (item as FileOrImage).description,
                    forceOnline: (item as FileOrImage).forceOnline,
                    niceUrl: (item as FileOrImage).niceUrl,
                    alternateUrls: (item as FileOrImage).alternateUrls,
                } as EditableFileProps;

            default:
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                throw new Error(`getItemProperties: ${(item as any).type} is not handled.`);
        }
    }

}
