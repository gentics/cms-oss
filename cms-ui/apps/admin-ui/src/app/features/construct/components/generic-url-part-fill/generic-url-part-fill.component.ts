import { BO_DISPLAY_NAME, BO_ID, BO_NODE_ID, BO_PERMISSIONS, ContentItem, ContentItemBO, PickableEntity, SelectableType } from '@admin-ui/common';
import { FileOperations, FolderOperations, FormOperations, ImageOperations, PageOperations, TemplateOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import {
    CmsFormTagPartProperty,
    EntityIdType,
    FileTagPartProperty,
    FolderTagPartProperty,
    ImageTagPartProperty,
    PageTagPartProperty,
    TagPropertyType,
    Template,
    TemplateTagTagPartProperty,
} from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { pick } from 'lodash';
import { Observable, Subscription } from 'rxjs';

type GenericUrlPartWithNodeIdProperty =
    | FileTagPartProperty
    | PageTagPartProperty
    | FolderTagPartProperty
    | ImageTagPartProperty
    ;

type GenericUrlPartProperty =
    | GenericUrlPartWithNodeIdProperty
    | TemplateTagTagPartProperty
    | CmsFormTagPartProperty
    ;

@Component({
    selector: 'gtx-generic-url-part-fill',
    templateUrl: './generic-url-part-fill.component.html',
    styleUrls: ['./generic-url-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(GenericUrlPartFillComponent)],
})
export class GenericUrlPartFillComponent
    extends BaseFormElementComponent<GenericUrlPartProperty>
    implements OnChanges {

    @Input()
    public type: TagPropertyType.FILE | TagPropertyType.FOLDER | TagPropertyType.CMSFORM | TagPropertyType.IMAGE | TagPropertyType.PAGE;

    @Input()
    public selectType: SelectableType;

    @Input()
    public clearable = true;

    public loadedEntity: PickableEntity;

    protected loadSubscription: Subscription;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected fileOps: FileOperations,
        protected folderOps: FolderOperations,
        protected formOps: FormOperations,
        protected imageOps: ImageOperations,
        protected pageOps: PageOperations,
        protected templateOps: TemplateOperations,
    ) {
        super(changeDetector);
        this.booleanInputs.push('clearable');
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.type) {
            this.reloadEntity();
        }
    }

    protected onValueChange(): void {
        if ((this.value as any) === CONTROL_INVALID_VALUE){
            return;
        }

        if (this.value == null) {
            this.loadedEntity = null;
            return;
        }

        this.reloadEntity();
    }

    /**
     * Loads the entity from the current value/state and saves it into `loadedEntity`.
     * Loading has to be done in order to display the element, since only the ID
     * is stored in the properties/response.
     */
    reloadEntity(force: boolean = false): void {
        const fieldName = this.getValueFieldName(this.selectType);
        if (!fieldName) {
            return;
        }

        const id = this.value?.[fieldName];
        if (!id) {
            return;
        }

        // Nothing to load, as the current element is already saved
        if (!force && this.loadedEntity != null && this.loadedEntity.entity.id === id) {
            return;
        }

        const loader = this.fetchEntity(id as EntityIdType);
        if (!loader) {
            return;
        }

        if (this.loadSubscription) {
            this.loadSubscription.unsubscribe();
        }

        this.loadSubscription = loader.subscribe(entity => {
            const bo: ContentItemBO = {
                ...entity,
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                [BO_ID]: `${entity.type}.${entity.id}${(entity as any).nodeId ? `:${(entity as any).nodeId}` : ''}`,
                [BO_PERMISSIONS]: [],
                [BO_DISPLAY_NAME]: entity.name,
                [BO_NODE_ID]: (this.value as GenericUrlPartWithNodeIdProperty)?.nodeId,
            };
            this.loadedEntity = {
                entity: bo,
                type: entity?.type ?? 'unknown',
                nodeId: (this.value as GenericUrlPartWithNodeIdProperty)?.nodeId,
            };
            this.changeDetector.markForCheck();
        }, err => {
            console.error('could not load picked entity!', err);
            this.triggerChange(null);
        });
    }

    fetchEntity(entityId: EntityIdType): Observable<ContentItem> {
        switch (this.selectType) {
            case SelectableType.FILE:
                return this.fileOps.get(entityId);

            case SelectableType.FOLDER:
                return this.folderOps.get(entityId as number);

            case SelectableType.FORM:
                return this.formOps.get(entityId as number);

            case SelectableType.IMAGE:
                return this.imageOps.get(entityId);

            case SelectableType.PAGE:
                return this.pageOps.get(entityId);

            case SelectableType.TEMPLATE:
                return this.templateOps.get(entityId) as any as Observable<Template>;

            default:
                return null;
        }
    }

    entityPicked(element: PickableEntity): void {
        this.loadedEntity = element;
        const newValue: GenericUrlPartProperty = {
            ...pick(this.value, ['globalId', 'id', 'partId']),
            type: this.type,
        } as any;
        const fieldName = this.getValueFieldName(this.selectType);

        if (fieldName) {
            newValue[fieldName as any] = element?.entity?.id ?? 0;

            // For Pages, Folders, Files, and Images; we have to save the Node-ID info as well.
            if (this.selectType !== SelectableType.TEMPLATE && this.selectType !== SelectableType.FORM) {
                (newValue as GenericUrlPartWithNodeIdProperty).nodeId = element?.nodeId ?? 0;
            }
        }

        this.triggerChange(newValue);
    }

    getValueFieldName(type: SelectableType): string {
        switch (type) {
            case SelectableType.FILE:
                return 'fileId';
            case SelectableType.FOLDER:
                return 'folderId';
            case SelectableType.FORM:
                return 'formId';
            case SelectableType.IMAGE:
                return 'imageId';
            case SelectableType.PAGE:
                return 'pageId';
            case SelectableType.TEMPLATE:
                return 'templateId';
            default:
                return null;
        }
    }
}
