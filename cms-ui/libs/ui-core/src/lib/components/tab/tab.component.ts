import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, HostBinding, Input, OnChanges, Output } from '@angular/core';
import { ChangesOf } from '../../common';

/**
 * For documentation, see the Tabs
 */
@Component({
    selector: 'gtx-tab',
    templateUrl: './tab.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TabComponent implements OnChanges {

    @Input()
    public title: string;

    @Input()
    public icon: string;

    @HostBinding('attr.data-id')
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

    /**
     * Reference to the `TabsComponent`, but without typings, as it would be cyclic otherwise.
     */
    public parentRef: any;

    constructor(
        public changeDetector: ChangeDetectorRef,
    ) {}

    public ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.title || changes.icon || changes.disabled || changes.hidden || changes.routerLink || changes.id) {
            if (this.parentRef != null) {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                this.parentRef.updateDisplayTabs();
            }
        }
    }
}
