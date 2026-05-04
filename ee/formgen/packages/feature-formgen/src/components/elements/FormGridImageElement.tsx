import React, { useMemo, useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  useWindowDimensions,
  Image,
  AccessibilityProps,
  Platform,
  PixelRatio,
  Pressable,
  Modal,
} from 'react-native';
import { SvgUri } from 'react-native-svg';
import { FormElementWrapper } from './helper/FormElementWrapper';
import {
  IFormComponentAttributes,
  IFormGridOptions,
  IFormGridImageOptions,
  FormState,
} from '../../types/interfaces';
import { TFunction } from 'i18next';
import { getI18nText } from '../../utils/Utils';
import { mergeStyles, fetchImageAsDataUri } from '@andp/core/src/AndpUtils';
import { formGridImageElementStyles } from '../../styles/common/common-style';
import { formGridImageElementStyles as customformGridImageElementStyles } from '../../styles/custom/custom-style';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { standardElement } from '@andp/core/src/styles/theme';
import { FormElementLoader } from './helper/UtilComponents';


type Align = 'start' | 'center' | 'end' | 'stretch';
type ObjectFit = 'contain' | 'cover' | 'fill' | 'none' | 'scale-down';

function isAbsolute(url: string): boolean {
  return /^https?:\/\//i.test(url);
}

function appendSizeParams(url: string, width?: number | 'auto', height?: number | 'auto'): string {
  const params: string[] = [];
  if (typeof width === 'number') params.push(`w=${width}`);
  if (typeof height === 'number') params.push(`h=${height}`);
  if (params.length === 0) return url;
  const hasQuery = url.includes('?');
  return `${url}${hasQuery ? '&' : '?'}${params.join('&')}`;
}

function resolveUrl(basePath: string, raw: string): string {
  if (!raw) return '';
  if (isAbsolute(raw)) return raw;
  const base = basePath?.replace(/\/$/, '') || '';
  const path = raw.startsWith('/') ? raw : `/${raw}`;
  return `${base}${path}`;
}

function mapAlign(align?: Align) {
  switch (align) {
    case 'center':
      return { alignItems: 'center' as const };
    case 'end':
      return { alignItems: 'flex-end' as const };
    case 'stretch':
      return { alignItems: 'stretch' as const };
    case 'start':
    default:
      return { alignItems: 'flex-start' as const };
  }
}

function mapResizeMode(fit?: ObjectFit) {
  switch (fit) {
    case 'cover':
      return 'cover' as const;
    case 'fill':
      return 'stretch' as const;
    case 'none':
      return 'contain' as const;
    case 'scale-down':
      return 'contain' as const;
    case 'contain':
    default:
      return 'contain' as const;
  }
}

function mapCaptionTextStyle(align?: Align) {
  switch (align) {
    case 'center':
      return { textAlign: 'center' as const, alignSelf: 'center' as const };
    case 'end':
      return { textAlign: 'right' as const, alignSelf: 'flex-end' as const };
    case 'stretch':
      return { textAlign: 'left' as const, alignSelf: 'stretch' as const };
    case 'start':
    default:
      return { textAlign: 'left' as const, alignSelf: 'flex-start' as const };
  }
}

const SUPPORTED_EXT = /\.(jpe?g|png|gif|svg)(\?|$)/i;

function isRenderableUrl(raw?: string): boolean {
  if (!raw) return false;
  if (isAbsolute(raw)) return true;
  return SUPPORTED_EXT.test(raw);
}

