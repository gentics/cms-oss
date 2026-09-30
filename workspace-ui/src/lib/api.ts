import type { Me } from '@/lib/genaix/types';
import { httpRequest } from '@/lib/http';

// Same-origin path of the CMS proxy, which adds the installation token and X-GCMS-Subject.
// In development the Vite dev server plays that role (see `vite.config.ts`).
export const GENAIX_API_BASE = '/genaix/api/v1';

// Sends a request to a GenAIx route, for example `genaixRequest<Me>('/me')`.
export function genaixRequest<T>(path: string, init?: RequestInit): Promise<T> {
    return httpRequest<T>(`${GENAIX_API_BASE}${path}`, init);
}

// GET /me: identity, capabilities and MCP connection status.
export function getMe(): Promise<Me> {
    return genaixRequest<Me>('/me');
}
