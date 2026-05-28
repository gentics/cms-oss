import React, { useMemo } from 'react';
import {
  IFormComponentBaseAttributes,
  IFormElement,
  SummaryOptions,
} from '../../types/interfaces';
import { getElementTypeByPlugin } from '../../custom/ElementTypes';
import FormGridTextElement from '../../components/elements/shared/FormGridTextElement';
import {
  ANAggregateElement,
  BinaryElement,
  CmsContainerElement,
} from '../../custom/ElementDefinitions';
import { SummaryElement } from '../../components/elements/helper/SummaryElement';
import { Row } from '../../components/elements/helper/Row';
import * as utils from '../../utils/Utils';
import { PluginNameEnum } from '../../constants/enums/PluginNameEnum';

export function ElementRenderer(props: IFormComponentBaseAttributes) {
  const { formState, uiState } = props;

  const schemas = formState.normalizedSchema;

  if (!props.elements) {
    return null;
  }

  const elements: (any | null)[] = props.elements.map(
    (obj: IFormElement, index: number) => {
      let jsonpath = `$.properties["${obj.id || index}"]`;
      const jsonpathParent = `${props.jsonpath ? props.jsonpath : ''}`;
      // only fields under aggregates will have the jsonpath set
      if (props.jsonpath) {
        jsonpath = `${props.jsonpath}..["${obj.id || index}"]`;
      }
      //Get schema for Element
      const schemaElem = schemas[obj.id] ?? { type: 'property' };
      let lowerCaseSchemaElementType = schemaElem.type.toLowerCase();
      let elementConfigurationType: string;
      let ElemTyp;
      if(lowerCaseSchemaElementType === "property" && obj && obj.formGridOptions && obj.formGridOptions?.type){
        elementConfigurationType = obj.formGridOptions?.type.toLowerCase();
      }else {
        elementConfigurationType = lowerCaseSchemaElementType;
      }
      ElemTyp = getElementTypeByPlugin(elementConfigurationType, formState.pluginName)

      if (!ElemTyp) {
        console.warn(`FormGrid: Element type could not be determined from: "${elementConfigurationType}"`, obj, schemaElem);
        return null;
      }

      // UVEK-651: if the value of a non editable, string property contains "<a " (an html link) it should be rendered as text
      if (schemaElem.type === 'string') {
        const elementFormObj = utils.getElementByJsonPath(
          formState.data,
          jsonpath,
          obj.id,
        );
        if (
          elementFormObj?.editable &&
          elementFormObj.value &&
          typeof elementFormObj.value === 'string' &&
          elementFormObj.value.indexOf('<a ') !== -1 //TODO v8.2 we cannot do it on phone devices.
        ) {
          ElemTyp = getElementTypeByPlugin('text', formState.pluginName);
        }
      }

      // if marked with renderingType, do use different component for rendering -- old comment
      if (lowerCaseSchemaElementType === 'catalog') {
        const elementFormObj = utils.getElementByJsonPath(
          formState.data,
          jsonpath,
          obj.id,
        );
        if (elementFormObj?.renderingType === 'reference') {
          ElemTyp = getElementTypeByPlugin('reference', formState.pluginName);
        }
      }

      // Check for FormGrid Fields (existing formGridOptionsType...)
      // if (
      //   lowerCaseSchemaElementType === 'property' &&
      //   obj.formGridOptions?.type
      // ) {
      //   ElemTyp = getElementTypeByPlugin(obj.formGridOptions?.type.toLowerCase(), formState.pluginName);
      // }

      if (ElemTyp === FormGridTextElement) {
        if (obj.formGridOptions?.dependsOn) {
          // if dependency is not visible do not render this element
          let dependencyElement;
          // aggregate must be dealed specially therefore we try to get the jsonpath of the element
          if (props.parentId && props.parentId.length > 0) {
            dependencyElement = utils.getElementByJsonPath(
              formState.data,
              jsonpathParent + `..["${obj.formGridOptions?.dependsOn}"]`,
            );
          } else {
            dependencyElement = utils.getElementByJsonPath(
              formState.data,
              '',
              obj.formGridOptions?.dependsOn,
            );
          }

          //UVEK-487 - added text is shown when element is not displayed
          if (
            (typeof dependencyElement?.visible === 'boolean' &&
              !dependencyElement.visible &&
              !obj.formGridOptions?.replacementText) ||
            (typeof dependencyElement?.visible === 'boolean' &&
              dependencyElement.visible &&
              obj.formGridOptions.replacementText) ||
            dependencyElement === null
          ) {
            return null;
          }
        }
      }

      // performance optimization to fetch elements on the first level directly, not using jsonpath
      let isVisible = formState.data.properties?.[obj.id]?.visible && obj.formGridOptions?.hidden !== true;

      if (typeof isVisible === 'undefined') {
        // CMS only: if visible is not set in data, evaluate condition as fallback
        if (formState.pluginName === PluginNameEnum.CMS_FORMS && obj.formGridOptions?.condition) {
          const condition = obj.formGridOptions.condition;
          const referencedValue = formState.data.properties?.[condition.source.control]?.value;
          isVisible = referencedValue === condition.equals;
        } else { // visible is set in AN forms by default
          isVisible = utils.getVisible(formState, jsonpath);
        }
      }
      const isCreatable = formState.data.properties?.[obj.id]?.createable
        ? formState.data.properties?.[obj.id]?.createable
        : utils.getCreatable(formState, jsonpath);

      let value = obj?.formGridOptions?.value
        ? obj.formGridOptions.value
        : utils.getValue(formState, jsonpath, obj.id);

      if ((schemaElem == null && !obj.formGridOptions?.type )|| !isVisible ) {
        return null;
      }

      if (
        props.summary === SummaryOptions.ReadOnly ||
        props.summary === SummaryOptions.SummaryText
      ) {
        if (ElemTyp === ANAggregateElement || ElemTyp === CmsContainerElement) {
          //Aggregates/containers are not handled directly by ReadOnly/Summary Element, instead we use normal Aggregate/Container Component to initialize sub elements - old comment
          return null;
        }

        value = ElemTyp.getSummaryValue(
          formState,
          jsonpath,
          obj.id,
          props.t,
          obj?.formGridOptions,
          props.restService
        );
        //for formgrid-elements in readonly forms, we are not using the summary value, instead the formgrid text should be displayed - old comment
        if (
          props.summary === SummaryOptions.ReadOnly &&
          utils.isFormGridType(ElemTyp)
        ) {
          if (obj?.formGridOptions?.value) {
            value = utils.getI18nText(
              formState,
              obj?.formGridOptions.value,
            );
            if (!value) value = '';
          }
        }
        // OLD COMMENTS
        //Formgrid-Text-Elements with an empty summary text should not be displayed (they use too much space)
        //Sometimes Formgrid-Text-Elements are used in a grid layout, and this can possible be destroyed by omitting those elements
        //So we only omit them if this Formgrid-Element use a full line (12 columns)
        //There are some cases where numberOfColumns is a string field, so we ignore tsErrors and check with ==
        /* eslint eqeqeq: 0 */
        if (
          (!value || value === '') &&
          utils.isFormGridType(ElemTyp) &&
          obj?.formGridOptions?.numberOfColumns == 12
        ) {
          return <></>;
        }
        let dangerouslySetInnerHTML: boolean = false;
        if (ElemTyp == BinaryElement) {
          dangerouslySetInnerHTML = true;
        }

        if(!value || value == '') {
          return null;
        }

        return (
          <SummaryElement
            uiState={uiState}
            key={
              'summaryElement-' + obj.id + '-' + jsonpath + '-' + index
            }
            formState={formState}
            summaryOption={props.summary}
            label={obj.label}
            value={value}
            t={props.t}
            numberOfColumns={obj?.formGridOptions?.numberOfColumns ?? 12}
            formGridElement={
              utils.isFormGridType(ElemTyp) ? true : false
            }
            dangerouslySetInnerHTML={dangerouslySetInnerHTML}
            initUrl={props.restService.initUrl}
          />
        );
      }

      return (
        <ElemTyp
          {...props}
          key={'ElemTyp-' + jsonpath + '-' + index}
          label={obj.label}
          value={value}
          id={obj.id}
          description={obj.description}
          required={utils.isRequired(formState, obj.id)}
          editable={utils.isEditable(
            formState,
            uiState,
            jsonpath,
            obj.id,
            props.parentId,
          )}
          error={utils.getError(formState, jsonpath, obj.id)}
          elements={obj.elements}
          type={obj.type}
          jsonpath={jsonpath}
          jsonpathParent={jsonpathParent}
          isList={utils.isListElement(formState.schema, obj.id)}
          possibleValues={utils.getPossibleValues(
            formState.data,
            jsonpath,
            formState.pluginName,
            obj.id,
          )}
          handleSearch={props.handleSearch}
          loading={utils.getLoading(formState, jsonpath)}
          ANFieldLoadError={utils.getANFieldLoadError(
            formState,
            jsonpath,
          )}
          formGridOptions={obj?.formGridOptions}
          csvUploadEnabled={obj.csvUploadEnabled}
          editMode={obj?.editMode}
          editModeDialogFormID={obj?.editModeDialogFormID}
          createable={isCreatable}
          t={props.t}
          numberOfColumns={obj?.formGridOptions?.numberOfColumns}
          ElemTypeNode={ElemTyp}
          elemTypeString={lowerCaseSchemaElementType === 'property' ? obj.formGridOptions?.type : lowerCaseSchemaElementType}
        />
      );
    }
  );

  return useMemo(
    () => (
      <Row
        key={'Row-' + elements.length}
        elementsNum={props.elements && props.elements.length}
        isAggregate={props.isAggregate}
        summary={
          props.summary === SummaryOptions.SummaryText ||
          props.summary === SummaryOptions.ReadOnly
        }
        formWidth={props.formState.form.formwidth || 8}
      >
        {elements}
      </Row>
    ),
    [elements.length, props.formState, props.uiState.currentPage, props.selectedElementId],
  );
}
