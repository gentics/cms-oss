type JsonObject = Record<string, unknown>;

function isObject(value: unknown): value is JsonObject {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * JSON Merge Patch (RFC 7386), as `part.delta` with `patch_format: merge_patch` uses it: `null`
 * deletes a member, objects merge recursively, everything else (arrays included) replaces.
 * Returns a new value and leaves `target` unchanged.
 */
export function applyMergePatch(target: unknown, patch: unknown): unknown {
    if (!isObject(patch)) {
        return patch;
    }

    const result: JsonObject = isObject(target) ? { ...target } : {};

    for (const [key, value] of Object.entries(patch)) {
        if (value === null) {
            delete result[key];
        } else {
            result[key] = applyMergePatch(result[key], value);
        }
    }

    return result;
}
