import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';
import { Alerts } from '@editor-ui/app/common/models';
import { EmbeddedTool } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { EmbeddedToolsService } from '../../../embedded-tools/providers/embedded-tools/embedded-tools.service';
import { ApplicationStateService } from '../../../state';

@Component({
    selector: 'alert-center',
    templateUrl: './alert-center.component.html',
    styleUrls: ['./alert-center.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class AlertCenterComponent {

    @Output()
    navigate = new EventEmitter<boolean>(false);

    toolsAvailable$: Observable<EmbeddedTool[]>;
    alerts$: Observable<Alerts>;
    alertCount$: Observable<number>;

    constructor(
        appState: ApplicationStateService,
        private toolService: EmbeddedToolsService,
    ) {
        this.alerts$ = appState.select(state => state.ui.alerts);
        this.toolsAvailable$ = appState.select(state => state.tools.available);

        this.alertCount$ = this.alerts$.pipe(
            filter(alerts => Object.values(alerts).length > 0),
            map(alerts => Object.values(alerts)
                .map(alertType => (alertType === Object(alertType) ? Object.values(alertType) : alertType) || [])
                // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
                .reduce((acc, val) => acc + val),
            ),
        );
    }

    openLinkChecker(event: Event): void {
        if (!event.defaultPrevented) {
            this.toolService.openOrFocus('linkchecker');
            this.navigate.emit(true);
            event.preventDefault();
        }
    }

    hasLinkCheckerTool(tool: EmbeddedTool): boolean {
        return tool.key === 'linkchecker';
    }
}
