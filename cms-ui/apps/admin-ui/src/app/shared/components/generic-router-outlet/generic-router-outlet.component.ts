import { Component } from '@angular/core';

/**
 * This component can be used as a parent component for child routes if
 * the children should completely replace the parent component.
 */
@Component({
    selector: 'gtx-generic-router-outlet',
    template: '<router-outlet></router-outlet>',
    standalone: false
})
export class GenericRouterOutletComponent { }
