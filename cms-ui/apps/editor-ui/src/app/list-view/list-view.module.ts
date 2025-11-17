import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ModalService } from '@gentics/ui-core';
import { SharedModule } from '../shared/shared.module';
import {
    ContentPackageSelectComponent,
    CreateFolderModalComponent,
    CreateFormModalComponent,
    CreatePageModalComponent,
    FolderContentsComponent,
    FolderStartPageComponent,
    GridItemComponent,
    ItemListComponent,
    ItemListHeaderComponent,
} from './components';
import { LIST_VIEW_ROUTES } from './list-view.routes';
import {
    AnyItemDeletedPipe,
    AnyItemInheritedPipe,
    AnyItemPublishedPipe,
    AnyPageUnpublishedPipe,
    FilterItemsPipe,
} from './pipes';
import { ListService } from './providers/list/list.service';

const COMPONENTS = [
    ContentPackageSelectComponent,
    FolderContentsComponent,
    FolderStartPageComponent,
    ItemListComponent,
    ItemListHeaderComponent,
    GridItemComponent,
    ItemListHeaderComponent,
];

const ENTRY_COMPONENTS = [
    CreateFolderModalComponent,
    CreateFormModalComponent,
    CreatePageModalComponent,
];

const PIPES = [
    AnyItemDeletedPipe,
    AnyItemInheritedPipe,
    AnyItemPublishedPipe,
    AnyPageUnpublishedPipe,
    FilterItemsPipe,
];

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
    declarations: [...COMPONENTS, ...ENTRY_COMPONENTS, ...PIPES],
    providers: PROVIDERS,
})
export class ListViewModule {}
