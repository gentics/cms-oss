import { ChangeDetectionStrategy, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Observable, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { FilterService } from '../../services/filter/filter.service';

interface EditorFilter {
    label: string;
    value: boolean;
}

@Component({
  selector: 'gtxct-filter-by-editor',
  templateUrl: './filter-by-editor.component.html',
  styleUrls: ['./filter-by-editor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FilterByEditorComponent implements OnInit {
    editors: EditorFilter[] = [
        { label: 'common.user_me', value: true },
        { label: 'common.user_all', value: false }
    ];

    editor$: Observable<EditorFilter>;

    /** Fired when a node has been selected. */
    @Output() editorSelected = new EventEmitter<string>();

    constructor(private filter: FilterService) { }

    ngOnInit(): void {
        this.editor$ = this.filter.options.events$.pipe(
            switchMap(options => of(this.editors.find(author => author.value === options.isEditor)))
        );
    }

    select(author: any): void {
        this.editorSelected.emit(author.value);
    }
}
