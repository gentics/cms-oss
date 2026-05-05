import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
} from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { CopilotAction } from '../../copilot.types';
import { CopilotConfigService, CopilotStateService } from '../../providers';

/**
 * Right-hand drawer that hosts the Content Copilot interface.
 *
 * For this UI iteration the body is intentionally empty — only the
 * scaffolding (header, action container, chat input) is present so the
 * full visual integration can be reviewed before any action wiring is
 * added. Once a customer drops a `copilot.yml` with a non-empty
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

    public open = false;
    public actions: CopilotAction[] = [];

    public open$: Observable<boolean>;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private copilotState: CopilotStateService,
        private copilotConfig: CopilotConfigService,
    ) {}

    ngOnInit(): void {
        this.open$ = this.copilotState.open$;

        this.subscriptions.push(this.copilotState.open$.subscribe((isOpen) => {
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
        this.copilotState.close();
    }

    public trackById = (_: number, action: CopilotAction): string => action.id;
}
