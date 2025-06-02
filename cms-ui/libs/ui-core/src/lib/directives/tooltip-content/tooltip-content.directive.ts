import { Directive, ElementRef } from '@angular/core';

@Directive({
    selector: '[gtx-tooltip-content],[gtxTooltipContent]',
    standalone: false
})
export class TooltipContentDirective {

    constructor(public element: ElementRef) { }
}
