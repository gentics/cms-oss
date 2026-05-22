import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Observable } from 'rxjs';

import { NotificationService, Toast } from '../../core/services/notification.service';

@Component({
  selector: 'gtx-toasts',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './toasts.component.html',
  styleUrls: ['./toasts.component.scss']
})
export class ToastsComponent {
  readonly toasts$: Observable<Toast[]>;

  constructor(private readonly notifications: NotificationService) {
    this.toasts$ = this.notifications.toasts;
  }

  onDismiss(id: number): void {
    this.notifications.dismiss(id);
  }

  trackById(_idx: number, toast: Toast): number {
    return toast.id;
  }

  iconFor(toast: Toast): string {
    switch (toast.kind) {
      case 'success': return '✓';
      case 'error':   return '✕';
      case 'warning': return '!';
      default:        return 'ℹ';
    }
  }
}
