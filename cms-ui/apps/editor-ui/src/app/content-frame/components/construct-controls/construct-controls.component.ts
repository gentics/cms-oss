import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { ConstructCategory, TagType } from '@gentics/cms-models';
import { DropdownListComponent } from '@gentics/ui-core';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

interface GroupedConstructs {
    id: number;
    label: string;
    order?: number;
    constructs: TagType[];
}

const UNCATEGORIZED_CONSTRUCTS_ID = -1;
const UNCATEGORIZED_CONSTRUCTS_LABEL = 'editor.construct_no_category';

@Component({
    selector: 'gtx-construct-controls',
    templateUrl: './construct-controls.component.html',
    styleUrls: ['./construct-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructControlsComponent extends BaseControlsComponent implements OnChanges {

    public readonly UNCATEGORIZED_CONSTRUCTS_ID = UNCATEGORIZED_CONSTRUCTS_ID;
    public readonly UNCATEGORIZED_CONSTRUCTS_LABEL = UNCATEGORIZED_CONSTRUCTS_LABEL;

    @Input()
    public constructs: TagType[] = [];

    @Input()
    public categories: ConstructCategory[] = [];

    @ViewChild('dropdown', { static: true })
    public dropdown: DropdownListComponent;

    public filterText: string;

    public availableConstructs: TagType[] = [];
    public groups: GroupedConstructs[] = [];

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.constructs || changes.categories || changes.range || changes.settings) {
            this.updateConstructs();
        }
    }

    public updateFilterText(text: string): void {
        this.filterText = text;
        this.updateConstructs();
    }

    public updateConstructs(): void {
        const haystack = (this.filterText || '').toLocaleLowerCase();
        this.availableConstructs = this.constructs
            .filter(construct => construct.visibleInMenu)
            .sort((a, b) => a.name.localeCompare(b.name));
        // TODO: Apply the correct white-/blacklist filtering for the current element

        const constructMap: Record<number, TagType> = this.availableConstructs
            // Filter out all constructs which do not match the current search/filtering
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
                label: UNCATEGORIZED_CONSTRUCTS_LABEL,
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

    public openDropdown(): void {
        if (!this.dropdown || this.availableConstructs.length === 0) {
            return;
        }
        this.dropdown.openDropdown(true);
    }
}
