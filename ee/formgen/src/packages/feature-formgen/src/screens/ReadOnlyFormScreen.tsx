import { View, ScrollView } from 'react-native';
import {
  FormState,
  IFormPage,
  SummaryOptions,
  UIState,
} from '../types/interfaces';
import { ElementRenderer } from './essentials/ElementRenderer';
import { getI18nText } from '../utils/Utils';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import { RestService } from '../services/RestService';
import { TFunction } from 'i18next';
import AndpText from '@andp/core/src/components/AndpText';
import { useMemo } from 'react';
import {
  mergeStyles,
  useBreakpoints,
  widthFromColumNumber,
} from '@andp/core/src/AndpUtils';
import FormgenScreenErrorOverlay from '../components/overlays/childOverlays/FormgenScreenErrorOverlay';
import { summaryStyles as customSummaryStyles } from '../styles/custom/custom-style';
import { summaryStyles } from '../styles/common/common-style';

interface ReadOnlyFormScreenProps {
  formState: FormState;
  uiState: UIState;
  restService: RestService;
  t: TFunction;
  updateState: (stateOption: StatesToUpdateEnum) => void;
}

const ReadOnlyFormScreen: React.FC<ReadOnlyFormScreenProps> = (props) => {
  const styles = useMemo(
    () => mergeStyles(summaryStyles, customSummaryStyles),
    [],
  );
  const formState = props.formState;
  const uiState = props.uiState;
  const breakpoints = useBreakpoints();
  const renderedElements: JSX.Element[] = [];
  const currentFormPageUiSchema: IFormPage | undefined =
    formState.form?.pages![uiState.currentPage];
  if (!currentFormPageUiSchema) {
    return (
      <FormgenScreenErrorOverlay
        title="Error loading resources"
        contentMessages={['No page found for the current index.']}
      />
    );
  }
  formState.form.pages!.forEach((obj: IFormPage, index: number) => {
    const objPageName = getI18nText(formState, obj.pagename);

    renderedElements.push(
      <AndpText
        key={`readonly-pageNumber-${index + '-' + obj.pagename}`}
        style={styles.sectionHeader}
      >
        {objPageName}
      </AndpText>,
    );

    renderedElements.push(
      <View
        key={`readonly-elements-${index + '-' + obj.pagename}`}
        style={styles.summarySection}
      >
        <ElementRenderer
          {...props}
          key={'readonly-elements-renderer-' + index + '-' + obj.pagename}
          elements={obj.elements}
          parentId={''}
          jsonpath={''}
          summary={SummaryOptions.ReadOnly}
          restService={props.restService}
          t={props.t}
          formState={props.formState}
        />
      </View>,
    );
  });

  return (
    <ScrollView
      style={[
        { marginHorizontal: 'auto' },
        widthFromColumNumber(formState.form.formwidth || 8, breakpoints),
      ]}
    >
      <View>{renderedElements}</View>
    </ScrollView>
  );
};

export default ReadOnlyFormScreen;
