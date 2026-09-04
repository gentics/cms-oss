import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ModalService } from '@gentics/ui-core';
import { SharedModule } from '../shared/shared.module';
import {
    ContentPackageSelectComponent,
    FolderContentsComponent,
    FolderStartPageComponent,
    GridItemComponent,
    ItemListComponent,
} from './components';
import { LIST_VIEW_ROUTES } from './list-view.routes';
import { ListService } from './providers/list/list.service';

const COMPONENTS = [
    ContentPackageSelectComponent,
    FolderContentsComponent,
    FolderStartPageComponent,
    ItemListComponent,
    GridItemComponent,
];

const PIPES = [];

const PROVIDERS = [
    ListService,
    ModalService,
];

@NgModule({
    imports: [
        SharedModule,
        RouterModule.forChild(LIST_VIEW_ROUTES),
    ],
    exports: [],
    declarations: [...COMPONENTS, ...PIPES],
    providers: PROVIDERS,
})
export class ListViewModule {}
