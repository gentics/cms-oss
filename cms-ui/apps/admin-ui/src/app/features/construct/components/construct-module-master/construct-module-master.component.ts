import { ChangeDetectionStrategy, Component } from '@angular/core';

export enum ConstructModuleTabs {
    CONSTRUCTS = 'constructs',
    CATEGORIES = 'categories',
}

@Component({
    selector: 'gtx-construct-module-master',
    templateUrl: './construct-module-master.component.html',
    styleUrls: ['./construct-module-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructModuleMasterComponent {

    readonly ConstructModuleTabs = ConstructModuleTabs;

    public activeTab: ConstructModuleTabs = ConstructModuleTabs.CONSTRUCTS;

    setTabAsActive(tabId: ConstructModuleTabs): void {
        this.activeTab = tabId;
    }

}
