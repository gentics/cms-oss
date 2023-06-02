import { v4 } from 'uuid';

export function newUUID(): string {
    return v4();
}
