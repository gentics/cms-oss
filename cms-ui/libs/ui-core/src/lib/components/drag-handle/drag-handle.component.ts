import { Component } from '@angular/core';

@Component({
    selector: 'gtx-drag-handle',
    template: `
        <div class="gtx-drag-handle">
            <icon>drag_handle</icon>
        </div>
    `,
    standalone: false
})
export class SortableListDragHandleComponent {}
