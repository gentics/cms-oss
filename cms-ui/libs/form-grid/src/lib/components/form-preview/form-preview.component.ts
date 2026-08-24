import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    model,
    signal,
    ViewChild,
} from '@angular/core';
import { FormFlow, FormPropertyData, FormSchema, FormTypeConfiguration, FormUISchema } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';

interface IFeatures {
    [key: string]: IFeaturesConfig & IFeatureUploadConstraintOptions;
}

interface IFeaturesConfig {
    [key: string]: string;
}

interface IFeatureUploadConstraintOptions {
    maxSizePerFile: number;
    maxSizeOverall: number;
    fileExtensionsAllowed: string[];
}

type FormgridPreviewData = {
    formId: string;
    formType: string;
    pluginName: string;
    flowId: string;
    flows: FormFlow[];
    schema: FormSchema;
    uiSchema: FormUISchema;
    language: string;
    features?: IFeatures;
    prefillContent?: Record<string, FormPropertyData>;
    currentPage?: number;
    selectedElementId?: string;
};

/**
 * Info about the preview-form which doesn't change
 */
interface PreviewInformation {
    formType: string;
    pluginName: string;
    flowId: string;
    flows: FormFlow[];
    features: IFeatures;
    availableLanguages: string[];
    cmsSid?: number;
}

/**
 * Form-data which may change when editing a form
 */
interface PreviewFormData {
    schema: FormSchema;
    uiSchema: FormUISchema;
}

interface ExtendedWindow extends Window {
    // Form Data that is assigned in the page directly.
    formPreviewData: FormgridPreviewData;
}

const PREVIEW_CONNECT_EVENT = 'preview-connect';
const PREVIEW_INTIALIZATION_EVENT_NAME = 'preview-init';
const PREVIEW_FORM_CHANGE_EVENT_NAME = 'preview-form-change';
const PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME = 'preview-selected-element-change';
const PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME = 'preview-current-page-change';
const PREVIEW_DISPLAY_LANGUAGE_CHANGE_EVENT_NAME = 'preview-language-change';
const PREVIEW_LOADED_EVENT_NAME = 'preview-loaded';

enum PreviewEventReceiver {
    CONTROLLER = 'controller',
    PUPPET = 'puppet',
}

interface BasePreviewEvent {
    receiver: PreviewEventReceiver;
}

type PreviewEvent = PreviewConnectEvent
  | PreviewInitializationEvent
  | PreviewLoadedEvent
  | PreviewFormChangeEvent
  | PreviewSelectedElementChangeEvent
  | PreviewCurrentPageChangeEvent
  | PreviewDisplayLanguageChangeEvent
  ;

interface PreviewConnectEvent extends BasePreviewEvent {
    eventType: typeof PREVIEW_CONNECT_EVENT;
}

interface PreviewInitializationEvent extends BasePreviewEvent,
    Omit<PreviewInformation, 'features'>, Partial<Pick<PreviewInformation, 'features'>>,
    PreviewFormData
{
    eventType: typeof PREVIEW_INTIALIZATION_EVENT_NAME;

    formId: string;
    language: string;
    pageIndex: number;
    elementId?: string;
}

interface PreviewLoadedEvent extends BasePreviewEvent {
    eventType: typeof PREVIEW_LOADED_EVENT_NAME;
    receiver: PreviewEventReceiver;
}

interface PreviewFormChangeEvent extends BasePreviewEvent, PreviewFormData {
    eventType: typeof PREVIEW_FORM_CHANGE_EVENT_NAME;

    prefillContent: Record<string, FormPropertyData>;
}

interface PreviewSelectedElementChangeEvent extends BasePreviewEvent {
    eventType: typeof PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME;

    elementId: string;
}

interface PreviewCurrentPageChangeEvent extends BasePreviewEvent {
    eventType: typeof PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME;
    receiver: PreviewEventReceiver;

    pageIndex: number;
}

interface PreviewDisplayLanguageChangeEvent extends BasePreviewEvent {
    eventType: typeof PREVIEW_DISPLAY_LANGUAGE_CHANGE_EVENT_NAME;

    language: string;
}

