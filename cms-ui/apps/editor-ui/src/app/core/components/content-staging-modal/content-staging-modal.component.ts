import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UIMode } from '@editor-ui/app/common/models';
import { ApplicationStateService, SetActiveContentPackageAction, SetUIModeAction } from '@editor-ui/app/state';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-content-staging-modal',
    templateUrl: './content-staging-modal.component.html',
    styleUrls: ['./content-staging-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    })
export class ContentStagingModal extends BaseModal<void> implements OnInit {

    public hadPackage = false;
    public activePackage = '';

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected state: ApplicationStateService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.activePackage = this.state.now.contentStaging.activePackage;
        this.hadPackage = !!this.activePackage;
    }

    updateActivePackage(packageId: string): void {
        this.activePackage = packageId;
        this.changeDetector.markForCheck();
    }

    updatePackageInState(): void {
        if (this.activePackage) {
            this.state.dispatch(new SetUIModeAction(UIMode.STAGING));
            this.state.dispatch(new SetActiveContentPackageAction(this.activePackage));
        } else {
            this.state.dispatch(new SetUIModeAction(UIMode.EDIT));
            this.state.dispatch(new SetActiveContentPackageAction(null));
        }

        this.closeFn();
    }
}
