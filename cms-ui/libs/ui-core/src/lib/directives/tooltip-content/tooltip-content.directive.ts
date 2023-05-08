import { Directive, ElementRef } from '@angular/core';

@Directive({
    selector: '[gtx-tooltip-content],[gtxTooltipContent]',
})
export class TooltipContentDirective {

    constructor(public element: ElementRef) { }
}
