import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy, ChangeDetectorRef, HostBinding } from '@angular/core';

/**
 * For documentation, see the Tabs
 */
@Component({
    selector: 'gtx-tab',
    templateUrl: './tab.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TabComponent {

    @Input()
    public title: string;

    @Input()
    public icon: string;

    @Input()
    public id: string;

    @Input()
    public routerLink: any[];

    @Input()
    public disabled: boolean;

    @Input()
    @HostBinding('class.hidden')
    public hidden: boolean;

    /**
     * When the tab is clicked, this event is fired with the tab id.
     */
    @Output()
    public select = new EventEmitter<string>();

    @HostBinding('class.is-active')
    public active = false;

    constructor(
        public changeDetector: ChangeDetectorRef,
    ) {}
}
