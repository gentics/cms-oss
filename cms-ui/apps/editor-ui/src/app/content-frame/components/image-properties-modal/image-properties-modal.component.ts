import {
    ChangeDetectionStrategy,
    Component,
    Input,
    OnInit,
    ViewChild,
} from '@angular/core';
import { EditableProperties } from '@editor-ui/app/common/models';
import {
    CombinedPropertiesEditorComponent,
} from '@editor-ui/app/content-frame/components/combined-properties-editor/combined-properties-editor.component';
import { FileOrImage } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { getItemProperties } from '../../utils';

@Component({
    selector: 'image-properties-modal',
    templateUrl: './image-properties-modal.component.html',
    styleUrls: ['./image-properties-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImagePropertiesModalComponent extends BaseModal<void> implements OnInit {

    @Input()
    nodeId: number;

    @Input()
    file: FileOrImage;

    @ViewChild(CombinedPropertiesEditorComponent, { static: true })
    public combinedPropertiesEditor: CombinedPropertiesEditorComponent;

    /** The properties of the editor/form */
    properties: EditableProperties;

    /** Observable which streams the permission to edit the properties */
    editPermission$: Observable<boolean>;

    ngOnInit(): void {
        this.properties = getItemProperties(this.file);
    }

    async saveAndClose(): Promise<void> {
        await this.combinedPropertiesEditor.saveItemProperties({ showNotification: false, fetchForConstruct: false, fetchForUpdate: false });
        await this.combinedPropertiesEditor.saveAllObjectProperties();

        this.closeFn();
    }
}
