import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    CreateObjectPropertyCategoryModalComponent,
    CreateObjectPropertyModalComponent,
    ObjectPropertyCategortTableComponent,
    ObjectPropertyCategoryDetailComponent,
    ObjectPropertyCategoryMasterComponent,
    ObjectPropertyCategoryPropertiesComponent,
    ObjectPropertyDetailComponent,
    ObjectPropertyMasterComponent,
    ObjectPropertyModuleMasterComponent,
    ObjectpropertyPropertiesComponent,
} from './components';
import { OBJECT_PROPERTY_ROUTES } from './object-property.routes';
import {
    CanActivateObjectPropertyCategoryGuard,
    CanActivateObjectPropertyGuard,
    ObjectPropertyCategoryTableLoaderService,
} from './providers';

@NgModule({
    declarations: [
        CreateObjectPropertyCategoryModalComponent,
        CreateObjectPropertyModalComponent,
        ObjectPropertyCategoryDetailComponent,
        ObjectPropertyCategoryMasterComponent,
        ObjectPropertyCategoryPropertiesComponent,
        ObjectPropertyCategortTableComponent,
        ObjectPropertyDetailComponent,
        ObjectPropertyMasterComponent,
        ObjectPropertyModuleMasterComponent,
        ObjectpropertyPropertiesComponent,
    ],
    providers: [
        CanActivateObjectPropertyGuard,
        CanActivateObjectPropertyCategoryGuard,
        ObjectPropertyCategoryTableLoaderService,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(OBJECT_PROPERTY_ROUTES),
    ],
})
export class ObjectPropertyModule {}
