import { Component } from '@angular/core';
import { IModalDialog } from '@gentics/ui-core';
import { DebugToolService } from '../../providers/debug-tool/debug-tool.service';

@Component({
    selector: 'gtx-debug-tool-modal',
    templateUrl: './debug-tool-modal.component.html',
    styleUrls: ['./debug-tool-modal.component.scss'],
})
export class DebugToolModalComponent implements IModalDialog {

    public inProgress = false;
    public clearingData = false;
    public debugToolService: DebugToolService = null;

    constructor() {}

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
            (result: any) => this.clearingData = true,
            (reject: any) => this.inProgress = false,
        );
    }

    closeFn(val: any): void { }
    cancelFn(val?: any): void { }

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }
}
