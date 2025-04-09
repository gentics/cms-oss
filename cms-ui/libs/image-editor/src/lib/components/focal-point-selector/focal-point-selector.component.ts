import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { ChangesOf } from '@gentics/ui-core';
import { Subject } from 'rxjs';
import { delay, takeUntil } from 'rxjs/operators';
import { FocalPointService } from '../../providers/focal-point/focal-point.service';

@Component({
    selector: 'gentics-focal-point-selector',
    templateUrl: 'focal-point-selector.component.html',
    styleUrls: ['focal-point-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FocalPointSelectorComponent implements OnInit, OnChanges, OnDestroy {

    /**
     * How many pixels of "deadzone" around the image are allowed/added, in order to
     * be able to select the edge of an image more accurately.
     */
    public readonly DEADZONE = 5;

    /**
     * The X-Coordinate of the focal point.
     * Floating point positon where `0` indicates the very left, and `1` the very right of the image.
     * Default is `0.5`, aka the center of the image.
     */
    @Input()
    public focalPointX = 0.5;

    /**
     * The Y-Coordinate of the focal point.
     * Floating point positon where `0` indicates the very top, and `1` the very bottom of the image.
     * Default is `0.5`, aka the center of the image.
     */
    @Input()
    public focalPointY = 0.5;

    @Input()
    public enabled = false;

    @Output()
    focalPointSelect = new EventEmitter<{ x: number; y: number; }>();

    @ViewChild('overlay')
    public overlayRef: ElementRef<HTMLDivElement>;

    public imageWidth: number;
    public imageHeight: number;

    public showCursor = false;
    /**
     * The cursor Y-Position in the overlay, where crosshair should be drawn.
     */
    public cursorX: number | null = null;
    /**
     * The cursor Y-Position in the overlay, where crosshair should be drawn.
     */
    public cursorY: number | null = null;

    private target: HTMLElement;
    private destroy$ = new Subject<void>();

    constructor(
        private focalPointService: FocalPointService,
        private changeDetector: ChangeDetectorRef,
    ) {}

    ngOnInit(): void {
        this.focalPointService.getTarget()
            .then(target => this.initTarget(target));
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if ('focalPointX' in changes || 'focalPointY' in changes) {
            this.clampFocalPoints();
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    overlayMouseMove(e: MouseEvent): void {
        this.showCursor = true;
        this.cursorX = e.offsetX - this.DEADZONE;
        this.cursorY = e.offsetY - this.DEADZONE;
    }

    overlayMouseLeave(): void {
        this.showCursor = false;
        this.cursorX = null;
        this.cursorY = null;
    }

    clampFocalPoints(): void {
        let needsChange = false;
        if (this.focalPointX < 0 || this.focalPointX > 1) {
            needsChange = true;
            this.focalPointX = Math.max(0, Math.min(this.focalPointX, 1));
        }
        if (this.focalPointY < 0 || this.focalPointY > 1) {
            needsChange = true;
            this.focalPointY = Math.max(0, Math.min(this.focalPointY, 1));
        }

        if (needsChange) {
            this.focalPointSelect.emit({ x: this.focalPointX, y: this.focalPointY });
        }
    }

    overlayClick(e: MouseEvent): void {
        if (!this.target) {
            return;
        }

        const xInPixels = e.offsetX - this.DEADZONE;
        const yInPixels = e.offsetY - this.DEADZONE;

        const xPercent = xInPixels / this.imageWidth;
        const yPercent = yInPixels / this.imageHeight;

        const xNormalized = Math.max(0, Math.min(xPercent, 1));
        const yNormalized = Math.max(0, Math.min(yPercent, 1));

        this.focalPointSelect.emit({ x: xNormalized, y: yNormalized });
    }

    private initTarget(target: HTMLElement): void {
        this.target = target;

        // Note: the following could also be achieved with a MutationObserver, but there appears to be some issue
        // (possibly with Zone.js) whereby it's use here causes an infinite loop.
        this.focalPointService.update$
            .pipe(
                // The delay is required to allow the changes to the target styles to be
                // reflected in the DOM
                delay(1),
                takeUntil(this.destroy$))
            .subscribe(() => {
                this.updatePositions();
                this.changeDetector.markForCheck();
            });
    }

    /**
     * This is necessary to fix the initially misplaced focal point when the image editor
     * is used in a Gentics UI Core modal, which uses CSS transitions for appearing.
     *
     * See https://github.com/gentics/gentics-ui-image-editor/issues/2
     */
    @HostListener('window:transitionend')
    onTransitionEnd(): void {
        this.updatePositions();
        this.changeDetector.markForCheck();
    }

    @HostListener('window:scroll')
    @HostListener('window:resize')
    updatePositions(): void {
        if (!this.target) {
            return;
        }

        const { width, height } = this.target.getBoundingClientRect();

        this.imageWidth = width;
        this.imageHeight = height;
    }
}
