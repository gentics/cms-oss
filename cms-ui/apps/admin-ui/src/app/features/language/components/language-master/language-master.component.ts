import { LanguageBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { NormalizableEntityTypesMap, AnyModelType, Language } from '@gentics/cms-models';

@Component({
    selector: 'gtx-language-master',
    templateUrl: './language-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageMasterComponent extends BaseTableMasterComponent<Language, LanguageBO> {
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'language';
}
