import { Component } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { DebugToolService } from '../../providers/debug-tool/debug-tool.service';

@Component({
    selector: 'gtx-debug-tool-modal',
    templateUrl: './debug-tool-modal.component.html',
    styleUrls: ['./debug-tool-modal.component.scss'],
})
export class DebugToolModalComponent extends BaseModal<void> {

    public inProgress = false;
    public clearingData = false;
    // FIXME: This circular dependency is really bad.
    // In general, the usefulness and architecture of this entire module is questionable.
    public debugToolService: DebugToolService;

    constructor() {
        super();
    }

    generateReport(): void {
        this.inProgress = true;
        this.debugToolService.generateReport().then((report: any) => {
            this.closeFn(report);
            this.inProgress = false;
        });
    }

    clearSiteData(): void {
        this.inProgress = true;
        this.debugToolService.clearSiteData().then(
            () => { this.clearingData = true; },
            () => { this.inProgress = false; },
        );
    }
}
