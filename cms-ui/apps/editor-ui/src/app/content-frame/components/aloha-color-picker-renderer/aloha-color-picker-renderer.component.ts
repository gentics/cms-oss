import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { AlohaColorPickerComponent, ColorValue, NormalizedColor, RGBAColor } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Color, ColorEvent, RGBA } from 'ngx-color';
import { colorToHex, colorToRGBA, constrastColor } from '../../utils';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

function toNormalizedColor(ngxColor: Color): NormalizedColor {
    return [ngxColor.rgb.r, ngxColor.rgb.g, ngxColor.rgb.b, ngxColor.rgb.a];
}

function toNGXColor(rgba: RGBAColor): RGBA {
    return rgba == null ? null : { r: rgba[0], g: rgba[1], b: rgba[2], a: rgba[3] };
}

interface PaletteColor {
    color: string;
    contrast: string;
}

const DEFAULT_COLOR: RGBAColor = [0, 0, 0, 255];

@Component({
    selector: 'gtx-aloha-color-picker-renderer',
    templateUrl: './aloha-color-picker-renderer.component.html',
    styleUrls: ['./aloha-color-picker-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaColorPickerRendererComponent)],
})
export class AlohaColorPickerRendererComponent extends BaseAlohaRendererComponent<AlohaColorPickerComponent, NormalizedColor> implements OnInit {

    public ngxColorValue: RGBA;
    public hexValue: string;
    public normalizedPalette: PaletteColor[] = [];

    public override ngOnInit(): void {
        super.ngOnInit();

        // If we have to display more then just the palette (i.E. the user is able to change the color values),
        // then we need a confirmation button.
        if (this.settings?.allowOutsidePalette) {
            this.requiresConfirm.emit(true);
        }

        if (this.value == null) {
            this.value = DEFAULT_COLOR;
        }

        this.updateAndNormalizePalette();
        this.updateAndNormalizeValue();
    }

    protected override setupAlohaHooks(): void {
        super.setupAlohaHooks();

        if (!this.settings) {
            return;
        }

        this.settings.setPalette = (palette) => {
            this.settings.palette = palette;
            this.updateAndNormalizePalette();
            this.changeDetector.markForCheck();
        };
    }

    protected override onValueChange(): void {
        if (this.value == null) {
            this.value = DEFAULT_COLOR;
        }

        this.updateAndNormalizeValue();
    }

    public handleColorChange(change: ColorEvent): void {
        this.triggerChange(toNormalizedColor(change.color));
    }

    public selectPaletteColor(color: string): void {
        this.triggerChange(colorToRGBA(color));
    }

    protected updateAndNormalizeValue(): void {
        this.hexValue = colorToHex(this.value);
        this.ngxColorValue = toNGXColor(this.value);
    }

    protected updateAndNormalizePalette(): void {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.normalizedPalette = (this.settings.palette || [])
            .map((color: ColorValue) => colorToHex(color))
            .filter(color => color != null)
            .map(color => ({
                color,
                contrast: constrastColor(color),
            }));
    }
}
