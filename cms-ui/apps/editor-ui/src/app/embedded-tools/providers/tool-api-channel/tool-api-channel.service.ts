import { Injectable, OnDestroy } from '@angular/core';
import { WindowRef } from '@gentics/cms-components';
import { ExposableEmbeddedToolAPI } from '@gentics/cms-integration-api-models';
import { Subscription } from 'rxjs';
import { EmbeddedToolsService } from '../embedded-tools/embedded-tools.service';
import { ExposedUIAPI } from '../exposed-ui-api/exposed-ui-api.service';
import { ToolMessagingChannel } from '../tool-messaging-channel/tool-messaging-channel.class';
import { ToolMessagingChannelFactory } from '../tool-messaging-channel/tool-messaging-channel.factory';

const CONNECT_MESSAGE = 'gcms-tool-api';

@Injectable()
export class ToolApiChannelService implements OnDestroy {

    private subscriptions = new Subscription();
    private toolChannels = new Map<string, ToolMessagingChannel>();
    private toolsService: EmbeddedToolsService;

    constructor(
        private api: ExposedUIAPI,
        private channelFactory: ToolMessagingChannelFactory,
        private windowRef: WindowRef) { }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    registerToolsService(service: EmbeddedToolsService): void {
        this.toolsService = service;
    }

    connect(toolKey: string, toolWindow: Window): Subscription {
        const window: Window = this.windowRef.nativeWindow;
        const windowSubscription = new Subscription();
        let channel: ToolMessagingChannel;

        const messageHandler = (event: MessageEvent) => {
            if (event.source === toolWindow && event.data === CONNECT_MESSAGE && event.ports[0]) {
                const port: MessagePort = event.ports[0];
                if (channel) {
                    channel.destroy();
                }

                channel = this.channelFactory.create(toolKey, port);
                channel.toolWantsToClose = () => {
                    this.toolsService.close(toolKey, true);
                };
                channel.toolWantsToNavigate = (subpath: string, replace?: boolean) => {
                    this.toolsService.toolWantsToNavigate(toolKey, subpath, replace);
                };

                this.toolChannels.set(toolKey, channel);
            }
        };

        const unbind = () => {
            window.removeEventListener('message', messageHandler);
        };

        window.addEventListener('message', messageHandler);
        windowSubscription.add(unbind).add(() => channel && channel.destroy());
        this.subscriptions.add(windowSubscription);
        return windowSubscription;
    }

    getApi(toolKey: string): ExposableEmbeddedToolAPI | undefined {
        const channel = this.toolChannels.get(toolKey);
        return channel && channel.remoteApi;
    }

    disconnect(toolKey: string): void {
        const channel = this.toolChannels.get(toolKey);
        if (channel) {
            channel.destroy();
        }
    }

}

