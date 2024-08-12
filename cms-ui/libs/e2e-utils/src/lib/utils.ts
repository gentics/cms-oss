import { File, Folder, Group, Image, Node, Page, User, Variant } from '@gentics/cms-models';
import type { Suite } from 'mocha';
import {
    ENV_CMS_VARIANT,
    FileImportData,
    FolderImportData,
    GroupImportData,
    ImageImportData,
    IMPORT_ID,
    ImportData,
    NodeImportData,
    PageImportData,
    UserImportData,
} from './common';

export function getItem(data: NodeImportData, entities: Record<string, any>): Node | null;
export function getItem(data: FolderImportData, entities: Record<string, any>): Folder | null;
export function getItem(data: PageImportData, entities: Record<string, any>): Page | null;
export function getItem(data: ImageImportData, entities: Record<string, any>): Image | null;
export function getItem(data: FileImportData, entities: Record<string, any>): File | null;
export function getItem(data: GroupImportData, entities: Record<string, any>): Group | null;
export function getItem(data: UserImportData, entities: Record<string, any>): User | null;
export function getItem(data: ImportData | string, entities: Record<string, any>): any {
    const importId = typeof data === 'string' ? data : data[IMPORT_ID];
    return entities[importId] || null;
}

export function envAll(env: string | string[]): boolean;
export function envAll(...vars: string[]): boolean {
    return vars.every(f => Cypress.env(f));
}

export function envAny(env: string | string[]): boolean;
export function envAny(...vars: string[]): boolean {
    return vars.some(f => Cypress.env(f));
}

export function envNone(env: string | string[]): boolean;
export function envNone(...vars: string[]): boolean {
    return vars.every(f => !Cypress.env(f));
}

export function isVariant(variant: Variant): boolean {
    return Cypress.env(ENV_CMS_VARIANT) === variant;
}

export function skipableSuite(doExecute: boolean, title: string, fn: (this: Suite) => void): Suite | void {
    return (doExecute ? describe : describe.skip)(title, fn);
}
