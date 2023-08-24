import { EditableEntity } from '@admin-ui/common';
import { ObjectPropertyCategoryHandlerService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ObjectPropertyCategory } from '@gentics/cms-models';
import { ObjectPropertyCategoryTableLoaderService } from '../../providers';
import { ObjectpropertyPropertiesMode } from '../object-property-properties/object-property-properties.component';

@Component({
    selector: 'gtx-object-property-category-editor',
    templateUrl: './object-property-category-editor.component.html',
    styleUrls: ['./object-property-category-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ObjectPropertyCategoryEditorComponent extends BaseEntityEditorComponent<EditableEntity.OBJECT_PROPERTY_CATEGORY> {

    public readonly ObjectpropertyPropertiesMode = ObjectpropertyPropertiesMode;

    public fgProperties: FormControl<ObjectPropertyCategory>;

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: ObjectPropertyCategoryHandlerService,
        protected tableLoader: ObjectPropertyCategoryTableLoaderService,
    ) {
        super(
            EditableEntity.OBJECT_PROPERTY_CATEGORY,
            changeDetector,
            route,
            router,
            appState,
            handler,
        );
    }

    protected initializeTabHandles(): void {
        this.fgProperties = new FormControl(this.entity);
        this.tabHandles[this.Tabs.PROPERTIES] = this.createTabHandle(this.fgProperties);
    }

    protected onEntityChange(): void {
        if (this.fgProperties) {
            this.fgProperties.setValue(this.entity);
            this.fgProperties.markAsPristine();
        }
    }

    override onEntityUpdate(): void {
        this.tableLoader.reload();
    }
}
