import { Route } from '@angular/router';
import { PAGES } from './common/page-list';

export const UI_CORE_DOCS_ROUTES: Route[] = [
    ...PAGES,
    { path: '', redirectTo: 'instructions', pathMatch: 'full' },
    { path: '**', redirectTo: 'instructions' },
];
