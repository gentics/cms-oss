import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { Folder, Node, Page, Template } from '@gentics/cms-models';
import { IBreadcrumbLink } from '@gentics/ui-core';
import { Observable, Subject } from 'rxjs';
import { map, withLatestFrom } from 'rxjs/operators';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { ApplicationStateService } from '../../../state';
import { BreadcrumbsService } from '../../providers/breadcrumbs.service';

/**
 * Breadcrumb and node selector for the repository browser.
 */
@Component({
    selector: 'repository-browser-breadcrumb',
    templateUrl: './repository-browser-breadcrumbs.component.html',
    styleUrls: ['./repository-browser-breadcrumbs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RepositoryBrowserBreadcrumbs implements OnInit, OnChanges {

    @Input()
    parents: (Folder | Page | Template)[] = [];

    @Input()
    nodes: Node[];

    @Input()
    hasFavourites: boolean;

    @Input()
    canChangeNode = true;

    @Output()
    changeNode = new EventEmitter<Node | 'favourites'>();

    @Output()
    changeParent = new EventEmitter<Node | Folder | Page | Template>();

    multilineExpanded$: Observable<boolean>;
    breadcrumbs$: Observable<IBreadcrumbLink[]>;
    breadcrumbLinks$ = new Subject<IBreadcrumbLink[]>();

    constructor(
        private appState: ApplicationStateService,
        private breadcrumbsService: BreadcrumbsService,
        private changeDetector: ChangeDetectorRef,
        private userSettings: UserSettingsService,
    ) { }

    ngOnInit(): void {
        this.multilineExpanded$ = this.appState.select(state => state.ui.repositoryBrowserBreadcrumbsExpanded);
        this.breadcrumbs$ = this.breadcrumbLinks$.pipe(
            withLatestFrom(this.multilineExpanded$),
            map(([breadcrumbs, isMultilineExpanded]) => !isMultilineExpanded ? this.breadcrumbsService.addTooltip(breadcrumbs) : breadcrumbs),
        );
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.parents) {
            const breadcrumbLinks = (this.parents || []).map(parent => ({
                item: parent,
                text: parent.name,
            }));
            this.breadcrumbLinks$.next(breadcrumbLinks);
        }
    }

    expandedChanged(multilineExpanded: boolean): void {
        this.userSettings.setRepositoryBrowserBreadcrumbsExpanded(multilineExpanded);
        this.changeDetector.detectChanges();
    }
}
