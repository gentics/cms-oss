import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
} from '@angular/core';
import { DevicePreset } from '../../models/device-preset';

/**
 * Dropdown menu listing the available device-preview presets. Pure
 * presentational component — the parent (typically `gtx-editor-toolbar`)
 * provides the data and reacts to the `select` / `clear` outputs by
 * delegating to the `DevicePreviewService`.
 */
@Component({
    selector: 'gtx-device-preview-menu',
    templateUrl: './device-preview-menu.component.html',
    styleUrls: ['./device-preview-menu.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class DevicePreviewMenuComponent {

    /** Presets to display. */
    @Input()
    public presets: DevicePreset[] = [];

    /** Currently active preset (used to mark the matching menu item as selected). */
    @Input()
    public activePresetId: string | null = null;

    /** Emitted when the user picks a preset. */
    @Output()
    public select = new EventEmitter<DevicePreset>();

    /** Emitted when the user clicks the "Volle Breite" entry. */
    @Output()
    public clear = new EventEmitter<void>();

    public trackById(_index: number, preset: DevicePreset): string {
        return preset.id;
    }

    public onSelect(preset: DevicePreset): void {
        this.select.emit(preset);
    }

    public onClear(): void {
        this.clear.emit();
    }
}
