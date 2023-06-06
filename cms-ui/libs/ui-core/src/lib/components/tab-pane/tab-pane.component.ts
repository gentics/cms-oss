import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChild,
    EventEmitter,
    Input,
    Output,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { TabContentDirective } from '../../directives/tab-content/tab-content.directive';
import { TabLabelDirective } from '../../directives/tab-label/tab-label.directive';
import { BaseComponent } from '../base-component/base.component';

/**
 * Tab Pane IDs need to be unique across components, so this counter exists outside of
 * the component definition.
 */
let uniqueTabPaneId = 0;

/**
 * For documentation, see the GroupedTabs
 */
@Component({
    selector: 'gtx-tab-pane',
    exportAs: 'gtxTabPane',
    template: `
        <ng-template>
            <ng-content></ng-content>
        </ng-template>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TabPaneComponent extends BaseComponent {

    public readonly UNIQUE_ID = `gtx-tab-pane-${uniqueTabPaneId++}`;

    /** Plain text label for the tab, used when there is no template label. */
    @Input()
    public label = '';

    /** The unique ID of the tab pane. */
    @Input()
    public id: string;

    /**
     * Hide status icon for this tab
     */
    @Input()
    public hideStatusIcon = false;

    /**
     * Sets read-only state
     */
    @Input()
    public readonly = false;

    /**
     * Sets inactive state
     */
    @Input()
    public inactive = false;

    /**
     * When the tab is clicked, this event is fired with the tab id.
     */
    @Output()
    public select = new EventEmitter<string>();

    /** Content for the tab label given by `<ng-template gtx-tab-label>`. */
    @ContentChild(TabLabelDirective, { read: TemplateRef, static: true })
    public templateLabel: TabLabelDirective;

    /**
     * Template provided in the tab content that will be used if present, used to enable lazy-loading
     */
    @ContentChild(TabContentDirective, { read: TemplateRef, static: true })
    protected explicitContent: TabContentDirective;

    /** Template inside the TabPane view that contains an `<ng-content>`. */
    @ViewChild(TemplateRef, { static: true })
    protected implicitContent: TemplateRef<any>;

    /** If this tab-pane is currently the active one in the group/collection */
    public active = false;

    constructor(public changeDetector: ChangeDetectorRef) {
        super(changeDetector);
        this.booleanInputs.push('hideStatusIcon', 'readonly', 'inactive');
    }

    get content(): TabContentDirective | TemplateRef<any> {
        return this.explicitContent || this.implicitContent;
    }
}
