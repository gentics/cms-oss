import {
    animate,
    animateChild,
    query,
    style,
    transition,
    trigger,
} from '@angular/animations';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
} from '@angular/core';
import { Language, Page, StagedItemsMap } from '@gentics/cms-models';
import { BaseComponent, ChangesOf } from '@gentics/ui-core';
import {
    FolderPermissionData,
    ItemLanguageClickEvent,
    ItemListRowMode,
    LanguageState,
    StageableItem,
    UIMode,
} from '../../../common/models';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { ApplicationStateService } from '../../../state';
import { PublishableStateUtil } from '../../util/entity-states';

interface VariantState extends Language, LanguageState {
    pageId: number;
    globalId: string;
}

/**
 * A component which displays the available languages for a page and (optionally) a page's status icons.
 */
@Component({
    selector: 'page-language-indicator',
    templateUrl: './page-language-indicator.component.html',
    styleUrls: ['./page-language-indicator.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('animNgForParent', [
            transition(':enter, :leave', [
                query('@animNgForChild', [animateChild()], { optional: true }),
            ]),
        ]),
        trigger('animNgForChild', [
            transition('void => *', [
                style({
                    "opacity": 0,
                    "width": '0',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                }),
                animate(
                    '0.2s ease-in-out',
                    style({
                        "opacity": 1,
                        "width": '*',
                        'padding-left': '*',
                        'padding-right': '*',
                        'margin-left': '*',
                        'margin-right': '*',
                    }),
                ),
            ]),
            transition('* => void', [
                style({
                    "opacity": 1,
                    "width": '*',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                }),
                animate(
                    '0.2s ease-in-out',
                    style({
                        "opacity": 0,
                        "width": '0',
                        'padding-left': '*',
                        'padding-right': '*',
                        'margin-left': '*',
                        'margin-right': '*',
                    }),
                ),
            ]),
        ]),
    ],
    standalone: false,
})
export class PageLanguageIndicatorComponent
    extends BaseComponent
    implements OnChanges {
    public readonly ItemListRowMode = ItemListRowMode;
    public readonly UIMode = UIMode;

    @Input({ required: true })
    public languages: Language[];

    @Input()
    public activeLanguage: Language;

    @Input({ required: true })
    public page: Page;

    @Input({ required: true })
    public permissions: FolderPermissionData;

    @Input()
    public mode: ItemListRowMode;

    @Input({ required: true })
    public uiMode: UIMode;

    @Input()
    public stagingMap: StagedItemsMap;

    @Input()
    public displayStatusInfo: boolean;

    @Input()
    public displayDeleted: boolean;

    @Input()
    public expandByDefault: boolean;

    /** Emits if an action from a langauge icon has been clicked */
    @Output()
    public languageClick = new EventEmitter<ItemLanguageClickEvent<Page>>();

    /** Emits if an lang icon is clicked */
    @Output()
    public languageIconClick = new EventEmitter<{
        item: Page;
        language: Language;
    }>();

    public hasUntranslated: boolean;
    public expanded = false;

    public inCurrentLanguage: boolean;
    public variants: VariantState[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private contextMenu: ContextMenuOperationsService,
    ) {
        super(changeDetector);
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.expandByDefault) {
            this.expanded = this.expandByDefault;
        }

        if (changes.activeLanguage || changes.page) {
            this.inCurrentLanguage = this.page != null
              && this.activeLanguage != null
              && this.activeLanguage.code === this.page.language;
        }

        if (changes.expandByDefault || changes.page || changes.languages || changes.stagingMap) {
            this.updateVariants();
        }
    }

    identifyVariant(index: number, variant: VariantState): string {
        return variant.code;
    }

    languageClicked(
        variant: VariantState,
        compare: boolean = false,
        source: boolean = true,
        restore: boolean = false,
    ): void {
        this.languageClick.emit({
            item: this.page,
            language: {
                id: variant.id,
                code: variant.code,
                name: variant.name,
            },
            compare,
            source,
            restore,
        });
    }

    /**
     * The "show more" ellipses or "show less" arrow was clicked.
     */
    toggleExpand(): void {
        this.expanded = !this.expanded;
        this.updateVariants();
    }

    onIconClicked(variant: VariantState): void {
        this.languageIconClick.emit({
            item: this.page,
            language: {
                id: variant.id,
                code: variant.code,
                name: variant.name,
            },
        });
    }

    onStageLanguageClick(variant: VariantState): void {
        const item = {
            type: 'page',
            id: variant.pageId,
            globalId: variant.globalId,
            language: variant.code,
        } as StageableItem;

        if (this.stagingMap?.[item.globalId]?.included) {
            this.contextMenu.unstageItemFromCurrentPackage(item);
        } else {
            this.contextMenu.stageItemToCurrentPackage(item);
        }
    }

    updateVariants(): void {
        this.hasUntranslated = false;
        this.variants = this.languages.flatMap((lang) => {
            let variantPage: Page | null = null;

            if (this.page.languageVariants) {
                if (Array.isArray(this.page.languageVariants)) {
                    variantPage = (this.page.languageVariants as number[])
                        .map((variantId) => this.appState.now.entities.page[variantId])
                        .find((variant) => variant != null && variant.language === lang.code);
                } else {
                    const tmpVal = this.page.languageVariants[lang.id];
                    if (typeof tmpVal === 'number') {
                        variantPage = this.appState.now.entities.page[tmpVal];
                    }
                }
            }

            if (variantPage == null) {
                this.hasUntranslated = true;
                if (!this.expanded) {
                    return [];
                }
            }

            return [{
                code: lang.code,
                id: lang.id,
                name: lang.name,

                pageId: variantPage?.id,
                globalId: variantPage?.globalId,

                available: variantPage != null,
                deleted:
                    variantPage != null
                    && PublishableStateUtil.stateDeleted(variantPage),
                inherited:
                    variantPage != null
                    && PublishableStateUtil.stateInherited(variantPage),
                localized:
                    variantPage != null
                    && PublishableStateUtil.stateLocalized(variantPage),
                modified:
                    variantPage != null
                    && PublishableStateUtil.stateModified(variantPage),
                planned:
                    variantPage != null
                    && PublishableStateUtil.statePlanned(variantPage),
                published:
                    variantPage != null
                    && PublishableStateUtil.statePublished(variantPage),
                queued:
                    variantPage != null
                    && PublishableStateUtil.stateInQueue(variantPage),
                staged: variantPage != null
                  && this.stagingMap?.[variantPage.globalId]?.included,
            }];
        });
    }
}
