// Sends a request and returns its JSON body. The body is typed by the caller and not
// validated at runtime.
export async function httpRequest<T>(url: string, init?: RequestInit): Promise<T> {
    const response = await fetch(url, init);

    if (!response.ok) {
        throw new Error(`Request to ${url} failed with status ${response.status}`);
    }

    return response.json() as Promise<T>;
}
