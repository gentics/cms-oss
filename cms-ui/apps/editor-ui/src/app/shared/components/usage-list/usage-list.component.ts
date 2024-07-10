import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Item, ItemType, Language, Normalized, Page, Template, UsageType } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService } from '../../../state';

@Component({
    selector: 'usage-list',
    templateUrl: './usage-list.tpl.html',
    styleUrls: ['./usage-list.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageListComponent implements OnInit {

    @Input()
    public items: Item<Normalized>[];

    @Input()
    public type: UsageType;

    @Output()
    public itemClick = new EventEmitter<Item>();

    languages$: Observable<Language[]>;
    activeNodeId$: Observable<number>;
    icon: string;

    constructor(
        private entityResolver: EntityResolver,
        private appState: ApplicationStateService,
    ) {}

    ngOnInit(): void {
        const itemType = this.usageToItemType(this.type);
        this.icon = iconForItemType(itemType);
        this.languages$ = this.appState.select(state => state.folder.activeNodeLanguages.list).pipe(
            map(list => list.map(id => this.entityResolver.getLanguage(id))),
        );
        this.activeNodeId$ = this.appState.select(state => state.folder.activeNode);
    }

    private usageToItemType(usageType: UsageType): ItemType {
        switch (usageType) {
            case 'variant':
            case 'tag':
                return 'page';
            default:
                return usageType as ItemType;
        }
    }

    /**
     * Because we group pages, in order to get an accurate count, we need to look inside the
     * languageVariants object.
     */
    private getItemCount(): number {
        if (this.usageToItemType(this.type) === 'page') {
            return this.items.reduce((total: number, page: Page<Normalized>) => {
                const variants = page.languageVariants ? Object.keys(page.languageVariants).length : 1;
                return total + variants;
            }, 0);
        } else {
            return this.items.length;
        }
    }

    /**
     * If a language variant button is clicked, we need to get the correct page variant to emit.
     */
    private languageClicked(page: Page<Normalized>, languageId: number): void {
        if (page.contentGroupId === languageId) {
            this.itemClick.emit(page);
        } else {
            const variantId = page.languageVariants[languageId];
            const variant = this.entityResolver.getPage(variantId);
            this.itemClick.emit(variant);
        }
    }

    private getLanguages(page: Page): Language[] {
        if (page.type !== 'page' || !page.languageVariants || Object.keys(page.languageVariants).length < 1) {
            return [];
        }
        const languages = Object.keys(page.languageVariants)
            .map(id => this.entityResolver.getLanguage(+id));
        // move the current page language to the front of the array.
        const currentLanguageIndex = languages.map(l => l.id).indexOf(page.contentGroupId);
        languages.splice(0, 0, languages.splice(currentLanguageIndex, 1)[0]);
        return languages;
    }

    private getTemplate(templateId: number): Template {
        return this.entityResolver.getTemplate(templateId);
    }
}
