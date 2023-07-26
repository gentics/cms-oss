import { NgModule } from '@angular/core';
import { RouterModule, provideRouter, withComponentInputBinding } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    CreateObjectPropertyCategoryModalComponent,
    CreateObjectPropertyModalComponent,
    ObjectPropertyCategortTableComponent,
    ObjectPropertyCategoryEditorComponent,
    ObjectPropertyCategoryMasterComponent,
    ObjectPropertyCategoryPropertiesComponent,
    ObjectPropertyEditorComponent,
    ObjectPropertyMasterComponent,
    ObjectPropertyModuleMasterComponent,
    ObjectpropertyPropertiesComponent,
} from './components';
import { OBJECT_PROPERTY_ROUTES } from './object-property.routes';
import { ObjectPropertyCategoryTableLoaderService } from './providers';

@NgModule({
    declarations: [
        CreateObjectPropertyCategoryModalComponent,
        CreateObjectPropertyModalComponent,
        ObjectPropertyCategoryEditorComponent,
        ObjectPropertyCategoryMasterComponent,
        ObjectPropertyCategoryPropertiesComponent,
        ObjectPropertyCategortTableComponent,
        ObjectPropertyEditorComponent,
        ObjectPropertyMasterComponent,
        ObjectPropertyModuleMasterComponent,
        ObjectpropertyPropertiesComponent,
    ],
    providers: [
        ObjectPropertyCategoryTableLoaderService,
        provideRouter(OBJECT_PROPERTY_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(OBJECT_PROPERTY_ROUTES),
    ],
})
export class ObjectPropertyModule {}
