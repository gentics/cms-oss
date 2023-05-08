import {Component} from '@angular/core';

/**
 * A work around to allow us to lazy-load feature modules into a named outlet.
 * See https://github.com/angular/angular/issues/12842#issuecomment-270836368
 */
@Component({
    selector: 'proxy-route',
    template: '<router-outlet></router-outlet>'
})
export class ProxyRouteComponent {
}
