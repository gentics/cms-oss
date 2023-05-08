import { Directive, ElementRef } from '@angular/core';

@Directive({
    selector: '[gtx-tooltip-trigger],[gtxTooltipTrigger]',
})
export class TooltipTriggerDirective {

    constructor(public element: ElementRef) { }
}
