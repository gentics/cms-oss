import { NgModule, Type } from '@angular/core';
import { SharedModule } from '../shared/shared.module';
import { CopilotSidebarComponent } from './components/copilot-sidebar/copilot-sidebar.component';

const COMPONENTS: Array<Type<any>> = [
    CopilotSidebarComponent,
];

/**
 * Bundles the Content Copilot scaffolding (sidebar component). The
 * toolbar button itself lives inside the existing `EditorToolbarComponent`
 * because that template already owns the surrounding action group.
 *
 * Note: the configuration loader (`CopilotConfigService`) and the
 * sidebar's open/close state (`CopilotStateService`) are deliberately
 * NOT registered here. Both use `providedIn: 'root'` so they survive
 * the fact that this module is reached only via the lazy-loaded
 * `ContentFrameModule` — a module-level `provideAppInitializer` would
 * silently miss the bootstrap window. The fetch is kicked off from
 * `AppComponent.ngOnInit()` instead, mirroring `UIOverridesService`.
 */
@NgModule({
    imports: [
        SharedModule,
    ],
    declarations: COMPONENTS,
    exports: COMPONENTS,
})
export class CopilotModule {}
