import { Directive, ElementRef } from '@angular/core';

@Directive({
    selector: '[gtx-tooltip-trigger],[gtxTooltipTrigger]',
    standalone: false
})
export class TooltipTriggerDirective {

    constructor(public element: ElementRef) { }
}
