import { Directive, TemplateRef } from '@angular/core';

/** Decorates the `ng-template` tags and reads out the template from it. */
@Directive({selector: '[gtx-tab-label]'})
export class TabLabelDirective {
    constructor(public template: TemplateRef<any>) {}
}
