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
import { NotificationDemoPage } from '../pages/notification-demo/notification-demo.component';
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

export enum PageType {
    COMPONENT = 'component',
    SERVICE = 'service',
    STYLING = 'css',
    INFORMATION = 'info',
}

export interface IPageInfo {
    path: string;
    component: Type<any>;
    type: PageType;
    keywords?: string[];
}

export const PAGES: IPageInfo[] = [
    {
        path: 'instructions',
        component: InstructionsPage,
        type: PageType.INFORMATION,
    },
    {
        path: 'breadcrumbs',
        component: BreadcrumbsDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'button',
        component: ButtonDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'checkbox',
        component: CheckboxDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'colors',
        component: ColorsDemoPage,
        type: PageType.STYLING,
    },
    {
        path: 'contents-list-item',
        component: ContentsListItemDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'date-time-picker',
        component: DateTimePickerDemoPage,
        type: PageType.COMPONENT,
        keywords: ['calendar'],
    },
    {
        path: 'date-time-picker-controls',
        component: DateTimePickerControlsDemoPage,
        type: PageType.COMPONENT,
        keywords: ['calendar'],
    },
    {
        path: 'dropdown-list',
        component: DropdownListDemoPage,
        type: PageType.COMPONENT,
        keywords: ['menu'],
    },
    {
        path: 'file-drop-area',
        component: FileDropAreaDemoPage,
        type: PageType.COMPONENT,
        keywords: ['file', 'upload', 'drag', 'drop'],
    },
    {
        path: 'file-picker',
        component: FilePickerDemoPage,
        type: PageType.COMPONENT,
        keywords: ['file', 'pick', 'upload'],
    },
    {
        path: 'grid',
        component: GridDemoPage,
        type: PageType.STYLING,
    },
    {
        path: 'grouped-tabs',
        component: GroupedTabsDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'icons',
        component: IconsDemoPage,
        type: PageType.STYLING,
    },
    {
        path: 'input',
        component: InputDemoPage,
        type: PageType.COMPONENT,
        keywords: ['text', 'number'],
    },
    {
        path: 'menu-toggle-button',
        component: MenuToggleButtonDemoPage,
        type: PageType.COMPONENT,
        keywords: ['hamburger'],
    },
    {
        path: 'modal-service',
        component: ModalServiceDemoPage,
        type: PageType.SERVICE,
        keywords: ['dialog', 'overlay'],
    },
    {
        path: 'notification',
        component: NotificationDemoPage,
        type: PageType.SERVICE,
        keywords: ['toast', 'message'],
    },
    {
        path: 'overlay-host',
        component: OverlayHostDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'progress-bar',
        component: ProgressBarDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'radio-button',
        component: RadioButtonDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'range',
        component: RangeDemoPage,
        type: PageType.COMPONENT,
        keywords: ['slider'],
    },
    {
        path: 'search-bar',
        component: SearchBarDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'select',
        component: SelectDemoPage,
        type: PageType.COMPONENT,
        keywords: ['options'],
    },
    {
        path: 'side-menu',
        component: SideMenuDemoPage,
        type: PageType.COMPONENT,
        keywords: ['off-canvas', 'hamburger'],
    },
    {
        path: 'sortable-list',
        component: SortableListDemoPage,
        type: PageType.COMPONENT,
        keywords: ['drag', 'drop'],
    },
    {
        path: 'split-button',
        component: SplitButtonDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'split-view-container',
        component: SplitViewContainerDemoPage,
        type: PageType.COMPONENT,
        keywords: ['panel', 'master-detail'],
    },
    {
        path: 'textarea',
        component: TextareaDemoPage,
        type: PageType.COMPONENT,
    },
    {
        path: 'tabs',
        component: TabsDemoPage,
        type: PageType.COMPONENT,
        keywords: [''],
    },
    {
        path: 'top-bar',
        component: TopBarDemoPage,
        type: PageType.COMPONENT,
        keywords: ['main', 'menu'],
    },
    {
        path: 'typography',
        component: TypographyDemoPage,
        type: PageType.STYLING,
        keywords: ['fonts'],
    },
    {
        path: 'tooltip',
        component: TooltipDemoPage,
        type: PageType.COMPONENT,
        keywords: ['tooltip', 'message'],
    },
    {
        path: 'table',
        component: TableDemoPage,
        type: PageType.COMPONENT,
        keywords: ['table']
    },
];

/**
 * Convert "my-best-string" to "MyBestString"
 */
export function kebabToPascal(str: string): string {
    let camel: string = str.replace(/-([a-z])/g, (m: string) => m[1].toUpperCase());
    return camel.charAt(0).toUpperCase() + camel.slice(1);
}
