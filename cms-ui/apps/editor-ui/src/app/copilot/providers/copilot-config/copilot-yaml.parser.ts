import { CopilotAction, CopilotConfig, DEFAULT_COPILOT_CONFIG } from '../../copilot.types';

/**
 * Hand-rolled, intentionally minimal YAML parser targeted at the
 * `copilot.yml` schema. Supports exactly the constructs documented for
 * the Content Copilot configuration:
 *
 *   - top-level scalar entries  (`enabled: true`)
 *   - one nested list named `actions` whose items are mappings
 *     of scalar key/value pairs
 *   - line comments starting with `#`
 *   - quoted ('...' or "...") and unquoted scalar values
 *   - boolean (true/false/yes/no), integer, and string values
 *
 * Anything beyond that scope (block scalars, anchors, flow style,
 * deep nesting, …) is *not* supported by design: the parser is meant
 * to be readable, dependency-free and to fail loudly on misuse instead
 * of silently misinterpreting unfamiliar YAML.
 *
 * If the configuration grows beyond this schema we should switch to a
 * proper YAML library — the call site only depends on `parseCopilotYaml`,
 * so swapping the implementation is a one-file change.
 */

interface RawEntry {
    indent: number;
    /** Raw text after the leading whitespace was stripped. */
    text: string;
    /** 1-based line number, used in error messages. */
    line: number;
}

/** Parse a `copilot.yml` document. Falls back to the default config on error. */
export function parseCopilotYaml(yaml: string): CopilotConfig {
    if (!yaml || !yaml.trim()) {
        return { ...DEFAULT_COPILOT_CONFIG };
    }

    try {
        const entries = tokenise(yaml);
        return buildConfig(entries);
    } catch (err) {
        // eslint-disable-next-line no-console
        console.warn('[Copilot] copilot.yml is invalid, ignoring:', (err as Error).message);
        return { ...DEFAULT_COPILOT_CONFIG };
    }
}

function tokenise(yaml: string): RawEntry[] {
    const out: RawEntry[] = [];
    const lines = yaml.split(/\r?\n/);
    for (let i = 0; i < lines.length; i++) {
        const raw = lines[i];
        // Strip trailing comment but only when `#` is unambiguously a comment
        // (preceded by whitespace or at the start of the line). This keeps `#`
        // inside quoted values intact for the simple cases we support.
        const stripped = stripComment(raw);
        if (!stripped.trim()) {
            continue;
        }
        const indent = leadingSpaces(stripped);
        out.push({ indent, text: stripped.slice(indent), line: i + 1 });
    }
    return out;
}

function stripComment(line: string): string {
    // Find first '#' that is at start-of-line or preceded by whitespace, and
    // not inside a quoted string. Cheap heuristic — adequate for our schema.
    let inSingle = false;
    let inDouble = false;
    for (let i = 0; i < line.length; i++) {
        const ch = line[i];
        if (ch === '\'' && !inDouble) inSingle = !inSingle;
        else if (ch === '"' && !inSingle) inDouble = !inDouble;
        else if (ch === '#' && !inSingle && !inDouble) {
            const prev = line[i - 1];
            if (i === 0 || prev === ' ' || prev === '\t') {
                return line.slice(0, i).replace(/\s+$/, '');
            }
        }
    }
    return line.replace(/\s+$/, '');
}

function leadingSpaces(line: string): number {
    let n = 0;
    while (n < line.length && line[n] === ' ') n++;
    return n;
}

function buildConfig(entries: RawEntry[]): CopilotConfig {
    const cfg: CopilotConfig = { ...DEFAULT_COPILOT_CONFIG, actions: [] };

    for (let i = 0; i < entries.length; i++) {
        const entry = entries[i];

        if (entry.indent !== 0) {
            throw new Error(`unexpected indentation at line ${entry.line}`);
        }

        const { key, value } = splitKeyValue(entry);

        switch (key) {
            case 'enabled':
                cfg.enabled = parseBoolean(value, entry.line);
                break;

            case 'actions': {
                // Accept the empty-list shorthand (`actions: []`) so a
                // customer can keep the key in a config file with the
                // feature flag flipped on but no actions wired up yet.
                if (value === '[]') {
                    cfg.actions = [];
                    break;
                }
                if (value !== '') {
                    throw new Error(`'actions:' must be followed by a list (line ${entry.line})`);
                }
                const block = takeChildBlock(entries, i + 1);
                cfg.actions = parseActionList(block);
                i += block.length;
                break;
            }

            default:
                // Unknown top-level keys are forward-compatibility friendly:
                // ignore with a warning instead of failing the whole load.
                // eslint-disable-next-line no-console
                console.warn(`[Copilot] ignoring unknown top-level key '${key}' (line ${entry.line})`);
                break;
        }
    }

    return cfg;
}

