import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostBinding,
    Input,
    OnChanges,
    SimpleChanges,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';
import { StyleObj } from '../../internal';

@Component({
    selector: 'gtx-tooltip-content-wrapper',
    templateUrl: './tooltip-content-wrapper.component.html',
    styleUrls: ['./tooltip-content-wrapper.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TooltipContentWrapperComponent implements OnChanges, AfterViewInit {

    @Input()
    public content: TemplateRef<any>;

    @Input()
    public styling: StyleObj;

    @ViewChild('wrapper', { static: true })
    public wrapper: ElementRef<HTMLDivElement>;

    @HostBinding('style')
    public appliedStyles: SafeStyle;

    constructor(
        public element: ElementRef,
        private changeDetector: ChangeDetectorRef,
        private sanitizer: DomSanitizer,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.content || changes.styling) {
            this.updateAppliedStyles();
        }
    }

    ngAfterViewInit(): void {
        this.updateAppliedStyles();
    }

    updateAppliedStyles(): void {
        if (!this.wrapper?.nativeElement) {
            return;
        }

        const rect = this.wrapper.nativeElement.getBoundingClientRect();
        const builtStyles: StyleObj = {
            ...this.styling,
            /* eslint-disable @typescript-eslint/naming-convention */
            '--content-width': `${rect.width}px`,
            '--content-height': `${rect.height}px`,
            /* eslint-enable @typescript-eslint/naming-convention */
        };

        const styles = Object.entries(builtStyles)
            .filter(([prop, value]) => prop && value)
            .map(([prop, value]) => `${prop}: ${value};`)
            .join(' ');
        this.appliedStyles = this.sanitizer.bypassSecurityTrustStyle(styles);
        this.changeDetector.markForCheck();
    }
}
