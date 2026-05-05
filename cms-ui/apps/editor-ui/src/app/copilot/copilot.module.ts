import { inject, NgModule, provideAppInitializer, Provider, Type } from '@angular/core';
import { SharedModule } from '../shared/shared.module';
import { CopilotSidebarComponent } from './components/copilot-sidebar/copilot-sidebar.component';
import { CopilotConfigService, CopilotStateService } from './providers';

const COMPONENTS: Array<Type<any>> = [
    CopilotSidebarComponent,
];

const PROVIDERS: Provider[] = [
    CopilotConfigService,
    CopilotStateService,
];

/**
 * Triggers the customer-config fetch once during application bootstrap.
 *
 * Mirrors the existing pattern used by `ContentFrameModule` for the
 * `CustomerScriptService`: a fire-and-forget call from
 * `provideAppInitializer` so the editor can render immediately and the
 * Copilot button simply switches on as soon as the YAML has been loaded.
 */
const MODULE_INITIALIZER = provideAppInitializer(() => {
    const config = inject(CopilotConfigService);
    config.load();
});

/**
 * Bundles the Content Copilot scaffolding (sidebar component, config
 * loader, runtime state). The toolbar button itself lives inside the
 * existing `EditorToolbarComponent` because that template already owns
 * the surrounding action group.
 */
@NgModule({
    imports: [
        SharedModule,
    ],
    declarations: COMPONENTS,
    exports: COMPONENTS,
    providers: [...PROVIDERS, MODULE_INITIALIZER],
})
export class CopilotModule {}
