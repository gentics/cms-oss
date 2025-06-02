import { SelectableType } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { pick } from'lodash-es'

enum PageLinkType {
    INTERNAL = 'internal',
    EXTERNAL = 'external',
}

@Component({
    selector: 'gtx-page-part-fill',
    templateUrl: './page-part-fill.component.html',
    styleUrls: ['./page-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(PagePartFillComponent)],
    standalone: false
})
export class PagePartFillComponent extends BaseFormElementComponent<PageTagPartProperty> {

    // tslint:disable-next-line: variable-name
    readonly PageLinkType = PageLinkType;
    // tslint:disable-next-line: variable-name
    readonly SelectableTypes = SelectableType;
    // tslint:disable-next-line: variable-name
    readonly TagPropertyType = TagPropertyType;

    public linkType: PageLinkType = PageLinkType.INTERNAL;

    protected override onValueChange(): void {
        if (this.value?.pageId) {
            this.linkType = PageLinkType.INTERNAL;
        } else if (this.value?.stringValue) {
            this.linkType = PageLinkType.EXTERNAL;
        }
    }

    onLinkTypeChange(type: PageLinkType): void {
        if (type === this.linkType) {
            return;
        }
        this.linkType = type;
        this.triggerChange({
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: TagPropertyType.PAGE,
        });
    }

    onExternalUrlChange(value: string): void {
        const newValue: PageTagPartProperty = {
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: TagPropertyType.PAGE,
            stringValue: value,
        };

        this.triggerChange(newValue);
    }
}
