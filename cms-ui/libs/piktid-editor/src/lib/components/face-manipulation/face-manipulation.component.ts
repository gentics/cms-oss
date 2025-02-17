import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { NewGenerationNotificationData } from '../../common/models';

@Component({
    selector: 'gtxpikt-face-manipulation',
    templateUrl: './face-manipulation.component.html',
    styleUrls: ['./face-manipulation.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FaceManipulationComponent {

    @Input()
    public faceId: number | null = null;

    @Input()
    public generations: NewGenerationNotificationData[] = [];

    @Input()
    public selectedGeneration: number | null = null;

    @Input()
    public progress: number | null = null;

    @Input()
    public waiting = false;

    @Input()
    public confirmed = false;

    @Input()
    public disabled = false;

    @Output()
    public generationSelected = new EventEmitter<number>();

    @Output()
    public generationConfirmed = new EventEmitter<void>();

    @Output()
    public generateNewFace = new EventEmitter<void>();

    @Output()
    public undoGeneration = new EventEmitter<void>();

    selectGeneration(generationId: number): void {
        if (this.confirmed || this.disabled) {
            return;
        }

        this.generationSelected.emit(generationId);
    }

    confirmGeneration(): void {
        this.generationConfirmed.emit();
    }

    generateNewGeneration(): void {
        this.generateNewFace.emit();
    }

    revertGeneration(): void {
        this.undoGeneration.emit();
    }
}
