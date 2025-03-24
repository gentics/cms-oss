import { PackageCheckTrableLoaderService, DevToolPackageHandlerService } from '@admin-ui/shared';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnDestroy,
} from '@angular/core';
import { Subscription } from 'rxjs';

@Component({
    selector: 'gtx-dev-tool-check-wrapper',
    templateUrl: './package-check-wrapper.component.html',
    styleUrls: ['./package-check-wrapper.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PackageCheckWrapperComponent
    implements OnChanges, OnDestroy
{
    @Input()
    public packageName;

    public isCheckResultAvailable = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private handler: DevToolPackageHandlerService,
        private packageCheckLoader: PackageCheckTrableLoaderService,
        private changeDetector: ChangeDetectorRef,
    ) {}


    public ngOnChanges(): void {
        this.isPackageCheckResultAvailable(this.packageName);
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((subscription) =>
            subscription.unsubscribe(),
        );
    }

    protected isPackageCheckResultAvailable(packageName: string): void {
        const sub = this.handler
            .isCheckResultAvailable({ packageName })
            .subscribe((isAvailable) => {
                this.isCheckResultAvailable = isAvailable;
                this.changeDetector.markForCheck();
            });

        this.subscriptions.push(sub);
    }

    public handleLoadButtonClick(packageName: string): void {
        const subscription = this.packageCheckLoader
            .triggerNewCheck({ packageName })
            .subscribe({complete: () => {
                this.isCheckResultAvailable = true;
                this.changeDetector.markForCheck();
            }});

        this.subscriptions.push(subscription);
    }

}
