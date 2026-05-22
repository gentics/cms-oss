import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';

import { LoadingState } from '../../models/app-state.model';

@Component({
  selector: 'gtx-save-bar',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './save-bar.component.html',
  styleUrls: ['./save-bar.component.scss']
})
export class SaveBarComponent {
  @Input() dirtyCount = 0;
  @Input() savingState: LoadingState = 'idle';

  @Output() readonly save = new EventEmitter<void>();
  @Output() readonly discard = new EventEmitter<void>();

  get isSaving(): boolean { return this.savingState === 'loading'; }
}
