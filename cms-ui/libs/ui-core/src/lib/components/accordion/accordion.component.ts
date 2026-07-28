import { booleanAttribute, ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, TemplateRef } from '@angular/core';
import { ChangesOf } from '../../common';
import { BaseComponent } from '../base-component/base.component';

/**
 * Very basic Accordion/Spoiler component to show/hide content with a customizable header.
 */
@Component({
    selector: 'gtx-accordion',
    templateUrl: './accordion.component.html',
    styleUrls: ['./accordion.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class AccordionComponent extends BaseComponent implements OnChanges {

    /* INPUTS
     * --------------------------------------------------------------------- */

    /**
     * The text to display, if `trigger` is not defined.
     */
    @Input()
    public text: string;

    /**
     * If the accordion is opened/content of the accordion is visible
     */
    @Input({ transform: booleanAttribute })
    public open = false;

    /**
     * A reference to a template which will be used as trigger/text.
     */
    @Input()
    public trigger: TemplateRef<any>;

    /**
     * If the trigger/text should be able to open/close the content.
     */
    @Input()
    public triggerToggle = false;

    /* OUTPUTS
     * --------------------------------------------------------------------- */

    /**
     * When the open status changes
     */
    @Output()
    public openChange = new EventEmitter<boolean>();

    /**
     * When the trigger is being clicked.
     * Is always emitted and is done before `openChange`.
     */
    @Output()
    public clickTrigger = new EventEmitter<any>();

    /* TEMPLATE VARIABLES
     * --------------------------------------------------------------------- */

    public showContent = false;

    /* CONSTRUCTOR
     * --------------------------------------------------------------------- */

    constructor(changeDetector: ChangeDetectorRef) {
        super(changeDetector);
    }

    /* LIFE-CYCLE HOOKS
     * --------------------------------------------------------------------- */

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.open) {
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
