import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
} from '@angular/core';
import { ContentPackageBO, IndexById } from '@gentics/cms-models';
import { Subscription } from 'rxjs';
import { ApplicationStateService } from '../../../state';

interface DisplayItem<T> {
    id: string;
    selected: boolean;
    value: T;
}

@Component({
    selector: 'gtx-content-package-list',
    templateUrl: './content-package-list.component.html',
    styleUrls: ['./content-package-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ContentPackageListComponent implements OnInit, OnChanges, OnDestroy {

    @Input()
    public selectable = false;

    @Input()
    public multiple = false;

    @Input()
    public selection: string | string[] = [];

    @Output()
    public selectionChange = new EventEmitter<string | string[]>();

    public allPackages: IndexById<ContentPackageBO> = {};
    public displayItems: DisplayItem<ContentPackageBO>[] = [];
    public page = 1;
    public perPage = 10;

    protected selectionList: string[] = [];
    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected state: ApplicationStateService,
    ) { }

    ngOnInit(): void {
        this.subscriptions.push(this.state.select(state => state.entities.contentPackage).subscribe(packages => {
            this.allPackages = packages;
            this.updateDisplayItems();
            this.changeDetector.markForCheck();
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.selection) {
            if (typeof this.selection === 'string') {
                this.selectionList = [this.selection];
            } else if (!Array.isArray(this.selection)) {
                this.selectionList = [];
            } else {
                this.selectionList = this.selection;
            }
            this.updateDisplayItems();
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public updateDisplayItems(): void {
        // Since there's no filtering, it's simply all packages
        this.displayItems = Object.values(this.allPackages).map((pkg: ContentPackageBO) => {
            return {
                id: pkg.name,
                selected: this.selectionList.includes(pkg.name),
                value: pkg,
            };
        });
    }

    public identify(item: DisplayItem<any>): string {
        return item.id;
    }

    public selectPackage(item: DisplayItem<ContentPackageBO>): void {
        if (!this.selectable) {
            return;
        }

        if (item.selected) {
            if (!this.multiple) {
                this.selectionChange.emit(null);
                return;
            }

            const clone = [...this.selectionList];
            clone.splice(clone.findIndex(pkg => pkg === item.id), 1);
            this.selectionChange.emit(clone);
            return;
        }

        if (!this.multiple) {
            this.selectionChange.emit(item.id);
            return;
        }

        this.selectionChange.emit([...this.selectionList, item.id]);
    }

    public updatePage(page: number): void {
        this.page = page;
    }
}
