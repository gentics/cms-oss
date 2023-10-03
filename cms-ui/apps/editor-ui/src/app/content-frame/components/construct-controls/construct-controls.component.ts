import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { GCNAlohaPlugin, GCNTags } from '@gentics/aloha-models';
import { ConstructCategory, TagPartType, TagType } from '@gentics/cms-models';
import { DropdownListComponent } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';
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
export class ConstructControlsComponent extends BaseControlsComponent implements OnInit, OnChanges, OnDestroy {

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
            this.updateConstructs();
            this.changeDetector.markForCheck();
        }));
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.constructs || changes.categories) {
            this.updateConstructs();
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public updateFilterText(text: string): void {
        this.filterText = text;
        this.updateConstructs();
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
                    .filter(part => part.keyword !== 'template' && part.typeId !== TagPartType.Velocity)
                    .filter(part => part.editable && !part.hideInEditor);

                if (editableParts.length > 0) {
                    // eslint-disable-next-line no-underscore-dangle
                    this.gcnPlugin.openTagFill(tag._data.id, tag._chain._data.id);
                }
            }, html);
        });
    }

    protected selectionOrEditableChanged(): void {
        this.updateConstructs();
    }

    public updateConstructs(): void {
        const haystack = (this.filterText || '').toLocaleLowerCase();
        let whitelist: string[] = [];
        const elem = this.aloha?.activeEditable?.obj?.get?.(0);

        if (this.gcnPlugin && elem) {
            whitelist = Object.entries(this.gcnPlugin?.settings?.editables || {}).find(([query]) => {
                return elem.matches(query);
            })?.[1]?.tagtypeWhitelist || [];
        }

        this.availableConstructs = this.constructs
            .filter(construct => construct.visibleInMenu && construct.mayBeSubtag)
            .filter(construct => {
                return whitelist == null || whitelist.length === 0 || whitelist.includes(construct.keyword);
            })
            .filter(construct => {
                return this.gcnPlugin?.settings?.magiclinkconstruct == null || this.gcnPlugin.settings.magiclinkconstruct !== construct.id;
            })
            .sort((a, b) => a.name.localeCompare(b.name));

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
