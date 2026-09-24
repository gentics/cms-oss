import { View, Text, StyleSheet, ScrollView, Platform } from 'react-native';
import {
  FormState,
  IFormFlowClassVariables,
  IFormPage,
  ISummaryDetailsUser,
  SummaryOptions,
  UIState,
} from '../types/interfaces';
import { t, TFunction } from 'i18next';
import SummaryDetailsScreenPart from './screen-parts/SummaryDetailsScreenPart';
import { ElementRenderer } from './essentials/ElementRenderer';
import { getI18nText } from '../utils/Utils';
import { RestService } from '../services/RestService';
import AndpText from '@andp/core/src/components/AndpText';
import theme from '@andp/core/src/styles/theme';
import { summaryStyles, baseStyles } from '../styles/common/common-style';
import {
  summaryStyles as customSummaryStyles,
  baseStyles as customBaseStyles,
} from '../styles/custom/custom-style';
import { mergeStyles, useBreakpoints } from '@andp/core/src/AndpUtils';
import { widthFromColumNumber } from '@andp/core/src/AndpUtils';
import { useMemo } from 'react';

interface SummaryScreenProps {
  formState: FormState;
  uiState: UIState;
  t: TFunction;
  variables: IFormFlowClassVariables[];
  structure: any;
  restService: RestService;
  updateState: () => void;
  userData?: ISummaryDetailsUser;
  printRef: any;
}

const SummaryScreen: React.FC<SummaryScreenProps> = (props) => {
  const styles = useMemo(
    () => mergeStyles(summaryStyles, customSummaryStyles),
    [],
  );
  const baseMergedStyles = useMemo(
    () => mergeStyles(baseStyles, customBaseStyles),
    [],
  );
  const breakpoints = useBreakpoints();
  const formState = props.formState;
  const renderedElements: JSX.Element[] = [];
  const userData: ISummaryDetailsUser = props.userData
    ? props.userData
    : { name: '', email: '', username: '' };
  const showDetails: IFormFlowClassVariables | undefined = props.variables.find(
    (x) => {
      if (x.name === 'showDetails') {
        return x;
      }
    },
  );
  const structure: any = formState.form;

  // TODO -> First Implementing childElements => new Name: ElementRenderer
  structure.pages.forEach((obj: IFormPage, index: number) => {
    const objPageName = getI18nText(formState, obj.pagename);

    renderedElements.push(
      <AndpText
        key={`summary-pageNumber-${index}`}
        style={styles.sectionHeader}
      >
        {objPageName}
      </AndpText>,
    );

    renderedElements.push(
      <View key={`summary-elements-${index}`} style={styles.summarySection}>
        <ElementRenderer
          elements={obj.elements}
          jsonpath={''}
          formState={formState}
          uiState={props.uiState}
          parentId={''}
          contextId={''}
          summary={SummaryOptions.SummaryText}
          restService={props.restService}
          updateState={props.updateState}
          t={props.t}
          key={'summary-elements-elementRenderer-' + index}
        />
      </View>,
    );
  });

  return (
    <ScrollView
      style={[
        baseMergedStyles.formContainer,
        widthFromColumNumber(formState.form.formwidth || 8, breakpoints),
      ]}
      {...(Platform.OS === 'web' && { nativeID: 'print-root' })}
      ref={props.printRef}
      key={'summaryView'}
    >
      <View key={'summaryView-View'}>
        {renderedElements}
        {showDetails != undefined &&
        showDetails != null &&
        showDetails.value == true ? (
          <SummaryDetailsScreenPart
            key={'summaryView-summaryDetails'}
            t={props.t}
            formState={formState}
            email={userData.email}
            username={userData.username}
            displayname={userData.name}
          />
        ) : (
          <></>
        )}
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  section: {
    borderBottomColor: theme.border.color.gray,
    borderBottomWidth: 2,
    paddingVertical: 12,
    paddingHorizontal: 15,
  },
  sectionHeader: {
    fontSize: theme.fontSize.extraLarge,
    fontWeight: 'bold',
    paddingVertical: 16,
    paddingHorizontal: 15,
  },
});

export default SummaryScreen;
