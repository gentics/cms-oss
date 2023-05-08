import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';

@Component({
    selector: 'gtx-loading-wrapper',
    templateUrl: './loading-wrapper.component.html',
    styleUrls: ['./loading-wrapper.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingWrapperComponent {

    @Input()
    public loading = false;

    @Input()
    public message: string;

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) { }
}
