import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { LanguageDetailComponent, LanguageMasterComponent } from './components';
import { LANGUAGE_ROUTES } from './language.routes';
import { CanActivateLanguageGuard } from './providers';

@NgModule({
    declarations: [
        LanguageMasterComponent,
        LanguageDetailComponent,
    ],
    providers: [
        CanActivateLanguageGuard,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(LANGUAGE_ROUTES),
    ],
})
export class LanguageModule {}
