import { NgModule } from '@angular/core';
import { RouterModule, provideRouter, withComponentInputBinding } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    CreateLanguageModalComponent,
    LanguageEditorComponent,
    LanguageMasterComponent,
    LanguagePropertiesComponent,
} from './components';
import { LANGUAGE_ROUTES } from './language.routes';

@NgModule({
    declarations: [
        LanguageMasterComponent,
        LanguagePropertiesComponent,
        CreateLanguageModalComponent,
        LanguageEditorComponent,
    ],
    providers: [
        provideRouter(LANGUAGE_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(LANGUAGE_ROUTES),
    ],
})
export class LanguageModule {}
