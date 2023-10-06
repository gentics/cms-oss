import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { GCNAlohaPlugin, GCNTags } from '@gentics/aloha-models';
import { ConstructCategory, TagPartType, TagType } from '@gentics/cms-models';
import { DropdownListComponent } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { Subscription } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

interface GroupedConstructs {
    id: number;
    label?: string;
    order?: number;
    constructs: TagType[];
}

const UNCATEGORIZED_CONSTRUCTS_ID = -1;

@Component({
    selector: 'gtx-construct-controls',
    templateUrl: './construct-controls.component.html',
    styleUrls: ['./construct-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructControlsComponent extends BaseControlsComponent implements OnInit, OnChanges, OnDestroy {

    public readonly UNCATEGORIZED_CONSTRUCTS_ID = UNCATEGORIZED_CONSTRUCTS_ID;

    /** All constructs which exist. */
    @Input()
    public constructs: TagType[] = [];

    /** All construct-categories which exist. */
    @Input()
    public categories: ConstructCategory[] = [];

    @ViewChild('dropdown', { static: false })
    public dropdown: DropdownListComponent;

    /** The text the user filtered/searched for in the search bar. */
    public filterText: string;

    /** If any selection/editable is active, so we can insert constructs there. */
    public active = false;

    /** Constructs which are available to be inseted. */
    public availableConstructs: TagType[] = [];
    /** Constructs without a category set. */
    public noCategroyConstructs: TagType[] = [];
    /** `availableConstructs`, but grouped by the category. */
    public groups: GroupedConstructs[] = [];

    /** Ids of all the constructs which were used for the previous grouping. Used for caching */
    protected previousConstructIds: number[] = [];
    /** Previous search term. Used for caching */
    protected previousHaystack = '';

    public gcnPlugin: GCNAlohaPlugin;
    public gcnTags: GCNTags;

    protected subscriptions: Subscription[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        protected integration: AlohaIntegrationService,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        this.subscriptions.push(this.integration.gcnPlugin$.pipe(distinctUntilChanged()).subscribe(plugin => {
            this.gcnPlugin = plugin;
            if (this.gcnPlugin) {
                this.gcnTags = this.safeRequire('gcn/gcn-tags');
            } else {
                this.gcnTags = null;
            }
            this.updateAvailableConstructs();
            this.changeDetector.markForCheck();
        }));
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.constructs || changes.categories) {
            this.updateAvailableConstructs();
        }
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public updateFilterText(text: string): void {
        this.filterText = text;
        this.updateGroups();
    }

    public insertConstructIntoPage(construct: TagType): void {
        if (!this.gcnPlugin) {
            return;
        }
        // Tags dependency simply don't want to properly load sometimes for whatever reason
        if (!this.gcnTags) {
            this.gcnTags = this.safeRequire('gcn/gcn-tags');
        }
        if (!this.gcnTags) {
            return;
        }

        this.gcnPlugin.createTag(construct.id, true, (html, tag, data) => {
            this.gcnPlugin.handleBlock(data, true, () => {
                this.gcnTags.decorate(tag, data);

                const editableParts = construct.parts
                    // Ignore template and velocity parts in all cases
                    .filter(part => part.keyword !== 'template' && part.typeId !== TagPartType.Velocity)
                    // Only check for parts which are editable and are shown in the editor
                    .filter(part => part.editable && !part.hideInEditor);

                if (editableParts.length > 0 || (construct.externalEditorUrl || '').trim().length > 0) {
                    // eslint-disable-next-line no-underscore-dangle
                    this.gcnPlugin.openTagFill(tag._data.id, tag._chain._data.id);
                }
            }, html);
        });
    }

    public openDropdown(): void {
        if (!this.dropdown || this.availableConstructs.length === 0) {
            return;
        }
        this.dropdown.openDropdown(true);
    }

    protected selectionOrEditableChanged(): void {
        this.updateAvailableConstructs();
        this.active = this.range && this.aloha.activeEditable?.obj != null;
    }

    protected updateAvailableConstructs(): void {
        let whitelist: string[] | null = this.gcnPlugin?.settings?.config?.tagtypeWhitelist;
        const elem = this.aloha?.activeEditable?.obj?.get?.(0);

        // Check for whitelists which are more specific
        if (elem != null) {
            const elementConfig = Object.entries(this.gcnPlugin?.settings?.editables || {})
                .find(([query]) => elem.matches(query))?.[1];

            if (elementConfig != null) {
                whitelist = elementConfig?.tagtypeWhitelist ?? whitelist;
            }
        }

        this.availableConstructs = this.constructs
            // Only allow constructs which can actually be added
            .filter(construct => construct.visibleInMenu && construct.mayBeSubtag)
            // The "magiclink" construct (aloha-link) has to be removed all the time
            .filter(construct => this.gcnPlugin?.settings?.magiclinkconstruct !== construct.id)
            // Then filter out all which aren't in the whitelist (if one is defined)
            .filter(construct => whitelist == null || (whitelist.length !== 0 && whitelist.includes(construct.keyword)))
            // Sort them by name to be nicely displayed
            .sort((a, b) => a.name.localeCompare(b.name));


        this.noCategroyConstructs = this.availableConstructs
            .filter(con => con.categoryId == null);

        this.updateGroups();
    }

    protected updateGroups(): void {
        const haystack = (this.filterText || '').toLocaleLowerCase();
        const newIds = this.availableConstructs.map(c => c.id);

        // Nothing has changed in the constructs or in the term, so we don't need to rebuild the groups
        if (isEqual(this.previousConstructIds, newIds) && this.previousHaystack === haystack) {
            return;
        }

        this.previousConstructIds = newIds;
        this.previousHaystack = haystack;

        const constructMap: Record<number, TagType> = this.availableConstructs
            // Filter out all constructs which do not match the current search/filtering.
            // Only do this for the grouped constructs, as these are shown in the dropdown.
            .filter(construct => haystack.length > 1
                ? construct.name.toLocaleLowerCase().includes(haystack)
                || construct.keyword.toLocaleLowerCase().includes(haystack)
                : true,
            )
            .reduce((agg, construct) => {
                agg[construct.id] = construct;
                return agg;
            }, {});

        const groups: GroupedConstructs[] = this.categories.map(category => {
            // Filter out all constructs which aren't allowed
            const allowed: TagType[] = Object.values(category.constructs).filter(construct => {
                const found = constructMap[construct.id] != null;
                // Delete for later lookup
                delete constructMap[construct.id];
                return found;
            });

            if (allowed.length === 0) {
                return null;
            }

            return {
                id: category.id,
                label: category.name,
                constructs: allowed.sort((a, b) => a.name.localeCompare(b.name)),
            };
        }).filter(group => group != null);

        const uncategorized = Object.values(constructMap);

        if (uncategorized.length > 0) {
            groups.unshift({
                id: UNCATEGORIZED_CONSTRUCTS_ID,
                order: -1,
                constructs: uncategorized.sort((a, b) => a.name.localeCompare(b.name)),
            });
        }

        this.groups = groups.sort((a, b) => {
            if (typeof a.order === 'number' && typeof b.order === 'number') {
                return a.order - b.order;
            }
            if (typeof a.order === 'number') {
                return -1;
            }
            return 1;
        });

        if (this.dropdown && this.dropdown.isOpen) {
            this.dropdown.resize();
        }
    }
}
