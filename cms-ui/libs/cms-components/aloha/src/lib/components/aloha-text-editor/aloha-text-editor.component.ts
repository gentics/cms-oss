import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    Inject,
    Input,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { AlohaComponent, AlohaSettings } from '@gentics/aloha-models';
import { GCMS_UI_SERVICES_PROVIDER, I18nService } from '@gentics/cms-components';
import { CNWindow, GcmsUiBridge, GcmsUiServices, ModalCloseError } from '@gentics/cms-integration-api-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { cancelEvent } from '@gentics/common';
import { BaseFormElementComponent, UserAgentProvider } from '@gentics/ui-core';
import { Store } from '@ngxs/store';
import { isEqual, mergeWith } from 'lodash-es';
import { distinctUntilChanged, filter } from 'rxjs';
import { AlohaStateModel } from '../../models';
import { AlohaComponentRenderedEvent, RenderedAlohaComponent } from '../../models/internal';
import { AlohaIntegrationService, NormalizedToolbarSizeSettings } from '../../providers/aloha-integration/aloha-integration.service';
import { AlohaOverlayService } from '../../providers/aloha-overlay/aloha-overlay.service';

enum IFrameState {
    NONE = 'none',
    SCHEDULED = 'scheduled',
    INITIALIZING = 'initializing',
    INITIALIZED = 'initialized',
}

const DEFAULT_SETTINGS: Partial<AlohaSettings & Record<string, any>> = {
    contentHandler: {
        initEditable: ['blockelement'],
        getContents: ['blockelement', 'basic'],
        insertHtml: ['word', 'generic', 'block', 'formatless'],
    },
    plugins: {
        block: {
            // Disabled on default, so the button doesn't show up
            toggleDragdropGlobal: false,
            dragdrop: false,
        },
    },
};

const EVENT_ALOHA_READY = 'aloha-ready';
/**
 * These plugins are required to work properly.
 * UI: So the ui can actually be properly used within this editor.
 * Contenthandler: So special copy/pasting handling and general cleanups happen.
 * Block: Only needed for translations.
 */
const REQUIRED_PLUGINS = ['common/ui', 'common/contenthandler', 'common/block'];
const DEFAULT_PLUGINS = ['common/format', 'common/link'];

