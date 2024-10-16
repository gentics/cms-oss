import { Component } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { DebugToolService } from '../../providers/debug-tool.service';

@Component({
    selector: 'gtx-debug-tool',
    templateUrl: './debug-tool.component.html',
    styleUrls: ['./debug-tool.component.scss'],
})
export class DebugTool extends BaseModal<void> {

    public inProgress = false;
    public clearingData = false;

    constructor(
        public debugToolService: DebugToolService,
    ) {
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
