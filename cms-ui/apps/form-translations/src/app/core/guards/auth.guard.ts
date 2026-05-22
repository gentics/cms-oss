import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';

import { AuthenticationService } from '../services/authentication.service';

/**
 * Verhindert den Aufruf der App ohne gültige Session-ID. Der AppService
 * versucht die SID beim Bootstrap zu etablieren — wenn das fehlschlägt,
 * zeigt die Shell bereits einen Fehlerzustand; dieser Guard ist primär
 * für zukünftige untergeordnete Routen relevant.
 */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private readonly auth: AuthenticationService) {}

  canActivate(): boolean | UrlTree {
    return this.auth.currentSid !== null;
  }
}
