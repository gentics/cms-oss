import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AppStateStrategy } from '../../../common/state';
import { I18nService } from '../../../core';

@Component({
    selector: 'gtx-demo-component',
    templateUrl: './demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DemoComponent {
    constructor(
        public appState: AppStateStrategy,
        public i18n: I18nService,
    ) {
        console.log('UI Lang in DemoComponent:', appState.now.ui.language);
        console.log('Admin UI string:', i18n.instant('common.cancel_button'));
        console.log('DemoComponent HelloWorld:', i18n.instant('cc.common.hello_world'));
    }
}
