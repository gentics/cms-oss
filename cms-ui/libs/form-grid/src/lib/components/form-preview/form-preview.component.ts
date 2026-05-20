import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    effect,
    ElementRef,
    input,
    signal,
    ViewChild,
} from '@angular/core';
import { FormSchema, FormUISchema } from '@gentics/cms-models';

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
    prefillContent?: string;
    currentPage?: number;
    selectedElementId?: string;
};

interface ExtendedWindow extends Window {
    // Form Data that is assigned in the page directly.
    formPreviewData: FormgridPreviewData;
}

const PREVIEW_DATA_EVENT_NAME = 'preview-data-change';
const PREVIEW_CONNECTION_EVENT_NAME = 'preview-connect';

interface InterchangeEvent {
    eventType: string;
}

interface PreviewDataChangeEvent extends InterchangeEvent {
    eventType: typeof PREVIEW_DATA_EVENT_NAME;
    data: Partial<FormgridPreviewData>;
}

@Component({
    selector: 'gtx-form-preview',
    templateUrl: './form-preview.component.html',
    styleUrls: ['./form-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormPreviewComponent implements AfterViewInit {

    public readonly schema = input.required<FormSchema>();
    public readonly uiSchema = input.required<FormUISchema>();
    /** TODO: Move language select in here */
    public readonly language = input.required<string>();
    public readonly pageIndex = input.required<number>();

    @ViewChild('iframe')
    public iframe: ElementRef<HTMLIFrameElement>;

    private readonly initialized = signal(false);

    constructor() {
        effect(() => {
            if (!this.initialized) {
                return;
            }
            this.postPreviewDataEvent();
        });
    }

    public ngAfterViewInit(): void {
        this.iframe.nativeElement.addEventListener('load', () => {
            const nat = this.iframe.nativeElement;
            const win = nat.contentWindow;

            if (
                win.location.toString() === 'about:blank'
                || nat.contentDocument.readyState !== 'complete'
            ) {
                console.log('empty load');
                return;
            }

            console.log('iframe loaded');

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

                if (data.eventType === PREVIEW_CONNECTION_EVENT_NAME) {
                    console.log('connection established!');
                    this.initialized.set(true);
                    this.postPreviewDataEvent();
                    return;
                }
            });
        });
    }

    private createPreviewData(): FormgridPreviewData {
        return {
            schema: this.schema(),
            uiSchema: this.uiSchema(),
            language: this.language(),
            currentPage: this.pageIndex(),
            formId: 'foobar123',
            features: {},
        };
    }

    private postPreviewDataEvent(): void {
        const win = this.iframe?.nativeElement?.contentWindow;
        if (!win) {
            return;
        }
        const event: PreviewDataChangeEvent = {
            eventType: PREVIEW_DATA_EVENT_NAME,
            data: this.createPreviewData(),
        };
        console.log('sending preview data event');
        win.postMessage(event, '*');
    }
}
