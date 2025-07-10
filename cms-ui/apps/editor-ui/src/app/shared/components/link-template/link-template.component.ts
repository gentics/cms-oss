import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { Folder, Node, Raw, Template } from '@gentics/cms-models';
import {
    BehaviorSubject,
    Observable,
    Subject,
    combineLatest,
    of,
} from 'rxjs';
import {
    debounceTime,
    filter,
    map,
    mergeMap,
    shareReplay,
    switchMap,
    takeUntil,
    tap,
} from 'rxjs/operators';
import { FolderActionsService } from '../../../state';


@Component({
    selector: 'link-template',
    templateUrl: './link-template.component.html',
    styleUrls: ['./link-template.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LinkTemplateComponent implements OnDestroy, OnInit {

    /** If 'edit' linked templates will be displayed, if 'create' all available templates will be listed to select from */
    @Input() mode: 'create' | 'edit' = 'edit';

    /** Current node */
    @Input() node: Node;

    /** Current folder */
    @Input() folder: Folder;

    /** If set to TRUE, this component won't display a searchbar and will search via input `searchTerm` */
    @Input() searchBarHidden = false;

    /** Search term user has entered in search bar */
    @Input() set searchTerm(v: string) {
        this.searchTerm$.next(v);
    }
    @Output() searchTermChange = new EventEmitter<string>();
    searchTerm$ = new BehaviorSubject<string>('');

    /** All templates that will be linked to current folder after user interaction */
    @Output() templatesLinked = new EventEmitter<Template<Raw>[]>();

    /** Get notified by parent if isInProgress status shall be indicated */
    @Input() set isInProgress(v: boolean) {
        this.isInProgressExternal$.next(v);
    }
    /** Notify parent if isInProgress data */
    @Output() isInProgressChange = new EventEmitter<boolean>();
    isInProgressExternal$ = new BehaviorSubject<boolean>(false);

    /** TRUE if internal component data is loading */
    isInProgressInternal$: Observable<boolean>;
    fetchingNodeTemplates$ = new BehaviorSubject<boolean>(true);
    fetchingfolderTemplates$ = new BehaviorSubject<boolean>(true);

    /** All templates linked to current node */
    nodeTemplates$: Observable<Template<Raw>[]>;
    nodeTemplates: Template<Raw>[] = [];

    /** All templates linked to current folder */
    folderTemplates$: Observable<Template<Raw>[]>;
    folderTemplates: Template<Raw>[] = [];

    /** TRUE if no templates are linked to current node */
    noTemplatesLinked$: Observable<boolean>;

    itemType = 'template';

    private destroy$ = new Subject<void>();

    constructor(
        private folderActions: FolderActionsService,
    ) { }

    ngOnInit(): void {

        this.isInProgressInternal$ = combineLatest([
            this.fetchingNodeTemplates$,
            this.fetchingfolderTemplates$,
        ]).pipe(
            map(([fetchingNodeTemplates, fetchingfolderTemplates]) => fetchingNodeTemplates || fetchingfolderTemplates),
            tap(isInProgressInternal => this.isInProgressChange.emit(isInProgressInternal)),
        );

        this.nodeTemplates$ = this.searchTerm$.pipe(
            tap(() => this.fetchingNodeTemplates$.next(true)),
            // debounce user input
            debounceTime(200),
            // fetch node templates sorted by name
            switchMap((term: string) => {
                return this.folderActions.getAllTemplatesOfNode(this.node.id, term, 'name').pipe(
                    filter((templates: Template<Raw>[]) => Array.isArray(templates)),
                    tap(() => this.fetchingNodeTemplates$.next(false)),
                );
            }),
            // provide value while debounce
            shareReplay(1),
        );

        this.folderTemplates$ = this.nodeTemplates$.pipe(
            mergeMap((nodeTemplates) => {
                this.fetchingfolderTemplates$.next(true);
                return (this.mode === 'edit' ? this.folderActions.getTemplatesRaw(this.node.id, this.folder.id, true).pipe(
                    map((templates: void | Template<Raw>[]) => {
                        this.fetchingfolderTemplates$.next(false);
                        if (Array.isArray(templates) && templates.length > 0) {
                            return templates;
                        } else {
                            return [];
                        }
                    }),
                ) : of([])).pipe(
                    map((folderTemplates: Template<Raw>[]) => [nodeTemplates, folderTemplates]),
                );
            }),
            switchMap(([nodeTemplates, folderTemplates]: [Template<Raw>[], Template<Raw>[]]) => {
                // remove those folder templates which got substracted from node templates by filtering
                if (Array.isArray(folderTemplates) && folderTemplates.length > 0 && Array.isArray(nodeTemplates) && nodeTemplates.length > 0) {
                    return of(folderTemplates.filter(folderTemplate => nodeTemplates.some(nodeTemplate => nodeTemplate.id === folderTemplate.id)));
                } else {
                    return [];
                }
            }),
            tap(() => this.fetchingfolderTemplates$.next(false)),
        );

        this.nodeTemplates$.pipe(
            takeUntil(this.destroy$),
        ).subscribe(templates => this.nodeTemplates = templates);

        this.folderTemplates$.pipe(
            takeUntil(this.destroy$),
        ).subscribe(templates => {
            this.folderTemplates = templates;
            this.templatesLinked.emit(templates);
            this.searchTermChange.emit(this.searchTerm$.getValue());
        });

        this.noTemplatesLinked$ = this.nodeTemplates$.pipe(
            map(templates => templates.length === 0),
        );
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    /** @returns TRUE if template is linked to folder */
    templateIsLinked(templateId: number): boolean {
        return this.folderTemplates.map(folderTemplate => folderTemplate.id).includes(templateId);
    }

    /** On template row checkbox check */
    toggleSelect(check: boolean, templateId: number): void {
        if (check) {
            const modifyTemplate = this.nodeTemplates.find(nodeTemplate => nodeTemplate.id === templateId);
            this.folderTemplates.push(modifyTemplate);
        } else {
            this.folderTemplates = this.folderTemplates.filter(folderTemplate => folderTemplate.id !== templateId);
        }
        this.templatesLinked.emit(this.folderTemplates);
        this.searchTermChange.emit(this.searchTerm$.getValue());
    }

    toggleSelectAll(check: boolean): void {
        if (check) {
            this.folderTemplates = [...this.nodeTemplates];
        } else {
            this.folderTemplates = [];
        }
        this.templatesLinked.emit(this.folderTemplates);
        this.searchTermChange.emit(this.searchTerm$.getValue());
    }

}
