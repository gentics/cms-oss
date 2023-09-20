import { animate, state, style, transition, trigger } from '@angular/animations';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, TemplateRef } from '@angular/core';
import { coerceToBoolean } from '../../utils';

@Component({
    selector: 'gtx-accordion',
    templateUrl: './accordion.component.html',
    styleUrls: ['./accordion.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('slide', [
            state('open', style({
                maxHeight: '*',
                marginTop: '1rem',
            })),
            state('closed', style({
                maxHeight: '0px',
                marginTop: '0px',
            })),
            transition('* <=> *', animate('200ms')),
        ]),
    ],
})
export class AccordionComponent implements OnChanges {

    /* INPUTS
     * --------------------------------------------------------------------- */

    @Input()
    public text: string;

    @Input()
    public open = false;

    @Input()
    public trigger: TemplateRef<any>;

    @Input()
    public triggerToggle = false;

    @Input()
    public disabled = false;

    /* OUTPUTS
     * --------------------------------------------------------------------- */

    @Output()
    public openChange = new EventEmitter<boolean>();

    @Output()
    public clickTrigger = new EventEmitter<any>();

    /* TEMPLATE VARIABLES
     * --------------------------------------------------------------------- */

    public showContent = false;

    /* CONSTRUCTOR
     * --------------------------------------------------------------------- */

    constructor(protected changeDetector: ChangeDetectorRef) { }

    /* LIFE-CYCLE HOOKS
     * --------------------------------------------------------------------- */

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.open) {
            this.open = coerceToBoolean(this.open);
            if (this.open) {
                this.showContent = true;
            }
        }
    }

    /* TEMPLATE INTERACTION FUNCTIONS
     * --------------------------------------------------------------------- */

    slideAnimationDone(): void {
        this.showContent = this.open;
    }

    toggleOpen(event?: MouseEvent, fromTrigger: boolean = false): void {
        this.clickTrigger.emit();

        if (this.disabled || (fromTrigger && !this.triggerToggle)) {
            return;
        }

        if (event != null) {
            event.stopPropagation();
            event.preventDefault();
        }

        this.open = !this.open;
        if (this.open) {
            this.showContent = true;
        }

        this.openChange.emit(this.open);
        this.changeDetector.markForCheck();
    }

}
