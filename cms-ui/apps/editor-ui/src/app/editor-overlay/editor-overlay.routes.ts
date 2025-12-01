import { EditorOverlay } from './components/editor-overlay/editor-overlay.component';

export const EDITOR_OVERLAY_ROUTES = [
    {
        path: ':type',
        component: EditorOverlay,
    },
    {
        path: ':nodeId/:type/:itemId/:editMode',
        component: EditorOverlay,
    },
];
