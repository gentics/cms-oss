import { ChangeDetectionStrategy, Component } from '@angular/core';

export enum SchedulerModuleTabs {
    SCHEDULES = 'schedules',
    TASKS = 'tasks',
}

@Component({
    selector: 'gtx-scheduler-module-master',
    templateUrl: './scheduler-module-master.component.html',
    styleUrls: ['./scheduler-module-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchedulerModuleMasterComponent {

    SchedulerModuleTabs = SchedulerModuleTabs;

    public activeTab: SchedulerModuleTabs = SchedulerModuleTabs.SCHEDULES;

    setTabAsActive(tabId: SchedulerModuleTabs): void {
        this.activeTab = tabId;
    }
}
