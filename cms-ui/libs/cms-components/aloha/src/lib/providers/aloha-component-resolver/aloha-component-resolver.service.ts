import { Injectable, Type } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AlohaComponentResolverService {

    private mapping = new Map<string, Type<any>>();

    public registerComponent(typeName: string, definition: Type<any>): void {
        this.mapping.set(typeName, definition);
    }

    public resolveComponent(typeName: string): Type<any> | null {
        return this.mapping.get(typeName);
    }
}