function takeChildBlock(entries: RawEntry[], startIndex: number): RawEntry[] {
    const block: RawEntry[] = [];
    for (let j = startIndex; j < entries.length; j++) {
        if (entries[j].indent === 0) break;
        block.push(entries[j]);
    }
    return block;
}

function parseActionList(block: RawEntry[]): CopilotAction[] {
    const actions: CopilotAction[] = [];
    let current: Partial<CopilotAction> | null = null;
    let itemIndent = -1;
    let fieldIndent = -1;

    for (const entry of block) {
        const isItemStart = entry.text.startsWith('- ');

        if (isItemStart) {
            if (itemIndent === -1) {
                itemIndent = entry.indent;
            }
            if (entry.indent !== itemIndent) {
                throw new Error(`inconsistent list indentation at line ${entry.line}`);
            }
            if (current) {
                actions.push(finaliseAction(current, entry.line));
            }
            current = {};
            const inner: RawEntry = {
                indent: entry.indent + 2,
                text: entry.text.slice(2),
                line: entry.line,
            };
            if (fieldIndent === -1) {
                fieldIndent = inner.indent;
            }
            applyField(current, inner);
        } else {
            if (!current) {
                throw new Error(`expected '- ' before key at line ${entry.line}`);
            }
            if (fieldIndent === -1) {
                fieldIndent = entry.indent;
            }
            if (entry.indent !== fieldIndent) {
                throw new Error(`inconsistent field indentation at line ${entry.line}`);
            }
            applyField(current, entry);
        }
    }

    if (current) {
        actions.push(finaliseAction(current, block.length ? block[block.length - 1].line : 0));
    }

    return actions;
}

function applyField(target: Partial<CopilotAction>, entry: RawEntry): void {
    const { key, value } = splitKeyValue(entry);
    switch (key) {
        case 'id':
        case 'label':
        case 'icon':
        case 'description':
        case 'prompt':
            target[key] = parseString(value);
            break;
        default:
            // eslint-disable-next-line no-console
            console.warn(`[Copilot] ignoring unknown action field '${key}' (line ${entry.line})`);
            break;
    }
}

function finaliseAction(partial: Partial<CopilotAction>, line: number): CopilotAction {
    if (!partial.id) {
        throw new Error(`action is missing required 'id' (around line ${line})`);
    }
    if (!partial.label) {
        throw new Error(`action '${partial.id}' is missing required 'label' (around line ${line})`);
    }
    return partial as CopilotAction;
}

function splitKeyValue(entry: RawEntry): { key: string; value: string } {
    const colon = entry.text.indexOf(':');
    if (colon === -1) {
        throw new Error(`expected 'key: value' at line ${entry.line}`);
    }
    const key = entry.text.slice(0, colon).trim();
    const value = entry.text.slice(colon + 1).trim();
    if (!key) {
        throw new Error(`empty key at line ${entry.line}`);
    }
    return { key, value };
}

function parseString(raw: string): string {
    if (raw.length >= 2) {
        const first = raw[0];
        const last = raw[raw.length - 1];
        if ((first === '"' && last === '"') || (first === '\'' && last === '\'')) {
            return raw.slice(1, -1);
        }
    }
    return raw;
}

function parseBoolean(raw: string, line: number): boolean {
    const v = raw.toLowerCase();
    if (v === 'true' || v === 'yes' || v === 'on') return true;
    if (v === 'false' || v === 'no' || v === 'off') return false;
    throw new Error(`expected boolean at line ${line}, got '${raw}'`);
}
