import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    input,
    model,
    signal,
    ViewChild,
} from '@angular/core';
import { FormPropertyData, FormSchema, FormUISchema } from '@gentics/cms-models';

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
    formId?: string;
    schema?: FormSchema;
    uiSchema?: FormUISchema;
    features?: IFeatures;
    language?: string;
    prefillContent?: Record<string, FormPropertyData>;
    currentPage?: number;
    selectedElementId?: string;
};

interface ExtendedWindow extends Window {
    // Form Data that is assigned in the page directly.
    formPreviewData: FormgridPreviewData;
}

const PREVIEW_DATA_EVENT_NAME = 'preview-data-change';
const PREVIEW_ELEMENT_SELECT_EVENT_NAME = 'preview-select-element';
const PREVIEW_CONNECTION_EVENT_NAME = 'preview-connect';
const PREVIEW_PAGE_CHANGE_EVENT_NAME = 'preview-change-page';

interface InterchangeEvent {
    eventType: string;
}

interface PreviewElementSelectEvent extends InterchangeEvent {
    eventType: typeof PREVIEW_ELEMENT_SELECT_EVENT_NAME;
    elementId: string;
}

interface PreviewDataChangeEvent extends InterchangeEvent {
    eventType: typeof PREVIEW_DATA_EVENT_NAME;
    data: Partial<FormgridPreviewData>;
}

interface PreviewPageChangeEvent extends InterchangeEvent {
    eventType: typeof PREVIEW_PAGE_CHANGE_EVENT_NAME;
    pageIndex: number;
}

@Component({
    selector: 'gtx-form-preview',
    templateUrl: './form-preview.component.html',
    styleUrls: ['./form-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormPreviewComponent implements AfterViewInit {

    /* INPUTS / OUTPUTS
     * ===================================================================== */

    public readonly formId = input.required<number>();
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
    public readonly initialized = signal(false);

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
            const data = this.createPreviewData();
            if (!this.initialized()) {
                return;
            }

            this.postPreviewDataEvent(data);
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
                this.initialized.set(false);
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
                this.initialized.set(false);
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

            this.loading.set(false);
            this.hasError.set(false);

            try {
                (win as ExtendedWindow).formPreviewData = this.createPreviewData();
            } catch (err) {
                // Ignore err
            }

            win.addEventListener('message', (event) => {
                const data = event.data as InterchangeEvent;
                if (data == null || typeof data !== 'object' || typeof data.eventType !== 'string') {
                    return;
                }

                switch (data.eventType) {
                    case PREVIEW_CONNECTION_EVENT_NAME:
                        this.initialized.set(true);
                        return;
                    case PREVIEW_ELEMENT_SELECT_EVENT_NAME:
                        this.selectedElementId.set((data as PreviewElementSelectEvent).elementId);
                        return;
                    case PREVIEW_PAGE_CHANGE_EVENT_NAME:
                        this.pageIndex.set((data as PreviewPageChangeEvent).pageIndex);
                        return;
                }
            });
        });
    }

    private createPreviewData(): FormgridPreviewData {
        return {
            schema: this.schema(),
            uiSchema: this.uiSchema(),
            language: this.activeLanguage(),
            currentPage: this.pageIndex(),
            formId: `${this.formId()}`,
            selectedElementId: this.selectedElementId(),
            prefillContent: this.prefill(),
        };
    }

    private postPreviewDataEvent(data: FormgridPreviewData): void {
        const win = this.iframe?.nativeElement?.contentWindow;
        if (!win) {
            return;
        }
        const event: PreviewDataChangeEvent = {
            eventType: PREVIEW_DATA_EVENT_NAME,
            data: data,
        };
        win.postMessage(event, '/');
    }
}
