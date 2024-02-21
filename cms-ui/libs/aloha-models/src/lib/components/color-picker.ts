import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export type RGBColor = [number, number, number];
export type RGBAColor = [number, number, number, number];

export type ColorValue = RGBColor | RGBAColor | string;
export type NormalizedColor = RGBAColor;

export interface AlohaColorPickerComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.COLOR_PICKER;

    value: ColorValue | null;
    palette: ColorValue[];
    allowOutsidePalette: boolean;
    allowCustomInput: boolean;
    allowTransparency: boolean;
    allowClear: boolean;

    updatePalette: (palette: ColorValue[]) => void;
    updateAllowOutsidePalette: (allowOutsidePalette: boolean) => void;
    updateAllowCustomInput: (allowCustomInput: boolean) => void;
    updateAllowClear: (allowTransparency: boolean) => void;
    updateAllowTransparency: (allowClear: boolean) => void;
}
