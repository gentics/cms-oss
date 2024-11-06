import {ContentFrameComponent} from './components/content-frame/content-frame.component';
import {ContentFrameGuard} from './guards/content-frame-guard';

export const contentFrameRoutes  = [
    {
        path: ':nodeId/:type/:itemId/:editMode',
        component: ContentFrameComponent,
        canDeactivate: [ContentFrameGuard]
    }
];
