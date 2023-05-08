import { FolderContentsComponent } from './components/folder-contents/folder-contents.component';

export const listViewRoutes  = [
    {
        path: 'node/:nodeId/folder/:folderId',
        component: FolderContentsComponent,
    },
];
