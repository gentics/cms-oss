import { ConstructPartPropertiesMode } from '@admin-ui/features/construct/components';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { DataSource, Language, TagPart } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-construct-part-modal',
    templateUrl: './create-construct-part-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateConstructPartModalComponent extends BaseModal<TagPart> implements OnInit {

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

    constructor() {
        super();
    }

    ngOnInit(): void {
        this.form = new UntypedFormControl({
            partOrder: this.defaultOrder,
        });
    }

    buttonCreateEntityClicked(): void {
        if (this.form.invalid) {
            return;
        }

        this.closeFn(this.form.value);
    }
}
