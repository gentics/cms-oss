import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { TagEditorService } from '@editor-ui/app/tag-editor';
import { GCNAlohaPlugin, GCNTags } from '@gentics/cms-integration-api-models';
import { Construct, ConstructCategory } from '@gentics/cms-models';
import { DropdownListComponent, cancelEvent } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { AlohaEditable } from '@gentics/aloha-models';
import { getTagPartPropertyValue } from '../../../tag-editor/util/part-value';
import { AlohaGlobal } from '../../models/content-frame';

interface DisplayGroup {
    id: number;
    label: string;
    order?: number;
    constructs: Construct[];
}

const FAVOURITES_ID = -2;
const FAVOURITES_LABEL = 'editor.construct_favourites';
const UNCATEGORIZED_ID = -1;
const UNCATEGORIZED_LABEL = 'editor.construct_no_category';

@Component({
    selector: 'gtx-construct-controls',
    templateUrl: './construct-controls.component.html',
    styleUrls: ['./construct-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructControlsComponent implements OnChanges {

    public readonly FAVOURITES_ID = FAVOURITES_ID;
    public readonly FAVOURITES_LABEL = FAVOURITES_LABEL;
    public readonly UNCATEGORIZED_ID = UNCATEGORIZED_ID;
    public readonly UNCATEGORIZED_LABEL = UNCATEGORIZED_LABEL;

    @Input()
    public constructs: Construct[] = [];

    @Input()
    public categories: ConstructCategory[] = [];

    @Input()
    public favourites: string[] = [];

    @Input()
    public gcnPlugin: GCNAlohaPlugin;

    @Input()
    public alohaRef: AlohaGlobal;

    @Input()
    public editable: AlohaEditable;

    @Output()
    public favouritesChange = new EventEmitter<string[]>();

    public filterText = '';

    public availableConstructs: Construct[] = [];
    public favouriteConstructs: Construct[] = [];
    public displayGroups: DisplayGroup[] = [];

    /** Ids of all the constructs which were used for the previous grouping. Used for caching */
    protected previousConstructIds: number[] = [];
    /** Ids of all the categories which were used for previous grouping. USed for caching */
    protected previousCategories: number[] = [];
    /** Previous search term. Used for caching */
    protected previousHaystack = '';

    protected gcnTags: GCNTags;

    protected dropdownIsFavourites = false;
    protected currentlyOpenDropdown: DropdownListComponent | null;

    constructor(
        protected i18n: I18nService,
        protected tagEditor: TagEditorService,
    ) {}

    public ngOnChanges(changes: SimpleChanges): void {
        let didChange = false;
        if (changes.constructs || changes.categories) {
            this.updateAvailableConstructs();
            this.updateFavouriteConstructs();
            this.updateDisplayGroups();
            didChange = true;
        }

        if (!didChange && changes.favourites && !changes.favourites.firstChange) {
            this.updateFavouriteConstructs();
        }
    }

    public updateFilterText(text: string): void {
        this.filterText = text;
        this.updateDisplayGroups();
    }

    public toggleFavourite(construct: Construct, event?: Event): void {
        cancelEvent(event);

        const idx = this.favourites.indexOf(construct.keyword);
        if (idx > -1) {
            this.favouritesChange.emit([
                ...this.favourites.slice(0, idx),
                ...this.favourites.slice(idx + 1),
            ]);
        } else {
            this.favouritesChange.emit([
                ...this.favourites,
                construct.keyword,
            ]);
        }
    }

    public handleDropdownOpen(instance: DropdownListComponent, isFavourites: boolean): void {
        if (this.currentlyOpenDropdown?.isOpen) {
            this.currentlyOpenDropdown.closeDropdown();
        }
        this.currentlyOpenDropdown = instance;
        this.dropdownIsFavourites = isFavourites;
    }

    public insertConstruct(construct: Construct, event?: Event): void {
        cancelEvent(event);

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
                if (this.currentlyOpenDropdown?.isOpen) {
                    this.currentlyOpenDropdown.closeDropdown();
                    this.currentlyOpenDropdown = null;
                }

                if (construct.openEditorOnInsert) {
                    // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/no-unsafe-call
                    this.tagEditor.openTagEditor(tag._data, construct, tag.parent()._data).then(res => {
                        // Save the updated tag data into the page
                        Object.entries(res.tag.properties).forEach(([propName, propValue]) => {
                            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                            tag.part(propName, getTagPartPropertyValue(propValue));
                        });
                    });
                }
            }, html);
        });
    }

    protected safeRequire(dependency: string): any {
        if (!this.alohaRef) {
            return null;
        }
        try {
            return this.alohaRef.require(dependency);
        } catch (err) {
            console.warn(`Could not require aloha element "${dependency}"!`, err);
            return null;
        }
    }

    protected updateAvailableConstructs(): void {
        this.availableConstructs = this.constructs
            // Only allow constructs which can actually be added
            .filter(construct => construct.visibleInMenu && construct.mayBeSubtag)
            // The "magiclink" construct (aloha-link) has to be removed all the time
            .filter(construct => this.gcnPlugin?.settings?.magiclinkconstruct !== construct.id)
            // Sort them by name to be nicely displayed
            .sort((a, b) => a.name.localeCompare(b.name));
    }

    protected updateFavouriteConstructs(): void {
        this.favouriteConstructs = this.availableConstructs
            .filter(construct => (this.favourites || []).includes(construct.keyword));

        if (this.currentlyOpenDropdown != null && this.dropdownIsFavourites) {
            if (this.favourites.length === 0) {
                this.currentlyOpenDropdown.closeDropdown();
            } else {
                this.currentlyOpenDropdown.resize();
            }
        }
    }

    protected updateDisplayGroups(): void {
        const haystack = (this.filterText || '').toLocaleLowerCase();
        const newIds = this.availableConstructs.map(c => c.id);
        const catIds = this.categories.map(c => c.id);

        // Nothing has changed in the constructs or in the term, so we don't need to rebuild the groups
        if (
            isEqual(this.previousConstructIds, newIds)
            && isEqual(this.previousCategories, catIds)
            && this.previousHaystack === haystack
        ) {
            return;
        }

        this.previousConstructIds = newIds;
        this.previousCategories = catIds;
        this.previousHaystack = haystack;

        const constructMap: Record<number, Construct> = this.availableConstructs
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

        const groups: DisplayGroup[] = this.categories.map(category => {
            // Filter out all constructs which aren't allowed
            const allowed: Construct[] = Object.values(category.constructs).filter(construct => {
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
                id: UNCATEGORIZED_ID,
                label: this.i18n.translate(UNCATEGORIZED_LABEL),
                order: -1,
                constructs: uncategorized.sort((a, b) => a.name.localeCompare(b.name)),
            });
        }

        this.displayGroups = groups.sort((a, b) => {
            if (typeof a.order === 'number' && typeof b.order === 'number') {
                return a.order - b.order;
            }
            if (typeof a.order === 'number') {
                return -1;
            }
            return 1;
        });
    }
}
