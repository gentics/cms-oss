import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnInit,
} from '@angular/core';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { Page, PublishLogEntry, PublishLogListOption, PublishType, ResponseCode } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseModal } from '@gentics/ui-core';


const PAGE_SIZE = 10;

@Component({
    selector: 'gtx-page-publish-protocol-modal',
    templateUrl: './page-publish-protocol-modal.html',
    styleUrls: ['./page-publish-protocol-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagePublishProtocolModalComponent extends BaseModal<void> implements OnInit {
    @Input()
    public page: Page;
    public loading = false;
    public languageVariants: Page[];
    public publishLogEntries: PublishLogEntry[];

    constructor(
        private api: GCMSRestClientService,
        private changeDetector: ChangeDetectorRef,
        private errorHandler: ErrorHandler,
    ) {
        super();
    }


    ngOnInit(): void {
        this.loading = true;
        Promise.all([
            this.fetchLogEntries(this.page.id),
            this.fetchLanguageVariant(this.page.id),
        ]).then(()=> {
            this.loading = false;
            this.changeDetector.markForCheck();
        })
    }

    private async fetchLanguageVariant(pageId: number): Promise<void> {
        const response = await this.api.page.get(pageId, {langvars: true}).toPromise();

        if (response.responseInfo.responseCode !== ResponseCode.OK) {
            return this.errorHandler.catch(new Error('Unable to retrieve language variants'));
        }

        this.languageVariants = [];
        for (const languageVariant of Object.values(response.page.languageVariants || {})) {
            this.languageVariants.push(languageVariant);
        }
    }

    private async fetchLogEntries(pageId: number): Promise<void>{
        this.loading = true;

        const options: PublishLogListOption = {
            objId: pageId,
            type:  PublishType.PAGE,
            pageSize: PAGE_SIZE,
        }
        const response = await this.api.publishProtocol.list(options).toPromise();

        if (response.responseInfo.responseCode !== ResponseCode.OK) {
            return this.errorHandler.catch(new Error('Unable to retrieve publish protocol'));
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

}
