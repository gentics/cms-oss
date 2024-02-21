import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnInit,
    ViewChild,
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AlohaIFrameComponent, AlohaIFrameEvent, AlohaIFrameEventNames } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { fromEvent } from 'rxjs';
import { AlohaIntegrationService } from '../../providers';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

const PARAM_FRAME_ID = 'aloha-iframe-id';
interface WindowSize {
    width?: number;
    height?: number;
}

@Component({
    selector: 'gtx-aloha-iframe-renderer',
    templateUrl: './aloha-iframe-renderer.component.html',
    styleUrls: ['./aloha-iframe-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaIFrameRendererComponent)],
})
export class AlohaIFrameRendererComponent<T>
    extends BaseAlohaRendererComponent<AlohaIFrameComponent, T>
    implements OnInit {

    @ViewChild('iframe')
    public iframeRef: ElementRef<HTMLIFrameElement>;

    public iframeUrl: SafeResourceUrl;
    public currentId: string;
    public size: WindowSize = {};

    protected currentOrigin: string;
    protected currentIframe: HTMLIFrameElement;

    constructor(
        changeDetector: ChangeDetectorRef,
        element: ElementRef<HTMLElement>,
        aloha: AlohaIntegrationService,
        protected sanitizer: DomSanitizer,
    ) {
        super(changeDetector, element, aloha);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(fromEvent(window, 'message').subscribe((event: MessageEvent) => {
            // Ignore invalid messages from a different origin
            if (event.origin !== this.currentOrigin) {
                return;
            }
            // Validate it's even a message meant for this component
            if (event.data == null || typeof event.data.eventName !== 'string') {
                return;
            }
            // In case that there're multiple iframes running, we have to be able to tell them apart
            // Therefore, the ID is checked on each event.
            if (event.data.id !== this.currentId) {
                return;
            }
            this.handleIncomingEvent(event.data);
        }));

        this.updateUrlToUse(this.settings.url);
    }

    public updateUrlToUse(newUrl: string): void {
        // We need a random ID which we use as cache busting.
        // Otherwise changes may never be properly loaded.
        this.currentId = Math.random().toString(36).substring(2, 7);
        // Also reset the size, as we want to start somewhat clean
        this.size = {};
        let urlToLoad: string;

        try {
            // Try to handle the URL nice and add the hash as param properly.
            const parsedUrl = new URL(newUrl);
            this.currentOrigin = parsedUrl.origin;
            parsedUrl.searchParams.append(PARAM_FRAME_ID, this.currentId);
            urlToLoad = parsedUrl.toString();
            this.iframeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(urlToLoad);
        } catch (ignored) {
            this.iframeUrl = null;
            console.warn('Supplied IFrame URL could not be parsed, and is therefore ignored!', newUrl);
        }
    }

    public handleIFrameLoad(event: Event): void {
        const iframe = this.iframeRef?.nativeElement;
        if (iframe) {
            this.initializeIframe(iframe);
        }
    }

    public handleIncomingEvent(event: AlohaIFrameEvent): void {
        switch (event.eventName) {
            case AlohaIFrameEventNames.CHANGE:
                this.triggerChange(event.value);
                break;

            case AlohaIFrameEventNames.TOUCH:
                this.triggerTouch();
                break;

            case AlohaIFrameEventNames.WINDOW_SIZE:
                if (typeof event.value.width === 'number' && !Number.isNaN(event.value.width)) {
                    this.size.width = event.value.width;
                }
                if (typeof event.value.height === 'number' && !Number.isNaN(event.value.height)) {
                    this.size.height = event.value.height;
                }
                this.changeDetector.markForCheck();
                break;
        }
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setUrl = (url) => {
            this.settings.url = url;
            this.updateUrlToUse(url);
            this.changeDetector.markForCheck();
        };
        this.settings.setOptions = (options) => {
            this.settings.options = options;
            this.sendMessageIfAvailable({
                eventName: AlohaIFrameEventNames.UPDATE_OPTIONS,
                id: this.currentId,
                value: options,
            });
        }
    }

    protected override onDisabledChange(): void {
        super.onDisabledChange();

        this.sendMessageIfAvailable({
            eventName: AlohaIFrameEventNames.DISABLED,
            id: this.currentId,
            value: this.disabled,
        });
    }

    protected override onValueChange(): void {
        super.onValueChange();

        if (this.settings) {
            this.settings.value = this.value;
        }
        this.sendMessageIfAvailable({
            eventName: AlohaIFrameEventNames.UPDATE_VALUE,
            id: this.currentId,
            value: this.value,
        });
    }

    protected sendMessageIfAvailable(message: AlohaIFrameEvent): void {
        const frameWindow = this.currentIframe?.contentWindow;
        if (frameWindow) {
            frameWindow.postMessage(message, this.currentOrigin);
        }
    }

    protected initializeIframe(iframe: HTMLIFrameElement): void {
        this.currentIframe = iframe;

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings?.onFrameLoad?.(iframe);

        const rect = iframe.getBoundingClientRect();
        this.sendMessageIfAvailable({
            eventName: AlohaIFrameEventNames.INIT,
            value: {
                id: this.currentId,
                value: this.value,
                disabled: this.disabled,
                options: this.settings.options,
                size: {
                    width: rect.width,
                    height: rect.height,
                },
                // Extra flag, so the implementations can check it
                gcmsui: true,
            } as any,
        });

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.settings?.onFrameInit?.(iframe);
    }
}
