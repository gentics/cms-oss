import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { DataSource, Language, PartType, TagPart } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { ConstructPartPropertiesMode } from '../construct-part-properties/construct-part-properties.component';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Subscription } from 'rxjs';

@Component({
    selector: 'gtx-create-construct-part-modal',
    templateUrl: './create-construct-part-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CreateConstructPartModalComponent extends BaseModal<TagPart> implements OnInit, OnDestroy {

    // tslint:disable-next-line: variable-name
    readonly ConstructPartPropertiesComponentMode = ConstructPartPropertiesMode;

    @Input()
    public supportedLanguages: Language[];

    @Input()
    public keywordBlacklist: string[];

    @Input()
    public orderBlacklist: number[];

    @Input()
    public dataSources: DataSource[];

    @Input()
    public defaultOrder = 1;

    public form: UntypedFormControl;
    public partTypes: PartType[] = [];

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(this.client.partType.list().subscribe((res) => {
            this.partTypes = res.items;
            this.changeDetector.markForCheck();
        }));

        this.form = new UntypedFormControl({
            partOrder: this.defaultOrder,
        });
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }

    buttonCreateEntityClicked(): void {
        if (this.form.invalid) {
            return;
        }

        this.closeFn(this.form.value);
    }
}
