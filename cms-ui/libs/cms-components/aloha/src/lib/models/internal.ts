import { ElementRef, EventEmitter } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { AlohaComponent } from '@gentics/aloha-models';

export interface RenderedAlohaComponent<C extends AlohaComponent, T> extends ControlValueAccessor {
    slot?: string;
    renderContext?: string;
    disabled?: boolean;
    settings?: C | Partial<C> | Record<string, any>;
    element: ElementRef<HTMLElement>;

    requiresConfirm?: EventEmitter<boolean>;
    manualConfirm?: EventEmitter<void>;
}
