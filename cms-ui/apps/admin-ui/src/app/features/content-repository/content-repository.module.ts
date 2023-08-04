import { NgModule } from '@angular/core';
import { RouterModule, provideRouter, withComponentInputBinding } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    ContentRepositoryEditorComponent,
    ContentRepositoryMasterComponent,
    ContentRepositoryPropertiesComponent,
    CreateContentRepositoryModalComponent,
} from './components';
import { CONTENT_REPOSIROTY_ROUTES } from './content-repository.routes';

@NgModule({
    declarations: [
        ContentRepositoryEditorComponent,
        ContentRepositoryMasterComponent,
        ContentRepositoryPropertiesComponent,
        CreateContentRepositoryModalComponent,
    ],
    providers: [
        provideRouter(CONTENT_REPOSIROTY_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(CONTENT_REPOSIROTY_ROUTES),
    ],
})
export class ContentRepositoryModule {}
