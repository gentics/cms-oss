import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { CmsFormElementBO, FORM_ELEMENT_MIME_TYPE_TYPE } from '@gentics/cms-models';
import { CmsFormType } from '@gentics/cms-models';
import { BehaviorSubject, combineLatest, Observable, Subject } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';
import { GTX_FORM_EDITOR_ANIMATIONS } from '../../animations/form-editor.animations';
import { FormEditorService } from '../../providers';

@Component({
    selector: 'gtx-form-editor-menu',
    templateUrl: './form-editor-menu.component.html',
    styleUrls: ['./form-editor-menu.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: GTX_FORM_EDITOR_ANIMATIONS,
})
export class FormEditorMenuComponent implements AfterViewInit, OnDestroy, OnInit {

    menuElements$: Observable<CmsFormElementBO[]>;

    @ViewChild('inputSearch', { read: ElementRef })
    inputSearch: ElementRef;

    searchTerm: string;
    private searchTerm$ = new BehaviorSubject<string>(null);

    private formType$ = new BehaviorSubject<CmsFormType>(null);

    private destroyed$ = new Subject<void>();

    @Input()
    set formType(v: CmsFormType) {
        this.formType$.next(v);
    }

    @Output()
    selected = new EventEmitter<CmsFormElementBO>();

    constructor(private formEditorService: FormEditorService) { }

    ngOnInit(): void {

        this.menuElements$ = combineLatest([
            this.formEditorService.activeUiLanguageCode$,
            this.formType$.pipe(
                filter((formType: CmsFormType) => !!formType),
                switchMap((formType: CmsFormType) => this.formEditorService.getFormElements$(formType)),
            ),
            this.searchTerm$,
        ]).pipe(
            map(([activeUiLanguageCode, formElements, searchTerm]) => {
                if (!searchTerm) {
                    return formElements;
                }
                const pattern = new RegExp(searchTerm, 'gi');
                return formElements.filter(el => pattern.test(el.type_label_i18n_ui[activeUiLanguageCode]));
            }),
        );
    }

    ngAfterViewInit(): void {
        if (!!this.inputSearch) {
            setTimeout(() => this.inputSearch.nativeElement.focus(), 1000);
        }
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    identify(index: number, element: CmsFormElementBO): string {
        return element.type;
    }

    onElementClick(event: MouseEvent, element: CmsFormElementBO): void {
        this.selected.emit(element);
    }

    onMenuFilterChange(searchTerm: string): void {
        this.searchTerm$.next(searchTerm);
    }

    onElementDragStart(event: DragEvent, element: CmsFormElementBO): void {
        // menu items must not be dropped anywhere else than in the Editor UI, thus, we only use our custom type
        event.dataTransfer.setData(`${FORM_ELEMENT_MIME_TYPE_TYPE}/${element.type}`, JSON.stringify({
            element: element,
            formId: '',
            parentContainerId: '',
            index: -1,
        }));
        event.dataTransfer.effectAllowed = 'copy';
    }
}
