import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import { patchConsoleObject } from './app/development/development-tools';
import { environment } from './environments/environment';

if (environment.production) {
    enableProdMode();
}
patchConsoleObject();

platformBrowserDynamic().bootstrapModule(AppModule, { preserveWhitespaces: true })
    .catch((err: any) => console.error(err));
