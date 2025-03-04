/* eslint-disable @typescript-eslint/naming-convention */
import { Type } from '@angular/core';
import { BreadcrumbsDemoPage } from '../pages/breadcrumbs-demo/breadcrumbs-demo.component';
import { ButtonDemoPage } from '../pages/button-demo/button-demo.component';
import { CheckboxDemoPage } from '../pages/checkbox-demo/checkbox-demo.component';
import { ColorsDemoPage } from '../pages/colors-demo/colors-demo.component';
import { ContentsListItemDemoPage } from '../pages/contents-list-item-demo/contents-list-item-demo.component';
import { DateTimePickerControlsDemoPage } from '../pages/date-time-picker-controls-demo/date-time-picker-controls-demo.component';
import { DateTimePickerDemoPage } from '../pages/date-time-picker-demo/date-time-picker-demo.component';
import { DropdownListDemoPage } from '../pages/dropdown-list-demo/dropdown-list-demo.component';
import { FileDropAreaDemoPage } from '../pages/file-drop-area-demo/file-drop-area-demo.component';
import { FilePickerDemoPage } from '../pages/file-picker-demo/file-picker-demo.component';
import { GridDemoPage } from '../pages/grid-demo/grid-demo.component';
import { GroupedTabsDemoPage } from '../pages/grouped-tabs-demo/grouped-tabs-demo.component';
import { IconsDemoPage } from '../pages/icons-demo/icons-demo.component';
import { InputDemoPage } from '../pages/input-demo/input-demo.component';
import { InstructionsPage } from '../pages/instructions/instructions.component';
import { MenuToggleButtonDemoPage } from '../pages/menu-toggle-button-demo/menu-toggle-button-demo.component';
import { ModalServiceDemoPage } from '../pages/modal-service-demo/modal-service-demo.component';
import { NotificationServiceDemoPage } from '../pages/notification-service-demo/notification-service-demo.component';
import { OverlayHostDemoPage } from '../pages/overlay-host-demo/overlay-host-demo.component';
import { ProgressBarDemoPage } from '../pages/progress-bar-demo/progress-bar-demo.component';
import { RadioButtonDemoPage } from '../pages/radio-button-demo/radio-button-demo.component';
import { RangeDemoPage } from '../pages/range-demo/range-demo.component';
import { SearchBarDemoPage } from '../pages/search-bar-demo/search-bar-demo.component';
import { SelectDemoPage } from '../pages/select-demo/select-demo.component';
import { SideMenuDemoPage } from '../pages/side-menu-demo/side-menu-demo.component';
import { SortableListDemoPage } from '../pages/sortable-list-demo/sortable-list-demo.component';
import { SplitButtonDemoPage } from '../pages/split-button-demo/split-button-demo.component';
import { SplitViewContainerDemoPage } from '../pages/split-view-container-demo/split-view-container-demo.component';
import { TabsDemoPage } from '../pages/tabs-demo/tabs-demo.component';
import { TextareaDemoPage } from '../pages/textarea-demo/textarea-demo.component';
import { TooltipDemoPage } from '../pages/tooltip-demo/tooltip-demo.component';
import { TopBarDemoPage } from '../pages/top-bar-demo/top-bar-demo.component';
import { TypographyDemoPage } from '../pages/typography-demo/typography-demo.component';
import { TableDemoPage } from '../pages/table-demo/table-demo.component';
import { TrableDemoPage } from '../pages/trable-demo/trable-demo.component';
import { BaseComponentDemoPage } from '../pages/base-component-demo/base-component-demo.component';
import { BaseFormElementDemoPage } from '../pages/base-form-element-demo/base-form-element-demo.component';
import { BaseTableDemoPage } from '../pages/base-table-demo/base-table-demo.component';
import { AccordionDemoPage } from '../pages/accordion-demo/accordion-demo.component';
import { JsonInputDemoPage } from '../pages/json-input-demo/json-input-demo.component';
import { PagingDemoPage } from '../pages/paging-demo/paging-demo.component';
import { ConcatDemoPage } from '../pages/concat-demo/concat-demo.component';

export enum PageType {
    COMPONENT = 'component',
    SERVICE = 'service',
    STYLING = 'css',
    INFORMATION = 'info',
    DIRECTIVE = 'directive',
    PIPE = 'pipe',
}

export interface IPageInfo {
    path: string;
    type: PageType;
    keywords?: string[];
    component: Type<any>;
}

