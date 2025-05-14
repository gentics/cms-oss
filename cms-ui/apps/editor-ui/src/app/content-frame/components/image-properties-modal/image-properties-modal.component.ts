import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnInit,
    ViewChild,
} from '@angular/core';
import { EditableProperties } from '@editor-ui/app/common/models';
import {
    CombinedPropertiesEditorComponent,
} from '@editor-ui/app/content-frame/components/combined-properties-editor/combined-properties-editor.component';
import { TagValidatorImpl } from '@editor-ui/app/tag-editor/validation/tag-validator-impl';
import { TagValidator } from '@gentics/cms-integration-api-models';
import {
    FileOrImage,
    ObjectTag,
    Tag,
} from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { getItemProperties } from '../../utils';

function isObjectProperty(tag: Tag): tag is ObjectTag {
    return tag.type === 'OBJECTTAG';
}

@Component({
    selector: 'image-properties-modal',
    templateUrl: './image-properties-modal.component.html',
    styleUrls: ['./image-properties-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
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

    public propertiesValid = false;
    public objPropValid = false;

    private objectPropertyNames: string[] = [];
    private validators: Record<string, TagValidator> = {};
    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) {
        super();
    }

    ngOnInit(): void {
        this.properties = getItemProperties(this.file);

        const requiredObjProps = Object.values(this.file.tags || {})
            .filter(prop => isObjectProperty(prop) && prop.required && !prop.readOnly);

        this.objectPropertyNames = requiredObjProps.map(prop => prop.name);
        this.validators = requiredObjProps.reduce((acc, prop) => {
            acc[prop.name] = new TagValidatorImpl(prop.construct);
            return acc;
        }, {});

        this.validateItem(this.file);
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    validateItem(element: FileOrImage): void {
        this.objPropValid = this.objectPropertyNames.every(name => {
            const validator = this.validators[name];
            const currentState = element?.tags?.[name];
            if (validator == null || currentState == null) {
                return false;
            }
            const isValid = validator.validateAllTagProperties(currentState?.properties).allPropertiesValid;
            return isValid;
        });

        /*
         * Actual validity state can not be fetched, as it's written into `appState.editor.modifiedObjectPropertiesValid`,
         * because it's shared with the object-properties, and only determines the validity state of the currenlt viewed
         * properties/object-property, which is not helpful at all in our case.
         * Needs at the very least the changes from GPU-1561 (6.2) to even get access to these outside of the editors.
         */
        this.propertiesValid = !!element.name;

        this.changeDetector.markForCheck();
    }

    async saveAndClose(): Promise<void> {
        if (!this.objPropValid) {
            return;
        }

        await this.combinedPropertiesEditor.saveItemProperties({ showNotification: false, fetchForConstruct: false, fetchForUpdate: false });
        await this.combinedPropertiesEditor.saveAllObjectProperties();

        this.closeFn();
    }
}
