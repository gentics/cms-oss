import { Pipe, PipeTransform } from '@angular/core';
import { EditMode, InheritableItem } from '@gentics/cms-models';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService } from '../../../state';

@Pipe({ name: 'routerCommandsForItem' })
export class RouterCommandsForItemPipe implements PipeTransform {

    constructor(
        private state: ApplicationStateService,
        private navigationService: NavigationService,
    ) { }

    transform(item: InheritableItem): any[] {
        if (item.type === 'folder') {
            return [`../${item.id}`];
        }

        const editMode: EditMode = (item.type === 'page' || item.type === 'form') ? 'preview' : 'editProperties';
        if (!this.state.now.folder.activeNode) {
            return null;
        }

        return this.navigationService.instruction({
            detail: {
                nodeId: this.state.now.folder.activeNode,
                itemType: item.type,
                itemId: item.id,
                editMode,
            },
        }).commands();
    }
}
