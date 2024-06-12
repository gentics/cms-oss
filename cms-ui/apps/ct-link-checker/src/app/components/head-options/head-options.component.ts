import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ExternalLinkStatistics } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { publishReplay, refCount, switchMap } from 'rxjs/operators';
import { AppService } from '../../services/app/app.service';
import { LinkCheckerService } from '../../services/link-checker/link-checker.service';

@Component({
    selector: 'gtxct-head-options',
    templateUrl: './head-options.component.html',
    styleUrls: ['./head-options.component.scss'],
})
export class HeadOptionsComponent implements OnInit {

    globalStats$: Observable<ExternalLinkStatistics>;
    statsLoading$: Observable<boolean>;

    constructor(
        private appService: AppService,
        private changeDetector: ChangeDetectorRef,
        private linkChecker: LinkCheckerService,
    ) { }

    ngOnInit(): void {
        const loaders = this.linkChecker.getLoaders();
        this.statsLoading$ = loaders.globalStats.pipe(
            publishReplay(1),
            refCount(),
        );
        this.globalStats$ = this.appService.update$.pipe(
            switchMap(() => this.linkChecker.fetchStats()),
        );
        this.changeDetector.detectChanges();
    }

    updateStatus(): void {
        this.appService.updateData();
    }

}
