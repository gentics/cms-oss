import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({providedIn: 'root'})
export abstract class AppStateStrategy {
    abstract now: any;
    abstract select<R>(selector: (state: any) => R): Observable<R>;
}
