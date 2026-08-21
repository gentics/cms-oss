import { EditableEntity, EntityUpdateRequestModel } from '@admin-ui/common';
import { ObjectPropertyHandlerService, ObjectPropertyTableLoaderService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ObjectProperty } from '@gentics/cms-models';
import { ObjectpropertyPropertiesMode } from '../object-property-properties/object-property-properties.component';

@Component({
    selector: 'gtx-object-property-editor',
    templateUrl: './object-property-editor.component.html',
    styleUrls: ['./object-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ObjectPropertyEditorComponent extends BaseEntityEditorComponent<EditableEntity.OBJECT_PROPERTY> {

    public readonly ObjectpropertyPropertiesMode = ObjectpropertyPropertiesMode;

    public fgProperties: FormControl<ObjectProperty>;

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: ObjectPropertyHandlerService,
        protected tableLoader: ObjectPropertyTableLoaderService,
    ) {
        super(
            EditableEntity.OBJECT_PROPERTY,
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
