import { ModuleWithProviders, NgModule } from "@angular/core";
import { provideStates } from "@ngxs/store";
import { KeycloakService } from "./providers";
import { AuthStateModule } from "./state/auth.state";

@NgModule({
    providers: [
        KeycloakService,
    ],
})
export class AuthenticationModule {
    static forRoot(): ModuleWithProviders<AuthenticationModule> {
        return {
            ngModule: AuthenticationModule,
            providers: [
                provideStates([AuthStateModule]),
            ],
        }
    }
}

