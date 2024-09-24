import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnInit,
} from '@angular/core';
import { Page, PublishLogEntry, ResponseCode } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-page-publish-protocol-modal',
    templateUrl: './page-publish-protocol-modal.html',
    styleUrls: ['./page-publish-protocol-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagePublishProtocolModalComponent implements IModalDialog, OnInit {

    @Input() page: Page;
    @Input() nodeId: number;

    loading = false;
    backgroundActivity = false;
    selectedPageVariant: Page;
    languageVariants: Page[];
    publishLogEntries: PublishLogEntry[];

    constructor(
        private api: GCMSRestClientService,
        private changeDetector: ChangeDetectorRef,
    ) { }


    ngOnInit(): void {
        Promise.resolve([
            this.fetchLogEntries(this.page.id),
            this.fetchLanguageVariant(this.page.id),
        ]).then(()=> {
            this.changeDetector.markForCheck();
        })
    }

    private async fetchLanguageVariant(pageId: number): Promise<void> {
        const response = await this.api.page.get(pageId, {langvars: true}).toPromise();

        if (response.responseInfo.responseCode !== ResponseCode.OK) {
            Promise.reject(new Error('Unable to retrieve language variants'));
        }

        this.languageVariants = [];
        for (const [key, languageVariant] of Object.entries(response.page.languageVariants)) {
            this.languageVariants.push(languageVariant);
        }
    }

    private async fetchLogEntries(pageId: number): Promise<void>{
        this.loading = true;

        const options = {
            objId: pageId,
        }
        const response = await this.api.publishProtocol.list(options).toPromise();

        if (response.responseInfo.responseCode !== ResponseCode.OK) {
            Promise.reject(new Error('Unable to retrieve publish protocol'));
        }

        this.publishLogEntries = response.items.map(item => {
            return {
                ...item,
                date: this.formatDate(item.date),
            }
        });

        this.loading = false;
        this.changeDetector.markForCheck();
    }

    private formatDate(unformattedDate: string): string {
        const date = new Date(unformattedDate);

        return `${date.getMonth() + 1}/${date.getDate()}/${date.getFullYear()} at
            ${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
    }

    public selectPageVariant(variant: Page): void {
        this.page = variant;
        this.fetchLogEntries(this.page.id);
    }

    closeFn(): void { }

    cancelFn(): void { }

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }
}
