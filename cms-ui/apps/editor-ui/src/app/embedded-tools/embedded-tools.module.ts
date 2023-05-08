import { NgModule, Provider, Type } from '@angular/core';
import { SharedModule } from '../shared/shared.module';
import { EmbeddedToolsHostComponent } from './components/embedded-tools-host/embedded-tools-host.component';
import { ToolBreadcrumbComponent } from './components/tool-breadcrumb/tool-breadcrumb.component';
import { ToolButtonComponent } from './components/tool-button/tool-button.component';
import { ToolIframeComponent } from './components/tool-iframe/tool-iframe.component';
import { ToolOverviewComponent } from './components/tool-overview/tool-overview.component';
import { ToolProxyComponent } from './components/tool-proxy/tool-proxy.component';
import { ToolSelectorComponent } from './components/tool-selector/tool-selector.component';

const COMPONENTS: Array<Type<any>> = [
    EmbeddedToolsHostComponent,
    ToolBreadcrumbComponent,
    ToolButtonComponent,
    ToolIframeComponent,
    ToolOverviewComponent,
    ToolProxyComponent,
    ToolSelectorComponent,
];

const ENTRY_COMPONENTS: Array<Type<any>> = [
];

const PROVIDERS: Provider[] = [
    // EmbeddedToolsService,
];


@NgModule({
    imports: [
        SharedModule,
    ],
    exports: [...COMPONENTS],
    declarations: [...COMPONENTS, ...ENTRY_COMPONENTS],
    providers: PROVIDERS,
})
export class EmbeddedToolsModule { }
