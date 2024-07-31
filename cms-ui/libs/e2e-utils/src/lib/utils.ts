import { File, Folder, Image, Node, NodeFeature, Page } from '@gentics/cms-models';
import type { Suite } from 'mocha';
import { FileImportData, FolderImportData, ImageImportData, IMPORT_ID, ImportData, NodeImportData, PageImportData } from './common';

export function getItem(data: NodeImportData, entities: Record<string, any>): Node | null;
export function getItem(data: FolderImportData, entities: Record<string, any>): Folder | null;
export function getItem(data: PageImportData, entities: Record<string, any>): Page | null;
export function getItem(data: ImageImportData, entities: Record<string, any>): Image | null;
export function getItem(data: FileImportData, entities: Record<string, any>): File | null;
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

export function skipableSuite(doExecute: boolean, title: string, fn: (this: Suite) => void): Suite | void {
    return (doExecute ? describe : describe.skip)(title, fn);
}

export function getActiveNodeFeatures(): NodeFeature[] {
    const activatedNodeFeatures = [];
    Object.keys(NodeFeature).forEach(feature => {
        if(envAny(`FEATURE_${feature}`)) {
            activatedNodeFeatures.push(NodeFeature[feature]);
        }
    })

    return activatedNodeFeatures;
}
