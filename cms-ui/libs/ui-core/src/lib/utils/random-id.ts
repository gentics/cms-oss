export function randomId(length: number = 12): string {
    let buffer = '';
    for (let i = 0; i < length; i++) {
        // Create a number between 0 and 62
        const rng = Math.floor(Math.random() * 62);
        // The codepoint, where 0-9 are the corresponding numbers,
        // 10 - 36 is A-Z, and 37-62 is a-z
        // ASCII-Table mapping
        // 0-9: POS 48, OFFSET: 0  -> 48 - 0  = 48
        // A-Z: POS 65, OFFSET: 10 -> 65 - 10 = 55
        // a-z: POS 97, OFFSET: 36 -> 97 - 36 = 61
        const pos = rng < 10
            // Numbers
            ? 48 + rng
            : rng < 36
                // A-Z
                ? 55 + rng
                // a-z
                : 61 + rng;
        buffer += String.fromCodePoint(pos);
    }
    return buffer;
}
