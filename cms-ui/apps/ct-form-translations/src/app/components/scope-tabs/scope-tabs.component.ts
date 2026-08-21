import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
} from '@angular/core';
import { Scope, ScopeId } from '../../models/translations.model';

export interface ScopeTabInfo {
    scope: Scope;
    translatedCount: number;
    totalCount: number;
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
    @Input() tabs: ScopeTabInfo[] = [];
    @Input() activeScopeId: ScopeId = '';
    @Output() readonly scopeSelect = new EventEmitter<ScopeId>();

    onClick(id: ScopeId): void {
        if (id !== this.activeScopeId) this.scopeSelect.emit(id);
    }

}
