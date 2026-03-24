import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';

export interface FormItem {
    id: number;
    name: string;
    description: string;
}

@Component({
    selector: 'gtx-node-forms-assignment',
    imports: [],
    templateUrl: './node-forms-assignment.component.html',
    styleUrl: './node-forms-assignment.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeFormsAssignmentComponent {
    constructor(private cdr: ChangeDetectorRef) {}

    // Mock Data
    availableForms: FormItem[] = [
        { id: 1, name: 'Support Request', description: 'Standard helpdesk support form' },
        { id: 2, name: 'Newsletter Signup', description: 'Monthly marketing updates' },
        { id: 3, name: 'Job Application', description: 'HR recruiting portal form' },
        { id: 4, name: 'Callback Request', description: 'Sales callback form' },
        { id: 5, name: 'Event Registration', description: 'Sign up for local events' }
    ];

    assignedForms: FormItem[] = [
        { id: 6, name: 'Customer Feedback', description: 'General site feedback survey' }
    ];

    selectedAvailable: number[] = [];
    selectedAssigned: number[] = [];
    isDirty: boolean = false;

    saveForms(): Promise<void> {
        console.log('Mock Save: Forms Assigned ->', this.assignedForms.map(f => f.name));
        this.isDirty = false;
        this.cdr.markForCheck(); // Zwingt Angular das Button-Disable-Binding neu zu prüfen
        return Promise.resolve();
    }

    private markDirty(): void {
        this.isDirty = true;
        this.cdr.markForCheck(); // Ensure view updates for OnPush strategy
    }

    // State for drag & drop
    draggedItem: { id: number, source: 'available' | 'assigned' } | null = null;
    dragOverTarget: 'available' | 'assigned' | null = null;

    toggleSelection(list: 'available' | 'assigned', id: number): void {
        const selectedList = list === 'available' ? this.selectedAvailable : this.selectedAssigned;
        const idx = selectedList.indexOf(id);
        if (idx > -1) {
            selectedList.splice(idx, 1);
        } else {
            selectedList.push(id);
        }
    }

    moveSelectedRight(): void {
        const toMove = this.availableForms.filter(f => this.selectedAvailable.includes(f.id));
        this.availableForms = this.availableForms.filter(f => !this.selectedAvailable.includes(f.id));
        this.assignedForms = [...this.assignedForms, ...toMove];
        this.selectedAvailable = [];
        this.markDirty();
    }

    moveSelectedLeft(): void {
        const toMove = this.assignedForms.filter(f => this.selectedAssigned.includes(f.id));
        this.assignedForms = this.assignedForms.filter(f => !this.selectedAssigned.includes(f.id));
        this.availableForms = [...this.availableForms, ...toMove];
        this.selectedAssigned = [];
        this.markDirty();
    }

    moveAllRight(): void {
        this.assignedForms = [...this.assignedForms, ...this.availableForms];
        this.availableForms = [];
        this.selectedAvailable = [];
        this.markDirty();
    }

    moveAllLeft(): void {
        this.availableForms = [...this.availableForms, ...this.assignedForms];
        this.assignedForms = [];
        this.selectedAssigned = [];
        this.markDirty();
    }

    // --- Drag & Drop Handlers ---

    onDragStart(event: DragEvent, id: number, source: 'available' | 'assigned'): void {
        if (event.dataTransfer) {
            event.dataTransfer.effectAllowed = 'move';
            // Firefox requires data to be set to allow dragging
            event.dataTransfer.setData('text/plain', id.toString());
        }

        // HACK für native D&D: Wir verzögern das 'Ausgrauen' des Listen-Eintrags mit setTimeout!
        setTimeout(() => {
            this.draggedItem = { id, source };
            this.cdr.markForCheck();
        }, 0);
    }

    onDragOver(event: DragEvent, targetList: 'available' | 'assigned'): void {
        // Prevent default to allow dropping
        event.preventDefault();
        if (event.dataTransfer) {
            event.dataTransfer.dropEffect = 'move';
        }
        
        // Highlight logic (only highlight if dragging from the opposite list)
        if (this.draggedItem && this.draggedItem.source !== targetList) {
            this.dragOverTarget = targetList;
        }
    }

    onDragLeave(): void {
        this.dragOverTarget = null;
    }

    onDragEnd(): void {
        this.draggedItem = null;
        this.dragOverTarget = null;
    }

    onDrop(event: DragEvent, targetList: 'available' | 'assigned'): void {
        event.preventDefault();
        this.dragOverTarget = null;
        
        if (!this.draggedItem || this.draggedItem.source === targetList) {
            this.draggedItem = null;
            return;
        }

        const id = this.draggedItem.id;
        
        if (targetList === 'assigned') {
            const itemToMove = this.availableForms.find(f => f.id === id);
            if (itemToMove) {
                // Remove from left, add to right
                this.availableForms = this.availableForms.filter(f => f.id !== id);
                this.assignedForms = [...this.assignedForms, itemToMove];
                // Unselect if it was selected
                this.selectedAvailable = this.selectedAvailable.filter(selId => selId !== id);
                this.markDirty();
            }
        } else {
            const itemToMove = this.assignedForms.find(f => f.id === id);
            if (itemToMove) {
                // Remove from right, add to left
                this.assignedForms = this.assignedForms.filter(f => f.id !== id);
                this.availableForms = [...this.availableForms, itemToMove];
                // Unselect
                this.selectedAssigned = this.selectedAssigned.filter(selId => selId !== id);
                this.markDirty();
            }
        }
        
        this.draggedItem = null;
    }
}
