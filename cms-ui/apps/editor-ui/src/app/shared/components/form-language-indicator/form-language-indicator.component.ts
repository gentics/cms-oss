import { animate, animateChild, query, style, transition, trigger } from '@angular/animations';
import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { Form, Language, StagedItemsMap } from '@gentics/cms-models';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { PublishableStateUtil } from '../../util/entity-states';
import { FolderPermissionData, ItemLanguageClickEvent, ItemListRowMode, LanguageState, UIMode } from '../../../common/models';
import { BaseComponent, ChangesOf } from '@gentics/ui-core';

interface VariantState extends Language, LanguageState {}

/**
 * A component which displays the available languages for a form and (optionally) a form's status icons.
 */
@Component({
    selector: 'form-language-indicator',
    templateUrl: './form-language-indicator.component.html',
    styleUrls: ['./form-language-indicator.component.scss'],
    animations: [
        trigger('animNgForParent', [
            transition(':enter, :leave', [
                query('@animNgForChild', [
                    animateChild(),
                ]),
            ]),
        ]),
        trigger('animNgForChild', [
            transition('void => *', [
                style({
                    opacity: 0,
                    width: '0',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                }),
                animate('0.2s ease-in-out', style({
                    opacity: 1,
                    width: '*',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                })),
            ]),
            transition('* => void', [
                style({
                    opacity: 1,
                    width: '*',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                }),
                animate('0.2s ease-in-out', style({
                    opacity: 0,
                    width: '0',
                    'padding-left': '*',
                    'padding-right': '*',
                    'margin-left': '*',
                    'margin-right': '*',
                })),
            ]),
        ]),
    ],
    standalone: false,
})
export class FormLanguageIndicatorComponent
    extends BaseComponent
    implements OnChanges {

    public readonly ItemListRowMode = ItemListRowMode;
    public readonly UIMode = UIMode;

    @Input({ required: true })
    public languages: Language[];

    @Input()
    public activeLanguage: Language;

    @Input({ required: true })
    public form: Form;

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
    public languageClick = new EventEmitter<ItemLanguageClickEvent<Form>>();

    /** Emits if an lang icon is clicked */
    @Output()
    public languageIconClick = new EventEmitter<{
        item: Form;
        language: Language;
    }>();

    public hasUntranslated: boolean;
    public expanded = false;

    public inCurrentLanguage: boolean;
    public variants: VariantState[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        private contextMenu: ContextMenuOperationsService,
    ) {
        super(changeDetector);
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.expandByDefault) {
            this.expanded = this.expandByDefault;
        }

        if (changes.activeLanguage || changes.form) {
            this.inCurrentLanguage = this.form != null
              && this.activeLanguage != null
              && this.form.languages.includes(this.activeLanguage.code);
        }

        if (changes.expandByDefault || changes.form || changes.languages || changes.stagingMap) {
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
            item: this.form,
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
            item: this.form,
            language: {
                id: variant.id,
                code: variant.code,
                name: variant.name,
            },
        });
    }

    onStageLanguageClick(): void {
        if (this.stagingMap?.[this.form.globalId]?.included) {
            this.contextMenu.unstageItemFromCurrentPackage(this.form);
        } else {
            this.contextMenu.stageItemToCurrentPackage(this.form);
        }
    }

    updateVariants(): void {
        this.hasUntranslated = false;
        this.variants = this.languages.flatMap((lang) => {
            const available = this.form.languages.includes(lang.code);

            if (!available) {
                this.hasUntranslated = true;
                if (!this.expanded) {
                    return [];
                }
            }

            return [{
                code: lang.code,
                id: lang.id,
                name: lang.name,

                available: available,
                deleted:
                    this.form != null
                    && PublishableStateUtil.stateDeleted(this.form),
                inherited:
                    this.form != null
                    && PublishableStateUtil.stateInherited(this.form),
                localized:
                    this.form != null
                    && PublishableStateUtil.stateLocalized(this.form),
                modified:
                    this.form != null
                    && PublishableStateUtil.stateModified(this.form),
                planned:
                    this.form != null
                    && PublishableStateUtil.statePlanned(this.form),
                published:
                    this.form != null
                    && PublishableStateUtil.statePublished(this.form),
                queued:
                    this.form != null
                    && PublishableStateUtil.stateInQueue(this.form),
                staged: this.form != null
                  && this.stagingMap?.[this.form.globalId]?.included,
            }];
        });
    }
}
