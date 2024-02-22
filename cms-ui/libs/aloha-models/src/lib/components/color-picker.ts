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

    setPalette: (palette: ColorValue[]) => void;
    setAllowOutsidePalette: (allowOutsidePalette: boolean) => void;
    setAllowCustomInput: (allowCustomInput: boolean) => void;
    setAllowClear: (allowTransparency: boolean) => void;
    setAllowTransparency: (allowClear: boolean) => void;
}
