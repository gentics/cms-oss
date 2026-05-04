import React, { useMemo, useState } from 'react';
import { Image, Linking, StyleSheet, ScrollView, View } from 'react-native';
import RenderHtml, { RenderHTMLProps, TNode } from 'react-native-render-html';
import { decode } from 'he';
import { Platform } from 'react-native';
import theme from '../styles/theme.native';
import { mergeStyles, fetchImageAsDataUri } from '@andp/core/src/AndpUtils';
import { renderHTMLStyles } from '../styles/common/common-style';
import { renderHTMLStyles as customRenderHTMLStyles } from '../styles/custom/custom-style';
import AndpText from './AndpText';

interface CustomRenderHTMLProps extends RenderHTMLProps {
  basePath: string;
  linkCallback?: (element: any) => void;
  t: (key: string, fallback?: string) => string;
}

const CustomRenderHTMLComponent: React.FC<CustomRenderHTMLProps> = ({
  basePath,
  linkCallback,
  t,
  ...restProps
}) => {
  //Grab the raw HTML string from restProps.source
  const rawHtml =
    typeof restProps.source === 'object' && 'html' in restProps.source
      ? restProps.source.html
      : '';

  // Web: Wenn kein <img> -> dangerouslySetInnerHTML
  // bei <img>-Tags -> RenderHtml, um Bild-Fehler mit Fallback-Text abzufangen
  if (
    Platform.OS === 'web' &&
    (!rawHtml ||
      (rawHtml.indexOf('<img') === -1 && rawHtml.indexOf('<figure') === -1))
  ) {
    return <span dangerouslySetInnerHTML={{ __html: rawHtml ?? '' }} />;
  }

  const styles = useMemo(
    () => mergeStyles(renderHTMLStyles, customRenderHTMLStyles),
    [],
  );
  // TagsStyles kommen aus den StyleSheets (common + custom), dadurch zentral konfigurierbar
  const tagsStyles = useMemo(
    () => ({
      ul: styles.ul,
      ol: styles.ol,
      li: styles.li,
      blockquote: styles.blockquote,
      code: styles.code,
      pre: styles.pre,
    }),
    [styles],
  );
  //Decode all HTML‐encoded entities into real Unicode
  const decodedHtml = decode(rawHtml || '');

  //If there were any stray BOMs (\uFEFF) at the start, strip them out
  const cleanHtml = decodedHtml.replace(/^\uFEFF/, '');

  const htmlSource = { html: cleanHtml };

  const renderImg = (props: { tnode: TNode }) => {
    const ImgWithFallback: React.FC = () => {
      const [error, setError] = useState(false);
      let src = props.tnode.attributes?.src || '';
      const alt = props.tnode.attributes?.alt || '';
      if (src.startsWith('/')) {
        src = `${basePath}${src}`;
      }
      if (error) {
        return (
          <AndpText style={styles.imageError}>
            {t('form_image_invalidImageUrl')}
          </AndpText>
        );
      }
      return (
        <Image
          source={{ uri: src }}
          style={styles.image}
          resizeMode="contain"
          onError={() => setError(true)}
        />
      );
    };
    return (
      <View style={styles.imageContainer}>
        <ImgWithFallback />
      </View>
    );
  };

  const renderLink = (props: {
    tnode: TNode;
    TDefaultRenderer: React.ComponentType<any>;
  }) => {
    let href = props.tnode.attributes?.href || '';
    const onPressHandler = () => {
      if (href.startsWith(basePath) && linkCallback) {
        const path = href.substring(basePath.length);
        linkCallback(path);
        return;
      }
      if (!href.startsWith('http') && linkCallback) {
        linkCallback(href);
        return;
      }
      if (href.startsWith('/')) {
        href = `${basePath}${href}`;
      }
      Linking.openURL(href);
    };
    const rendered = <props.TDefaultRenderer {...props} />;
    return React.cloneElement(rendered, { onPress: onPressHandler });
  };

  const renderText = (props: {
    tnode: TNode;
    TDefaultRenderer: React.ComponentType<any>;
  }) => {
    return <props.TDefaultRenderer {...props} />;
  };
  const renderParagraph = (props: {
    tnode: TNode;
    TDefaultRenderer: React.ComponentType<any>;
  }) => {
    return <props.TDefaultRenderer {...props} />;
  };

  const renderHeading = (props: {
    tnode: TNode;
    TDefaultRenderer: React.ComponentType<any>;
  }) => {
    return <props.TDefaultRenderer {...props} />;
  };

  const renderFigure = (props: {
    tnode: TNode;
    TDefaultRenderer: React.ComponentType<any>;
  }) => {
    const attr = props.tnode.attributes || ({} as any);
    if (!('data-andp-imgblock' in attr)) {
      return <props.TDefaultRenderer {...props} />;
    }
    let src = attr['data-src'] || '';
    const alt = attr['data-alt'] || '';
    const caption = attr['data-caption'] || '';
    const align = attr['data-align'] || 'start';

    const Caption: React.FC<{ hidden?: boolean }> = ({ hidden }) => {
      if (hidden || !caption) return null;
      const textAlign =
        align === 'center' ? 'center' : align === 'end' ? 'right' : 'left';
      return (
        <AndpText style={[styles.caption, { textAlign }]}>{caption}</AndpText>
      );
    };

    if (src.startsWith('/')) src = `${basePath}${src}`;

    const ImgWithFallback: React.FC = () => {
      const [error, setError] = useState(false);
      const [dataUri, setDataUri] = useState<string | undefined>(undefined);
      const [fallbackStarted, setFallbackStarted] = useState(false);
      const isGif = /\.gif($|\?)/i.test(src);
      const isAbs = /^https?:\/\//i.test(src);
      if (error) {
        return (
          <>
            <AndpText style={styles.imageError}>
              {t('form_image_invalidImageUrl')}
            </AndpText>
            <Caption hidden={true} />
          </>
        );
      }

      const tryFetchAsDataUri = async () => {
        if (fallbackStarted) return;
        if (!(Platform.OS === 'android' && isGif && isAbs)) return;
        try {
          setFallbackStarted(true);
          const uri = await fetchImageAsDataUri(src);
          setDataUri(uri);
        } catch (e: any) {
          try { console.warn('[CustomRenderHTML][fallback-fetch][err]', e?.message || e); } catch {}
          setError(true);
        }
      };

      return (
        <>
          <Image
            source={{ uri: dataUri || src }}
            style={styles.image}
            resizeMode="contain"
            onError={() => {
              if (Platform.OS === 'android' && isGif && isAbs && !fallbackStarted) {
                tryFetchAsDataUri();
                return;
              }
              setError(true);
            }}
          />
          <Caption />
        </>
      );
    };

    return <ImgWithFallback />;
  };

  const customRenderers = useMemo(
    () => ({
      img: renderImg,
      a: renderLink,
      p: renderParagraph,
      h1: renderHeading,
      h2: renderHeading,
      h3: renderHeading,
      h4: renderHeading,
      h5: renderHeading,
      h6: renderHeading,
      span: renderText,
      b: renderText,
      i: renderText,
      figure: renderFigure,
    }),
    [],
  );

  return (
    <ScrollView
      style={styles.scrollContainer}
      alwaysBounceVertical={false} // Prevent "rubber band"/"elastic scrolling" effect on iOS
    >
      <RenderHtml
        {...restProps}
        source={htmlSource}
        baseStyle={{ fontFamily: theme.fontFamily.regular }}
        tagsStyles={tagsStyles}
        renderers={customRenderers}
        enableExperimentalBRCollapsing={true}
      />
    </ScrollView>
  );
};

// Custom "areEqual"-Funktion, die die für das erneute Rendern relevanten Props vergleicht.
const areEqual = (
  prevProps: CustomRenderHTMLProps,
  nextProps: CustomRenderHTMLProps,
): boolean => {
  if (prevProps.basePath !== nextProps.basePath) return false;
  if (prevProps.linkCallback !== nextProps.linkCallback) return false;
  if (prevProps.t !== nextProps.t) return false;
  if (prevProps.contentWidth !== nextProps.contentWidth) return false;

  // Hier vergleichen wir z. B. den baseStyle und den HTML-Source.
  if (
    JSON.stringify(prevProps.baseStyle) !== JSON.stringify(nextProps.baseStyle)
  )
    return false;
  if (JSON.stringify(prevProps.source) !== JSON.stringify(nextProps.source))
    return false;

  return true;
};

// Mit React.memo wird die Komponente nur neu gerendert, wenn "areEqual" false zurückgibt.
const CustomRenderHTML = React.memo(CustomRenderHTMLComponent, areEqual);
export default CustomRenderHTML;
