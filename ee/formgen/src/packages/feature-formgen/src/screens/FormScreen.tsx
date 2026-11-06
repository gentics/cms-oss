import React, { useMemo } from 'react';
import { View } from 'react-native';
import {
  IFormPage,
  IFormPageComponent,
  SummaryOptions,
} from '../types/interfaces';
import { ElementRenderer } from './essentials/ElementRenderer';
import { ExpertSearchTable } from './screen-parts/ExpertSearchTable';
import AndpText from '@andp/core/src/components/AndpText';
import { OverlayTypeEnum } from '../custom/OverlayTypes';

// IFormPageComponent
const FormScreen: React.FC<IFormPageComponent> = (
  props: IFormPageComponent,
) => {
  const formState = props.formState;
  const uiState = props.uiState;
  const restService = props.restService;

  const currentFormPageUiSchema: IFormPage | undefined =
    formState.form?.pages?.[uiState.currentPage];
  const elements = currentFormPageUiSchema?.elements;
  if (!currentFormPageUiSchema && !uiState.isExpertSearch) {
    // Transition → nichts anzeigen. Sonst wird beim Wechseln 
    // zwischen Formularen immer kurz "No page found..." angezeigt
    if (!uiState.initDone || uiState.loading) return null; 
    return (
      <View>
        <AndpText>No page found for the current index.</AndpText>
      </View>
    );
  }
  let summary = SummaryOptions.No;

  if (uiState.isReadOnlyForm) {
    summary = SummaryOptions.ReadOnly;
  }

  const ElementRendererMemo = useMemo(() => {
    // If there are no elements for this page, render nothing.
    if (!elements || elements.length === 0) {
      return null;
    }

    // Memoized rendering of the form elements for the current page. This avoids
    // re-rendering the whole tree unless either the underlying data or the
    // current page (and therefore the elements) change.
    return (
      <ElementRenderer
        {...props}
        summary={summary}
        elements={elements}
        parentId=""
        contextId=""
        jsonpath=""
        t={props.t}
        isLoggedIn={props.isLoggedIn}
        handleSearch={props.handleSearch}
        handleExpertSearch={props.handleExpertSearch}
      />
    );
  }, [
    formState,
    uiState,
  ]);

  const ExpertSearchRendererMemo = useMemo(() => {
    // Only render the expert search table when the corresponding mode is active.
    if (!uiState.isExpertSearch || uiState.loading != false) {
      return null;
    }

    return (
      <ExpertSearchTable
        uiState={uiState}
        formState={formState}
        restService={restService}
        updateState={props.updateState}
        handleExpertSearch={props.handleExpertSearch}
        hasSearched={props.hasSearched != null ? props.hasSearched : true}
        t={props.t}
        reportResult={props.reportResult}
        // setCallbackExpertSearch={props.setCallbackExpertSearch}
      />
    );
  }, [
    props.reportResult,
    uiState.loading
  ]);
  

  return (
    <View>
      {ElementRendererMemo}
      {ExpertSearchRendererMemo}
    </View>
  );
};

export default FormScreen;
