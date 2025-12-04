import { ContentFrameComponent } from './components/content-frame/content-frame.component';
import { ContentFrameGuard } from './guards/content-frame-guard';

export const CONTENT_FRAME_ROUTES = [
    {
        path: ':nodeId/:type/:itemId/:editMode',
        component: ContentFrameComponent,
        canDeactivate: [ContentFrameGuard],
    },
];
