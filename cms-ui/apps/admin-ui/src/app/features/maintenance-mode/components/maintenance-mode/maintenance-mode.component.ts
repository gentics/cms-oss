import { AdminOperations } from '@admin-ui/core';
import { MaintenanceModeStateModel, SelectState } from '@admin-ui/state';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MaintenanceModeRequestOptions } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';

@Component({
    selector: 'gtx-maintenance-mode',
    templateUrl: './maintenance-mode.component.html',
    styleUrls: ['maintenance-mode.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MaintenanceModeViewComponent implements OnInit {

    @SelectState(state => state.maintenanceMode)
    settings$: Observable<MaintenanceModeStateModel>;

    formGroup: UntypedFormGroup;

    constructor(
        private router: Router,
        private adminOperations: AdminOperations,
        private formBuilder: UntypedFormBuilder,
    ) {}

    ngOnInit(): void {
        this.formGroup = this.formBuilder.group({
            maintenance: [ false ],
            banner: [ false ],
            message: ['', Validators.required],
        });

        // get settings from state
        this.settings$.pipe(
            take(1),
        ).subscribe(settings => {
            this.formGroup.get('maintenance').setValue(settings.active);
            this.formGroup.get('banner').setValue(settings.showBanner);
            this.formGroup.get('message').setValue(settings.message);
        });
    }

    public setMaintenanceMode(): void {
        const request: MaintenanceModeRequestOptions = {
            maintenance: this.formGroup.get('maintenance').value,
            banner: this.formGroup.get('banner').value,
            message: this.formGroup.get('message').value,
        };
        this.adminOperations.setMaintenanceMode(request).toPromise();
    }

}
