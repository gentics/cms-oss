import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'description-tooltip',
    templateUrl: './description-tooltip.component.html',
    styleUrls: ['./description-tooltip.component.scss'],
})
export class DescriptionTooltipComponent {

    @Input()
    target: string;

    @Input()
    objectProperty: any;

    @Input()
    position: string;

    @Input()
    visible: boolean;

    @Output()
    elementHover = new EventEmitter<boolean>();

    checkHoverState(hover: boolean): void {
        this.elementHover.emit(hover);
    }
}
