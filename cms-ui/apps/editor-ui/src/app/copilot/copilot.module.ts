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
 * Note: the configuration loader (`CopilotConfigService`) is deliberately
 * NOT registered here — it uses `providedIn: 'root'` so it survives the
 * fact that this module is reached only via the lazy-loaded
 * `ContentFrameModule`. The fetch is kicked off from
 * `AppComponent.ngOnInit()` instead, mirroring `UIOverridesService`.
 *
 * The sidebar's open/close flag lives in the NGXS UI state slice
 * (`state.ui.copilotOpen`) — see `SetCopilotOpenAction`.
 */
@NgModule({
    imports: [
        SharedModule,
    ],
    declarations: COMPONENTS,
    exports: COMPONENTS,
})
export class CopilotModule {}
