import { HttpClient, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { NgxPaginationModule } from 'ngx-pagination';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './components/app/app.component';
import { DetailChipComponent } from './components/detail-chip/detail-chip.component';
import { DisplayFieldSelectorComponent } from './components/display-field-selector/display-field-selector.component';
import { FilterByEditorComponent } from './components/filter-by-editor/filter-by-editor.component';
import { FilterEditableComponent } from './components/filter-editable/filter-editable.component';
import { FilterOptionsComponent } from './components/filter-options/filter-options.component';
import { FilterTranslationComponent } from './components/filter-translation/filter-translation.component';
import { HeadOptionsComponent } from './components/head-options/head-options.component';
import { ItemListHeaderComponent } from './components/item-list-header/item-list-header.component';
import { ItemListRowComponent } from './components/item-list-row/item-list-row.component';
import { ItemListComponent } from './components/item-list/item-list.component';
import { ListFilterComponent } from './components/list-filter/list-filter.component';
import { ListItemDetailsComponent } from './components/list-item-details/list-item-details.component';
import { ListViewComponent } from './components/list-view/list-view.component';
import { NodeSelectorTreeComponent } from './components/node-selector-tree/node-selector-tree.component';
import { NodeSelectorComponent } from './components/node-selector/node-selector.component';
import { PagingControlsComponent } from './components/paging-controls/paging-controls.component';
import { SortingModalComponent } from './components/sorting-modal/sorting-modal.component';
import { StatusIndicatorComponent } from './components/status-indicator/status-indicator.component';
import { UpdateLinkModalComponent } from './components/update-link-modal/update-link-modal.component';
import { CoreModule } from './core/core.module';
import { HighlightPipe } from './pipes/highlight/highlight.pipe';
import { I18nDatePipe } from './pipes/i18n-date/i18n-date.pipe';
import { UserFullNamePipe } from './pipes/user-full-name/user-full-name.pipe';
import { AppService } from './services/app/app.service';
import { FilterService } from './services/filter/filter.service';
import { LinkCheckerService } from './services/link-checker/link-checker.service';
import { NodeHierarchyBuilderService } from './services/node-hierarchy-builder/node-hierarchy-builder.service';
import { ToolApiService } from './services/tool-api/tool-api.service';
import { UserSettingsService } from './services/user-settings/user-settings.service';

export const createTranslateLoader = (http: HttpClient): TranslateHttpLoader => new TranslateHttpLoader(http, './assets/i18n/', '.json');

@NgModule({ declarations: [
        AppComponent,
        DetailChipComponent,
        DisplayFieldSelectorComponent,
        FilterOptionsComponent,
        ListViewComponent,
        ListFilterComponent,
        ItemListComponent,
        ItemListHeaderComponent,
        ItemListRowComponent,
        ListItemDetailsComponent,
        HeadOptionsComponent,
        NodeSelectorComponent,
        NodeSelectorTreeComponent,
        FilterByEditorComponent,
        FilterEditableComponent,
        FilterTranslationComponent,
        PagingControlsComponent,
        StatusIndicatorComponent,
        SortingModalComponent,
        HighlightPipe,
        I18nDatePipe,
        UserFullNamePipe,
        UpdateLinkModalComponent,
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        GenticsUICoreModule.forRoot(),
        CoreModule,
        NgxPaginationModule,
        RouterTestingModule,
        FormsModule,
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: (createTranslateLoader),
                deps: [HttpClient],
            },
        }),
        AppRoutingModule], providers: [
        AppService,
        FilterService,
        LinkCheckerService,
        NodeHierarchyBuilderService,
        ToolApiService,
        UserSettingsService,
        provideHttpClient(withInterceptorsFromDi()),
    ] })
export class AppModule { }
