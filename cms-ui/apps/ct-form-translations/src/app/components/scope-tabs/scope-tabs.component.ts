import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
    inject,
} from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { Scope, ScopeId } from '../../models/translations.model';

/** What one tab renders. Every scope is measured the same way. */
export interface ScopeTabInfo {
    scope: Scope;
    /** Placeholders with at least one empty active language. */
    open: number;
    /** Placeholders in the scope. */
    total: number;
    hasDirty: boolean;
}

@Component({
    selector: 'gtx-scope-tabs',
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './scope-tabs.component.html',
    styleUrls: ['./scope-tabs.component.scss'],
})
export class ScopeTabsComponent {

    private readonly i18n = inject(I18nService);

    @Input() tabs: ScopeTabInfo[] = [];
    @Input() activeScopeId: ScopeId = '';
    @Output() readonly scopeSelect = new EventEmitter<ScopeId>();

    onClick(id: ScopeId): void {
        if (id !== this.activeScopeId) this.scopeSelect.emit(id);
    }

    /** Spells out the short second line. */
    hint(tab: ScopeTabInfo): string {
        return this.i18n.instant('tool.scope_tab_open_hint', {
            open: tab.open,
            total: tab.total,
        });
    }
}
