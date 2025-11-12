import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    HostListener,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Optional,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { WindowRef } from '@gentics/cms-components';
import { Item, Page, Template, TimeManagement } from '@gentics/cms-models';
import { SplitViewContainerComponent } from '@gentics/ui-core';
import { combineLatest, fromEventPattern, merge, Observable, of, Subject, Subscription } from 'rxjs';
import { debounceTime, map, mapTo, startWith } from 'rxjs/operators';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { getFormattedTimeMgmtValue } from '../../../core/providers/i18n/i18n-utils';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { I18nDatePipe } from '../../pipes/i18n-date/i18n-date.pipe';


const MAX_HEIGHT = '80px';

/**
 * Responsible for displaying the additional item properties as specified by the DisplayFieldSelector component.
 */
@Component({
    selector: 'list-item-details',
    templateUrl: './list-item-details.component.tpl.html',
    styleUrls: ['./list-item-details.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [I18nDatePipe],
    standalone: false
})
export class ListItemDetails implements OnInit, OnChanges, OnDestroy {
    @Input() fields: string[];
    @Input() item: Item;
    /**
     * If true, the component observes the size of the the splitViewContainer and
     * automatically applies "compact" styling when needed.
     */
    @Input() autoCompact = true;
    @Output() usageClick = new EventEmitter<Item>();
    @Input() isDeleted = false;

    @HostBinding('style.maxHeight')
    maxHeight = 'initial';

    @HostBinding('class.compact')
    compact = false;

    @ViewChild('fieldsWrapper', { static: true })
    fieldsWrapper: ElementRef;

    activeNodeId: number;

    private sizeSub: Subscription;
    private enterTimer: any;

    private fields$ = new Subject<string[]>();

    @HostListener('mouseenter')
    hoverHandler(): void {
        this.enterTimer = setTimeout(() => {
            this.maxHeight = this.autoCompact && this.compact ?
                this.fieldsWrapper.nativeElement.getBoundingClientRect().height + 'px' : 'initial';
            this.changeDetector.markForCheck();
        }, 200);
    }

    @HostListener('mouseleave')
    leaveHandler(): void {
        clearTimeout(this.enterTimer);
        this.maxHeight = this.autoCompact && this.compact ? MAX_HEIGHT : 'initial';
    }

    constructor(
        private entityResolver: EntityResolver,
        private changeDetector: ChangeDetectorRef,
        private windowRef: WindowRef,
        private i18n: I18nService,
        private i18nDate: I18nDatePipe,
        private folderActions: FolderActionsService,
        private state: ApplicationStateService,
        @Optional() private splitViewContainer?: SplitViewContainerComponent,
    ) { }

    ngOnInit(): void {
        if (this.splitViewContainer && this.autoCompact) {
            const splitViewData$ = merge(
                this.splitViewContainer.rightPanelOpened.pipe(
                    map(() => this.splitViewContainer.split),
                ),
                this.splitViewContainer.rightPanelClosed.pipe(
                    mapTo(100),
                ),
                this.splitViewContainer.splitDragEnd,
            ).pipe(
                startWith(this.splitViewContainer.rightPanelVisible ? this.splitViewContainer.split : 100),
            );

            this.sizeSub = combineLatest([
                splitViewData$,
                this.observeWindowResizing(),
                this.fields$.pipe(
                    startWith(this.fields),
                ),
            ]).subscribe(([split, widthInPixels])  => {
                if (widthInPixels > 1600 && split < 100) {
                    widthInPixels = widthInPixels * (split ) / 100;
                }
                return this.setCompactStatus(widthInPixels);
            });
        }

        // get current node
        this.activeNodeId = this.state.now.folder.activeNode;
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['fields']) {
            if (this.splitViewContainer && this.autoCompact) {
                this.fields$.next(this.fields);
                this.leaveHandler();
            }
        }
    }

    ngOnDestroy(): void {
        if (this.sizeSub) {
            this.sizeSub.unsubscribe();
        }
    }

    /**
     * Sets the "compact" and "maxHeight" bindings based on the splitPercentage, which is the percentange of
     * the SplitViewContainer taken up by the left (list) panel.
     */
    private setCompactStatus(widthInPixels: number): void {
        this.compact = this.fields != null && (
            7 <= this.fields.length && widthInPixels < 960 ||
            6 <= this.fields.length && widthInPixels < 840 ||
            5 <= this.fields.length && widthInPixels < 720 ||
            4 <= this.fields.length && widthInPixels < 600 ||
            3 <= this.fields.length && widthInPixels < 480
        );

        this.maxHeight = this.compact ? MAX_HEIGHT : 'initial';
        this.changeDetector.markForCheck();
    }

    /**
     * Displays the total usage of an item if it is available.
     */
    totalUsage(item: Item): string {
        if (item.usage) {
            return item.usage.total.toString(10);
        } else {
            return '-';
        }
    }

    usageClicked(e: Event, item: Item): void {
        e.preventDefault();
        e.stopPropagation();
        this.usageClick.emit(item);
    }

    getFormattedTimeMgmtValue(page: Page, field: keyof TimeManagement): Observable<string | boolean> {
        if (!this.activeNodeId) {
            return of(false);
        }
        return getFormattedTimeMgmtValue(page, field, this.activeNodeId, this.i18n, this.i18nDate, this.folderActions);
    }

    getUserName(item: Item): string {
        if (item.deleted && item.deleted.by && item.deleted.by.firstName && item.deleted.by.lastName) {
            return `${item.deleted.by.firstName} ${item.deleted.by.lastName}`;
        } else {
            return 'INVALID USER OBJECT';
        }
    }

    private observeWindowResizing(): Observable<number> {
        const window = this.windowRef.nativeWindow;

        return fromEventPattern(
            (handler: any) => window.addEventListener('resize', handler),
            (handler: any) => window.removeEventListener('resize', handler),
        ).pipe(
            map(ev => window.innerWidth),
            debounceTime(200),
            startWith(window.innerWidth),
        );
    }
}
