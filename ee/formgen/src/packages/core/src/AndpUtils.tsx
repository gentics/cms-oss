import { useWindowDimensions, StyleSheet } from 'react-native';
import {
  interpolate,
  interpolateColor,
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from 'react-native-reanimated';

export const mergeStyles = (common: any, custom: any) => {
  return new Proxy(common, {
    get(target, prop: string) {
      const baseStyle = target[prop];
      const overrideStyle = custom?.[prop];

      const clean = (style: any) => {
        if (!style || typeof style !== 'object') return style;
        return Object.fromEntries(
          Object.entries(style).filter(
            ([_, value]) => value !== undefined && value !== null,
          ),
        );
      };

      const cleanBase = Array.isArray(baseStyle)
        ? baseStyle.map(clean)
        : clean(baseStyle);
      const cleanOverride = Array.isArray(overrideStyle)
        ? overrideStyle.map(clean)
        : clean(overrideStyle);

      const combined =
        Array.isArray(baseStyle) || Array.isArray(overrideStyle)
          ? [
              ...(Array.isArray(cleanBase) ? cleanBase : [cleanBase]),
              ...(Array.isArray(cleanOverride)
                ? cleanOverride
                : [cleanOverride]),
            ]
          : [cleanBase, cleanOverride];

      // Entfernt null/undefined und komplett leere Objekte
      return StyleSheet.flatten(combined.filter((item) => {
        if (item == null) return false;
        if (typeof item === 'object') {
          return Object.keys(item).length > 0;
        }
        return true;
      }));
    },
  });
};

export const useBreakpoints = () => {
  const { width } = useWindowDimensions();

  return {
    minimumSm: width >= 576,
    minimumMd: width >= 768,
    minimumLg: width >= 992,
    minimumXl: width >= 1200,
    minimumXxl: width >= 1400,
    maximumSm: width < 576,
    maximumMd: width < 768,
    maximumLg: width < 992,
    maximumXl: width < 1200,
    maximumX: width < 1400,
  };
};

const bootstrapColWidths: { [key: number]: string } = {
  1: '8.3333%',
  2: '16.6667%',
  3: '25%',
  4: '33.3333%',
  5: '41.6667%',
  6: '50%',
  7: '58.3333%',
  8: '66.6667%',
  9: '75%',
  10: '83.3333%',
  11: '91.6667%',
  12: '100%',
};

export const widthFromColumNumber = (
  formWidth: number | undefined,
  breakpoints: ReturnType<typeof useBreakpoints>,
): { width: any } => {
  if (formWidth && breakpoints) {
    return {
      width: breakpoints.maximumMd
        ? '100%'
        : bootstrapColWidths[Number(formWidth) || 12],
    };
  } else {
    return { width: bootstrapColWidths[12] };
  }
};

export const enhancedWidthFromColumNumber = (
  formWidth: number | undefined,
  maximumMd: boolean,
  useMinWidth: boolean = false,
): { width: any } | { minWidth: any } => {
  const prop: 'width' | 'minWidth' = useMinWidth ? 'minWidth' : 'width';

  const value = maximumMd ? '100%' : bootstrapColWidths[formWidth || 12];

  return { [prop]: value } as { width: any } | { minWidth: any };
};

export const useHoverStyle = (
  baseStyle: any,
  styleOnHover: any,
  transitionDuration: number = 100,
) => {
  const hover = useSharedValue(0);

  const mergeStyles = (styles: any): Record<string, any> => {
    if (!Array.isArray(styles)) {
      return styles || {};
    }
    return styles.reduce((acc: any, curr: any) => {
      if (curr && typeof curr === 'object') {
        return { ...acc, ...curr };
      }
      return acc;
    }, {});
  };

  const mergedBaseStyle = mergeStyles(baseStyle);
  const mergedHoverStyle = mergeStyles(styleOnHover);

  const animatedStyle = useAnimatedStyle(() => {
    const resolved: any = {};

    const allKeys = new Set([
      ...Object.keys(mergedBaseStyle || {}),
      ...Object.keys(mergedHoverStyle || {}),
    ]);

    for (const key of allKeys) {
      const from = mergedBaseStyle?.[key];
      const to = mergedHoverStyle?.[key];

      if (typeof from === 'number' && typeof to === 'number') {
        resolved[key] = interpolate(hover.value, [0, 1], [from, to]);
      } else if (
        key === 'backgroundColor' &&
        typeof from === 'string' &&
        typeof to === 'string'
      ) {
        resolved[key] = interpolateColor(hover.value, [0, 1], [from, to]);
      } else {
        resolved[key] = hover.value ? to : from;
      }
    }

    return resolved;
  });

  const onHoverIn = () => {
    console.log('Hovering');
    hover.value = withTiming(1, { duration: transitionDuration });
  };

  const onHoverOut = () => {
    hover.value = withTiming(0, { duration: transitionDuration });
  };

  return { animatedStyle, onHoverIn, onHoverOut };
};

export const useToggleStyle = (
  baseStyle: any,
  toggledStyle: any,
  duration: number = 200,
) => {
  const toggle = useSharedValue(0);

  const mergeStyles = (styles: any): Record<string, any> => {
    if (!Array.isArray(styles)) return styles || {};
    return styles.reduce((acc: any, curr: any) => {
      if (curr && typeof curr === 'object') {
        return { ...acc, ...curr };
      }
      return acc;
    }, {});
  };

  const mergedBaseStyle = mergeStyles(baseStyle);
  const mergedToggledStyle = mergeStyles(toggledStyle);

  const animatedStyle = useAnimatedStyle(() => {
    const resolved: any = {};

    const allKeys = new Set([
      ...Object.keys(mergedBaseStyle || {}),
      ...Object.keys(mergedToggledStyle || {}),
    ]);

    for (const key of allKeys) {
      const from = mergedBaseStyle?.[key];
      const to = mergedToggledStyle?.[key];

      if (typeof from === 'number' && typeof to === 'number') {
        resolved[key] = interpolate(toggle.value, [0, 1], [from, to]);
      } else if (
        key === 'backgroundColor' &&
        typeof from === 'string' &&
        typeof to === 'string'
      ) {
        resolved[key] = interpolateColor(toggle.value, [0, 1], [from, to]);
      } else {
        resolved[key] = toggle.value ? to : from;
      }
    }

    return resolved;
  });

  const toggleOn = () => {
    toggle.value = withTiming(1, { duration });
  };

  const toggleOff = () => {
    toggle.value = withTiming(0, { duration });
  };

  const toggleSwitch = () => {
    toggle.value = withTiming(toggle.value === 1 ? 0 : 1, { duration });
  };

  return { animatedStyle, toggleOn, toggleOff, toggleSwitch };
};

type StyleObject = Record<string, string | number | undefined>;

/**
 * Wandelt camelCase-Eigenschaften in kebab-case um.
 */
function toKebabCase(prop: string): string {
  return prop
    .replace(/([a-z])([A-Z])/g, '$1-$2')
    .replace(/^ms-/, '-ms-')
    .toLowerCase();
}

/**
 * Listet CSS-Eigenschaften auf, die keine Einheit ("px") benötigen.
 */
const unitlessProps = new Set([
  'opacity', 'zIndex', 'lineHeight', 'fontWeight',
  'flex', 'flexGrow', 'flexShrink', 'flexBasis',
]);

/**
 * Konvertiert ein Style-Objekt in einen CSS-Block ohne Selector.
 * @param styles Ein Objekt mit camelCase-Keys und string|number-Werten
 * @returns String wie "font-size:16px;\ncolor:#333;\n…"
 */
function styleObjectToCssDeclarations(styles: StyleObject): string {
  return Object.entries(styles)
    .filter(([, value]) => value != null)
    .map(([key, value]) => {
      const cssProp = toKebabCase(key);
      const cssVal =
        typeof value === 'number' && !unitlessProps.has(key)
          ? `${value}px`
          : value?.toString();
      return `${cssProp}: ${cssVal};`;
    })
    .join('\n');
}

/**
 * Baut aus einem Selector und einem Style-Objekt einen kompletten CSS-Block.
 * @param selector z. B. "h1" oder ".my-class"
 * @param styles Style-Objekt
 * @returns CSS-Block als String
 */
export function convertStyleToCssBlock(selector: string, styles: StyleObject | Array<any>): string {
  const flattenStyles = StyleSheet.flatten(styles)
  const decls = styleObjectToCssDeclarations(flattenStyles);
  return `${selector} {\n${decls}\n}`;
}

/**
 * Lädt eine Bild-URL (z. B. GIF) per fetch und gibt eine data:-URI zurück.
 * Wird als Android-Workaround verwendet, wenn RN/Image das Asset aufgrund von UA/Range/Caching nicht lädt.
 */
export async function fetchImageAsDataUri(
  url: string,
  opts?: { userAgent?: string; accept?: string }
): Promise<string> {
  const ua = opts?.userAgent || 'ANDP-Mobile/1.0 (+https://gentics.com; contact: dev@andp.local)';
  const accept = opts?.accept || 'image/*';
  const res = await fetch(url, {
    method: 'GET',
    headers: {
      'User-Agent': ua,
      Accept: accept,
      'Cache-Control': 'no-cache',
    } as any,
  } as any);
  const ct = (res as any)?.headers?.get?.('content-type') || 'image/gif';
  if (!(res as any).ok) {
    throw new Error(`fetchImageAsDataUri !ok status=${(res as any).status} contentType=${ct}`);
  }
  const buf = await (res as any).arrayBuffer();
  const bytes = new Uint8Array(buf);
  let binary = '';
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk) as unknown as number[]);
  }
  const base64 = (global as any).btoa
    ? (global as any).btoa(binary)
    : (typeof Buffer !== 'undefined'
        ? Buffer.from(binary, 'binary').toString('base64')
        : undefined);
  if (!base64) throw new Error('fetchImageAsDataUri base64 encode failed');
  return `data:${ct};base64,${base64}`;
}


