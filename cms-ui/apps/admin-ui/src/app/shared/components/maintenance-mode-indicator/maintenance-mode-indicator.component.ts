import { MaintenanceModeStateModel, SelectState } from '@admin-ui/state';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Visually indicates whether CMS Maintenance Mode is active and what banner text is configured.
 * @usage
 * ```
 * <gtx-maintenance-mode-indicator
 * </gtx-maintenance-mode-indicator>
 * ```
 */
@Component({
    selector: 'gtx-maintenance-mode-indicator',
    templateUrl: './maintenance-mode-indicator.component.html',
    styleUrls: ['./maintenance-mode-indicator.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MaintenanceModeIndicatorComponent {
    
    @SelectState(state => state.maintenanceMode)
    settings$: Observable<MaintenanceModeStateModel>;
}
