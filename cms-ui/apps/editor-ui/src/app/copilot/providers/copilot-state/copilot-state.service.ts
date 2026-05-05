import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Holds the runtime UI state of the Content Copilot panel.
 *
 * Kept deliberately small and outside of the NGXS store: the open/closed
 * flag is purely a view concern, never persisted, and only ever toggled
 * from the toolbar button → sidebar pair. Promoting it into the global
 * editor state would add ceremony without benefit.
 *
 * Provided at the root level so the toolbar button (inside the lazily
 * loaded `ContentFrameModule`) and the sidebar (rendered from the same
 * module) share a single instance with anyone else who might want to
 * read the state.
 */
@Injectable({ providedIn: 'root' })
export class CopilotStateService {

    private readonly openSubject = new BehaviorSubject<boolean>(false);

    /** Whether the sidebar is currently expanded. */
    public readonly open$: Observable<boolean> = this.openSubject.asObservable();

    /** Synchronous accessor for templates that need a non-reactive value. */
    public get isOpen(): boolean {
        return this.openSubject.value;
    }

    public toggle(): void {
        this.openSubject.next(!this.openSubject.value);
    }

    public open(): void {
        if (!this.openSubject.value) {
            this.openSubject.next(true);
        }
    }

    public close(): void {
        if (this.openSubject.value) {
            this.openSubject.next(false);
        }
    }
}
