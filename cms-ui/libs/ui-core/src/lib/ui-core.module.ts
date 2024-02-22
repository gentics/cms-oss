import { CommonModule } from '@angular/common';
import { ModuleWithProviders, NgModule, Type } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HammerModule } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import 'hammerjs';
import { AutosizeModule } from 'ngx-autosize';
import { NgxPaginationModule } from 'ngx-pagination';
import {
    AccordionComponent,
    BlankModal,
    BreadcrumbsComponent,
    ButtonComponent,
    CheckboxComponent,
    ContentsListItem,
    DateTimePickerComponent,
    DateTimePickerControlsComponent,
    DateTimePickerModal,
    DropdownContentComponent,
    DropdownContentWrapperComponent,
    DropdownItemComponent,
    DropdownListComponent,
    DynamicModal,
    FilePickerComponent,
    GroupedTabsComponent,
    InputComponent,
    JsonInputComponent,
    MenuToggleButtonComponent,
    ModalDialogComponent,
    OverlayHostComponent,
    PaginationComponent,
    ProgressBarComponent,
    RadioButtonComponent,
    RangeComponent,
    ScrollMaskComponent,
    SearchBarComponent,
    SelectComponent,
    SideMenuComponent,
    SortableListComponent,
    SortableListDragHandleComponent,
    SplitButtonComponent,
    SplitButtonPrimaryActionComponent,
    SplitViewContainerComponent,
    TabComponent,
    TabGroupComponent,
    TableComponent,
    TabPaneComponent,
    TabsComponent,
    TextareaComponent,
    ToastComponent,
    TooltipComponent,
    TooltipContentWrapperComponent,
    TopBarComponent,
    TrableComponent,
} from './components';
import {
    AutofocusDirective,
    DropdownTriggerDirective,
    FileDropAreaDirective,
    IconDirective,
    PreventFileDropDirective,
    RadioGroupDirective,
    SelectOptionDirective,
    SelectOptionGroupDirective,
    SideMenuToggleDirective,
    SortableItemDirective,
    TabContentDirective,
    TabLabelDirective,
    TooltipContentDirective,
    TooltipTriggerDirective,
} from './directives';
import { configFactory, ConfigService, CustomConfig, defaultConfig, optionsConfig, PredefinedConfig } from './module.config';
import { ConcatPipe, IncludesPipe, MatchesMimeTypePipe, RangePipe, SortPipe, TableActionEnabledPipe, TableCellMapperPipe, TransformPipe, ValuePathPipe } from './pipes';
import {
    DateTimePickerFormatProvider,
    DragStateTrackerFactoryService,
    ModalService,
    NotificationService,
    OverlayHostService,
    PageFileDragHandlerService,
    UserAgentProvider,
} from './providers';

export const UI_CORE_COMPONENTS: Type<any>[] = [
    AccordionComponent,
    BlankModal,
    BreadcrumbsComponent,
    ButtonComponent,
    CheckboxComponent,
    ContentsListItem,
    DateTimePickerComponent,
    DateTimePickerControlsComponent,
    DateTimePickerModal,
    DropdownListComponent,
    DropdownContentWrapperComponent,
    DropdownContentComponent,
    DropdownItemComponent,
    DynamicModal,
    FilePickerComponent,
    GroupedTabsComponent,
    InputComponent,
    JsonInputComponent,
    MenuToggleButtonComponent,
    ModalDialogComponent,
    OverlayHostComponent,
    PaginationComponent,
    ProgressBarComponent,
    RadioButtonComponent,
    RangeComponent,
    ScrollMaskComponent,
    SearchBarComponent,
    SelectComponent,
    SideMenuComponent,
    SortableListComponent,
    SortableListDragHandleComponent,
    SplitButtonComponent,
    SplitButtonPrimaryActionComponent,
    SplitViewContainerComponent,
    TabComponent,
    TabsComponent,
    TabPaneComponent,
    TableComponent,
    TabGroupComponent,
    TextareaComponent,
    ToastComponent,
    TooltipComponent,
    TooltipContentWrapperComponent,
    TopBarComponent,
    TrableComponent,
];

export const UI_CORE_DIRECTIVES: Type<any>[] = [
    AutofocusDirective,
    DropdownTriggerDirective,
    FileDropAreaDirective,
    IconDirective,
    PreventFileDropDirective,
    RadioGroupDirective,
    SelectOptionDirective,
    SelectOptionGroupDirective,
    SideMenuToggleDirective,
    SortableItemDirective,
    TabContentDirective,
    TabLabelDirective,
    TooltipContentDirective,
    TooltipTriggerDirective,
];

export const UI_CORE_PIPES: Type<any>[] = [
    ConcatPipe,
    IncludesPipe,
    MatchesMimeTypePipe,
    RangePipe,
    SortPipe,
    TableActionEnabledPipe,
    TableCellMapperPipe,
    TransformPipe,
    ValuePathPipe,
];

export const UI_CORE_PROVIDERS: Type<any>[] = [
    DateTimePickerFormatProvider,
    DragStateTrackerFactoryService,
    ModalService,
    NotificationService,
    OverlayHostService,
    PageFileDragHandlerService,
    UserAgentProvider,
];

export const UI_CORE_DECLATATIONS = [...UI_CORE_COMPONENTS, ...UI_CORE_DIRECTIVES, ...UI_CORE_PIPES];
export const routerModuleForChild: ModuleWithProviders<GenticsUICoreModule> = RouterModule.forChild([]);

@NgModule({
    imports: [
        CommonModule,
        HammerModule,
        FormsModule,
        ReactiveFormsModule,
        routerModuleForChild,
        AutosizeModule,
        NgxPaginationModule,
    ],
    declarations: UI_CORE_DECLATATIONS,
    exports: UI_CORE_DECLATATIONS,
})
export class GenticsUICoreModule {
    /**
     * Gentics UI Core exposes several providers which are intended to be used a singleton services, i.e. there should only
     * be a single instance of each in an app. When this module is imported into lazy-loaded child modules, we do not want
     * to include those providers, otherwise the app injector would create new instances of them to use in that child
     * module.
     *
     * Therefore this method should be used only once in the app, at the level of the root module to ensure that only one
     * instance of each provider is instantiated.
     */
    static forRoot(configValue?: optionsConfig | (() => optionsConfig)): ModuleWithProviders<GenticsUICoreModule> {
        return {
            ngModule: GenticsUICoreModule,
            providers: [
                {
                    provide: CustomConfig,
                    useValue: configValue,
                },
                {
                    provide: PredefinedConfig,
                    useValue: defaultConfig,
                },
                {
                    provide: ConfigService,
                    useFactory: configFactory,
                    deps: [PredefinedConfig, CustomConfig],
                },
                ...UI_CORE_PROVIDERS,
            ],
        };
    }
}
