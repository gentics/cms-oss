import {BehaviorSubject} from 'rxjs';

export class MockPageVisibility {
    visible$ = new BehaviorSubject(true);
    get visible(): boolean {
        return this.visible$.value;
    }
    setVisibility(v: boolean): void {
        if (v != this.visible$.value) {
            this.visible$.next(v);
        }
    }
}
