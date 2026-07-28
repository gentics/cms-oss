import { Injectable } from '@angular/core';
import { WindowRef } from '@gentics/cms-components';
import { ApplicationStateService, ToolBreadcrumbAction } from '../../../state';
import { ExposedUIAPI } from '../exposed-ui-api/exposed-ui-api.service';
import { ToolMessagingChannel } from './tool-messaging-channel.class';

@Injectable()
export class ToolMessagingChannelFactory {

    constructor(
        private state: ApplicationStateService,
        private exposedApi: ExposedUIAPI,
        private windowRef: WindowRef,
    ) { }

    create(toolKey: string, port: MessagePort): ToolMessagingChannel {
        const initialPath = this.state.now.tools.subpath[toolKey] || '';
        const remoteApi = {};

        // eslint-disable-next-line prefer-const
        let channel: ToolMessagingChannel;
        const exposedApi = ExposedUIAPI.clone(this.exposedApi);
        exposedApi.close = () => Promise.resolve(channel.toolWantsToClose());
        exposedApi.navigated = (path: string, replace?: boolean) => Promise.resolve(channel.toolWantsToNavigate(path, replace));
        exposedApi.provideBreadcrumbs = breadcrumbs => {
            this.state.dispatch(new ToolBreadcrumbAction(toolKey, breadcrumbs));
        };

        channel = new ToolMessagingChannel(initialPath, port, exposedApi, remoteApi);

        const toolOpensInNewTab = this.state.now.tools.available.find(tool => tool.key === toolKey).newtab;
        if (toolOpensInNewTab) {
            // Most browsers disallow this, but we try to focus the UI window
            // any time a tool in a different tab calls any exposed method.
            const uiWindow: Window = this.windowRef.nativeWindow;
            channel.onMessageCallHook = () => {
                uiWindow.focus();
            };
        }

        return channel;
    }

}
