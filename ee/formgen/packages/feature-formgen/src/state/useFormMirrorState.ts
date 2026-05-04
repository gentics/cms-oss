import { create } from 'zustand';
import { FormState, UIState } from '../types/interfaces';

/**
 * Global mirror of the form renderer state.
 *
 * Important:
 * - The local React state inside RendererScreen remains the *single* mutable source of truth.
 * - This store only holds *snapshots* of `formState` and `uiState` for read-only access
 *   from anywhere in the app (e.g., overlays, DevTools, debug panels, other screens).
 */
interface FormMirrorStoreState {
  formState: FormState | null;
  uiState: UIState | null;
  /**
   * Replaces the current mirrored formState with a fresh snapshot.
   * The RendererScreen is responsible for providing an already mutated/updated instance.
   */
  setFormState: (next: FormState) => void;
  /**
   * Replaces the current mirrored uiState with a fresh snapshot.
   * The RendererScreen is responsible for providing an already mutated/updated instance.
   */
  setUIState: (next: UIState) => void;
  /**
   * Resets the mirror (typically when the RendererScreen unmounts),
   * so that consumers never see stale state from a previous form.
   */
  reset: () => void;
}

/**
 * Computes a deep diff between two JSON-serialisable values.
 *
 * The result is a flat map from "path" -> { from, to }, where "path" is a
 * dotted notation such as:
 *   data.properties.person.firstName.value
 *
 * This is only meant for debug logging, so we keep the implementation simple
 * and defensive. Non-object / non-array values are treated as leaves.
 */
function diffDeep(
  prev: Record<string, unknown> | null | undefined,
  next: Record<string, unknown> | null | undefined,
): Record<string, { from: unknown; to: unknown }> | null {
  const result: Record<string, { from: unknown; to: unknown }> = {};

  const walk = (a: any, b: any, path: string) => {
    if (a === b) {
      return;
    }

    const isObjA = a !== null && typeof a === 'object';
    const isObjB = b !== null && typeof b === 'object';

    // If either side is not an object/array, or types differ, we treat this as a leaf.
    if (!isObjA || !isObjB || Array.isArray(a) !== Array.isArray(b)) {
      result[path || 'root'] = { from: a, to: b };
      return;
    }

    if (Array.isArray(a) && Array.isArray(b)) {
      const maxLen = Math.max(a.length, b.length);
      for (let i = 0; i < maxLen; i++) {
        const keyPath = path ? `${path}[${i}]` : `[${i}]`;
        if (i >= a.length) {
          // new item
          walk(undefined, b[i], keyPath);
        } else if (i >= b.length) {
          // removed item
          walk(a[i], undefined, keyPath);
        } else {
          walk(a[i], b[i], keyPath);
        }
      }
      return;
    }

    // Plain object case
    const keys = new Set([...Object.keys(a ?? {}), ...Object.keys(b ?? {})]);
    keys.forEach((key) => {
      const keyPath = path ? `${path}.${key}` : key;
      walk(a?.[key], b?.[key], keyPath);
    });
  };

  // Handle null/undefined root values explicitly
  if (prev === next) {
    return null;
  }
  if (!prev && next) {
    result['root'] = { from: undefined, to: next };
  } else if (prev && !next) {
    result['root'] = { from: prev, to: undefined };
  } else if (prev && next) {
    walk(prev, next, '');
  }

  return Object.keys(result).length > 0 ? result : null;
}

/**
 * Takes a JSON-serialisable snapshot of the given value.
 *
 * Functions and other non-serialisable members are dropped, which is fine for
 * our debug use-case where we only care about data (schema, form, data, flags...).
 * The important part is that the snapshot is immutable and won't be mutated
 * later via shared references.
 */
function createSnapshot(
  value: unknown,
): Record<string, unknown> | null {
  if (value == null) {
    return null;
  }

  try {
    return JSON.parse(JSON.stringify(value));
  } catch (error) {
    console.warn('[FormMirrorStore] Failed to create snapshot', error);
    return null;
  }
}

// Last immutable snapshots used for diffing. We do not rely on the
// previous Zustand state object itself, because that may share references
// with the local React state and be mutated in place.
let lastFormSnapshot: any = null;
let lastUISnapshot: any = null;

export const useFormMirrorStore = create<FormMirrorStoreState>((set) => ({
  formState: null,
  uiState: null,
  setFormState: (next) => set({ formState: next }),
  setUIState: (next) => set({ uiState: next }),
  reset: () => set({ formState: null, uiState: null }),
}));

// Determine whether we are in development mode.
// Works for React Native (__DEV__) and web/Node via process.env.NODE_ENV,
// but without referencing undeclared globals in TypeScript.
const isDev: boolean = (() => {
  if (typeof globalThis === 'undefined') return false;
  const g = globalThis as any;

  // React Native / Metro
  if (g.__DEV__ === true) return true;

  // Web / Node bundlers
  if (g.process?.env?.NODE_ENV === 'development') return true;

  return false;
})();

/**
 * Debug helper: log only the *difference* for each state change of the mirror store.
 *
 * We diff against our own immutable JSON snapshots rather than the raw previous
 * Zustand state object, because the latter may share references with the local
 * React state and be mutated in place before the next update.
 *
 * This runs once at module evaluation time and stays subscribed for
 * the lifetime of the app. It is enabled only in development.
 */
if (isDev) {
  useFormMirrorStore.subscribe((state) => {
    const nextFormSnapshot = createSnapshot(state.formState);
    const nextUISnapshot = createSnapshot(state.uiState);

    const formDiff = diffDeep(lastFormSnapshot, nextFormSnapshot);
    const uiDiff = diffDeep(lastUISnapshot, nextUISnapshot);

    // If nothing relevant changed, skip logging to keep the console clean.
    if (!formDiff && !uiDiff) {
      lastFormSnapshot = nextFormSnapshot;
      lastUISnapshot = nextUISnapshot;
      return;
    }

    const MAX_ENTRIES = 50;
    const trimDiff = (
      diff: Record<string, { from: unknown; to: unknown }> | null,
    ) => {
      if (!diff) return null;
      const entries = Object.entries(diff);
      if (entries.length <= MAX_ENTRIES) return diff;
      const trimmed = Object.fromEntries(entries.slice(0, MAX_ENTRIES));
      (trimmed as any)['__truncated__'] = {
        from: entries.length,
        to: `Only first ${MAX_ENTRIES} changes shown`,
      };
      return trimmed;
    };

    const trimmedFormDiff = trimDiff(formDiff);
    const trimmedUIDiff = trimDiff(uiDiff);

    console.log('[FormMirrorStore] diff', {
      ...(trimmedFormDiff ? { formState: trimmedFormDiff } : {}),
      ...(trimmedUIDiff ? { uiState: trimmedUIDiff } : {}),
    });

    // Update snapshots for the next diff
    lastFormSnapshot = nextFormSnapshot;
    lastUISnapshot = nextUISnapshot;
  });
}