import { Component } from '@angular/core';

/**
 * A simple wrapper for displaying small details with optional icons.
 */
@Component({
    selector: 'detail-chip',
    template: `<ng-content></ng-content>`,
    styleUrls: ['./detail-chip.scss']
})
export class DetailChipComponent {
    constructor() { }

}
