import { HttpClientModule } from '@angular/common/http';
import { ModuleWithProviders, NgModule, Type } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { NgxPaginationModule } from 'ngx-pagination';
import { App } from './app.component';
import { UI_CORE_DOCS_ROUTES } from './app.routes';
import { AutodocTableComponent } from './components/autodoc-table/autodoc-table.component';
import { AutodocsComponent } from './components/autodocs/autodocs.component';
import { DemoBlockComponent } from './components/demo-block/demo-block.component';
import { HighlightedCodeComponent } from './components/highlighted-code/highlighted-code.component';
import { InheritanceDetailsComponent } from './components/inheritance-details/inheritance-details.component';
import { DemoFormatDirective } from './directives/demo-format/demo-format.directive';
import { AccordionDemoPage } from './pages/accordion-demo/accordion-demo.component';
import { BaseComponentDemoPage } from './pages/base-component-demo/base-component-demo.component';
import { BaseFormElementDemoPage } from './pages/base-form-element-demo/base-form-element-demo.component';
import { BaseTableDemoPage } from './pages/base-table-demo/base-table-demo.component';
import { BreadcrumbsDemoPage } from './pages/breadcrumbs-demo/breadcrumbs-demo.component';
import { ButtonDemoPage } from './pages/button-demo/button-demo.component';
import { CheckboxDemoPage } from './pages/checkbox-demo/checkbox-demo.component';
import { ColorsDemoPage } from './pages/colors-demo/colors-demo.component';
import { ContentsListItemDemoPage } from './pages/contents-list-item-demo/contents-list-item-demo.component';
import { DateTimePickerControlsDemoPage } from './pages/date-time-picker-controls-demo/date-time-picker-controls-demo.component';
import { DateTimePickerDemoPage } from './pages/date-time-picker-demo/date-time-picker-demo.component';
import { DropdownListDemoPage } from './pages/dropdown-list-demo/dropdown-list-demo.component';
import { FileDropAreaDemoPage } from './pages/file-drop-area-demo/file-drop-area-demo.component';
import { FilePickerDemoPage } from './pages/file-picker-demo/file-picker-demo.component';
import { GridDemoPage } from './pages/grid-demo/grid-demo.component';
import { GroupedTabsDemoPage } from './pages/grouped-tabs-demo/grouped-tabs-demo.component';
import { IconsDemoPage } from './pages/icons-demo/icons-demo.component';
import { InputDemoPage } from './pages/input-demo/input-demo.component';
import { InstructionsPage } from './pages/instructions/instructions.component';
import { JsonInputDemoPage } from './pages/json-input-demo/json-input-demo.component';
import { MenuToggleButtonDemoPage } from './pages/menu-toggle-button-demo/menu-toggle-button-demo.component';
import { ModalServiceDemoPage, MyModalComponent } from './pages/modal-service-demo/modal-service-demo.component';
import { NotificationServiceDemoPage } from './pages/notification-service-demo/notification-service-demo.component';
import { OverlayHostDemoPage } from './pages/overlay-host-demo/overlay-host-demo.component';
import { PagingDemoPage } from './pages/paging-demo/paging-demo.component';
import { ProgressBarDemoPage } from './pages/progress-bar-demo/progress-bar-demo.component';
import { RadioButtonDemoPage } from './pages/radio-button-demo/radio-button-demo.component';
import { RangeDemoPage } from './pages/range-demo/range-demo.component';
import { SearchBarDemoPage } from './pages/search-bar-demo/search-bar-demo.component';
import { SelectDemoPage } from './pages/select-demo/select-demo.component';
import { SideMenuDemoPage } from './pages/side-menu-demo/side-menu-demo.component';
import { SortableListDemoPage } from './pages/sortable-list-demo/sortable-list-demo.component';
import { SplitButtonDemoPage } from './pages/split-button-demo/split-button-demo.component';
import { SplitViewContainerDemoPage } from './pages/split-view-container-demo/split-view-container-demo.component';
import { TableDemoPage } from './pages/table-demo/table-demo.component';
import { TabsDemoPage } from './pages/tabs-demo/tabs-demo.component';
import { TextareaDemoPage } from './pages/textarea-demo/textarea-demo.component';
import { TooltipDemoPage } from './pages/tooltip-demo/tooltip-demo.component';
import { TopBarDemoPage } from './pages/top-bar-demo/top-bar-demo.component';
import { TrableDemoPage } from './pages/trable-demo/trable-demo.component';
import { TypographyDemoPage } from './pages/typography-demo/typography-demo.component';
import { LinkToPagePipe } from './pipes/link-to-page/link-to-page.pipe';
import { TrustedHTMLPipe } from './pipes/trusted-html/trusted-html.pipe';
import { TypeOfPipe } from './pipes/typeof/typeof.pipe';
import { DemoDateFormatService } from './providers/demo-date-format/demo-date-format.service';

const DEMO_APP_PAGES: Type<any>[] = [
    AccordionDemoPage,
    BaseComponentDemoPage,
    BaseFormElementDemoPage,
    BaseTableDemoPage,
    BreadcrumbsDemoPage,
    ButtonDemoPage,
    CheckboxDemoPage,
    ColorsDemoPage,
    ContentsListItemDemoPage,
    DateTimePickerDemoPage,
    DateTimePickerControlsDemoPage,
    DemoFormatDirective,
    DropdownListDemoPage,
    FileDropAreaDemoPage,
    FilePickerDemoPage,
    GridDemoPage,
    GroupedTabsDemoPage,
    IconsDemoPage,
    InputDemoPage,
    InstructionsPage,
    JsonInputDemoPage,
    MenuToggleButtonDemoPage,
    ModalServiceDemoPage,
    MyModalComponent,
    NotificationServiceDemoPage,
    OverlayHostDemoPage,
    PagingDemoPage,
    ProgressBarDemoPage,
    PagingDemoPage,
    RadioButtonDemoPage,
    RangeDemoPage,
    SearchBarDemoPage,
    SelectDemoPage,
    SideMenuDemoPage,
    SortableListDemoPage,
    SplitButtonDemoPage,
    SplitViewContainerDemoPage,
    TableDemoPage,
    TrableDemoPage,
    TabsDemoPage,
    TextareaDemoPage,
    TopBarDemoPage,
    TypographyDemoPage,
    TooltipDemoPage,
];

const DEMO_APP_DECLARATIONS: Type<any>[] = [
    AutodocsComponent,
    AutodocTableComponent,
    DemoBlockComponent,
    HighlightedCodeComponent,
    InheritanceDetailsComponent,
    LinkToPagePipe,
    TrustedHTMLPipe,
    TypeOfPipe,
    DemoFormatDirective,
    App,
];

export const DECLARATIONS = [...DEMO_APP_PAGES, ...DEMO_APP_DECLARATIONS];
export const ROUTER_MODULE_FOR_ROOT: ModuleWithProviders<GenticsUICoreModule> = RouterModule.forRoot(UI_CORE_DOCS_ROUTES, {
    useHash: true,
});

@NgModule({
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        ROUTER_MODULE_FOR_ROOT,
        GenticsUICoreModule.forRoot(),
        HttpClientModule,
        NgxPaginationModule,
    ],
    declarations: DECLARATIONS,
    providers: [
        DemoDateFormatService,
    ],
    bootstrap: [App],
})
export class DocsModule { }
