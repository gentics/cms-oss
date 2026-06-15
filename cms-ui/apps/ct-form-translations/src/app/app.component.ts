import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * App-level bootstrap (translations, REST-client init, SID detection) lives
 * in `provideAppInitializer` in `app.module.ts`. All UI work happens in
 * `ShellComponent`.
 */
@Component({
    selector: 'gtx-root',
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: '<gtx-shell></gtx-shell>',
})
export class AppComponent {}
