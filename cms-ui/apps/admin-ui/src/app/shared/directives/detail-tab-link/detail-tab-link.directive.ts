import { ObservableStopper } from '@admin-ui/common';
import { Directive, OnDestroy, OnInit, Self } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TabComponent } from '@gentics/ui-core';
import { takeUntil } from 'rxjs/operators';

/**
 * Used to make a `<gtx-tab>` trigger a navigation by replacing the last segment of
 * the `ActivatedRoute` with the tab's ID.
 *
 * This directive should be used when a `<gtx-tab>` with content children should change the route.
 * Since the tab has content children, `[routerLink]` cannot be used on the tab
 * (see https://jira.gentics.com/browse/GUIC-238).
 */
@Directive({
    selector: 'gtx-tab[gtxDetailTabLink]',
})
export class DetailTabLinkDirective implements OnInit, OnDestroy {

    private stopper = new ObservableStopper();

    constructor(
        @Self() private gtxTab: TabComponent,
        private router: Router,
        private route: ActivatedRoute,
    ) { }

    ngOnInit(): void {
        this.gtxTab.select.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(tabId => {
            this.router.navigate([ '../', tabId ], { relativeTo: this.route })
        });
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

}
