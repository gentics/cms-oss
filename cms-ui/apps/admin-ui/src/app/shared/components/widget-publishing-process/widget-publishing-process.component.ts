import { AdminUIModuleRoutes } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { PublishInfo, SchedulerStatus } from '@gentics/cms-models';
import { ChangesOf } from '@gentics/ui-core';

type ObjectType = 'folder' | 'file' | 'page' | 'form';
type PluralName = 'folders' | 'files' | 'pages' | 'forms';

interface TypePublishStatus {
    type: string;
    icon: string;
    i18n: string;
    data: { count: number, total?: number };
}

const DATA_TYPES: ObjectType[] = ['folder', 'page', 'file', 'form'];

const PLURAL_MAPPING: Record<ObjectType, PluralName> = {
    folder: 'folders',
    file: 'files',
    page: 'pages',
    form: 'forms',
};

const ICON_MAPPING: Record<ObjectType, string> = {
    folder: 'folder',
    page: 'subject',
    file: 'image',
    form: 'list_alt',
};

@Component({
    selector: 'gtx-widget-publishing-process',
    templateUrl: './widget-publishing-process.component.html',
    styleUrls: ['./widget-publishing-process.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WidgetPublishingProcessComponent implements OnChanges {

    public readonly AdminUIModuleRoutes = AdminUIModuleRoutes;

    @Input()
    public showTitle = false;

    @Input()
    public info: PublishInfo = null;

    @Input()
    public hasFailedJobs = false;

    @Input()
    public publisherStatus: SchedulerStatus = null;

    public typedStatus: TypePublishStatus[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) {}

    public ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.info) {
            this.updateTypedStatus();
        }
    }

    updateTypedStatus(): void {
        this.typedStatus = [];

        if (!this.info) {
            return;
        }

        for (const type of DATA_TYPES) {
            const plural = PLURAL_MAPPING[type];
            const data = this.info[plural];

            if (this.info.running) {
                this.typedStatus.push({
                    type,
                    icon: ICON_MAPPING[type],
                    i18n: 'widget.publishing_process_published',
                    data: {
                        count: data.published,
                        total: data.toPublish,
                    },
                });
            } else {
                this.typedStatus.push({
                    type,
                    icon: ICON_MAPPING[type],
                    i18n: 'widget.publishing_process_to_be_published',
                    data: {
                        count: data.toPublish,
                    },
                });
            }
        }

        this.changeDetector.markForCheck();
    }
}