const FormGridImageElement: React.FC<IFormComponentAttributes> & {
  validate?: (
    state: FormState,
    jsonpath: string,
    id: string,
    updateErrorMessages: boolean,
    t: TFunction,
    formGridOptions?: IFormGridOptions,
    localValue?: any,
  ) => { isValid: boolean; errorMessage?: string };
  getSummaryValue?: (
    state: FormState,
    jsonpath: string,
    id: string,
    t: TFunction,
    formGridOptions?: IFormGridOptions,
    restService?: any
  ) => string | undefined;
} = (props: IFormComponentAttributes) => {

  const { formGridOptions, formState, uiState, restService, summary, t } = props;
  const { width: screenW, height: screenH } = useWindowDimensions();
  const [containerW, setContainerW] = useState<number>(0);
  const [localErrMsg, setLocalErrMsg] = useState<string>('');
  const img: IFormGridImageOptions | undefined = formGridOptions?.image;
  const objectFit: ObjectFit = img?.objectFit ?? 'contain';
  const align: Align = img?.align ?? 'start';
  const alt = (img?.alt && formState ? getI18nText(formState, img.alt) : '') || '';
  const caption = (img?.caption && formState ? getI18nText(formState, img.caption) : '') || '';

  const styles = useMemo(() => mergeStyles(formGridImageElementStyles, customformGridImageElementStyles), []);

  // URL
  const resolvedUrl = useMemo(() => {
    const raw = img?.url || '';
    if (!raw) return '';
    const baseResolved = resolveUrl(restService?.initUrl || '', raw);
    // absolute / externe URLs KEINE Query-Parameter anhängen -> Android/iOS Bild-Lader problem
    if (isAbsolute(baseResolved)) return baseResolved;
    return appendSizeParams(baseResolved, img?.width, img?.height);
  }, [img?.url, img?.width, img?.height, restService?.initUrl]);

  const isSvg = /\.svg($|\?)/i.test(resolvedUrl);
  const isGif = /\.gif($|\?)/i.test(resolvedUrl);
  const [dataUri, setDataUri] = useState<string | undefined>(undefined);
  const [fallbackStarted, setFallbackStarted] = useState(false);
  const probeStartedRef = useRef<boolean>(false);
  const [natW, setNatW] = useState<number | undefined>(undefined);
  const [natH, setNatH] = useState<number | undefined>(undefined);
  const measuredOnceRef = useRef<boolean>(false);

  const [ratio, setRatio] = useState<number | undefined>(undefined);
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);
  const explicitW = typeof img?.width === 'number' ? img.width : undefined;
  const explicitH = typeof img?.height === 'number' ? img.height : undefined;
  const baseW = containerW > 0 ? containerW : screenW;
  const targetW = explicitW ? Math.min(baseW, explicitW) : baseW;
  const targetH = explicitH;
  const lockedViewportHRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    setRatio(undefined);
    setLocalErrMsg('');
    setDataUri(undefined);
    setFallbackStarted(false);
    setNatW(undefined);
    setNatH(undefined);
    lockedViewportHRef.current = undefined;
    probeStartedRef.current = false;
  }, [resolvedUrl]);


  useEffect(() => {
    if (!resolvedUrl || isSvg) return;
    // Android + GIF: versuche Header-Probe statt getSize
    if (Platform.OS === 'android' && isGif) {
      setRatio((r) => r ?? 1);
      const tryProbeGifHeader = async () => {
        if (probeStartedRef.current) return;
        if (!isAbsolute(resolvedUrl)) return; // relative URLs: lassen wir regulär laden
        probeStartedRef.current = true;
        try {
          const res = await fetch(resolvedUrl, {
            method: 'GET',
            headers: {
              Range: 'bytes=0-9',
              'User-Agent': 'ANDP-Mobile/1.0 (+https://example.org)'
            },
          });
          if (!res.ok) { probeStartedRef.current = true; return; }
          const buf = new Uint8Array(await res.arrayBuffer());
          if (buf.length >= 10 && buf[0] === 0x47 && buf[1] === 0x49 && buf[2] === 0x46) {
            const w = buf[6] | (buf[7] << 8);
            const h = buf[8] | (buf[9] << 8);
            if (w > 0 && h > 0) {
              setNatW((p) => p ?? w);
              setNatH((p) => p ?? h);
              if (!targetH) setRatio((r) => r ?? h / w);
            }
          }
        } catch {}
      };
      tryProbeGifHeader();
      return;
    }
    Image.prefetch(resolvedUrl).catch(() => undefined);
    // Versuche die Bildgröße nur zu ermitteln, wenn keine feste Höhe definiert ist
    if (typeof img?.height !== 'number') {
      Image.getSize(
        resolvedUrl,
        (w, h) => {
          if (w && h) setRatio(h / w);
          if (w && h) { setNatW((p) => p ?? w); setNatH((p) => p ?? h); }
        },
        () => {
          // Fallback: quadratisches Verhältnis
          setRatio((r) => r ?? 1);
        },
      );
    }
  }, [resolvedUrl, isSvg, isGif, img?.height, targetH]);

  // Timeout-Fallback: Wenn auf Android bei GIF innerhalb von 5s kein onLoad/onError -> Fehlermeldung
  const [loaded, setLoaded] = useState(false);
  useEffect(() => {
    setLoaded(false);
  }, [resolvedUrl]);
  // SVG lädt -> kein Loader 
  useEffect(() => {
    if (isSvg) setLoaded(true);
  }, [isSvg]);
  useEffect(() => {
    // Für UA-Fallback-Fall (Android + GIF + absolute URL) keine frühzeitige Fehlermeldung per Timeout
    if (Platform.OS === 'android' && isGif && isAbsolute(resolvedUrl)) {
      return;
    }
    if (!(Platform.OS === 'android' && isGif)) return;
    if (loaded) return;
    const tm = setTimeout(() => {
      if (!loaded) setLocalErrMsg(t('form_image_invalidImageUrl'));
    }, 5000);
    return () => clearTimeout(tm);
  }, [loaded, isGif, t, resolvedUrl]);

  const a11y: AccessibilityProps = {
    accessibilityLabel: alt || undefined,
    accessible: !!alt,
  };

  const viewportW = targetW;
  const computedHFromNat = (natW && natH) ? Math.round((viewportW * natH) / natW) : undefined;
  const viewportH = targetH ?? (explicitW && computedHFromNat ? computedHFromNat : undefined);
  const fallbackSquareH = explicitW && !viewportH ? explicitW : undefined;
  const natDpW = natW;
  const natDpH = natH;
  const placeholderH = objectFit === 'none'
    ? (lockedViewportHRef.current ?? viewportH ?? natDpH ?? fallbackSquareH)
    : (viewportH ?? (ratio && viewportW ? viewportW * ratio : (explicitW ?? 200)));
  const warningIconSize = Math.max(24, Math.round(Math.min(viewportW, typeof placeholderH === 'number' ? placeholderH : 120) / 3));
  useEffect(() => {
    const cand = ((): number | undefined => {
      if (typeof targetH === 'number') return targetH;
      if (typeof explicitW === 'number') return computedHFromNat ?? explicitW;
      return natH;
    })();
    if (lockedViewportHRef.current === undefined && typeof cand === 'number' && cand > 0) {
      lockedViewportHRef.current = cand;
    }
  }, [targetH, explicitW, computedHFromNat, natH]);
  

  // Fallback Android Problem -> bei 403 gegen OkHttp/RN-Image liefert, einmalig -> Datei via fetch mit User-Agent
  // laden und als data: URI anzuzeigen. -> Volle Datei landet im JS-Speicher — für Mock/kleine GIFs okay.
  const tryFetchAsDataUri = async () => {
    if (fallbackStarted || !resolvedUrl) return;
    if (!(Platform.OS === 'android' && isGif && isAbsolute(resolvedUrl))) return;
    try {
      setFallbackStarted(true);
      const uri = await fetchImageAsDataUri(resolvedUrl);
      setDataUri(uri);
    } catch (e: any) {
      console.warn('[Image][fallback-fetch][android][err]', e?.message || e, resolvedUrl);
      setLocalErrMsg(t('form_image_invalidImageUrl'));
      setLoaded(true);
    }
  };

  const handlePressImage = () => {
    if (!resolvedUrl) return;
    if (Platform.OS === 'web') {
      try {
        (window as any)?.open(resolvedUrl, '_blank', 'noopener,noreferrer');
      } catch {}
    } else {
      setIsFullscreen(true);
    }
  };


  const showInForm = formGridOptions?.inForm !== false;
  const showInSummary = formGridOptions?.inSummary !== false;
  const isSummaryContext = summary !== undefined && summary !== 0; // SummaryOptions.No === 0
  if ((isSummaryContext && !showInSummary) || (!isSummaryContext && !showInForm)) {
    return null;
  }

  // Kein gültiges Bild
  if (!img?.url || (!isRenderableUrl(img.url) && !isAbsolute(resolvedUrl))) {
    return (
      <FormElementWrapper
        formState={formState}
        numberOfColumns={formGridOptions?.numberOfColumns}
        formComponentAttributes={props}
        uiState={uiState}
        hideLoading
        localErrorMsg={t('form_image_invalidImageUrl')}
        jsonPath={props.jsonpath}
        objectId={props.id}
      >
        <View style={{ width: targetW, ...(targetH ? { height: targetH } : {}), alignItems: 'center', justifyContent: 'center' }}>
          <AndpIcon name="warning" size={Math.max(24, Math.round((targetH || 120) / 3))} style={{ color: standardElement.validationError.color }} />
        </View>
      </FormElementWrapper>
    );
  }

  return (
    <FormElementWrapper
      formState={formState}
      numberOfColumns={formGridOptions?.numberOfColumns}
      formComponentAttributes={props}
      uiState={uiState}
      hideLoading
      localErrorMsg={localErrMsg}
      jsonPath={props.jsonpath}
      objectId={props.id}
    >
      <View
        style={[styles.container, mapAlign(align)]}
        onLayout={(e) => {
          const w = e?.nativeEvent?.layout?.width;
          if (typeof w === 'number' && w > 0 && w !== containerW) setContainerW(w);
        }}
      >
        {isSvg ? (
          localErrMsg ? (
            <View style={{ width: targetW, ...(targetH ? { height: targetH } : {}), alignItems: 'center', justifyContent: 'center' }}>
              <AndpIcon name="warning" size={warningIconSize} style={{ color: standardElement.validationError.color }} />
            </View>
          ) : (
            <Pressable accessibilityRole="imagebutton" onPress={handlePressImage} style={{ alignSelf: 'stretch' }}>
              <SvgUri
                uri={resolvedUrl}
                width={targetW}
                {...(targetH ? { height: targetH } : {})}
                {...a11y}
                onError={() =>
                  setLocalErrMsg(
                    t('form_image_invalidImageUrl'),
                  )
                }
              />
            </Pressable>
          )
        ) : (
          objectFit === 'none' ? (
            <View style={[{ width: viewportW, height: lockedViewportHRef.current ?? viewportH ?? natDpH ?? fallbackSquareH, overflow: 'hidden', position: 'relative' }, mapAlign(align)]}>
              {!loaded && !localErrMsg && (
                <View style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, alignItems: 'center', justifyContent: 'center', pointerEvents: 'none' }}>
                  <FormElementLoader loading={true} />
                </View>
              )}
              {localErrMsg ? (
                <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
                  <AndpIcon name="warning" size={warningIconSize} style={{ color: standardElement.validationError.color }} />
                </View>
              ) : (
              <Pressable accessibilityRole="imagebutton" onPress={handlePressImage} style={{ alignSelf: 'stretch' }}>
                <Image
                  source={{ uri: dataUri || resolvedUrl }}
                  onLoad={(() => {
                    // Stabilität + Android-Workaround: Auf iOS keine Größenänderungen, auf Android nat. Maße übernehmen
                    return (e?: any) => {
                      try {
                        if (Platform.OS === 'android' && !measuredOnceRef.current) {
                          const w = e?.nativeEvent?.source?.width;
                          const h = e?.nativeEvent?.source?.height;
                          if (w && h) {
                            if (!targetH) setRatio((r) => r ?? h / w);
                            setNatW((p) => p ?? w);
                            setNatH((p) => p ?? h);
                          }
                        }
                      } catch {}
                      if (localErrMsg) setLocalErrMsg('');
                      setLoaded(true);
                      measuredOnceRef.current = true;
                    };
                  })()}
                  onError={(err) => {
                    try { const nativeErr = err?.nativeEvent || err; console.warn('[Image][onError]', Platform.OS, { isGif, url: resolvedUrl, nativeErr }); } catch {}
                    // Android + GIF + absolute URL: versuche UA-Fallback, zeige solange keinen Fehler
                    if (Platform.OS === 'android' && isGif && isAbsolute(resolvedUrl)) {
                      setLoaded(false); // Spinner bleibt sichtbar
                      tryFetchAsDataUri();
                    } else {
                      setLocalErrMsg(t('form_image_invalidImageUrl'));
                      setLoaded(true);
                    }
                  }}
                  fadeDuration={0}
                  {...(Platform.OS === 'ios' ? { progressiveRenderingEnabled: false as any } : {})}
                  resizeMode={'stretch'}
                  style={{ width: natDpW ?? viewportW, height: natDpH ?? (lockedViewportHRef.current ?? viewportH ?? ((natDpW && ratio) ? (natDpW * (natH ?? 0)) / (natW ?? 1) : fallbackSquareH)) }}
                  {...a11y}
                />
              </Pressable>
              )}
            </View>
          ) : (
          <View
            style={((): any => {
              if (objectFit === 'scale-down' && natDpW && natDpH) {
                const fw = viewportW / natDpW;
                const fh = typeof viewportH === 'number' ? viewportH / natDpH : Number.POSITIVE_INFINITY;
                const f = Math.min(1, isFinite(fh) ? Math.min(fw, fh) : fw);
                return {
                  width: Math.max(1, Math.round(natDpW * f)),
                  height: Math.max(1, Math.round(natDpH * f)),
                  position: 'relative',
                };
              }
              return {
                width: viewportW,
                maxWidth: '100%',
                height: viewportH ?? (ratio && viewportW ? viewportW * ratio : undefined),
                ...(!targetH && !ratio ? { aspectRatio: 1 as any, backgroundColor: 'transparent' } : {}),
                position: 'relative',
              };
            })()}
            collapsable={false}
          >
            {!loaded && !localErrMsg && (
              <View style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, alignItems: 'center', justifyContent: 'center', pointerEvents: 'none' }}>
                <FormElementLoader loading={true} />
              </View>
            )}
            {localErrMsg ? (
              <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
                <AndpIcon name="warning" size={warningIconSize} style={{ color: standardElement.validationError.color }} />
              </View>
            ) : (
            <Pressable accessibilityRole="imagebutton" onPress={handlePressImage} style={{ alignSelf: 'stretch' }}>
              <Image
                source={{ uri: dataUri || resolvedUrl }}
                fadeDuration={0}
                onLoad={(() => {
                  // Stabilität: vermeide Größenänderungen im onLoad (insb. GIF/iOS)
                  return (e?: any) => {
                    // Android: nat. Maße aus onLoad erfassen (GIF)
                    try {
                      if (Platform.OS === 'android' && !measuredOnceRef.current) {
                        const w = e?.nativeEvent?.source?.width;
                        const h = e?.nativeEvent?.source?.height;
                        if (w && h) {
                          if (!targetH) setRatio((r) => r ?? h / w);
                          setNatW((p) => p ?? w);
                          setNatH((p) => p ?? h);
                        }
                      }
                    } catch {}
                    if (localErrMsg) setLocalErrMsg('');
                    setLoaded(true);
                    measuredOnceRef.current = true;
                  };
                })()}
                onError={(err) => {
                  // Log extended error info if available
                  try {
                    const nativeErr = err?.nativeEvent || err;
                    console.warn('[Image][onError]', Platform.OS, { isGif, url: resolvedUrl, nativeErr });
                  } catch {}
                  // Android + GIF + absolute URL: versuche UA-Fallback, zeige solange keinen Fehler
                  if (Platform.OS === 'android' && isGif && isAbsolute(resolvedUrl)) {
                    setLoaded(false); // Spinner bleibt sichtbar
                    tryFetchAsDataUri();
                  } else {
                    setLocalErrMsg(t('form_image_invalidImageUrl'));
                    setLoaded(true);
                  }
                }}
                resizeMode={mapResizeMode(objectFit)}
                {...(Platform.OS === 'ios' ? { progressiveRenderingEnabled: false as any } : {})}
                style={((): any => {
                  if (objectFit === 'scale-down' && natDpW && natDpH) {
                    const fw = viewportW / natDpW;
                    const fh = typeof viewportH === 'number' ? viewportH / natDpH : Number.POSITIVE_INFINITY;
                    const f = Math.min(1, isFinite(fh) ? Math.min(fw, fh) : fw);
                    return {
                      width: Math.max(1, Math.round(natDpW * f)),
                      height: Math.max(1, Math.round(natDpH * f)),
                    };
                  }
                  return {
                    width: viewportW,
                    maxWidth: '100%',
                    height: viewportH ?? (ratio && viewportW ? viewportW * ratio : undefined),
                    ...(!targetH && !ratio ? { aspectRatio: 1 as any, backgroundColor: 'transparent' } : {}),
                  };
                })()}
                {...a11y}
              />
            </Pressable>
            )}
          </View>
          )
        )}

        {!!caption && (
          <Text style={[styles.caption, mapCaptionTextStyle(align)]} selectable>
            {caption}
          </Text>
        )}
      </View>
      {/* Fullscreen Modal for native platforms */}
      {Platform.OS !== 'web' && (
        <Modal
          visible={isFullscreen}
          onRequestClose={() => setIsFullscreen(false)}
          animationType="fade"
          transparent={false}
        >
          <Pressable
            onPress={() => setIsFullscreen(false)}
            style={{ flex: 1, backgroundColor: 'black', alignItems: 'center', justifyContent: 'center' }}
            accessibilityRole="button"
          >
            {isSvg ? (
              <SvgUri uri={resolvedUrl} width={screenW} height={screenH} />
            ) : (
              <Image
                source={{ uri: dataUri || resolvedUrl }}
                resizeMode="contain"
                style={{ width: screenW, height: screenH }}
              />
            )}
          </Pressable>
        </Modal>
      )}
    </FormElementWrapper>
  );
};


