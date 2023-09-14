import { PackageCheckTrableLoaderService } from '@admin-ui/core';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { Subscription, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

@Component({
    selector: 'gtx-dev-tool-check-wrapper',
    templateUrl: './package-check-wrapper.component.html',
    styleUrls: ['./package-check-wrapper.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PackageCheckWrapperComponent
    implements OnInit, OnChanges, OnDestroy
{
    @Input()
    public packageName;

    public isCheckResultAvailable = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private packageCheckLoader: PackageCheckTrableLoaderService,
        private changeDetector: ChangeDetectorRef,
    ) {}

    ngOnInit(): void {
    }

    public ngOnChanges(): void {
        this.isPackageCheckResultAvailable(this.packageName);
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((subscription) =>
            subscription.unsubscribe(),
        );
    }

    protected isPackageCheckResultAvailable(packageName: string): void {
        const sub = this.packageCheckLoader
            .isCheckResultAvailable({ packageName })
            .subscribe((isAvailable) => {
                this.isCheckResultAvailable = isAvailable;
                this.changeDetector.markForCheck();
            });

        this.subscriptions.push(sub);
    }

    public handleLoadButtonClick(packageName: string): void {
        const subscription = this.packageCheckLoader
            .getNewCheckResult({ packageName })
            .pipe(
                tap(() => this.isCheckResultAvailable = true),
                catchError(() => {
                    this.isCheckResultAvailable = false;
                    // todo: poll the result
                    return of(false);
                }),
            )
            .subscribe(() =>
                this.changeDetector.markForCheck(),
            );

        this.subscriptions.push(subscription);
    }

}
