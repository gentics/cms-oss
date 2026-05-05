import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { Subscription } from 'rxjs';
import { ApplicationStateService, SetCopilotOpenAction } from '../../../state';
import { CopilotAction } from '../../copilot.types';
import { CopilotConfigService } from '../../providers';

/**
 * Right-hand drawer that hosts the Content Copilot interface.
 *
 * For this UI iteration the body is intentionally empty — only the
 * scaffolding (header, action container, chat input) is present so the
 * full visual integration can be reviewed before any action wiring is
 * added. Once a customer drops a `copilot.json` with a non-empty
 * `actions:` list, those entries will appear inside `.copilot-actions`
 * automatically; nothing in this component changes.
 *
 * The drawer is rendered as a fixed-position overlay on the right edge
 * of the content frame, slides in via a CSS transform, and never
 * intercepts clicks while closed (`pointer-events: none`).
 */
@Component({
    selector: 'gtx-copilot-sidebar',
    templateUrl: './copilot-sidebar.component.html',
    styleUrls: ['./copilot-sidebar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CopilotSidebarComponent implements OnInit, OnDestroy {

    /**
     * Mirror of the service streams as plain properties — the template
     * binds against these directly. Following the project convention
     * (cf. `editor-toolbar.component`) of avoiding async-pipe / `$`
     * properties in templates because each binding would re-subscribe
     * on every change-detection cycle.
     */
    public open = false;
    public actions: CopilotAction[] = [];

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private copilotConfig: CopilotConfigService,
    ) {}

    ngOnInit(): void {
        this.subscriptions.push(this.appState.select((state) => state.ui.copilotOpen).subscribe((isOpen) => {
            this.open = isOpen;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.copilotConfig.actions$.subscribe((actions) => {
            this.actions = actions;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }

    public close(): void {
        this.appState.dispatch(new SetCopilotOpenAction(false));
    }

    public trackById = (_: number, action: CopilotAction): string => action.id;
}
