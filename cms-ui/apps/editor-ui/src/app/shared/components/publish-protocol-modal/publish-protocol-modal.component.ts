import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnInit,
} from '@angular/core';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { Form, Page, PublishLogEntry, PublishLogListOption, PublishType, ResponseCode } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseModal } from '@gentics/ui-core';


const PAGE_SIZE = 15;

@Component({
    selector: 'gtx-page-publish-protocol-modal',
    templateUrl: './publish-protocol-modal.html',
    styleUrls: ['./publish-protocol-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublishProtocolModalComponent extends BaseModal<void> implements OnInit {
    @Input()
    public item: Page | Form;
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
            this.fetchLogEntries(this.item),
            this.fetchLanguageVariant(this.item.id),
        ]).then(()=> {
            this.loading = false;
            this.changeDetector.markForCheck();
        })
    }

    private async fetchLanguageVariant(pageId: number): Promise<void> {
        if (this.item.type !== 'page') {
            return;
        }
        const response = await this.api.page.get(pageId, {langvars: true}).toPromise();

        if (response.responseInfo.responseCode !== ResponseCode.OK) {
            return this.errorHandler.catch(new Error('Unable to retrieve language variants'));
        }

        this.languageVariants = [];
        for (const languageVariant of Object.values(response.page.languageVariants || {})) {
            this.languageVariants.push(languageVariant);
        }
    }

    private async fetchLogEntries(item: Page | Form): Promise<void>{
        this.loading = true;

        const options: PublishLogListOption = {
            objId: item.id,
            type:  item.type === 'page'? PublishType.PAGE: PublishType.FORM,
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
        this.item = variant;
        this.fetchLogEntries(this.item);
    }

}