@Component({
    selector: 'gtx-aloha-text-editor',
    templateUrl: './aloha-text-editor.component.html',
    styleUrls: ['./aloha-text-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class AlohaTextEditorComponent extends BaseFormElementComponent<string> implements OnInit, OnDestroy, AfterViewInit {

    public readonly IFrameState = IFrameState;

    @Input()
    public plugins: string[] = DEFAULT_PLUGINS;

    @Input()
    public settings: Partial<AlohaSettings & Record<string, any>>;

    @ViewChild('host')
    public iframe: ElementRef<HTMLIFrameElement>;

    @ViewChild('container')
    public container: ElementRef<HTMLDivElement>;

    public isFocused = false;
    public state = IFrameState.NONE;
    public contentHeight: number;
    public toolbarSettings: NormalizedToolbarSizeSettings;

    public components: Record<string, AlohaComponent> = {};
    public renderedComponents: Record<string, RenderedAlohaComponent<any, any>> = {};

    private focusAfterInit = false;
    private markWithin = false;
    public openOverlayCount = 0;
    private jsFiles: string[];
    private cssFiles: string[];
    private viewInitialized = false;
    private sizeObserver: ResizeObserver;
    private changeObserver: MutationObserver;
    private intersectionObserver: IntersectionObserver;

    constructor(
        changeDetector: ChangeDetectorRef,
        private store: Store,
        private i18n: I18nService,
        private client: GCMSRestClientService,
        private aloha: AlohaIntegrationService,
        private overlay: AlohaOverlayService,
        private userAgent: UserAgentProvider,
        @Inject(GCMS_UI_SERVICES_PROVIDER)
        private uiService: GcmsUiServices,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        this.subscriptions.push(this.store.select<{ aloha: AlohaStateModel }>((state) => state.aloha).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe((state: AlohaStateModel) => {
            this.jsFiles = state.jsFiles;
            this.cssFiles = state.cssFiles;
            this.scheduleIframeInit();
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.activeToolbarSettings$.pipe(
            filter(() => this.isFocused),
        ).subscribe((settings) => {
            this.toolbarSettings = settings;
            this.changeDetector.markForCheck();
        }));
    }

    public ngAfterViewInit(): void {
        this.viewInitialized = true;
        this.scheduleIframeInit();
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();
        if (this.sizeObserver) {
            this.sizeObserver.disconnect();
        }
        if (this.changeObserver) {
            this.changeObserver.disconnect();
        }
        if (this.intersectionObserver) {
            this.intersectionObserver.disconnect();
        }
        if (this.isFocused) {
            this.aloha.clearReferences();
        }
    }

    public focusAloha(event?: Event): void {
        if (this.disabled) {
            return;
        }

        if (this.state !== IFrameState.INITIALIZED) {
            this.focusAfterInit = true;
            return;
        }
        // Don't need to focus again
        if (this.isFocused) {
            cancelEvent(event);
            return;
        }

        const editable = this.getEditable();
        if (editable) {
            editable.focus();
        }
    }

    public clickedWithin(): void {
        this.markWithin = true;
    }

    public registerRenderedComponent(event: AlohaComponentRenderedEvent<any, any>): void {
        this.renderedComponents[event.name] = event.component;
    }

    public destroyRenderedComponent(name: string): void {
        delete this.renderedComponents[name];
    }

    protected onValueChange(): void {
        // Only allow simple strings
        this.value = this.value || '';

        if (this.isFocused) {
            return;
        }
        const editable = this.getEditable();
        if (editable) {
            editable.innerHTML = this.value;
        }
        this.resizeHandler();
    }

    protected override onDisabledChange(): void {
        super.onDisabledChange();

        this.markWithin = false;
        if (this.isFocused) {
            const editable = this.getEditable();
            if (editable) {
                editable.blur();
            }
        }
        this.isFocused = false;
    }

    private scheduleIframeInit(): void {
        if (this.state !== IFrameState.NONE || !this.viewInitialized) {
            return;
        }

        this.state = IFrameState.SCHEDULED;

        /*
         * We check here if the user is on firefox/gecko, as there's a "bug" in it,
         * where the iframes `window.Selection` is `undefined` as long as the iframe hasn't been rendered once.
         * Rendering of it is in gecko defered until it's element is in the DOM-Structure visible, which it is
         * on certain cases not when one of it's parents (or itself) has `display: none` set.
         */
        if (!this.userAgent.isGecko) {
            this.initializeIframe();
            return;
        }

        this.intersectionObserver = new IntersectionObserver(this.intersectHandler, {
            threshold: 0.1,
        });
        this.intersectionObserver.observe(this.container.nativeElement);
    }

    private intersectHandler = (entries: IntersectionObserverEntry[]) => {
        if (!entries.some((en) => en.isIntersecting)) {
            return;
        }
        // We only need this check while the iframe hasn't been initialized yet.
        // Since everything is setup now, we can disconnect from the observer to redcuce usage.
        this.intersectionObserver.disconnect();
        this.initializeIframe();
    };

    private initializeIframe(): void {
        if (
            !this.iframe?.nativeElement
            || (this.state !== IFrameState.NONE && this.state !== IFrameState.SCHEDULED)
            || this.jsFiles == null
            || this.cssFiles == null
        ) {
            return;
        }

        this.state = IFrameState.INITIALIZING;

        const handler = (event: MessageEvent) => {
            if (event.data === EVENT_ALOHA_READY) {
                this.iframe.nativeElement.contentWindow.removeEventListener('message', handler);

                const editable = this.getEditable();
                if (editable) {
                    editable.addEventListener('focusin', this.focusInHandler);
                    editable.addEventListener('blur', this.focusOutHandler);

                    this.changeObserver = new MutationObserver(this.inputHandler);
                    this.changeObserver.observe(editable, {
                        childList: true,
                        subtree: true,
                        characterData: true,
                    });

                    this.sizeObserver = new ResizeObserver(this.resizeHandler);
                    this.sizeObserver.observe(editable.ownerDocument.body);
                }

                this.state = IFrameState.INITIALIZED;
                if (this.focusAfterInit) {
                    this.focusAloha();
                    this.focusAfterInit = false;
                }
            }
        };

        // First wait for the iframe to load, as it replaces the `contentWindow`
        this.iframe.nativeElement.addEventListener('load', () => {
            const cw: CNWindow = this.iframe.nativeElement.contentWindow as any;
            const wrapOverlayHandle: <T>(cb: () => Promise<any>) => Promise<T> = (cb) => {
                this.openOverlayCount++;
                return cb().finally(() => {
                    this.openOverlayCount--;
                    if (this.openOverlayCount === 0) {
                        this.focusAloha();
                    }
                });
            };
            cw.GCMSUI = {
                runPreLoadScript: () => {},
                runPostLoadScript: () => {},

                gcmsUiStylesUrl: '',
                appState: {},
                paths: {
                    alohapageUrl: '',
                    apiBaseUrl: '',
                    imagestoreUrl: '',
                },
                onStateChange: (_state) => {},
                setContentModified: (_modified) => {},
                callDebugTool: () => {},
                getConstructs: () => Promise.resolve({}),
                restClient: this.client.getClient(),

                openImageEditor: (options) => {
                    return wrapOverlayHandle(() => this.uiService.openImageEditor(options));
                },
                openRepositoryBrowser: (options) => {
                    return wrapOverlayHandle(() => this.uiService.openRepositoryBrowser(options));
                },
                openTagEditor: (tag, tagType, page, options) => {
                    return wrapOverlayHandle(() => this.uiService.openTagEditor(tag, tagType, page, options));
                },
                openUploadModal: (config) => {
                    return wrapOverlayHandle(() => this.uiService.openUploadModal(config));
                },

                openDynamicModal: (config) => {
                    this.openOverlayCount++;
                    return this.overlay.openDynamicModal(config).then((ctl) => {
                        ctl.onClose.then(() => {
                            this.openOverlayCount--;
                            if (this.openOverlayCount === 0) {
                                this.focusAloha();
                            }
                        });
                        return ctl;
                    });
                },
                openDynamicDropdown: (config, slot) => {
                    this.openOverlayCount++;
                    return this.overlay.openDynamicDropdown(config, slot).then((ctl) => {
                        ctl.onClose.then(() => {
                            this.openOverlayCount--;
                            if (this.openOverlayCount === 0) {
                                this.focusAloha();
                            }
                        });
                        return ctl;
                    });
                },
                openDialog: (config) => {
                    this.openOverlayCount++;
                    return this.overlay.openDialog(config).then((ctl) => {
                        ctl.onClose.then(() => {
                            this.openOverlayCount--;
                            if (this.openOverlayCount === 0) {
                                this.focusAloha();
                            }
                        });
                        return ctl;
                    });
                },
                focusEditorTab: (tabId) => {
                    this.aloha.changeActivePageEditorTab(tabId);
                },
                closeErrorClass: ModalCloseError,

                registerComponent: (slot, component) => {
                    this.components[slot] = component;
                    this.aloha.registerComponent(slot, component);
                },
                unregisterComponent: (slot) => {
                    delete this.components[slot];
                    this.aloha.unregisterComponent(slot);
                },
            } as Partial<GcmsUiBridge> as any;
            // Now we can attach the message listener
            cw.addEventListener('message', handler);

            cw.Aloha.trigger('gcmsui.ready', {});
            cw.Aloha.require(['gcn/texteditor-plugin'], (plugin) => {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                plugin.init();
            });
        });

        const plugins = Array.from(new Set([...REQUIRED_PLUGINS, ...(this.plugins || DEFAULT_PLUGINS)]));
        const settings = mergeWith({}, DEFAULT_SETTINGS, this.settings, {
            readonly: false,
            sidebar: {
                disabled: false,
            },
            locale: this.i18n.getCurrentLanguage(),
            i18n: {
                current: this.i18n.getCurrentLanguage(),
            },
            // Config so the gcn ressources load properly
            requireConfig: {
                paths: {
                    gcn: '../plugins/gcn/gcn/lib',
                },
            },
        });

        const cssHtml = this.cssFiles.map((file) => `<link rel="stylesheet" href="${file}" />`).join('\n');
        const jsHtml = this.jsFiles
            .filter((file) => !file.endsWith('gcmsui-scripts-launcher.js'))
            .map((file) => {
                if (file.endsWith('aloha.js')) {
                    return `<script src="${file}" data-aloha-plugins="${plugins.join(',')}"></script>`;
                } else {
                    return `<script src="${file}"></script>`;
                }
            })
            .join('\n');

        this.iframe.nativeElement.srcdoc = `
<!DOCTYPE html>
<html>
    <head></head>
    <body>
        <div id="main">${this.value || ''}</div>
        <script type="text/javascript">
            Aloha = {};
            Aloha.settings = ${JSON.stringify(settings)};
        </script>

        ${cssHtml}
        ${jsHtml}

        <style>
            @font-face {
                font-family: 'Roboto';
                font-style: normal;
                font-weight: 100 900;
                font-stretch: 100%;
                src: url('Roboto-VariableFont.ttf') format('truetype');
            }

            @font-face {
                font-family: 'Roboto';
                font-style: italic;
                font-weight: 100 900;
                font-stretch: 100%;
                src: url('Roboto-Italic-VariableFont.ttf') format('truetype');
            }

            html {
                max-height: 100%;
            }

            body {
                display: block;
                height: 100%;
                width: 100%;
                margin: 0;
                overflow: hidden;
                font-family: 'Roboto', sans-serif;
                font-size: 14px;
                line-height: 1.5;
            }

            .aloha-editable {
                outline: none !important;

                > p:first-child {
                    margin-top: 0;
                }
            }

            .aloha-sidebar-bar,
            .aloha-surface.aloha-toolbar {
                display: none !important;
            }
        </style>

        <script type="text/javascript">
            (() => {
                function init() {
                    Aloha.ready(() => {
                        // Make it actually editable
                        Aloha.jQuery('#main').aloha();
                        window.postMessage("${EVENT_ALOHA_READY}");
                    });
                }

                // Run the Post-Load Script when the load event is fired.
                if (document.readyState === 'complete') {
                    init();
                } else {
                    window.addEventListener('load', init);
                }
            })();
        </script>
    </body>
</html>
`;
    }

    private getEditable(): HTMLElement | null {
        if (this.state === IFrameState.NONE) {
            return null;
        }

        return this.iframe.nativeElement.contentDocument.querySelector('[contenteditable]');
    }

    public focusInHandler = (): void => {
        if (this.isFocused) {
            return;
        }

        this.aloha.clearReferences();
        this.isFocused = true;

        if (this.iframe?.nativeElement) {
            const cw: CNWindow = this.iframe.nativeElement.contentWindow as any;
            this.aloha.setWindow(cw);
            this.aloha.reference$.next(cw.Aloha);
            this.aloha.settings$.next(cw.Aloha.settings);
            this.aloha.setComponents({ ...this.components });

            // Explicitly a reference
            this.aloha.renderedComponents = this.renderedComponents;

            cw.Aloha.ready(() => {
                this.aloha.ready$.next(true);
                this.aloha.windowLoaded$.next(true);
            });
        }

        this.changeDetector.markForCheck();
    };

    public focusOutHandler = (): void => {
        // Ignore focus out/blurs while the user interacts with a overlay element
        if (this.openOverlayCount > 0) {
            return;
        }

        if (this.markWithin) {
            this.markWithin = false;
            return;
        }

        this.isFocused = false;
        this.changeDetector.markForCheck();
    };

    private inputHandler = (): void => {
        if (!this.isFocused) {
            return;
        }
        const editable = this.getEditable();
        if (!editable) {
            return;
        }
        this.triggerChange(editable.innerHTML);
        this.resizeHandler();
    };

    private resizeHandler = (): void => {
        const editable = this.getEditable();
        if (!editable) {
            return;
        }
        this.contentHeight = editable.scrollHeight;
        this.changeDetector.markForCheck();
    };
}
