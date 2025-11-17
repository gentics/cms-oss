import { FolderContentsComponent } from './components/folder-contents/folder-contents.component';

export const LIST_VIEW_ROUTES = [
    {
        path: 'node/:nodeId/folder/:folderId',
        component: FolderContentsComponent,
    },
];