FormGridImageElement.validate = (
  _state,
  _jsonpath,
  _id,
  _updateErrorMessages,
  _t,
  fgo,
) => {
  if (fgo?.type === 'formgridImage') {
    return { isValid: true };
  }
  return { isValid: true };
};


FormGridImageElement.getSummaryValue = function (
  state: FormState,
  _jsonpath: string,
  _id: string,
  _t: TFunction,
  formGridOptions?: IFormGridOptions,
  restService?: { initUrl?: string }
) {
  if (formGridOptions && formGridOptions.inSummary === false) return '';
  if (formGridOptions?.type === 'formgridSpacer') return ' ';
  if (formGridOptions?.type === 'formgridImage') {
    const img = formGridOptions.image;
    const errMsg = _t ? _t('form_image_invalidImageUrl') : 'Bild konnte nicht geladen werden.';
    const base = restService?.initUrl || '';
    const baseResolved = resolveUrl(base, img?.url || '');
    const rawHasKnownExt = !!img?.url && SUPPORTED_EXT.test(img.url);
    const resolvedIsAbs = !!baseResolved && isAbsolute(baseResolved);

    if (!img?.url || (!rawHasKnownExt && !resolvedIsAbs)) {
      const captionCss = ((): string => {
        switch (img?.align) {
          case 'center':
            return 'text-align:center;';
          case 'end':
            return 'text-align:right;';
          case 'stretch':
            return 'text-align:left;';
          case 'start':
          default:
            return 'text-align:left;';
        }
      })();
      const errorHtml = `<div style=\"${captionCss}color:#B00020;\">${errMsg}</div>`;
      return errorHtml;
    }
    const sizeUrl = isAbsolute(baseResolved)
      ? baseResolved
      : appendSizeParams(
          baseResolved,
          typeof img.width === 'number' ? img.width : undefined,
          typeof img.height === 'number' ? img.height : undefined,
        );
    const alt = (img.alt ? getI18nText(state, img.alt) : '') || '';
    const cap = img.caption ? getI18nText(state, img.caption) : '';
    const alignAttr = img.align ?? 'start';
    const figureHtml = `<figure data-andp-imgblock="1" data-src="${sizeUrl}" data-alt="${alt.replace(/"/g, '&quot;')}" data-caption="${cap.replace(/"/g, '&quot;')}" data-align="${alignAttr}"></figure>`;
    return figureHtml;
  }
  if (formGridOptions?.valueSummary) {
    return getI18nText(state, formGridOptions.valueSummary) || '';
  }
  return '';
};

export default FormGridImageElement;