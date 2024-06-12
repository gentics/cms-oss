import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    QueryList,
    SimpleChange,
    ViewChildren,
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subscription, fromEvent, of } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';

/**
 * Wraps an IFrame, provides easy access to its `load` event,
 * and allows setting its dimensions. The IFrame has the `data-gcms-ui-styles`
 * attribute set to the URL from where it can load the GCMS UI stylesheet.
 */
@Component({
    selector: 'iframe-wrapper',
    templateUrl: './iframe-wrapper.component.html',
    styleUrls: ['./iframe-wrapper.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IFrameWrapperComponent implements AfterViewInit, OnChanges, OnDestroy {

    /** The URL from which the IFrame should be loaded. */
    @Input()
    srcUrl: string;

    /** The width of the IFrame as a CSS width string. */
    @Input()
    width: string;

    /** The height of the IFrame as a CSS height string. */
    @Input()
    height: string;

    /** Disabled state for iframe to avoid eg.: guards to triggered even if something changed. */
    @Input()
    disableValidation = false;

    /** Emits the IFrame when its `load` event has fired. */
    @Output()
    iFrameLoad = new EventEmitter<HTMLIFrameElement>();

    /** Emits the state of the IFrame. */
    @Output()
    iFrameLoadedStateChange = new EventEmitter<boolean>();

    /** Used to actually bind the src attribute of the iframe element. */
    safeSrcUrl: SafeResourceUrl;

    /** The URL to the Blob that contains the styles for the IFrame. */
    iFrameStylesUrl: string;

    /** Indicates if the `load` event of the IFrame has already fired. */
    iFrameLoaded = false;

    /**
     * Used to access the IFrame.
     * There can actually only be one IFrame, but we use a QueryList because
     * the IFrame is added when an ngIf condition becomes true.
     */
    @ViewChildren('iFrame')
    iFrameList: QueryList<ElementRef>;

    private subscriptions = new Subscription();

    constructor(
        private changeDetector: ChangeDetectorRef,
        private domSanitizer: DomSanitizer,
    ) {}

    ngAfterViewInit(): void {
        // This observable emits the IFrame when it has completed loading.
        const iFrame$ = this.iFrameList.changes.pipe(
            filter((newIFrames: QueryList<ElementRef>) => newIFrames.length === 1),
            map((newIFrames: QueryList<ElementRef>) => newIFrames.first.nativeElement as HTMLIFrameElement),
            tap(iFrame => this.applyIFrameSize(iFrame)),
            switchMap(iFrame => {
                if (iFrame.contentDocument.readyState === 'complete' && this.checkIfCorrectSrc(iFrame)) {
                    this.iFrameLoadedStateChange.emit(true);
                    return of(iFrame);
                }
                return fromEvent(iFrame, 'load').pipe(
                    map(() => iFrame),
                );
            }),
        );

        const iFrameUnload$ = iFrame$.pipe(
            switchMap(iFrame => fromEvent(iFrame.contentWindow, 'unload')),
        );

        this.subscriptions.add(
            iFrame$.subscribe(iFrame => {
                if (this.checkIfCorrectSrc(iFrame)) {
                    this.iFrameLoaded = true;
                    this.changeDetector.markForCheck();
                    this.iFrameLoad.emit(iFrame);
                }
                this.iFrameLoadedStateChange.emit(true);
            }),
        );
        this.subscriptions.add(
            iFrameUnload$.subscribe(() => {
                this.iFrameLoadedStateChange.emit(false);
            }),
        );
        this.iFrameList.notifyOnChanges();
    }

    ngOnChanges(changes: { [K in keyof IFrameWrapperComponent]: SimpleChange }): void {
        if (changes.srcUrl) {
            if (changes.srcUrl.currentValue) {
                this.safeSrcUrl = this.domSanitizer.bypassSecurityTrustResourceUrl(changes.srcUrl.currentValue);
            } else {
                this.safeSrcUrl = null;
            }
            this.iFrameLoaded = false;
            this.iFrameLoadedStateChange.emit(false);
        }
        if ((changes.width || changes.height) && this.iFrameList && this.iFrameList.length === 1) {
            this.applyIFrameSize(this.iFrameList.first.nativeElement);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    private applyIFrameSize(iFrame: HTMLIFrameElement): void {
        if (this.width) {
            iFrame.style.width = this.width;
        } else {
            iFrame.style.width = '';
        }
        if (this.height) {
            iFrame.style.height = this.height;
        } else {
            iFrame.style.height = '';
        }
    }

    /**
     * Chrome seems to initialize IFrames with the about:blank page and loads the src URL afterwards.
     * On fast systems we would detect `iFrame.contentDocument.readyState === 'complete'`, but for the
     * about:blank page. Thus we also need to check if the location of the contentWindow matches
     * our srcUrl.
     */
    private checkIfCorrectSrc(iFrame: HTMLIFrameElement): boolean {
        return this.srcUrl && iFrame && iFrame.contentWindow && iFrame.contentWindow.location &&
            iFrame.contentWindow.location.href && iFrame.contentWindow.location.href.endsWith(this.srcUrl);
    }

}
