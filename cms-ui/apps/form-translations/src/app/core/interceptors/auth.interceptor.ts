import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthenticationService } from '../services/authentication.service';
import { NotificationService } from '../services/notification.service';

/**
 * Hängt an jeden Request gegen `/rest/...` automatisch den `?sid=…`-Parameter.
 * Behandelt 401 (Session abgelaufen) und 403 (keine Berechtigung) zentral.
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private readonly auth: AuthenticationService,
    private readonly notifications: NotificationService
  ) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!req.url.includes('/rest/')) {
      return next.handle(req);
    }

    const sid = this.auth.currentSid;
    const reqWithSid = sid
      ? req.clone({ params: req.params.set('sid', sid) })
      : req;

    return next.handle(reqWithSid).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 401) {
          this.auth.clearSid();
          this.notifications.error('Ihre Sitzung ist abgelaufen. Bitte melden Sie sich erneut im CMS an.');
        } else if (err.status === 403) {
          this.notifications.error('Sie haben keine Berechtigung für diese Aktion.');
        }
        return throwError(() => err);
      })
    );
  }
}
