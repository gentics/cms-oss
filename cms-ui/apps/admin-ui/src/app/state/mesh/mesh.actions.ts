import { SchemaContainer } from '@admin-ui/features/mesh-browser/models/mesh-browser-models';
import { AppState } from '../app-state';
import { ActionDeclaration } from '../utils';


const MESH: keyof AppState = 'mesh';


@ActionDeclaration(MESH)
export class SchemasLoaded {
    static readonly TYPE = 'SchemaLoaded';
    constructor(public schemas: SchemaContainer[]) {}
}
