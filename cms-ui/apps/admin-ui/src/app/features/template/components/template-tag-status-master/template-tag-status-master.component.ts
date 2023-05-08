import { TagStatusBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap, TagStatus } from '@gentics/cms-models';

@Component({
    selector: 'gtx-template-tag-status-master',
    templateUrl: './template-tag-status-master.component.html',
    styleUrls: ['./template-tag-status-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TemplateTagStatusMasterComponent extends BaseTableMasterComponent<TagStatus, TagStatusBO> {

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'templateTagStatus';

    @Input()
    public disabled: boolean;

    @Input()
    public templateId: number | string;

}
