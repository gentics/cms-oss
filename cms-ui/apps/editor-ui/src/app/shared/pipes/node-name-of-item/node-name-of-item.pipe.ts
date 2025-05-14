import { Pipe, PipeTransform } from '@angular/core';
import { File, Folder, Image, Page } from '@gentics/cms-models';
import { ApplicationStateService } from '../../../state';

@Pipe({
    name: 'nodeNameOfItem',
    standalone: false
})
export class NodeNameOfItemPipe implements PipeTransform {

    constructor(private state: ApplicationStateService) { }

    transform(item: Folder | Page | File | Image): string {
        const nodes = this.state.now.entities.node;
        const nodeId: number = (item as any).nodeId;

        if (nodeId && nodes && nodes[nodeId]) {
            return nodes[nodeId].name;
        } else if (typeof item.path === 'string') {
            const pathParts = item.path.split('/');
            return pathParts && pathParts[1] || '';
        } else {
            return '';
        }
    }

}
