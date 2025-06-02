import { ActivityManagerService, GtxActivityManagerActivity } from '@admin-ui/core/providers/activity-manager';
import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

@Component({
    selector: 'gtx-activity-manager',
    templateUrl: './activity-manager.component.html',
    styleUrls: ['./activity-manager.component.scss'],
    standalone: false
})
export class ActivityManagerComponent implements OnInit {

    activities$: Observable<GtxActivityManagerActivity[]>;

    constructor(
        private activityManager: ActivityManagerService,
    ) { }

    ngOnInit(): void {
        this.activities$ = this.activityManager.activities$;
    }

    activitySetExpanded(id: number, value: boolean): void {
        this.activityManager.activitySetExpanded(id, value);
    }

    activityRemove(id: number): void {
        this.activityManager.activityRemove(id);
    }

}
