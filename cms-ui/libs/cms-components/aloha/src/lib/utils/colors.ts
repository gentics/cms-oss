import { ColorValue, RGBAColor } from '@gentics/aloha-models';

const colorCache: Record<string, RGBAColor> = {};
const colorCanvas = document.createElement('canvas');
colorCanvas.width = colorCanvas.height = 1;
const colorCanvasCtx = colorCanvas.getContext('2d');

const contrastCache: Record<string, string> = {};

/**
 * @see https://stackoverflow.com/questions/11068240/what-is-the-most-efficient-way-to-parse-a-css-color-in-javascript
 */
// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function colorToRGBA(inputColor: any): RGBAColor {
    if (inputColor == null || typeof inputColor !== 'string') {
        // If it's an array, we're gonna assume it's an already parsed color
        return Array.isArray(inputColor) ? inputColor as RGBAColor : null;
    }
    if (colorCache.hasOwnProperty(inputColor)) {
        const cached = colorCache[inputColor];
        return cached ? cached.slice() as RGBAColor : null;
    }

    colorCanvasCtx.clearRect(0, 0, 1, 1);
    // In order to detect invalid values,
    // we can't rely on col being in the same format as what fillStyle is computed as,
    // but we can ask it to implicitly compute a normalized value twice and compare.
    colorCanvasCtx.fillStyle = '#000';
    colorCanvasCtx.fillStyle = inputColor;
    const computed = colorCanvasCtx.fillStyle;
    colorCanvasCtx.fillStyle = '#fff';
    colorCanvasCtx.fillStyle = inputColor;
    if (computed !== colorCanvasCtx.fillStyle) {
        // invalid color
        colorCache[inputColor] = null;
        return null;
    }
    colorCanvasCtx.fillRect(0, 0, 1, 1);

    const outputColor = Array.from(colorCanvasCtx.getImageData(0, 0, 1, 1).data) as RGBAColor;
    colorCache[inputColor] = outputColor;

    return outputColor.slice() as RGBAColor;
}

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function colorToHex(inputColor: any): string {
    let rgba: RGBAColor;
    if (Array.isArray(inputColor)) {
        rgba = inputColor as RGBAColor;
    } else if (inputColor instanceof Uint8ClampedArray) {
        rgba = Array.from(inputColor) as RGBAColor;
    } else {
        rgba = colorToRGBA(inputColor);
        if (!rgba) {
            return null;
        }
        rgba = Array.from(rgba) as RGBAColor;
    }

    // In case we have a color without an alpha-channel, we add it
    if ((rgba as any).length === 3) {
        rgba.push(255);
    }

    return '#' + rgba.map(function (channelValue) {
        return channelValue < 10 ? '0' + channelValue.toString() : channelValue.toString(16);
    }).join('');
}

export function constrastColor(color: ColorValue): string {
    const rgba = colorToRGBA(color);
    if (!rgba) {
        return null;
    }

    const hex = colorToHex(rgba);
    if (contrastCache.hasOwnProperty(hex)) {
        return contrastCache[hex];
    }

    // https://stackoverflow.com/a/3943023/112731
    const contrast = (rgba[0] * 0.299 + rgba[1] * 0.587 + rgba[2] * 0.114) > 186
        ? '#000000'
        : '#FFFFFF';
    contrastCache[hex] = contrast;

    return contrast;
}
