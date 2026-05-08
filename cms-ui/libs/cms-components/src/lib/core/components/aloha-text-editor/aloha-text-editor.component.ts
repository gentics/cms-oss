import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { AlohaSettings } from '@gentics/aloha-models';
import { BaseFormElementComponent } from '@gentics/ui-core';

enum IFrameState {
    NONE = 'none',
    INITIALIZING = 'initializing',
    INITIALIZED = 'initialized',
}

const DEFAULT_SETTINGS: Partial<AlohaSettings & Record<string, any>> = {
    readonly: false,
    sidebar: {
        disabled: false,
    },
    contentHandler: {
        initEditable: ['blockelement'],
        getContents: ['blockelement', 'basic'],
        insertHtml: ['word', 'generic', 'block', 'formatless'],
    },
};

const EVENT_ALOHA_READY = 'aloha-ready';

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
    public plugins: string[] = ['common/ui', 'common/block', 'common/contenthandler', 'common/format'];

    @Input()
    public settings: Partial<AlohaSettings & Record<string, any>>;

    @ViewChild('host')
    public iframe: ElementRef<HTMLIFrameElement>;

    public isFocused = false;
    public state = IFrameState.NONE;

    private focusAfterInit = false;

    public ngOnInit(): void {
        // TODO: Load the aloha sources
    }

    public ngAfterViewInit(): void {
        this.initializeIframe();
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();
    }

    public focusAloha(): void {
        if (this.state !== IFrameState.INITIALIZED) {
            this.focusAfterInit = true;
            return;
        }

        const editable = this.getEditable();
        if (editable) {
            editable.focus();
        }
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
    }

    private initializeIframe(): void {
        if (!this.iframe.nativeElement || this.state !== IFrameState.NONE) {
            return;
        }

        this.state = IFrameState.INITIALIZING;

        const handler = (event: MessageEvent) => {
            if (event.data === EVENT_ALOHA_READY) {
                this.iframe.nativeElement.contentWindow.removeEventListener('message', handler);

                const editable = this.getEditable();
                if (editable) {
                    editable.addEventListener('focusin', this.focusInHandler);
                    editable.addEventListener('focusout', this.focusOutHandler);
                    editable.addEventListener('input', this.inputHandler);
                }

                this.state = IFrameState.INITIALIZED;
                if (this.focusAfterInit) {
                    this.focusAloha();
                    this.focusAfterInit = false;
                }
            }
        };

        this.iframe.nativeElement.contentWindow.addEventListener('message', handler);

        const settings = {
            ...DEFAULT_SETTINGS,
            ...this.settings,
        };

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

        <link rel="stylesheet" href="/alohaeditor/DEV/css/aloha.css" />
        <script src="/alohaeditor/DEV/lib/require.js"></script>
        <script src="/alohaeditor/DEV/lib/vendor/jquery-3.7.0.js"></script>
        <script src="/alohaeditor/DEV/lib/vendor/jquery.layout.js"></script>
        <script src="/alohaeditor/DEV/lib/aloha-jquery-noconflict.js"></script>
        <script src="/alohaeditor/DEV/lib/aloha.js" data-aloha-plugins="${this.plugins.join(',')}"></script>

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
        if (this.state !== IFrameState.INITIALIZED) {
            return null;
        }

        return this.iframe.nativeElement.contentDocument.querySelector('[contenteditable]');
    }

    private focusInHandler = () => {
        this.isFocused = true;
        this.changeDetector.markForCheck();
    };

    private focusOutHandler = () => {
        this.isFocused = false;
        this.changeDetector.markForCheck();
    };

    private inputHandler = (event: InputEvent) => {
        if (!this.isFocused) {
            return;
        }
        this.triggerChange((event.target as HTMLElement).innerHTML);
    };
}
