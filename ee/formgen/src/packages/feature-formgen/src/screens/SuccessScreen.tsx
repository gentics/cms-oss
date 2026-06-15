import React, { useEffect, useMemo, useRef } from 'react';
import {
  Platform,
  StyleSheet,
  Animated,
  Easing,
  Dimensions,
  View,
} from 'react-native';
import {
  FormState,
  IFormFlowClassVariables,
  UIState,
} from '../types/interfaces';
import { t, TFunction } from 'i18next';
import { RestService } from '../services/RestService';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import AndpButton from '@andp/core/src/components/AndpButton';
import AndpText from '@andp/core/src/components/AndpText';
import CustomRenderHTML from '@andp/core/src/components/CustomRenderHTML';
import { successStyles as cusotmSuccessStyles } from '../styles/custom/custom-style';
import { successStyles } from '../styles/common/common-style';

interface SuccessScreenProps {
  formState: FormState;
  uiState: UIState;
  t: any;
  variables: IFormFlowClassVariables[];
  structure: any;
  restService: RestService;
  updateState: () => void;
  successContent?: string;
}

const { width } = Dimensions.get('window');

const SuccessScreen: React.FC<SuccessScreenProps> = (props) => {
  const fade = useRef(new Animated.Value(0)).current;
  const scale = useRef(new Animated.Value(0.8)).current;
  const breakPoints = useBreakpoints();
  const styles = useMemo(
    () => mergeStyles(successStyles, cusotmSuccessStyles),
    [],
  );

  const handleDone = () => {
    if (props.uiState?.andpNavigation?.previous) {
      props.uiState.andpNavigation.previous();
    }
  };

  // Resolve the redirect target for the current language, falling back to the
  // short culture code (e.g. "de-AT" -> "de") and finally the first entry.
  // only for CMS forms 
  const successUrl = useMemo(() => {
    const urls = props.formState?.successUrl;
    if (!urls) return undefined;
    const culture = props.formState?.culture ?? '';
    return (
      urls[culture] ?? urls[culture.split('-')[0]] ?? Object.values(urls)[0]
    );
  }, [props.formState?.successUrl, props.formState?.culture]);

  const handleSuccessUrl = () => {
    if (!successUrl) return;
    if (Platform.OS === 'web') {
      window.open(successUrl, '_self')?.focus();
    }
  };

  const renderSuccessUrlButton = () => {
    if (!successUrl) return null;
    return (
      <AndpButton
        title={t('form_success_redirect')}
        onPress={handleSuccessUrl}
        wrapperStyle={[
          styles.buttonWrapper,
          breakPoints.maximumSm && styles.buttonWrapperScreenMaxSmall,
          styles.primaryWrapper,
          { alignSelf: 'center', marginBottom: 24, flexGrow: 0, width: 'auto', minWidth: 180, marginTop: 20 },
        ]}
        type="primary"
        buttonStyle={[
          styles.primaryButton,
          { width: '100%', minWidth: 180 }
        ]}
        textStyle={[styles.primaryButtonText]}
      />
    );
  };

  useEffect(() => {
    Animated.parallel([
      Animated.timing(fade, {
        toValue: 1,
        duration: 500,
        easing: Easing.out(Easing.exp),
        useNativeDriver: Platform.OS !== 'web' ? true : false,
      }),
      Animated.sequence([
        Animated.spring(scale, {
          toValue: 1,
          friction: 4,
          useNativeDriver: Platform.OS !== 'web' ? true : false,
        }),

        Animated.sequence([
          Animated.timing(scale, {
            toValue: 1.05,
            duration: 650,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: Platform.OS !== 'web' ? true : false,
          }),
          Animated.timing(scale, {
            toValue: 1,
            duration: 650,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: Platform.OS !== 'web' ? true : false,
          }),
        ]),
      ]),
    ]).start();
  }, []);
  /* ---------- Markup ---------- */
  if (Platform.OS === 'web' && props.successContent?.trim()) {
    return (
      <Animated.View
        style={[
          breakPoints.maximumLg && styles.containerMaximumLg,
          breakPoints.minimumLg && styles.containerMinimumLg,
          { opacity: fade },
        ]}
      >
        <div
          style={StyleSheet.flatten(styles.htmlBox)}
          dangerouslySetInnerHTML={{ __html: props.successContent }}
        />
      </Animated.View>
    );
  }
  if (Platform.OS !== 'web' && props.successContent?.trim()) {
    return (
      <Animated.View style={[styles.container, { opacity: fade }]}>
        <CustomRenderHTML
          basePath={props.restService.initUrl}
          contentWidth={width}
          source={{ html: props.successContent }}
          defaultTextProps={{
            selectable: true,
          }}
          t={props.t}
          linkCallback={props.uiState.linkCallback}
        ></CustomRenderHTML>
        <AndpButton
          title={t('app_form_success_finish')}
          onPress={handleDone}
          wrapperStyle={[
            styles.buttonWrapper,
            breakPoints.maximumSm && styles.buttonWrapperScreenMaxSmall,
            styles.primaryWrapper,
            { alignSelf: 'center', marginBottom: 24 },
          ]}
          type="primary"
          buttonStyle={[styles.primaryButton]}
          textStyle={[styles.primaryButtonText]}
        />
      </Animated.View>
    );
  } else {
    return (
      <Animated.View
        style={[
          styles.successContainer,
          {
            opacity: fade,
          },
        ]}
      >
        <View style={{ alignItems: 'center', flex: 1, justifyContent: 'center'}}>
          <AndpText style={styles.successText}>
            {props.t('form_successDefault')}
          </AndpText>
          <Animated.View style={{ transform: [{ scale }] }}>
            <View style={styles.successIconBadge}>
              <AndpIcon name="checkMark" style={styles.successIcon} />
            </View>
          </Animated.View>
          {renderSuccessUrlButton()}
        </View>
        {Platform.OS != 'web' ? (
          <AndpButton
            title={t('app_form_success_finish')}
            onPress={handleDone}
            wrapperStyle={[
              styles.buttonWrapper,
              breakPoints.maximumSm && styles.buttonWrapperScreenMaxSmall,
              styles.primaryWrapper,
              { alignSelf: 'center', marginBottom: 24, flexGrow: 0, width: 'auto', minWidth: 180 },
            ]}
            type={'primary'}
            buttonStyle={[
              styles.primaryButton,
              { width: '100%', minWidth: 180 }
            ]}
            textStyle={[styles.primaryButtonText]}
          />
        ) : null}
      </Animated.View>
    );
  }
};

export default SuccessScreen;