import { Component } from '@angular/core';

/**
 * A component that receives router events for tool routes.
 * The {@link EmbeddedToolsHostComponent} is the component that actually shows/hides tool iframes.
 */
@Component({
    selector: 'tool-proxy',
    template: `<router-outlet></router-outlet>`
})
export class ToolProxyComponent { }
