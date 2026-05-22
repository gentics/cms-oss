import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import { ShellComponent } from './components/shell/shell.component';
import { ScopeTabsComponent } from './components/scope-tabs/scope-tabs.component';
import { TranslationsToolbarComponent } from './components/translations-toolbar/translations-toolbar.component';
import { TranslationsTableComponent } from './components/translations-table/translations-table.component';
import { SaveBarComponent } from './components/save-bar/save-bar.component';
import { ToastsComponent } from './components/toasts/toasts.component';
import { CoreModule } from './core/core.module';
import { TranslatePipe } from './pipes/translate.pipe';

@NgModule({
  declarations: [
    AppComponent,
    ShellComponent,
    ScopeTabsComponent,
    TranslationsToolbarComponent,
    TranslationsTableComponent,
    SaveBarComponent,
    ToastsComponent,
    TranslatePipe
  ],
  imports: [
    BrowserModule,
    CommonModule,
    FormsModule,
    CoreModule.forRoot()
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
