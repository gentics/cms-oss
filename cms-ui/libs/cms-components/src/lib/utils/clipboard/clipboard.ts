/**
 * Copy the text to the clipboard. Where the Clipboard API fails (an insecure connection or otherwise),
 * it falls back to selecting the text in a temporary textarea and `execCommand('copy')`.
 * @throws the error of the fallback, if that failed as well
 */
export async function copyTextToClipboard(text: string): Promise<void> {
    try {
        await navigator.clipboard.writeText(text);
        return;
    } catch {
        // fall back below
    }

    const textArea = document.createElement('textarea');
    textArea.value = text;
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        document.execCommand('copy');
    } finally {
        document.body.removeChild(textArea);
    }
}
