import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TagType, ConstructCategory } from '@gentics/cms-models';
import { BaseControlsComponent } from '../base-controls/base-controls.component';

interface GroupedConstructs {
    id: number;
    label: string;
    order?: number;
    constructs: TagType[];
}

const DRAG_TYPE = 'x-application/gtx-aloha';
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

    public filterText: string;

    public groups: GroupedConstructs[] = [];

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.constructs || changes.categories || changes.selectedElement || changes.settings) {
            this.updateConstructs();
        }
    }

    public updateFilterText(text: string): void {
        this.filterText = text;
        this.updateConstructs();
    }

    public updateConstructs(): void {
        const haystack = (this.filterText || '').toLocaleLowerCase();
        const constructMap: Record<number, TagType> = this.constructs
            .filter(construct => construct.visibleInMenu)
            // TODO: Apply the correct white-/blacklist filtering for the current element
            // .filter(construct => true)
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
    }

    public setupDragOnElement(event: DragEvent, construct: TagType): void {
        event.dataTransfer.setData(DRAG_TYPE, JSON.stringify({
            id: construct.id,
            keyword: construct.keyword,
            name: construct.name,
        }));
        event.dataTransfer.effectAllowed = 'copy';
    }
}
