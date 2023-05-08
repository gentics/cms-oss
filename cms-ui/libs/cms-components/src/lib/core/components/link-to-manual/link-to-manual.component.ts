import { Component } from '@angular/core';

/**
 * For displaying the link to the local user manual.
 */
@Component({
    selector: 'gtx-link-to-manual',
    templateUrl: './link-to-manual.component.html',
    styleUrls: ['./link-to-manual.component.scss'],
})
export class GtxLinkToManualComponent {
    /** Actual URL string pointing at manual web resource. */
    manualUrl = '/guides/manuals/user-manual/';
}
