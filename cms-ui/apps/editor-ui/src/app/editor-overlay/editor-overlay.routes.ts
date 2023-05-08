import { EditorOverlay } from './components/editor-overlay/editor-overlay.component';

export const editorOverlayRoutes = [
    {
        path: ':type',
        component: EditorOverlay
    },
    {
        path: ':nodeId/:type/:itemId/:editMode',
        component: EditorOverlay
    }
];
