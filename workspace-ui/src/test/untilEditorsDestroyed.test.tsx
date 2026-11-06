import { cleanup, render } from '@testing-library/react';
import type { Editor } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import { describe, expect, it } from 'vitest';

import { composerExtensions } from '@/components/ComposerTextInput/composerExtensions';

import { untilEditorsDestroyed } from './untilEditorsDestroyed';

// The composer's editor, as `useComposer` makes it; hands it out to the test.
function Field({ onEditor }: { onEditor: (editor: Editor) => void }) {
    const editor = useEditor({ extensions: composerExtensions });

    if (editor) {
        onEditor(editor);
    }

    return <EditorContent editor={editor} />;
}

describe('untilEditorsDestroyed', () => {
    it('waits until an editor unmounted by the cleanup is destroyed, which Tiptap defers to a timer', async () => {
        let editor: Editor | undefined;

        render(<Field onEditor={(made) => { editor = made; }} />);
        expect(editor?.isDestroyed).toBe(false);

        cleanup();
        // Tiptap destroys it only on a timer (`scheduleDestroy`), which may come after the test environment is gone.
        expect(editor?.isDestroyed).toBe(false);

        await untilEditorsDestroyed();

        expect(editor?.isDestroyed).toBe(true);
    });
});
