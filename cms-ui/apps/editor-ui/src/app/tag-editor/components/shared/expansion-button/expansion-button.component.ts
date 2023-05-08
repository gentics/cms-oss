import {ChangeDetectionStrategy, Component, Input, ChangeDetectorRef} from '@angular/core';

/**
 * Displays a button that toggles the expansion of a content panel.
 */
@Component({
    selector: 'expansion-button',
    templateUrl: './expansion-button.component.html',
    styleUrls: ['./expansion-button.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpansionButtonComponent {

    /**
     * The label of the button.
     */
    @Input()
    label: string;

    /**
     * Optional tooltip for the button.
     */
    @Input()
    tooltip: string = '';

    /**
     * Controls whether the panel is expanded or not.
     */
    @Input()
    expanded: boolean = false;

    constructor(private changeDetector: ChangeDetectorRef) { }

    /**
     * Expands the content panel.
     */
    expand(): void {
        this.expanded = true;
        this.changeDetector.markForCheck();
    }

    /**
     * Collapses the content panel.
     */
    collapse(): void {
        this.expanded = false;
        this.changeDetector.markForCheck();
    }

}