@Component({
    selector: 'gtx-form-preview',
    templateUrl: './form-preview.component.html',
    styleUrls: ['./form-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormPreviewComponent implements AfterViewInit {

    /* INJECTS
     * ===================================================================== */

    private readonly client = inject(GCMSRestClientService);

    /* INPUTS / OUTPUTS
     * ===================================================================== */

    public readonly formId = input.required<number>();
    public readonly formType = input.required<string>();
    public readonly config = input.required<FormTypeConfiguration>();
    public readonly flowId = input.required<string>();

    public readonly schema = input.required<FormSchema>();
    public readonly uiSchema = input.required<FormUISchema>();
    public readonly languages = input.required<string[]>();

    public readonly pageIndex = model.required<number>();
    public readonly activeLanguage = model.required<string>();
    public readonly selectedElementId = model<string>();

    /* LOCAL STATE
     * ===================================================================== */

    public readonly loading = signal(true);
    public readonly hasError = signal(false);
    private initialized = false;

    @ViewChild('iframe')
    public iframe: ElementRef<HTMLIFrameElement>;

    private readonly prefill = computed<Record<string, FormPropertyData>>(() => {
        const propData: Record<string, FormPropertyData> = {};
        const raw = this.schema().properties || {};

        for (const [id, def] of Object.entries(raw)) {
            propData[id] = {
                editable: false,
                visible: true,
                name: def.name,
                value: def.formGridOptions?.defaultValue,
            };
        }

        return propData;
    });

    /* CONSTRUCTOR
     * ===================================================================== */

    constructor() {
        effect(() => {
            this.postEvent({
                eventType: PREVIEW_FORM_CHANGE_EVENT_NAME,
                receiver: PreviewEventReceiver.PUPPET,

                schema: this.schema(),
                uiSchema: this.uiSchema(),
                prefillContent: this.prefill(),
            });
        });

        effect(() => {
            this.postEvent({
                eventType: PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME,
                receiver: PreviewEventReceiver.PUPPET,

                pageIndex: this.pageIndex(),
            });
        });

        effect(() => {
            this.postEvent({
                eventType: PREVIEW_DISPLAY_LANGUAGE_CHANGE_EVENT_NAME,
                receiver: PreviewEventReceiver.PUPPET,

                language: this.activeLanguage(),
            });
        });

        effect(() => {
            this.postEvent({
                eventType: PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME,
                receiver: PreviewEventReceiver.PUPPET,

                elementId: this.selectedElementId(),
            });
        });
    }

    /* LIFECYCLE HOOKS
     * ===================================================================== */

    public ngAfterViewInit(): void {
        this.iframe.nativeElement.addEventListener('error', () => {
            console.error('Error loading preview iframe!');
            this.loading.set(false);
            this.hasError.set(true);

            const nat = this.iframe.nativeElement;
            const win = nat.contentWindow;
            win.addEventListener('unload', () => {
                this.loading.set(true);
                this.hasError.set(false);
                this.initialized = false;
            });
        });

        this.iframe.nativeElement.addEventListener('load', () => {
            const nat = this.iframe.nativeElement;
            const win = nat.contentWindow;

            // "Empty" load events from iframes which aren't what we look for.
            if (
                win.location.toString() === 'about:blank'
                || nat.contentDocument.readyState !== 'complete'
            ) {
                return;
            }

            win.addEventListener('unload', () => {
                this.loading.set(true);
                this.hasError.set(false);
                this.initialized = false;
            });

            // For some reason, if the response has a non 200/300 code, it is still treated
            // as a successful reaquest and doesn't trigger the error handler above.
            // Therefore, hacky check if the page contains the formgen app root - if it doesn't,
            // then it's some kind of error page and we have to display an error.
            if (win.document.querySelector('div#root') == null) {
                this.loading.set(false);
                this.hasError.set(true);
                return;
            }

            try {
                (win as ExtendedWindow).formPreviewData = {
                    schema: this.schema(),
                    uiSchema: this.uiSchema(),
                    language: this.activeLanguage(),
                    currentPage: this.pageIndex(),
                    formId: `${this.formId()}`,
                    formType: this.formType(),
                    pluginName: this.config().pluginName,
                    flowId: this.flowId(),
                    flows: this.config().flows,
                    selectedElementId: this.selectedElementId(),
                    prefillContent: this.prefill(),
                };
            } catch (err) {
                // Ignore err
            }

            win.addEventListener('message', (raw) => {
                const event = raw.data as PreviewEvent;
                if (
                    event == null
                    || typeof event !== 'object'
                    || typeof event.eventType !== 'string'
                    || event.receiver !== PreviewEventReceiver.CONTROLLER
                ) {
                    return;
                }

                // eslint-disable-next-line no-console
                console.debug('<< FormGrid', event.eventType);

                switch (event.eventType) {
                    case PREVIEW_CONNECT_EVENT:
                        this.initialized = true;
                        this.postEvent({
                            eventType: PREVIEW_INTIALIZATION_EVENT_NAME,
                            receiver: PreviewEventReceiver.PUPPET,

                            language: this.activeLanguage(),
                            formId: `${this.formId()}`,
                            formType: this.formType(),
                            pluginName: this.config().pluginName,
                            flowId: this.flowId(),
                            flows: this.config().flows,

                            pageIndex: this.pageIndex(),
                            elementId: this.selectedElementId(),
                            availableLanguages: this.languages(),
                            schema: this.schema(),
                            uiSchema: this.uiSchema(),
                        });
                        return;

                    case PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME:
                        this.selectedElementId.set(event.elementId);
                        return;

                    case PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME:
                        this.pageIndex.set(event.pageIndex);
                        return;

                    case PREVIEW_LOADED_EVENT_NAME:
                        this.loading.set(false);
                        return;
                }
            });
        });
    }

    private postEvent(event: PreviewEvent): void {
        const win = this.iframe?.nativeElement?.contentWindow;
        if (!win || !this.initialized) {
            return;
        }

        // eslint-disable-next-line no-console
        console.debug('>> FormGrid', event.eventType);

        win.postMessage(event, '*');
    }
}
