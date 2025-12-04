import { enableProdMode } from '@angular/core';
import { platformBrowser } from '@angular/platform-browser';
import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

if (environment.production) {
    enableProdMode();
}

platformBrowser().bootstrapModule(AppModule,
    // Enable preservation of whitespaces for default spacing between components.
    { preserveWhitespaces: true },
)
    .catch((err) => console.error(err));
