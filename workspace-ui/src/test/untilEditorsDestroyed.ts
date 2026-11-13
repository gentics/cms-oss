/**
 * Resolves once the Tiptap editors that the last unmount left are destroyed. `useEditor` destroys an unmounted
 * editor only on a timer of 1 ms (`EditorInstanceManager.scheduleDestroy` in `@tiptap/react`), and that destruction
 * reads the global `document`. A timer of the same delay set after it runs after it (timers of one delay run in the
 * order they were set), so this waits for exactly that step, not for an arbitrary time.
 */
export function untilEditorsDestroyed(): Promise<void> {
    return new Promise((resolve) => {
        setTimeout(resolve, 1);
    });
}
