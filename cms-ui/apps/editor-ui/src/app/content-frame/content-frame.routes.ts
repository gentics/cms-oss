import {ContentFrame} from './components/content-frame/content-frame.component';
import {ContentFrameGuard} from './providers/guards/content-frame-guard';

export const contentFrameRoutes  = [
    {
        path: ':nodeId/:type/:itemId/:editMode',
        component: ContentFrame,
        canDeactivate: [ContentFrameGuard]
    }
];
