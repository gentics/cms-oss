import {Component} from '@angular/core';
import {IModalDialog} from '@gentics/ui-core';
import {DebugToolService} from '../../providers/debug-tool.service';

@Component({
    selector: 'gtx-debug-tool',
    templateUrl: './debug-tool.component.html',
    styleUrls: ['./debug-tool.component.scss']
})
export class DebugTool implements IModalDialog {
    public inProgress: boolean = false;
    public clearingData: boolean = false;
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
            (reject: any) => this.inProgress = false
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