export const PAGES: Record<string, IPageInfo> = {
    _instructions: {
        path: 'instructions',
        component: InstructionsPage,
        type: PageType.INFORMATION,
    },
    'accordion.component': {
        path: 'accordion',
        component: AccordionDemoPage,
        type: PageType.COMPONENT,
        keywords: ['spoiler'],
    },
    'base.component': {
        path: 'base',
        component: BaseComponentDemoPage,
        type: PageType.COMPONENT,
    },
    'base-form-element.component': {
        path: 'base-form-element',
        component: BaseFormElementDemoPage,
        type: PageType.COMPONENT,
    },
    'base-table.component': {
        path: 'base-table',
        component: BaseTableDemoPage,
        type: PageType.COMPONENT,
    },
    'breadcrumbs.component': {
        path: 'breadcrumbs',
        component: BreadcrumbsDemoPage,
        type: PageType.COMPONENT,
    },
    'button.component': {
        path: 'button',
        component: ButtonDemoPage,
        type: PageType.COMPONENT,
    },
    'checkbox.component': {
        path: 'checkbox',
        component: CheckboxDemoPage,
        type: PageType.COMPONENT,
    },
    'concat.pipe': {
        path: 'concat',
        component: ConcatDemoPage,
        type: PageType.PIPE,
    },
    _colors: {
        path: 'colors',
        component: ColorsDemoPage,
        type: PageType.STYLING,
    },
    _contentsListItem: {
        path: 'contents-list-item',
        component: ContentsListItemDemoPage,
        type: PageType.COMPONENT,
    },
    'date-time-picker.component': {
        path: 'date-time-picker',
        component: DateTimePickerDemoPage,
        type: PageType.COMPONENT,
        keywords: ['calendar'],
    },
    'date-time-picker-controls.component': {
        path: 'date-time-picker-controls',
        component: DateTimePickerControlsDemoPage,
        type: PageType.COMPONENT,
        keywords: ['calendar'],
    },
    'dropdown-list.component': {
        path: 'dropdown-list',
        component: DropdownListDemoPage,
        type: PageType.COMPONENT,
        keywords: ['menu'],
    },
    'file-drop-area.directive': {
        path: 'file-drop-area',
        component: FileDropAreaDemoPage,
        type: PageType.DIRECTIVE,
        keywords: ['file', 'upload', 'drag', 'drop'],
    },
    'file-picker.component': {
        path: 'file-picker',
        component: FilePickerDemoPage,
        type: PageType.COMPONENT,
        keywords: ['file', 'pick', 'upload'],
    },
    _grid: {
        path: 'grid',
        component: GridDemoPage,
        type: PageType.STYLING,
    },
    'grouped-tabs.component': {
        path: 'grouped-tabs',
        component: GroupedTabsDemoPage,
        type: PageType.COMPONENT,
    },
    _icons: {
        path: 'icons',
        component: IconsDemoPage,
        type: PageType.STYLING,
    },
    'input.component': {
        path: 'input',
        component: InputDemoPage,
        type: PageType.COMPONENT,
        keywords: ['text', 'number'],
    },
    'json-input.component': {
        path: 'json-input',
        component: JsonInputDemoPage,
        type: PageType.COMPONENT,
    },
    'menu-toggle-button.component': {
        path: 'menu-toggle-button',
        component: MenuToggleButtonDemoPage,
        type: PageType.COMPONENT,
        keywords: ['hamburger'],
    },
    'modal.service': {
        path: 'modal-service',
        component: ModalServiceDemoPage,
        type: PageType.SERVICE,
        keywords: ['dialog', 'overlay'],
    },
    'notification.service': {
        path: 'notification-service',
        component: NotificationServiceDemoPage,
        type: PageType.SERVICE,
        keywords: ['toast', 'message'],
    },
    'overlay-host.component': {
        path: 'overlay-host',
        component: OverlayHostDemoPage,
        type: PageType.COMPONENT,
    },
    'paging.component': {
        path: 'paging',
        component: PagingDemoPage,
        type: PageType.COMPONENT,
    },
    'progress-bar.component': {
        path: 'progress-bar',
        component: ProgressBarDemoPage,
        type: PageType.COMPONENT,
    },
    'radio-button.component': {
        path: 'radio-button',
        component: RadioButtonDemoPage,
        type: PageType.COMPONENT,
    },
    'range.component': {
        path: 'range',
        component: RangeDemoPage,
        type: PageType.COMPONENT,
        keywords: ['slider'],
    },
    'search-bar.component': {
        path: 'search-bar',
        component: SearchBarDemoPage,
        type: PageType.COMPONENT,
    },
    'select.component': {
        path: 'select',
        component: SelectDemoPage,
        type: PageType.COMPONENT,
        keywords: ['options'],
    },
    'side-menu.component': {
        path: 'side-menu',
        component: SideMenuDemoPage,
        type: PageType.COMPONENT,
        keywords: ['off-canvas', 'hamburger'],
    },
    'sortable-list.component': {
        path: 'sortable-list',
        component: SortableListDemoPage,
        type: PageType.COMPONENT,
        keywords: ['drag', 'drop'],
    },
    'split-button.component': {
        path: 'split-button',
        component: SplitButtonDemoPage,
        type: PageType.COMPONENT,
    },
    'split-view-container.component': {
        path: 'split-view-container',
        component: SplitViewContainerDemoPage,
        type: PageType.COMPONENT,
        keywords: ['panel', 'master-detail'],
    },
    'table.component': {
        path: 'table',
        component: TableDemoPage,
        type: PageType.COMPONENT,
        keywords: ['table'],
    },
    'tabs.component': {
        path: 'tabs',
        component: TabsDemoPage,
        type: PageType.COMPONENT,
        keywords: [''],
    },
    'textarea.component': {
        path: 'textarea',
        component: TextareaDemoPage,
        type: PageType.COMPONENT,
    },
    'top-bar.component': {
        path: 'top-bar',
        component: TopBarDemoPage,
        type: PageType.COMPONENT,
        keywords: ['main', 'menu'],
    },
    'trable.component': {
        path: 'trable',
        component: TrableDemoPage,
        type: PageType.COMPONENT,
        keywords: ['trable', 'table'],
    },
    _typography: {
        path: 'typography',
        component: TypographyDemoPage,
        type: PageType.STYLING,
        keywords: ['fonts'],
    },
    'tooltip.component': {
        path: 'tooltip',
        component: TooltipDemoPage,
        type: PageType.COMPONENT,
        keywords: ['tooltip', 'message'],
    },
};

/**
 * Convert "my-best-string" to "MyBestString"
 */
export function kebabToPascal(str: string): string {
    const camel: string = str.replace(/-([a-z])/g, (m: string) => m[1].toUpperCase());
    return camel.charAt(0).toUpperCase() + camel.slice(1);
}
