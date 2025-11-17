import { Injectable } from '@angular/core';
import { Folder, IndexById, Normalized } from '@gentics/cms-models';
import { combineLatest, Observable, of } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';
import { RecentItem } from '../../../common/models';
import { ApplicationStateService } from '../../../state';

export interface SuggestionItem extends RecentItem {

}

@Injectable({
    providedIn: 'root'
})
export class SuggestionSearchService {

    constructor(
        private state: ApplicationStateService,
    ) {}

    searchInState(searchTerm: number): Observable<SuggestionItem[]> {
        if (searchTerm === 0) {
            return of([]).pipe(take(1));
        }

        return combineLatest([
            this.state.select((state) => state.entities.folder).pipe(
                map((folders: IndexById<Folder<Normalized>>) => {
                    const folder = folders[searchTerm];

                    return folder ? {
                        id: folder.id,
                        type: 'folder',
                        mode: 'navigate',
                        nodeId: this.state.now.folder.activeNode,
                    } : null;
                }),
            ),
            this.state.select((state) => state.entities.page).pipe(
                map((pages) => {
                    const page = pages[searchTerm];

                    return page ? this.parsePreviewType({ id: page.id, type: 'page' }) : null;
                }),
            ),
            this.state.select((state) => state.entities.file).pipe(
                map((files) => {
                    const file = files[searchTerm];

                    return file ? this.parsePreviewType({ id: file.id, type: 'file' }) : null;
                }),
            ),
            this.state.select((state) => state.entities.image).pipe(
                map((images) => {
                    const image = images[searchTerm];

                    return image ? this.parsePreviewType({ id: image.id, type: 'image' }) : null;
                }),
            ),
        ]).pipe(
            switchMap(([folder, page, file, image]: [SuggestionItem, SuggestionItem, SuggestionItem, SuggestionItem]) => {
                return of([folder, page, file, image].filter(n => n));
            }),
        )
    }

    private parsePreviewType({ id, type }: { id: number, type: string }): SuggestionItem {
        return {
            id,
            type,
            mode: 'preview',
            nodeId: this.state.now.folder.activeNode,
        } as SuggestionItem;
    }
}
