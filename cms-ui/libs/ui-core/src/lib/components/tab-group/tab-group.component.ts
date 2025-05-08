import { Component, ContentChildren, QueryList, Input, TemplateRef, Output, EventEmitter } from '@angular/core';
import { TabPaneComponent } from '../tab-pane/tab-pane.component';
import { TabLabelDirective } from '../../directives/tab-label/tab-label.directive';
import { coerceToBoolean } from '../../utils';

// Counter for unique group ids.
let uniqueTabGroupId = 0;

/**
 * For documentation, see the GroupedTabs
 */
@Component({
    selector: 'gtx-tab-group',
    exportAs: 'gtxTabGroup',
    template: '',
    standalone: false
})
export class TabGroupComponent {

    /** Unique id for the tab group. */
    uniqueId = `gtx-tag-group-${uniqueTabGroupId++}`;

    /** Expand state for the group */
    public expand = false;

    /** Plain text label for the tab, used when there is no template label. */
    @Input('label')
    textLabel = '';

    @Input()
    set expanded(val: any) {
        this.expand = coerceToBoolean(val);
    }

    @Input()
    set id(val: string) {
        this.uniqueId = val;
    }

    get id(): string { return this.uniqueId; }

    /**
     * Fires an event whenever the tab group is toggled. Argument is the id and state of the tab group.
     */
    @Output()
    tabGroupToggle = new EventEmitter<{ id: string, expand: boolean }>();

    /** Content for the tab label given by `<ng-template gtx-tab-label>`. */
    @ContentChildren(TabLabelDirective, { read: TemplateRef, descendants: false })
    templateLabels: QueryList<TabLabelDirective>;

    /** All of the defined tab panes. */
    @ContentChildren(TabPaneComponent, { descendants: false })
    tabs: QueryList<TabPaneComponent>;

    get templateLabel(): TabLabelDirective {
        return this.templateLabels.first || null;
    }

    get hasActiveChild(): boolean {
        return this.tabs.some(tab => tab.active)
    }

    toggle(): void {
        this.expand = !this.expand;
        this.tabGroupToggle.emit({ id: this.id, expand: this.expand });
    }
}
