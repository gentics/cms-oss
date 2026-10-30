/** The folder of a devtools package's `files-internal` storage that holds its guidelines. */
export const GUIDELINES_DIR = 'guidelines';

// `<prio>_<name>.md` or `<prio>_<name>.default.md`; the first `_` ends the priority. A name never
// starts with `.`, so `10_.default.md` is no guideline named `.default`.
const FILE_NAME = /^(\d+)_([^.].*?)(\.default)?\.md$/;

const DEFAULT_SUFFIX = '.default';

/** One guideline: a Markdown file in `files-internal/guidelines/` of a devtools package. */
export interface Guideline {
    /** `<package>/<name>`, plus `.default` for a default guideline: the id in `SessionContext.guidelines`. */
    slug: string;
    package: string;
    name: string;
    /** The number before the first `_`; not part of the slug. */
    prio: number;
    /** A default guideline applies to every session of the package's nodes; any other is added by the user. */
    isDefault: boolean;
    /** The file in the package's `files-internal/guidelines/`, e.g. `10_tone-of-voice.default.md`. */
    fileName: string;
}

/** The slug of the guideline `name` of `packageName`. */
export function guidelineSlug(packageName: string, name: string, isDefault: boolean): string {
    return `${packageName}/${name}${isDefault ? DEFAULT_SUFFIX : ''}`;
}

/**
 * The guideline that the file `fileName` (the last path segment, e.g. `10_tone-of-voice.default.md`) of
 * `packageName` is, or `undefined` for a file whose name does not follow `<prio>_<name>[.default].md`.
 */
export function guidelineFromFile(packageName: string, fileName: string): Guideline | undefined {
    const match = FILE_NAME.exec(fileName);

    if (!match) {
        return undefined;
    }

    const [, prio, name, defaultSuffix] = match;
    const isDefault = defaultSuffix !== undefined;

    return { slug: guidelineSlug(packageName, name!, isDefault), package: packageName, name: name!, prio: Number(prio), isDefault, fileName };
}

/** The path of a guideline's file in its package's `files-internal` storage. */
export function guidelinePath({ fileName }: Pick<Guideline, 'fileName'>): string {
    return `${GUIDELINES_DIR}/${fileName}`;
}

/**
 * What a slug says by itself: its package (up to the first `/`), its name and whether it is a default
 * guideline. Enough to name a guideline whose file no longer exists. `undefined` for an id that is no
 * `<package>/<name>` slug.
 */
export function parseGuidelineSlug(slug: string): Pick<Guideline, 'package' | 'name' | 'isDefault'> | undefined {
    const separator = slug.indexOf('/');

    if (separator <= 0 || separator === slug.length - 1) {
        return undefined;
    }

    const rest = slug.slice(separator + 1);
    const isDefault = rest.endsWith(DEFAULT_SUFFIX) && rest.length > DEFAULT_SUFFIX.length;

    return { package: slug.slice(0, separator), name: isDefault ? rest.slice(0, -DEFAULT_SUFFIX.length) : rest, isDefault };
}

/** The guidelines by `prio`, lowest first, then by slug. */
export function sortGuidelines(guidelines: Guideline[]): Guideline[] {
    return [...guidelines].sort((a, b) => a.prio - b.prio || a.slug.localeCompare(b.slug));
}

/** The ids of `guidelines` that name no guideline in `known`: guidelines removed or renamed since. */
export function unknownGuidelines(guidelines: string[], known: Guideline[]): string[] {
    const slugs = new Set(known.map(({ slug }) => slug));

    return guidelines.filter((slug) => !slugs.has(slug));
}
