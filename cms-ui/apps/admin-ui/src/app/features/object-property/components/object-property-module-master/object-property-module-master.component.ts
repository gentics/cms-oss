import { ChangeDetectionStrategy, Component } from '@angular/core';

export enum ObjectPropertyModuleTabs {
    OBJECT_PROPERTIES = 'object-properties',
    CATEGORIES = 'categories',
}

@Component({
    selector: 'gtx-object-property-module-master',
    templateUrl: './object-property-module-master.component.html',
    styleUrls: ['./object-property-module-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ObjectPropertyModuleMasterComponent {

    // tslint:disable-next-line: variable-name
    readonly ObjectPropertyModuleTabs = ObjectPropertyModuleTabs;

    public activeTab: ObjectPropertyModuleTabs = ObjectPropertyModuleTabs.OBJECT_PROPERTIES;

    setTabAsActive(tabId: ObjectPropertyModuleTabs): void {
        this.activeTab = tabId;
    }
}
