import {
    Component,
    Input,
    OnDestroy,
    OnInit, ViewChild,
} from '@angular/core';
import {
    EditableFileProps,
    File,
    FileOrImage,
    InheritableItem,
    Node,
} from '@gentics/cms-models';
import {
    EditableProperties,
} from '@editor-ui/app/content-frame/components/properties-editor/properties-editor.component';
import {Observable} from 'rxjs';
import {BaseModal} from '@gentics/ui-core';
import {
    CombinedPropertiesEditorComponent,
} from '@editor-ui/app/content-frame/components/combined-properties-editor/combined-properties-editor.component';

@Component({
    selector: 'image-properties-modal',
    templateUrl: './image-properties-modal.component.html',
    styleUrls: ['./image-properties-modal.component.scss'],
})
export class ImagePropertiesModalComponent extends BaseModal<void> implements OnInit, OnDestroy {

    @Input()
    nodeId: number;

    @Input()
    file: File;

    @ViewChild(CombinedPropertiesEditorComponent)
    private combinedPropertiesEditor: CombinedPropertiesEditorComponent;

    /** The properties of the editor/form */
    properties: EditableProperties;

    /** Observable which streams the permission to edit the properties */
    editPermission$: Observable<boolean>;

    ngOnInit(): void {
        this.properties = this.getItemProperties(this.file);
    }

    ngOnDestroy(): void {
    }

    saveAndClose(): void {
        console.log(`Saving changes to file ${this.file.id}`);

        this.combinedPropertiesEditor.saveChanges()
            .catch(err => {
                console.log('Saving failed:', err);
            })
            .then(() => {
                console.log('Closing modal');

                this.closeFn()
            });
    }

    simplePropertiesChanged(changes: EditableProperties): void {
        console.log('simplePropertiesChanged() called')

        /*
        if (this.viewInitialized) {
            this.valueChange.emit(changes);
            this.changes.next(changes);
        }
         */
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
