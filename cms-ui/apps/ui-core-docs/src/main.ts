import { enableProdMode } from '@angular/core';
import { platformBrowser } from '@angular/platform-browser';
import hljs from 'highlight.js/lib/core';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';
import { DocsModule } from './app/app.module';
import { environment } from './environments/environment';

if (environment.production) {
    enableProdMode();
}

// Register only relevant languages
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('xml', xml);
hljs.registerAliases(['html'], { languageName: 'xml' });

platformBrowser().bootstrapModule(DocsModule, {
    preserveWhitespaces: true,
})
    .catch((err: any) => console.error(err));
