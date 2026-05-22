import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ModuleWithProviders, NgModule, Optional, SkipSelf } from '@angular/core';

import { environment } from '../../environments/environment';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { MockApiInterceptor } from './interceptors/mock-api.interceptor';

/**
 * Stellt Core-Infrastruktur (HTTP-Interceptors, providedIn:root-Services
 * sind bereits global verfügbar). Wird genau einmal von AppModule importiert.
 */
@NgModule({
  imports: [],
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    /* AuthInterceptor immer aktiv. Mock-Interceptor läuft VOR ihm,
       sodass Mock-Antworten kein SID-Anhängen benötigen. */
    ...(environment.useMockData
      ? [{ provide: HTTP_INTERCEPTORS, useClass: MockApiInterceptor, multi: true }]
      : []),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ]
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() parent?: CoreModule) {
    if (parent) {
      throw new Error('CoreModule darf nur einmal in AppModule importiert werden.');
    }
  }

  static forRoot(): ModuleWithProviders<CoreModule> {
    return { ngModule: CoreModule };
  }
}
