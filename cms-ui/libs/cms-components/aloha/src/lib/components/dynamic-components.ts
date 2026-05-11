/* Super hacky workaround for Angular, as it can't resolve cyclic components in libraries. */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { generateFormProvider } from '@gentics/ui-core';
import { AlohaComponentRendererComponent } from './aloha-component-renderer/aloha-component-renderer.component';
import { AlohaSelectMenuRendererComponent } from './aloha-select-menu-renderer/aloha-select-menu-renderer.component';

@Component({
    selector: 'gtx-aloha-component-renderer',
    templateUrl: './aloha-component-renderer/aloha-component-renderer.component.html',
    styleUrls: ['./aloha-component-renderer/aloha-component-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaComponentRendererComponentImpl)],
    standalone: false,
})
export class AlohaComponentRendererComponentImpl extends AlohaComponentRendererComponent {}

@Component({
    selector: 'gtx-aloha-select-menu-renderer',
    templateUrl: './aloha-select-menu-renderer/aloha-select-menu-renderer.component.html',
    styleUrls: ['./aloha-select-menu-renderer/aloha-select-menu-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaSelectMenuRendererComponentImpl)],
    standalone: false,
})
export class AlohaSelectMenuRendererComponentImpl extends AlohaSelectMenuRendererComponent {}
