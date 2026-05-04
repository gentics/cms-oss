import React, { useEffect, useState, useRef, useMemo, JSX } from 'react';
import {
  View,
  TouchableOpacity,
  TextInput,
  Pressable,
  Platform,
  useWindowDimensions,
  TouchableWithoutFeedback,
  UIManager,
  Easing
} from 'react-native';
import {
  IOverlayConfirmationState,
  SummaryOptions,
  FormState,
  IFormGridOptions,
  CommonValidation,
  IFormComponentAttributes,
  IFormElement,
  IDataAggregationListEntry,
  IOverlayAggregateDeleteConfirmationExtraProps,
  IAggregateAddButtonProps,
} from '../../types/interfaces';
import { aggregateElementStyles } from '../../styles/common/common-style';
import { aggregateElementStyles as customAggregateElementStyles } from '../../styles/custom/custom-style';
import { elementTypes } from '../../custom/ElementTypes';
import { TFunction } from 'i18next';
import { ElementRenderer } from '../../screens/essentials/ElementRenderer';
import Pagination from './helper/CustomAggregatePagination';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import {
  getElementByJsonPath,
  listDeleteElement,
  listAddElement,
  getI18nText,
} from '../../utils/Utils';
import * as overlayUtils from '../../utils/OverlayUtils';
import { useTranslation } from 'react-i18next';
import { StatesToUpdateEnum } from '../../constants/enums/StateToUpdateEnum';
import { OverlayTypeEnum } from '../../custom/OverlayTypes';
import * as formEventsUtils from '../../utils/FormEventsUtils';
import AndpText from '@andp/core/src/components/AndpText';
import {
  mergeStyles,
  useBreakpoints,
  useHoverStyle,
  widthFromColumNumber,
} from '@andp/core/src/AndpUtils';
import theme, {
  aggregateElementTheme,
  formElementWrapperTheme,
} from '@andp/core/src/styles/theme';
import Reanimated from 'react-native-reanimated';
import Animated, { LinearTransition, Keyframe } from 'react-native-reanimated';
import { Animated as RNAnimated } from 'react-native';

import { SummaryElement } from './helper/SummaryElement';
import BaseDropDownInputElement from './DropDownElements/BaseDropDownInputElement';

interface AggregationItem {
  id: string;
  name: string;
  detailsVisible: boolean;
}

function ActionButtons({
  agg,
  index,
  toggleFn,
  deleteFn,
  addFn,
  styles,
  showText,
  t,
  detail,
  animating,
  isMobile,
  isDeletable,
  isCreateable,
  isEditable,
}: {
  agg: { id: string; detailsVisible: boolean };
  index: number;
  toggleFn: (id: string) => void;
  deleteFn: (id: string) => void;
  addFn: (idx: number) => Promise<void>;
  styles: any;
  showText: boolean;
  t: TFunction;
  detail: boolean;
  animating: Record<string, boolean>;
  isMobile: boolean;
  isDeletable: boolean;
  isCreateable: boolean;
  isEditable: boolean;
}) {
  const [toggleButtonOnHover, setToggleButtonOnHover] = React.useState(false);
  const [deleteButtonOnHover, setDeleteButtonOnHover] = React.useState(false);

  const toggleLabelKey = agg.detailsVisible
    ? isMobile
      ? 'form_show_less_mobile'
      : 'form_show_less'
    : isMobile
    ? 'form_show_more_mobile'
    : 'form_show_more';

  const allowDelete = isEditable && isDeletable;
  const allowAdd = isEditable && isCreateable;

  return (
    <View style={styles.actionButtons}>
      {detail && (
        <Pressable
          //@ts-ignore
          dataSet={{ class: 'agg-entry-toggle-details-btn' }}
          accessible
          accessibilityRole="button"
          accessibilityLabel={
            agg.detailsVisible ? t('form_show_less') : t('form_show_more')
          }
          accessibilityHint={t(
            'form_accessibility_aggregate_toggle_details_hint',
          )}
          accessibilityState={{ expanded: agg.detailsVisible }}
          onPress={() => toggleFn(agg.id)}
          disabled={animating[agg.id]}
          onHoverIn={() => {
            Platform.OS === 'web' && setToggleButtonOnHover(true);
          }}
          onHoverOut={() => {
            Platform.OS === 'web' && setToggleButtonOnHover(false);
          }}
          style={[
            styles.aggToggleDetailButton,
            agg.detailsVisible && styles.aggToggleDetailButtonActive,
            toggleButtonOnHover && styles.aggToggleDetailButtonOnHover,
            isMobile && styles.aggToggleDetailButtonSmallView,
          ]}
          key={agg.id}
        >
          <AndpIcon
            name={agg.detailsVisible ? 'caretUp' : 'caretDown'}
            style={[
              styles.aggToggleDetailButtonIcon,
              agg.detailsVisible && styles.aggToggleDetailButtonIconActive,
              toggleButtonOnHover && styles.aggToggleDetailButtonIconOnHover,
            ]}
          />
          <AndpText
            numberOfLines={1}
            ellipsizeMode="tail"
            style={[
              styles.aggToggleDetailButtonText,
              agg.detailsVisible && styles.aggToggleDetailButtonTextActive,
              toggleButtonOnHover && styles.aggToggleDetailButtonTextOnHover,
              isMobile && styles.aggToggleDetailButtonTextSmallView,
            ]}
          >
            {t(toggleLabelKey)}
          </AndpText>
        </Pressable>
      )}

      {allowDelete ? (
        <Pressable
          //@ts-ignore
          dataSet={{ class: 'agg-entry-delete-btn' }}
          accessible
          accessibilityRole="button"
          accessibilityLabel={t('form_delete_entry')}
          accessibilityHint={t(
            'form_accessibility_aggregate_delete_entry_hint',
          )}
          onPress={() => deleteFn(agg.id)}
          onHoverIn={() => setDeleteButtonOnHover(true)}
          onHoverOut={() => setDeleteButtonOnHover(false)}
          style={[
            styles.aggDeleteButton,
            deleteButtonOnHover && styles.aggDeleteButtonOnHover,
            isMobile && styles.aggDeleteButtonMobile,
          ]}
        >
          <AndpIcon
            name="trashCan"
            style={[
              styles.aggDeleteButtonContent,
              deleteButtonOnHover && styles.aggDeleteButtonContentOnHover,
              isMobile && styles.aggDeleteButtonMobileContent,
            ]}
          />
          {!isMobile && showText && (
            <AndpText
              style={[
                styles.aggDeleteButtonContent,
                deleteButtonOnHover && styles.aggDeleteButtonOnHover,
                isMobile && styles.aggDeleteButtonMobileContent,
              ]}
            >
              {t('form_delete_entry')}
            </AndpText>
          )}
        </Pressable>
      ) : null}
      {allowAdd ? (
        <AggregateAddButton
          className="agg-entry-add-btn"
          onPress={() => addFn(index)}
          showButtonText={!isMobile && showText}
        />
      ) : null}
    </View>
  );
}

function AggregateElement(props: IFormComponentAttributes) {
  const styles = useMemo(
    () => mergeStyles(aggregateElementStyles, customAggregateElementStyles),
    [],
  );
  const breakpoints = useBreakpoints();
  const { formState, uiState, label } = props;
  if (
    (Platform.OS === 'android' || Platform.OS === 'ios') &&
    UIManager.setLayoutAnimationEnabledExperimental
  ) {
    UIManager.setLayoutAnimationEnabledExperimental(true);
  }

  // expected editModes:
  // 1 ... inline
  // 2 ... dialog
  // 4 ... reference
  // 8 ... referenceDialog
  // 16 ... inlineDialog
  // 2, 8 and 16 should render dialog form
  const isRenderDialogForm =
    props.editMode === 2 || props.editMode === 8 || props.editMode === 16;
  const isRenderAggregateAsTable =
    isRenderDialogForm && props.elements && props.isList;
  const { t } = useTranslation();
  const defaultFilter = {
    id: 'all',
    value: '',
    label: t('form_filter_text_all'),
  };

  const [page, setPage] = useState(0);

  const [filterValue, setFilterValue] = useState([defaultFilter]);

  const [focusLastOnNextRender, setFocusLastOnNextRender] = useState(false);

  const wrapperId = 'aggregate-wrapper-' + props.jsonpath;
  // Merke aktuelle Sortierung (asc/desc) je Spalte
  const [sortDirection, setSortDirection] = useState<string[]>([]);
  const [sortColumn, setSortColumn] = useState<number | null>(null);
  const [hoveredRowIndex, setHoveredRowIndex] = useState<number | null>(null);

  const [overlayConfirmationState, setOverlayConfirmationState] =
    useState<IOverlayConfirmationState>();

  const { width } = useWindowDimensions();
  const isMobile = width < 768;
  const schemas = formState.normalizedSchema;
  const showIcons =
    typeof props.formGridOptions?.buttonDisplayStyle === 'undefined' ||
    props.formGridOptions?.buttonDisplayStyle === 'icon' ||
    props.formGridOptions?.buttonDisplayStyle === 'icontext';
  const showText =
    props.formGridOptions?.buttonDisplayStyle === 'text' ||
    props.formGridOptions?.buttonDisplayStyle === 'icontext';

  let localValues: IDataAggregationListEntry[] = props.value;

  //Pagination
  const pageSize = 20;
  const totalPages = Math.ceil(localValues.length / pageSize);
  const showPagination = totalPages > 1;

  const [aggregates, setAggregates] = useState<AggregationItem[]>(() => {
    if (Array.isArray(props.value)) {
      return props.value.map((aggObject: any) => ({
        id: aggObject.id,
        name: aggObject.name || props.label || '',
        detailsVisible: aggObject.detailsVisible || false,
      }));
    }
    return [];
  });

  const elementRef = useRef<View>(null);

  // +----------------------------------------------------------------------+
  // | Animation Definitions (States, Components & Hooks) for hover effects |
  // +----------------------------------------------------------------------+
  const {
    animatedStyle: uploadButtonHoverStyle,
    onHoverIn: uploadButtonHoverIn,
    onHoverOut: uploadButtonHoverOut,
  } = useHoverStyle(
    styles.uploadButton,
    styles.uploadButtonOnHover,
    aggregateElementTheme.functionButtons.uploadButton.onHover
      .transitionDuration,
  );
  const {
    animatedStyle: uploadButtonTextHoverStyle,
    onHoverIn: uploadButtonTextHoverIn,
    onHoverOut: uploadButtonTextHoverOut,
  } = useHoverStyle(
    styles.uploadButtonText,
    styles.uploadButtonTextOnHover,
    aggregateElementTheme.functionButtons.uploadButton.onHover
      .transitionDuration,
  );
  const {
    animatedStyle: addFilterButtonHoverStyle,
    onHoverIn: addFilterButtonHoverIn,
    onHoverOut: addFilterButtonHoverOut,
  } = useHoverStyle(
    styles.addFilterButtonActive,
    styles.addFilterButtonActiveOnHover,
    aggregateElementTheme.filter.addFilterButton.onHover.transitionDuration,
  );
  const [addFilterBtnOnHover, setAddFilterBtnOnHover] = useState(false);
  const AnimatedPressable = Reanimated.createAnimatedComponent(Pressable);
  
  // ----------------------------------------------------------------------
  // | ////////////////////////////////////////////////////////////////// |
  // ----------------------------------------------------------------------

  // +-------------------------------------------------------------------------+
  // | Animation Definitions for Aggregate Item Detail Indicator (on the left) |
  // +-------------------------------------------------------------------------+
  const [indicatorOpacity, setIndicatorOpacity] = useState<
    Record<string, RNAnimated.Value>
  >({});

  useEffect(() => {
    setIndicatorOpacity((prev) => {
      const next = { ...prev };
      aggregates.forEach((a) => {
        if (!next[a.id]) next[a.id] = new RNAnimated.Value(0);
      });
      return next;
    });
  }, [aggregates]);
  // ----------------------------------------------------------------------
  // | ////////////////////////////////////////////////////////////////// |
  // ----------------------------------------------------------------------

  useEffect(() => {
    const newAggregatesData: AggregationItem[] =
      props.value && props.value.length > 0
        ? props.value.map(
            (aggObject: any): AggregationItem => ({
              id: aggObject.id,
              name: aggObject.name || props.label || '',
              detailsVisible: aggObject.detailsVisible || false,
            }),
          )
        : [];

    const currentIds = aggregates.map((agg: AggregationItem) => agg.id);
    const newIds = newAggregatesData.map(
      (newAgg: AggregationItem) => newAgg.id,
    );
    const aggregatesToAdd: AggregationItem[] = newAggregatesData
      .filter((newAgg: AggregationItem) => !currentIds.includes(newAgg.id))
      .map(
        (newAgg: AggregationItem): AggregationItem => ({
          ...newAgg,
          detailsVisible: newAgg.detailsVisible || false,
        }),
      );

    const aggregatesToKeep = aggregates.filter((agg: AggregationItem) =>
      newIds.includes(agg.id),
    );
    setAggregates([...aggregatesToKeep, ...aggregatesToAdd]);
  }, [props.value, props.value.length]);

  const [savedElementCount, setSavedElementCount] = useState(-1);
  // Ref, der an den neuen Aggregat-Input weitergegeben wird:
  const newAggregateInputRef = useRef<TextInput>(null);

  useEffect(() => {
    if (
      focusLastOnNextRender &&
      props.value.length > 0 &&
      savedElementCount < props.value.length
    ) {
      setFocusLastOnNextRender(false);
      let attempts = 0;
      const intervalID = setInterval(() => {
        if (
          newAggregateInputRef.current &&
          newAggregateInputRef.current.focus
        ) {
          newAggregateInputRef.current.focus();
          clearInterval(intervalID);
        }
        attempts++;
        if (attempts === 40) clearInterval(intervalID);
      }, 50);
      return () => clearInterval(intervalID);
    }
  }, [focusLastOnNextRender, props.value.length, savedElementCount]);

  // === Filter-Logik ===
  const [filterTerm, setFilterTerm] = useState('');
  const [filterColumn, setFilterColumn] = useState<number | 'all'>('all');
  const [activeFilters, setActiveFilters] = useState<
    { column: number | 'all'; term: string }[]
  >([]);
  const [filterInputIsFocused, setFilterInputIsFocused] = useState(false);

  const addFilter = () => {
    if (filterTerm) {
      setActiveFilters((prev) => [
        ...prev,
        { column: filterColumn, term: filterTerm },
      ]);
      setFilterTerm('');
      setFilterColumn('all');
    }
    handleFilterInputBlur(true);
  };

  const removeFilter = (index: number) => {
    setActiveFilters((prev) => prev.filter((_, i) => i !== index));
  };

  const transformedFilters = activeFilters.map((filter) => ({
    id: filter.column.toString(),
    value: filter.term,
  }));

  function applyFilter(
    activeFilters: { id: string; value: string }[],
    valueList: IDataAggregationListEntry[],
  ): { valueFiltered: IDataAggregationListEntry[]; indexSave: number[] } {
    let valueFiltered =
      valueList && valueList.length > 0
        ? valueList.filter((elem) => elem.visible)
        : [];
    let indexSave: number[] = [];
    let indexNew = 0;

    if (activeFilters.length === 0) {
      return {
        valueFiltered,
        indexSave: valueFiltered.map((_, index) => index),
      };
    }

    activeFilters.forEach((filterV, filterVIndex) => {
      const first = filterVIndex === 0;
      if (!first) {
        indexNew = 0;
      }

      if (filterV.id === 'all' && filterV.value !== '') {
        valueFiltered = valueFiltered.filter((elem, index) => {
          let found = false;
          for (let subElem in elem.value) {
            found = containsText(elem.value[subElem], filterV.value);
            if (found) break;
          }
          if (found) {
            indexSave[indexNew] = index;
            indexNew++;
            return true;
          }
          return false;
        });
      } else {
        valueFiltered = valueFiltered.filter((elem, index) => {
          const valuesArray = Object.values(elem.value);
          const ind = parseInt(filterV.id, 10);
          const fieldValue =
            !isNaN(ind) && ind < valuesArray.length
              ? valuesArray[ind]
              : undefined;

          let found = containsText(fieldValue, filterV.value);
          if (found) {
            if (!first) {
              indexSave[indexNew] = indexSave[index];
            } else {
              indexSave[indexNew] = index;
            }
            indexNew++;
            return true;
          }
          return false;
        });
      }
    });
    return { valueFiltered, indexSave };
  }

  // +-----------------------------------------+
  // | Initialisiere die Placeholder-Animation |
  // +-----------------------------------------+
  const animatedPlaceholderTop = useRef(
    new RNAnimated.Value(
      filterTerm
        ? aggregateElementTheme.filter.filterInput.focused
            .placeholderTopPosition
        : aggregateElementTheme.filter.filterInput.placeholderTopPosition,
    ),
  ).current;
  const animatedPlaceholderFontSize = useRef(
    new RNAnimated.Value(
      filterTerm
        ? aggregateElementTheme.filter.filterInput.focused.placeholderFontSize
        : aggregateElementTheme.filter.filterInput.placeholderFontSize,
    ),
  ).current;
  const handleFilterInputFocus = () => {
    setFilterInputIsFocused(true);
    RNAnimated.timing(animatedPlaceholderTop, {
      toValue:
        aggregateElementTheme.filter.filterInput.focused.placeholderTopPosition,
      duration: 300,
      easing: Easing.bezier(0.133, 0.652, 0.415, 1),
      useNativeDriver: false,
    }).start();
    RNAnimated.timing(animatedPlaceholderFontSize, {
      toValue:
        aggregateElementTheme.filter.filterInput.focused.placeholderFontSize,
      duration: 300,
      easing: Easing.bezier(0.133, 0.652, 0.415, 1),
      useNativeDriver: false,
    }).start();
  };
  const handleFilterInputBlur = async (close?: boolean) => {
    setFilterInputIsFocused(false);
    if (filterTerm.trim() === '' || close) {
      RNAnimated.timing(animatedPlaceholderTop, {
        toValue:
          aggregateElementTheme.filter.filterInput.placeholderTopPosition,
        duration: 300,
        easing: Easing.out(Easing.quad),
        useNativeDriver: false,
      }).start();
      RNAnimated.timing(animatedPlaceholderFontSize, {
        toValue: aggregateElementTheme.filter.filterInput.placeholderFontSize,
        duration: 300,
        easing: Easing.out(Easing.quad),
        useNativeDriver: false,
      }).start();
    }
  };
  // +-----------------------------------------+
  // | /////////////////////////////////////// |
  // +-----------------------------------------+

  function containsText(elem: any, filterValue: string): boolean {
    if (filterValue.trim() === '') return true;

    const compareValue = filterValue.toLocaleLowerCase();

    if (elem.value && typeof elem.value === 'string') {
      if (elem.value.toLocaleLowerCase().indexOf(compareValue) !== -1) {
        return true;
      }
    }

    if (elem.value && typeof elem.value === 'object') {
      if (
        elem.value.displayName &&
        typeof elem.value.displayName === 'string'
      ) {
        if (
          elem.value.displayName.toLocaleLowerCase().indexOf(compareValue) !==
          -1
        ) {
          return true;
        }
      }
    }

    if (elem.values && typeof elem.values === 'object') {
      for (let key in elem.values) {
        const subVal = elem.values[key];
        if (subVal.displayName && typeof subVal.displayName === 'string') {
          if (
            subVal.displayName.toLocaleLowerCase().indexOf(compareValue) !== -1
          ) {
            return true;
          }
        }
      }
    }

    return false;
  }

  function sortTable(columnNumber: number) {
    if (localValues.length === 0) return;
    setSortColumn(columnNumber);
    const newDirection =
      sortDirection[columnNumber] === undefined ||
      sortDirection[columnNumber] === 'asc'
        ? 'desc'
        : 'asc';
    const updatedSortDirection = [...sortDirection];
    updatedSortDirection[columnNumber] = newDirection;
    setSortDirection(updatedSortDirection);

    const keys = Object.keys(localValues[0].value);
    const valueObjectKeyToCompare = keys[columnNumber];

    if (Array.isArray(localValues) && localValues.length > 0) {
      localValues.sort((a, b) => {
        const valA = a.value[valueObjectKeyToCompare].value;
        const valB = b.value[valueObjectKeyToCompare].value;

        if (valA === valB) return 0;

        const numA = parseFloat(valA);
        const numB = parseFloat(valB);

        if (!isNaN(numA) && !isNaN(numB)) {
          return newDirection === 'asc' ? numA - numB : numB - numA;
        }

        const valAString = valA.toLowerCase();
        const valBString = valB.toLowerCase();
        if (newDirection === 'asc') {
          return valAString > valBString ? 1 : -1;
        } else {
          return valAString > valBString ? -1 : 1;
        }
      });

      if (formState?.data?.properties?.[props.id]?.value) {
        (formState.data.properties as any)[props.id].value = localValues;
      }
      props.updateState(StatesToUpdateEnum.FormState);
    }
  }

  useEffect(() => {
    if (!isRenderAggregateAsTable) return;
    if (sortColumn !== null) return;

    const defaultSortableIndex = (props.elements || []).findIndex(
      (el) => el?.type === 'number',
    );

    if (defaultSortableIndex !== -1) {
      sortTable(defaultSortableIndex);
    }
  }, [isRenderAggregateAsTable, sortColumn, props.elements]);

  const renderAggregateAsTable = () => {
    if (!Array.isArray(props.value)) return null;

    const tableHeaders: string[] = [];
    type TableRow = {
      originalIndex: number;
      tableCells: string[];
      validationMessages: string[];
      isRowDeletable?: boolean;
      isRowEditable?: boolean;
      isRowVisible?: boolean;
    };
    const tableRows: TableRow[] = [];
    const filterResult = applyFilter(transformedFilters, props.value);
    let localValues = filterResult.valueFiltered;
    const start = calculatePagingStart();

    for (let idx = start; idx < localValues.length; idx++) {
      if (idx >= start + pageSize) break;
      const originalIndex = filterResult.indexSave[idx];
      const jsonPath = `${props.jsonpath}.value[${originalIndex}]`;
      const isRowDeletable = localValues[idx]?.deletable;
      const isRowEditable = localValues[idx]?.editable;
      const isRowVisible = localValues[idx]?.visible;
      tableRows[idx] = {
        originalIndex,
        tableCells: [],
        validationMessages: [],
        isRowDeletable,
        isRowEditable,
        isRowVisible,
      };

      if (isRowVisible && props.elements) {
        props.elements.forEach((obj: IFormElement, indexAgg: number) => {
          if (obj.editModeDialogUsage && obj.editModeDialogUsage !== 0) return;
          const schemaElem = schemas[obj.id] ?? { type: 'property' };
          const lowerCaseSchemaElementType = schemaElem.type.toLowerCase();
          const ElemTyp = elementTypes[lowerCaseSchemaElementType];
          const jsonPathOfElement = `${jsonPath}.value["${obj.id}"]`;
          const subElement = getElementByJsonPath(
            formState.data,
            jsonPathOfElement,
          );
          var value = ElemTyp.getSummaryValue
            ? ElemTyp.getSummaryValue(
                formState,
                jsonPathOfElement,
                obj.id,
                t,
                obj?.formGridOptions,
              )
            : '';

          if (value === '') {
            value = subElement?.value;
          }
          if (subElement?.validationMessage) {
            tableRows[idx].validationMessages.push(
              `${getI18nText(formState, obj.label) || ''}: ${
                subElement.validationMessage
              }`,
            );
          }
          tableRows[idx].tableCells.push(value);
          if (idx === start) {
            tableHeaders.push(getI18nText(formState, obj.label) || '');
          }
        });
      }
    }

    if (isMobile) {
      return (
        <View
          style={styles.mobileCardList}
          key={'agg-mobile-container-' + props.jsonpath}
        >
          {tableRows.map((row, ri) => (
            <Pressable
              accessible
              accessibilityRole="button"
              accessibilityLabel={t('form_accessibility_aggregate_record', {
                index: ri + 1,
              })}
              accessibilityHint={t(
                'form_accessibility_aggregate_record_card_hint',
                { index: ri + 1 },
              )}
              key={
                'agg-mobile-Pressable-' +
                props.jsonpath +
                '-' +
                row.originalIndex
              }
              onHoverIn={() => setHoveredRowIndex(ri)}
              onHoverOut={() => setHoveredRowIndex(null)}
              style={[
                styles.mobileCard,
                hoveredRowIndex === ri && styles.mobileCardHover,
              ]}
            >
              {tableHeaders.map((header, ci) => (
                <View
                  key={'agg-tableheadersView-' + ci}
                  style={styles.mobileFieldRow}
                >
                  <AndpText style={styles.mobileFieldLabel}>{header}</AndpText>
                  <AndpText style={styles.mobileFieldValue}>
                    {row.tableCells[ci]}
                  </AndpText>
                </View>
              ))}

              {row.validationMessages.length > 0 && (
                <AndpText style={styles.mobileValidation}>
                  {row.validationMessages.join(', ')}
                </AndpText>
              )}

              <View style={styles.mobileActionsRow}>
                {props.createable && (
                  <Pressable
                    onPress={() => handleDialogFormCopy(row.originalIndex)}
                    style={styles.mobileActionButton}
                  >
                    <AndpIcon
                      name="copy"
                      style={styles.mobileActionButtonContent}
                    />
                  </Pressable>
                )}
                {row.isRowEditable && (
                  <Pressable
                    onPress={() => handleDialogAggFormEdit(row.originalIndex)}
                    style={styles.mobileActionButton}
                  >
                    <AndpIcon
                      name="pencil"
                      style={styles.mobileActionButtonContent}
                    />
                  </Pressable>
                )}
                {row.isRowDeletable && (
                  <Pressable
                    onPress={() =>
                      handleDialogFormDelete(
                        row.originalIndex,
                        row.tableCells[0],
                      )
                    }
                    style={styles.mobileDeleteButton}
                  >
                    <AndpIcon
                      name="trashCan"
                      style={styles.mobileDeleteButtonContent}
                    />
                  </Pressable>
                )}
              </View>
            </Pressable>
          ))}
        </View>
      );
    }

    return (
      <View key={'aggregate-table-container-' + props.jsonpath}>
        {/* Tabellenheader */}
        <View
          key={'aggregate-table-header-' + props.jsonpath}
          style={styles.tableHeader}
        >
          {tableHeaders.map((header, i) => (
            <TouchableOpacity
              accessible
              accessibilityRole="button"
              accessibilityLabel={header.toUpperCase()}
              accessibilityHint={t(
                'form_accessibility_aggregate_sort_column_hint',
                { column: header },
              )}
              accessibilityState={{ selected: sortColumn === i }}
              key={i}
              onPress={() => sortTable(i)}
              style={styles.tableHeaderCell}
            >
              <AndpIcon
                name={
                  sortColumn === i && sortDirection[i] === 'desc'
                    ? 'caretDown'
                    : 'caretUp'
                }
                size={aggregateElementTheme.tableView.header.fontSize}
                color={aggregateElementTheme.tableView.header.color}
              />
              <AndpText style={styles.tableAggHeaderText}>{header}</AndpText>
            </TouchableOpacity>
          ))}
          {/* Used only as a placeholder for the Action Buttons Column */}
          <View style={styles.tableHeaderCell}></View>
        </View>

        {/* Tabellenbody */}
        {tableRows.map((row, rowIndex) => (
          <Pressable
            id={`${props.jsonpath}.value[${rowIndex}]`}
            key={row.originalIndex}
            accessible
            accessibilityLabel={t('form_accessibility_aggregate_row', {
              index: rowIndex + 1,
            })}
            accessibilityHint={t('form_accessibility_aggregate_row_hint', {
              index: rowIndex + 1,
            })}
            onHoverIn={() => setHoveredRowIndex(row.originalIndex)}
            onHoverOut={() => setHoveredRowIndex(null)}
            style={[
              styles.tableRow,
              hoveredRowIndex === row.originalIndex && styles.tableRowHover,
            ]}
          >
            {row.tableCells.map((cell, vi) => (
              <AndpText key={vi} style={styles.tableCell}>
                {cell}
              </AndpText>
            ))}
            <View style={styles.tableCell}>
              {row.validationMessages.length > 0 && (
                <AndpText style={styles.valueText}>
                  {row.validationMessages.join(', ')}
                </AndpText>
              )}
              {props.editModeDialogFormID && props.editable && (
                <>
                  {row.isRowDeletable && (
                    <TouchableOpacity
                      //@ts-ignore
                      dataSet={{ class: 'agg-entry-delete-btn' }}
                      accessible
                      accessibilityRole="button"
                      accessibilityLabel={t('form_delete_entry')}
                      accessibilityHint={t(
                        'form_accessibility_aggregate_delete_entry_hint',
                      )}
                      onPress={() =>
                        handleDialogFormDelete(
                          row.originalIndex,
                          row.tableCells[0],
                        )
                      }
                      style={styles.tableAggActionButton}
                    >
                      {showIcons && (
                        <AndpIcon
                          name="trashCan"
                          size={
                            aggregateElementTheme.tableView.actionButtons
                              .fontSize
                          }
                          color={
                            aggregateElementTheme.tableView.actionButtons
                              .deleteColor
                          }
                        />
                      )}
                      <AndpText style={styles.deleteButton}>
                        {' '}
                        {showText && t('form_delete_entry')}
                      </AndpText>
                    </TouchableOpacity>
                  )}
                  {row.isRowEditable && (
                    <TouchableOpacity
                      //@ts-ignore
                      dataSet={{ class: 'agg-entry-edit-btn' }}
                      accessible
                      accessibilityRole="button"
                      accessibilityLabel={t('form_edit')}
                      accessibilityHint={t(
                        'form_accessibility_aggregate_edit_hint',
                      )}
                      onPress={() => handleDialogAggFormEdit(row.originalIndex)}
                      style={styles.tableAggActionButton}
                    >
                      {showIcons && (
                        <AndpIcon
                          name="pencil"
                          size={
                            aggregateElementTheme.tableView.actionButtons
                              .fontSize
                          }
                          color={
                            aggregateElementTheme.tableView.actionButtons
                              .actionColor
                          }
                        />
                      )}
                      <AndpText style={styles.editButton}>
                        {' '}
                        {showText && t('form_edit')}
                      </AndpText>
                    </TouchableOpacity>
                  )}
                  {props.createable && (
                    <>
                      <TouchableOpacity
                        //@ts-ignore
                        dataSet={{ class: 'agg-entry-copy-btn' }}
                        accessible
                        accessibilityRole="button"
                        accessibilityLabel={t('form_copy')}
                        accessibilityHint={t(
                          'form_accessibility_aggregate_copy_hint',
                        )}
                        onPress={() => handleDialogFormCopy(row.originalIndex)}
                        style={styles.tableAggActionButton}
                      >
                        {showIcons && (
                          <AndpIcon
                            name="file"
                            size={
                              aggregateElementTheme.tableView.actionButtons
                                .fontSize
                            }
                            color={
                              aggregateElementTheme.tableView.actionButtons
                                .actionColor
                            }
                          />
                        )}
                        <AndpText style={styles.copyButton}>
                          {' '}
                          {showText && t('form_copy')}
                        </AndpText>
                      </TouchableOpacity>
                    </>
                  )}
                </>
              )}
            </View>
          </Pressable>
        ))}
      </View>
    );
  };

  function calculatePagingStart(): number {
    let start = page * pageSize;
    if (start >= localValues.length || start < 0) {
      let newPage = Math.ceil(localValues.length / pageSize) - 1;
      if (newPage < 0) newPage = 0;
      if (newPage !== page) {
        setPage(newPage);
      }
      start = newPage * pageSize;
    }
    return start;
  }

  function handleDialogFormCopy(indexToCopyFrom: number) {
    handleDialogAggFormEdit(indexToCopyFrom, true);
  }

  /**
   * Remove entry the current aggregate
   */
  function handleDialogFormDelete(index: number, valueToDisplay: string) {
    const overlayExtraProps: IOverlayAggregateDeleteConfirmationExtraProps = {
      index: index,
      parentJsonPath: props.jsonpath,
    };
    overlayUtils.setOverlay(
      uiState,
      formState,
      OverlayTypeEnum.AGGREGATE_DELETE_CONFIRMATION,
      props.updateState,
      overlayExtraProps,
    );
  }

  function handleDialogAggFormEdit(index: number, isCopy: boolean = false) {
    if (!props.editModeDialogFormID) return;
    const aggregateValueToEdit =
      formState.data.properties?.[props.id]?.value?.[index].value;
    if (!aggregateValueToEdit) {
      console.warn(`aggregate values on index ${index} is not defined`);
      return;
    }
    const hostObjectId =
      formState.data.properties?.[props.id]?.value?.[index]?.id;
    if (!hostObjectId) {
      console.warn('hostObjectId / businessObjectId missing');
      return;
    }
    const preFixedHostObjectId = hostObjectId;
    console.log(
      `Initializing dialog form for editing existing value. Dialog formId: ${props?.editModeDialogFormID}, property id of the aggregate: ${props.id}, businessObjectID of entry for editing: ${preFixedHostObjectId}`,
    );
    props.handleDialogFormEdit?.(
      wrapperId,
      props.editModeDialogFormID,
      props.id,
      undefined,
      preFixedHostObjectId,
      isCopy,
    );
  }

  async function handleDialogFormAdd(createAfterIndex?: number) {
    if (!props.editModeDialogFormID) return;
    await props.handleDialogFormEdit?.(
      wrapperId,
      props.editModeDialogFormID,
      props.id,
      createAfterIndex,
    );
    console.log(
      'Initializing dialog form for adding new value, dialog formId: ',
      props?.editModeDialogFormID,
    );
  }

  async function addAggregationElement(createAfterIndex?: number) {

    let size = 0;
    if (props.value.length) {
      size = props.value.length;
    }
    setSavedElementCount(size);
    if (isRenderDialogForm) {
      handleDialogFormAdd(createAfterIndex);
    } else {
      await listAddElement(
        formState,
        props.uiState,
        props.restService,
        props.updateState,
        props.id,
        props.jsonpath,
        createAfterIndex,
      );
    }
    setFilterValue([defaultFilter]);
    if (createAfterIndex === undefined) {
      setPage(-1);
    }
    setFocusLastOnNextRender(true);
  }

  function renderSingleSummaryElement(
    jsonpath: string,
    index: number,
    props: any,
  ) {
    const key = `Agg-RenderSingleSummaryElement-${jsonpath}-${index}-${props.elements.length}`;
    return (
      <View key={key}>
        <ElementRenderer
          {...props}
          elements={props.elements}
          jsonpath={jsonpath}
          key={key}
          aggregatShowOnlyFirstLine={props.formGridOptions?.showOnlyFirstLine}
          parentId={props.id}
          isAggregate={true}
          summary={SummaryOptions.SummaryText}
        />
      </View>
    );
  }

  const collapseInProgressRef = useRef(false);

  const [defaultHeights, setDefaultHeights] = useState<Record<string, number>>(
    {},
  ); // Höhe des Standardinhalts
  const [contentHeights, setContentHeights] = useState<Record<string, number>>(
    {},
  ); // Höhe der Detailinhalte
  const [renderDetails, setRenderDetails] = useState<Record<string, boolean>>(
    () => Object.fromEntries(aggregates.map((agg) => [agg.id, false])),
  );
  const [aggregateHeightsDetail, setAggregateHeightsDetail] = useState<
    Record<string, RNAnimated.Value>
  >(() =>
    Object.fromEntries(
      aggregates.map((agg) => [agg.id, new RNAnimated.Value(0)]),
    ),
  );

  useEffect(() => {
    setAggregateHeightsDetail((prev) => {
      const newState = { ...prev };
      aggregates.forEach((agg) => {
        if (!newState[agg.id]) {
          newState[agg.id] = new RNAnimated.Value(0);
        }
      });
      return newState;
    });
  }, [aggregates]);

  useEffect(() => {
    props.updateFirstErrorElementRef &&
      props.updateFirstErrorElementRef(props, elementRef);
  });
  const [animating, setAnimating] = useState<Record<string, boolean>>({});

  // Web Animation
  const toggleDetails = (id: string) => {
    const measuredDetailHeight = contentHeights[id] || 0;
    const aggregate = aggregates.find((agg) => agg.id === id);
    const isVisible = aggregate?.detailsVisible;
    if (!isVisible) {
      // Expand
      setRenderDetails((prev) => ({ ...prev, [id]: true }));
      setAggregates((prev) =>
        prev.map((agg) =>
          agg.id === id ? { ...agg, detailsVisible: true } : agg,
        ),
      );
      if (measuredDetailHeight > 0) {
        RNAnimated.timing(aggregateHeightsDetail[id], {
          toValue: measuredDetailHeight,
          duration: 300,
          useNativeDriver: false,
        }).start();
      }
      RNAnimated.timing(indicatorOpacity[id], {
        toValue: 1,
        duration: 200,
        useNativeDriver: true,
      }).start();
    } else {
      // Collapse
      collapseInProgressRef.current = true;
      RNAnimated.timing(aggregateHeightsDetail[id], {
        toValue: 0,
        duration: 300,
        useNativeDriver: false,
      }).start(() => {
        collapseInProgressRef.current = false;
        setRenderDetails((prev) => ({ ...prev, [id]: false }));
        setAggregates((prev) =>
          prev.map((agg) =>
            agg.id === id ? { ...agg, detailsVisible: false } : agg,
          ),
        );
      });
      RNAnimated.timing(indicatorOpacity[id], {
        toValue: 0,
        duration: 200,
        useNativeDriver: true,
      }).start();
    }
  };

  // IOS Animation
  const [activeBounceId, setActiveBounceId] = useState<string | null>(null);

  const toggleDetailsNative = (id: string) => {
    setActiveBounceId(id);
    const wasVisible = aggregates.find(a => a.id === id)?.detailsVisible ?? false;
    const willBeVisible = !wasVisible;
    animateIndicator(id, willBeVisible);
    
    setAggregates((prev) =>
      prev.map((a) =>
        a.id === id ? { ...a, detailsVisible: !a.detailsVisible } : a,
      ),
    );
    setTimeout(() => setActiveBounceId(null), 500);
  };

  const RowEnter = new Keyframe({
    0: { opacity: 0, transform: [{ translateY: -8 }, { scaleY: 0.985 }] },
    70: { opacity: 1, transform: [{ translateY: 4 }, { scaleY: 1.005 }] },
    100: { transform: [{ translateY: 0 }, { scaleY: 1 }] },
  }).duration(240);

  const RowExit = new Keyframe({
    0: { opacity: 1, transform: [{ translateY: 0 }, { scaleY: 1 }] },
    35: { transform: [{ translateY: -3 }, { scaleY: 0.993 }] },
    100: { opacity: 0, transform: [{ translateY: -8 }, { scaleY: 0.985 }] },
  }).duration(170);  
  
  const animateIndicator = (id: string, visible: boolean) => {
    const v = indicatorOpacity[id];
    if (!v) return;
    RNAnimated.timing(v, {
      toValue: visible ? 1 : 0,
      duration: 200,
      useNativeDriver: true,
    }).start();
  };

  useEffect(() => {
    // Nicht in Tabelle oder "nur erste Zeile"
    if (!props.isList) return;
    if (!props.formGridOptions?.showOnlyFirstLine) return;
    if (isRenderAggregateAsTable) return;

    const rawErrorMsgs = Array.isArray(props.rawErrorMsgs)
      ? props.rawErrorMsgs
      : [];
    if (rawErrorMsgs.length === 0) {
      return;
    }

    // Nur Fehler dieses Aggregats berücksichtigen, Zeilen-IDs sammeln
    const rowIds = Array.from(
      new Set(
        rawErrorMsgs
          .filter(
            (m: any) =>
              m?.elementInfo?.aggregateID === props.id &&
              typeof m?.elementInfo?.subAggregateID === 'string' &&
              m.elementInfo.subAggregateID.length > 0,
          )
          .map((m: any) => m.elementInfo.subAggregateID),
      ),
    );

    if (rowIds.length === 0) {
      return;
    }

    // Nur die IDs öffnen, die aktuell zu sind (wichtig nach manuellem Zuklappen)
    const toOpenIds = rowIds.filter((id) => {
      const agg = aggregates.find((a) => a.id === id);
      return agg ? !agg.detailsVisible : true;
    });

    // Wenn es nichts zu öffnen gibt, Signatur aktualisieren und beenden
    if (toOpenIds.length === 0) {
      return;
    }

    // Betroffene Zeilen öffnen (nur wenn sie noch zu sind)
    const doToggle =
      Platform.OS === 'web'
        ? toggleDetails
        : toggleDetailsNative;
    toOpenIds.forEach((id) => {
      const agg = aggregates.find((a) => a.id === id);

      if (agg && !agg.detailsVisible) {
        doToggle(id);
      }
    });
  }, [props.formState.errorOpenSubAggNonce]);

  const buildSingleAggregationElem = (agg: AggregationItem, index: number) => {
    const showOnlyFirstLine = props.formGridOptions?.showOnlyFirstLine;
    const elementJsonPath = `${props.jsonpath}.value[${index}]`;
    const allElems = props.elements ?? [];

    if (props.summary) {
      return renderSingleSummaryElement(elementJsonPath, index, props);
    }

    // SHOW ONLY FIRST LINE
    if (showOnlyFirstLine) {
      const elements =
        props.elements && props.elements.length > 0 ? props.elements : [];

      let firstLineElements: IFormElement[] = [];
      let sumNumberOfColumnsFirstLine = 0;
      let idx = 0;
      for (; idx < elements.length; idx++) {
        const el = elements[idx];
        const cols = el?.formGridOptions?.numberOfColumns || 12;
        const newSum = sumNumberOfColumnsFirstLine + Number(cols);
        if (newSum <= 12 && el) {
          sumNumberOfColumnsFirstLine = newSum;
          firstLineElements.push(el);
        } else {
          break;
        }
      }
      const firstLineElem = elements.slice(0, idx);
      const detailElem = elements.slice(idx);

      const isActive = activeBounceId === agg.id;
      const DetailLayout = isActive
        ? LinearTransition.springify().stiffness(400).damping(20)
        : LinearTransition.springify().stiffness(360).damping(45);
      const ButtonsLayout = isActive
        ? LinearTransition.springify().stiffness(400).damping(40)
        : LinearTransition.springify().stiffness(360).damping(45);

      if (Platform.OS != 'web') {
        // Ansicht Native
        return (
          <View
            key={
              'AggBuildSingleAggElementRow-native-' +
              agg.id +
              '-' +
              index +
              '-' +
              props.jsonpath
            }
            style={{ flexDirection: 'row' }}
          >
            <RNAnimated.View
              style={[
                styles.expandedAggregateItemIdicator,
                { opacity: indicatorOpacity[agg.id] || 0 },
              ]}
            />
            <View
              key={
                'AggBuildSingleAggElement-' +
                agg.id +
                '-' +
                index +
                '-' +
                props.jsonpath
              }
            >
              <Animated.View
                layout={DetailLayout}
                style={[
                  isMobile &&
                    agg.detailsVisible &&
                    styles.singleDetailAggregateContainerMobile,
                ]}
              >
                {/* unsichtbares, aber fokussierbares TextInput */}
                {index === savedElementCount && (
                  <TextInput
                    ref={newAggregateInputRef}
                    accessible={false}
                    style={{ height: 0, opacity: 0 }}
                  />
                )}
                {/* Standard-Firstline-Inhalt (immer sichtbar) */}
                <View style={styles.firstLineContainer}>
                  {firstLineElem && (
                    <ElementRenderer
                      {...props}
                      elements={firstLineElem}
                      key={`firstLine-${agg.id}-${firstLineElem[0].id || index}`}
                      jsonpath={elementJsonPath}
                      aggregatShowOnlyFirstLine={
                        props.formGridOptions?.showOnlyFirstLine
                      }
                      parentId={props.id}
                      isAggregate={true}
                      insideSubaggregatIndex={index}
                    />
                  )}
                </View>

                {/* — nur wenn detailsVisible: Detail-Panel */}
                {agg.detailsVisible && (
                  <Animated.View
                    key={`detail-${agg.id}`}
                    layout={DetailLayout}
                    style={[styles.detailContainer, { overflow: 'hidden' }]}
                  >
                    <Animated.View>
                      {detailElem.map((el, i) => (
                        <Animated.View
                          key={el.id ?? i}
                          entering={RowEnter.delay(i * 28)}
                          exiting={RowExit.delay(
                            (detailElem.length - i - 1) * 20,
                          )}
                        >
                          <ElementRenderer
                            {...props}
                            elements={[el]}
                            key={`detailElem-${agg.id}-${el.id ?? i}`}
                            jsonpath={elementJsonPath}
                            aggregatShowOnlyFirstLine={
                              props.formGridOptions?.showOnlyFirstLine
                            }
                            parentId={props.id}
                            isAggregate
                            insideSubaggregatIndex={index}
                          />
                        </Animated.View>
                      ))}
                    </Animated.View>
                  </Animated.View>
                )}
              </Animated.View>
              {/* Aktionsbuttons */}
              <Animated.View layout={ButtonsLayout}>
                <ActionButtons
                  agg={agg}
                  index={index}
                  toggleFn={toggleDetailsNative}
                  deleteFn={(id) => {
                    const idx = props.value.findIndex(
                      (item: any) => item.id === agg.id,
                    );
                    if (idx !== -1) {
                      listDeleteElement(
                        formState,
                        props.jsonpath,
                        idx,
                        props.updateState,
                      );
                    }
                  }}
                  addFn={addAggregationElement}
                  styles={styles}
                  showText={showText}
                  t={t}
                  detail={true}
                  animating={animating}
                  isMobile={isMobile}
                  isDeletable={props.value?.[index]?.deletable}
                  isCreateable={!!props.createable}
                  isEditable={!!props.editable}
                />
                <View style={styles.borderLine} />
              </Animated.View>
            </View>
          </View>
        );
      }

      // Ansicht Web
      return (
        <View
          id={wrapperId}
          key={
            'AggBuildSingleAggElementRow-web-' +
            agg.id +
            '-' +
            index +
            '-' +
            props.jsonpath
          }
          style={{ flexDirection: 'row' }}
        >
          <RNAnimated.View
            style={[
              styles.expandedAggregateItemIdicator,
              { opacity: indicatorOpacity[agg.id] || 0 },
            ]}
          />
          <View
            key={
              'AggBuildSingleAggElement-' +
              agg.id +
              '-' +
              index +
              '-' +
              props.jsonpath
            }
            style={styles.singleAggregateContainer}
          >
            {/* Standard-Firstline-Inhalt (immer sichtbar) */}
            <View
              onLayout={(event) => {
                const { height } = event.nativeEvent.layout;
                if (height > 0 && !defaultHeights[agg.id]) {
                  setDefaultHeights((prev) => ({
                    ...prev,
                    [agg.id]: height + 71,
                  }));
                }
              }}
            >
              {/* unsichtbares, aber fokussierbares TextInput */}
              {index === savedElementCount && (
                <TextInput
                  ref={newAggregateInputRef}
                  accessible={false}
                  style={{ height: 0, opacity: 0 }}
                />
              )}
              {firstLineElem && (
                <ElementRenderer
                  {...props}
                  elements={firstLineElem}
                  key={`firstLine-${agg.id}-${firstLineElem[0].id || index}`}
                  jsonpath={elementJsonPath}
                  aggregatShowOnlyFirstLine={
                    props.formGridOptions?.showOnlyFirstLine
                  }
                  parentId={props.id}
                  isAggregate={true}
                  insideSubaggregatIndex={index}
                />
              )}
            </View>

            {/* Animierter Detailbereich */}
            <RNAnimated.View
              style={{
                height: aggregateHeightsDetail[agg.id],
                overflow: 'hidden',
              }}
            >
              {renderDetails[agg.id] && (
                <View
                  onLayout={(event) => {
                    const { height } = event.nativeEvent.layout;
                    if (height > 0 && contentHeights[agg.id] !== height) {
                      setContentHeights((prev) => ({
                        ...prev,
                        [agg.id]: height,
                      }));
                      if (
                        agg.detailsVisible &&
                        !collapseInProgressRef.current
                      ) {
                        RNAnimated.timing(aggregateHeightsDetail[agg.id], {
                          toValue: height,
                          duration: 300,
                          useNativeDriver: false,
                        }).start();
                      }
                    }
                  }}
                >
                  {detailElem && (
                    <ElementRenderer
                      {...props}
                      elements={detailElem}
                      key={`detailElem-${agg.id}-${detailElem[0].id || index}`}
                      jsonpath={elementJsonPath}
                      aggregatShowOnlyFirstLine={
                        props.formGridOptions?.showOnlyFirstLine
                      }
                      parentId={props.id}
                      isAggregate={true}
                      insideSubaggregatIndex={index}
                    />
                  )}
                </View>
              )}
            </RNAnimated.View>

            {/* Aktionsbuttons */}
            <ActionButtons
              agg={agg}
              index={index}
              toggleFn={toggleDetails}
              deleteFn={(id) => {
                const idx = props.value.findIndex(
                  (item: any) => item.id === agg.id,
                );
                if (idx !== -1) {
                  listDeleteElement(
                    formState,
                    props.jsonpath,
                    idx,
                    props.updateState,
                  );
                }
              }}
              addFn={addAggregationElement}
              styles={styles}
              showText={showText}
              t={t}
              detail={true}
              animating={animating}
              isMobile={isMobile}
              isDeletable={props.value?.[index]?.deletable}
              isCreateable={!!props.createable}
              isEditable={!!props.editable}
            />

            <View style={styles.borderLine} />
          </View>
        </View>
      );
    } else {
      // ALL AGGREGATE ELEMENTS WITHOUT DETAILS
      return (
        <View
          key={
            'Agg-View-BuildSingleAggElement-' +
            agg.id +
            '-' +
            props.jsonpath +
            '-' +
            allElems.length +
            '-' +
            index
          }
        >
          <ElementRenderer
            {...props}
            key={`BuildSingleAggElement-ElementRenderer-${agg.id}-full-${allElems.length}`}
            elements={allElems}
            jsonpath={elementJsonPath}
            parentId={props.id}
            isAggregate={true}
            insideSubaggregatIndex={index}
          />
          {/* Aktionsbuttons */}
          <ActionButtons
            agg={agg}
            index={index}
            toggleFn={
              Platform.OS === 'web'
                ? toggleDetails
                : toggleDetailsNative
            }
            deleteFn={(id) => {
              const idx = props.value.findIndex(
                (item: any) => item.id === agg.id,
              );
              if (idx !== -1) {
                listDeleteElement(
                  formState,
                  props.jsonpath,
                  idx,
                  props.updateState,
                );
              }
            }}
            addFn={addAggregationElement}
            styles={styles}
            showText={showText}
            t={t}
            detail={false}
            animating={animating}
            isMobile={isMobile}
            isDeletable={props.value?.[index]?.deletable}
            isCreateable={!!props.createable}
            isEditable={!!props.editable}
          />
          <View style={styles.borderLine} />
        </View>
      );
    }
  };

  const { valueFiltered: filteredValues } = applyFilter(
    transformedFilters,
    props.value,
  );
  const hasFilteredEntries = filteredValues.length > 0;
  // === Render-Logik ===
  let elements: React.ReactElement[] = [];
  const addButton = (isAddToTop: boolean = false) => {
    const createAfterIndex = isAddToTop ? -1 : undefined;
    return (
      <View
        key={'AggAddButton-' + isAddToTop + '-' + props.jsonpath}
        style={styles.aggAddButtonContainer}
      >
        <AggregateAddButton
          id={`${props.id}-${isAddToTop ? 'add-agg-item-top' : 'add-agg-item-bottom'}`}
          onPress={async () => await addAggregationElement(createAfterIndex)}
        />
      </View>
    );
  };

  if (isRenderAggregateAsTable && props.summary === 0) {
    elements = [];
    const table = renderAggregateAsTable();
    if (table) {
      elements[0] = table;
    }
  } else if (props.isList) {
    if (props.value != null && Array.isArray(props.value)) {
      const filterResult = applyFilter(transformedFilters, props.value);
      const indexSave: number[] = filterResult.indexSave;
      const localValues = filterResult.valueFiltered;
      let newAggregates = undefined;

      if (props.value.length > aggregates.length) {
        aggregates.forEach((v) => {
          if (v.detailsVisible) {
            toggleDetails(v.id);
          }
        });
        const sortedValues = props.value
          .slice()
          .sort((a: any, b: any) => a.sortIndex - b.sortIndex);

        newAggregates = sortedValues.map((v: any) => {
          const existing = aggregates.find((agg) => agg.id === v.id);

          if (existing) {
            return existing;
          } else {
            return {
              id: v.id,
              name: v.name || label || '',
              detailsVisible: v.detailsVisible || false,
            };
          }
        });
      }
      const start = calculatePagingStart();
      if (props.summary === 0) {
        for (let i = start; i < localValues.length; i++) {
          if (i >= start + pageSize) break;
          const agg = newAggregates
            ? newAggregates[indexSave[i]]
            : aggregates[indexSave[i]];
          elements.push(buildSingleAggregationElem(agg, indexSave[i]));
        }
      } else {
        aggregates.forEach((agg, i) => {
          elements.push(buildSingleAggregationElem(agg, i));
        });
      }
      if (localValues.length != aggregates.length) {
        setAggregates(
          props.value.map((aggObject: any) => ({
            id: aggObject.id,
            name: aggObject.name || label || '',
            detailsVisible: aggObject.detailsVisible || false,
          })),
        );
      }
    }
  } else {
    const jsonpath = props.jsonpath + '.value';
    if (props.summary > 0) {
      elements.push(
        <View key={'AggRowView-summary-' + jsonpath}>
          <ElementRenderer
            {...props}
            elements={props.elements}
            jsonpath={jsonpath}
            key={
              'AggRowViewSummary-ElementRenderer-' +
              jsonpath +
              '-' +
              props.parentId
            }
            aggregatShowOnlyFirstLine={props.formGridOptions?.showOnlyFirstLine}
            parentId={props.id}
            isAggregate={true}
            summary={SummaryOptions.SummaryText}
          />
        </View>,
      );
    } else {
      elements.push(
        <View key={'AggRowView-' + jsonpath + '-' + props.parentId}>
          <ElementRenderer
            {...props}
            jsonpath={props.jsonpath + '.value'}
            parentId={props.id}
            isAggregate={true}
            key={
              'AggRowView-ElementRenderer-' + jsonpath + '-' + props.parentId
            }
          />
        </View>,
      );
    }
  }

  type ValueEntry = { value?: unknown; visible?: boolean };
  type ValueMap = Record<string, ValueEntry>;

  type AggregateProps = {
    elements?: Array<{
      id: string;
      formGridOptions?: { valueSummary?: Record<string, unknown> };
    }>;
    value?: ValueMap;
    values?: ValueMap;
    summary?: number;
  };

  const isNonEmpty = (val: unknown): boolean => {
    if (val == null) return false;
    if (Array.isArray(val)) return val.length > 0;
    if (typeof val === 'object') {
      return Object.values(val as Record<string, unknown>).some(isNonEmpty);
    }
    return String(val).trim() !== '';
  };

  const isValueMapLike = (o: unknown): o is ValueMap => {
    if (!o || typeof o !== 'object' || Array.isArray(o)) return false;
    const vals = Object.values(o as Record<string, unknown>);
    return vals.some(
      (x) =>
        !!x &&
        typeof x === 'object' &&
        ('value' in (x as any) || 'visible' in (x as any)),
    );
  };

  const hasAnyValueRecursive = (map?: ValueMap): boolean => {
    if (!map) return false;

    return Object.values(map).some((entry) => {
      const v = entry?.value;

      if (isValueMapLike(v)) {
        return hasAnyValueRecursive(v as ValueMap);
      }

      return isNonEmpty(v);
    });
  };

  function shouldRenderSummary(props: AggregateProps): boolean {
    const elements = props.elements ?? [];
    const valuesMap: ValueMap = props.values ?? props.value ?? {};

    const hasVisibleSummary = elements.some((el) => {
      const vs = el.formGridOptions?.valueSummary;
      return isNonEmpty(vs);
    });

    const hasAnyValue = hasAnyValueRecursive(valuesMap);
    return hasVisibleSummary || hasAnyValue;
  }

  if (props.summary > 0) {
    if (!shouldRenderSummary(props)) {
      return;
    }
    return renderSummary(props, elements);
  }

  //Dropdown
  const [filterDropdownOpen, setFilterDropdownOpen] = useState(false);

  const visibleElements =
    props.elements?.filter(
      (element) =>
        typeof element?.editModeDialogUsage === 'undefined' ||
        element.editModeDialogUsage === 0,
    ) ?? [];

  const dropdownItems = [
    { identifier: 'all', displayName: t('form_filter_text_all') },
    // nur sichtbare Elemente
    ...visibleElements.map((el, idx) => ({
      identifier: idx,
      displayName: getI18nText(formState, el.label) || 'N/A',
    })),
  ];
  const selectedFilterItem =
    dropdownItems.find((item) => item.identifier === filterColumn) ||
    dropdownItems[0];

  return (
    <View
      key={'AggregateView-' + props.jsonpath}
      ref={elementRef}
      accessible
      accessibilityLabel={t('form_accessibility_aggregate_group', {
        label: getI18nText(formState, label),
      })}
      style={styles.container}
    >
      {/* CSV-Upload Button */}
      {props.csvUploadEnabled && props.editable && (
        <View
          key={'AggregateBtnContainer-' + props.jsonpath}
          style={styles.uploadButtonContainer}
        >
          <AnimatedPressable
            id={`${props.id}-csv-upload-btn`}
            accessible
            accessibilityRole="button"
            accessibilityLabel={t(
              'form_accessibility_aggregate_csv_upload_label',
            )}
            accessibilityHint={t(
              'form_accessibility_aggregate_csv_upload_hint',
            )}
            accessibilityState={{ disabled: !props.editable }}
            accessibilityLiveRegion="polite"
            importantForAccessibility="yes"
            style={uploadButtonHoverStyle}
            onHoverIn={() => {
              uploadButtonHoverIn();
              uploadButtonTextHoverIn();
            }}
            onHoverOut={() => {
              uploadButtonHoverOut();
              uploadButtonTextHoverOut();
            }}
            onPress={() => {
              formEventsUtils.submitCSVUpload(
                props.jsonpath,
                props.id,
                props.formState,
                props.uiState,
                props.updateState,
                props.restService,
              );
            }}
          >
            <AndpText style={uploadButtonTextHoverStyle}>
              {t('form_csv_upload_label')}
            </AndpText>
          </AnimatedPressable>
        </View>
      )}
      {/* Globaler Filterbereich – wird immer angezeigt, sofern showFilter in den Options aktiv ist */}
      {props.isList && props.formGridOptions?.showFilter && (
        <View key={'GlobalFilter-' + props.jsonpath} style={styles.filter}>
          <View style={styles.filterSettingsContainer}>
            <View
              style={[
                styles.filterSettings,
                isMobile && styles.filterSettingsMobile,
                widthFromColumNumber(6, breakpoints),
                breakpoints.maximumXl && widthFromColumNumber(8, breakpoints),
                breakpoints.maximumLg && widthFromColumNumber(10, breakpoints),
              ]}
            >
              <View
                id={props.id + '-filter-dropdown'}
                style={styles.filterDropdownContainer}
              >
                <BaseDropDownInputElement
                  //TODO accessibility
                  t={props.t}
                  onlyDropdown={true}
                  isList={false}
                  value={selectedFilterItem}
                  inputValue={selectedFilterItem.displayName}
                  placeholderText={t('form_filter_text_all')}
                  labelText={t('form_filter_label_text_all')}
                  dropdownData={dropdownItems}
                  onChangeInputValue={() => {}}
                  dropdownRenderItem={({ item, index }) => (
                    <View
                      key={`filter-dropdown-item-${item.identifier ?? index}`}
                    >
                      <AndpText>{item.displayName}</AndpText>
                    </View>
                  )}
                  dropdownOnSelect={(item) => {
                    setFilterColumn(
                      item.identifier === 'all'
                        ? 'all'
                        : (item.identifier as number),
                    );
                    setFilterDropdownOpen(false);
                  }}
                  dropdownOpen={filterDropdownOpen}
                  onFocus={() => setFilterDropdownOpen(true)}
                  onBlur={() => setFilterDropdownOpen(false)}
                />
              </View>
              <View style={styles.filterInputAndBtnContainer}>
                <TouchableWithoutFeedback
                  onBlur={() => handleFilterInputBlur(false)}
                  onPress={handleFilterInputFocus}
                >
                  <View style={styles.filterInputContainer}>
                    <RNAnimated.Text
                      style={[
                        styles.filterPlaceholder,
                        {
                          top: animatedPlaceholderTop,
                          fontSize: animatedPlaceholderFontSize,
                        },
                      ]}
                      numberOfLines={1}
                      ellipsizeMode="tail"
                      // @ts-ignore
                      pointerEvents="none"
                    >
                      {t('form_filter_label')}
                    </RNAnimated.Text>
                    <TextInput
                      id={props.id + '-filter-input'}
                      accessible
                      accessibilityRole="search"
                      accessibilityLabel={t(
                        'form_accessibility_aggregate_filter_label',
                      )}
                      accessibilityHint={t(
                        'form_accessibility_aggregate_filter_input_hint',
                      )}
                      accessibilityState={{ disabled: !props.editable }}
                      accessibilityValue={{ text: filterTerm }}
                      onBlur={() => handleFilterInputBlur(false)}
                      onFocus={handleFilterInputFocus}
                      value={filterTerm}
                      onChangeText={setFilterTerm}
                      onSubmitEditing={addFilter}
                      style={[
                        styles.filterInput,
                        filterInputIsFocused && styles.filterInputFocused,
                      ]}
                    />
                  </View>
                </TouchableWithoutFeedback>
                <Reanimated.View
                  style={[
                    styles.aggAddFilterButton,
                    filterTerm.trim() !== '' && styles.addFilterButtonActive,
                    filterTerm.trim() !== '' && addFilterButtonHoverStyle,
                  ]}
                >
                  <Pressable
                    id={props.id + '-add-filter-btn'}
                    accessible
                    accessibilityRole="button"
                    accessibilityLabel={t(
                      'form_accessibility_aggregate_add_filter',
                    )}
                    accessibilityHint={t(
                      'form_accessibility_aggregate_add_filter_hint',
                    )}
                    accessibilityState={{ disabled: filterTerm.trim() === '' }}
                    onPress={addFilter}
                    disabled={filterTerm.trim() === ''}
                    style={styles.aggAddFilterPressable}
                    onHoverIn={() => {
                      addFilterButtonHoverIn();
                      setAddFilterBtnOnHover(true);
                    }}
                    onHoverOut={() => {
                      addFilterButtonHoverOut();
                      setAddFilterBtnOnHover(false);
                    }}
                  >
                    <AndpIcon
                      name="plus"
                      size={
                        aggregateElementTheme.filter.addFilterButton.iconSize
                      }
                      color={
                        filterTerm.trim() === ''
                          ? aggregateElementTheme.filter.addFilterButton
                              .iconColor
                          : addFilterBtnOnHover
                            ? aggregateElementTheme.filter.addFilterButton
                                .onHover.iconColor
                            : aggregateElementTheme.filter.addFilterButton
                                .active.iconColor
                      }
                    />
                  </Pressable>
                </Reanimated.View>
              </View>
            </View>
          </View>
          <View style={styles.filterDivider}></View>
          {activeFilters.length > 0 && (
            <View
              key={'AggregateAppliedFilterContainer-' + props.jsonpath}
              style={styles.appliedFiltersContainer}
            >
              {activeFilters.map((filter, index) => {
                const headerText =
                  filter.column === 'all'
                    ? t('form_filter_text_all')
                    : props.elements && props.elements[Number(filter.column)]
                      ? getI18nText(
                          formState,
                          props.elements[Number(filter.column)].label,
                        )
                      : 'N/A';
                return (
                  <View
                    key={`agg-${props.jsonpath}-global-filter-${index}`}
                    style={styles.appliedFilterCard}
                  >
                    <TouchableOpacity onPress={() => removeFilter(index)}>
                      <AndpIcon
                        name="trashCan"
                        size={
                          aggregateElementTheme.filter.appliedFilter.iconSize
                        }
                        color={
                          aggregateElementTheme.filter.appliedFilter.iconColor
                        }
                      />
                    </TouchableOpacity>
                    <View
                      style={{
                        flexDirection: 'row',
                        gap: theme.space.extraSmall,
                      }}
                    >
                      <AndpText style={styles.appliedFilterCardTypeText}>
                        {headerText}:
                      </AndpText>
                      <AndpText style={styles.appliedFilterCardText}>
                        {filter.term}
                      </AndpText>
                    </View>
                  </View>
                );
              })}
            </View>
          )}
        </View>
      )}

      {/* Pagination oben */}
      {showPagination && (
        <Pagination
          key={'AggPaginationOben-' + props.jsonpath}
          previousLabel={t('form_aggregat_previous')}
          nextLabel={t('form_aggregat_next')}
          breakLabel={'...'}
          pageCount={Math.ceil(localValues.length / pageSize)}
          marginPagesDisplayed={2}
          pageRangeDisplayed={5}
          currentPage={page}
          onPageChange={(selectedPage) => {
            setPage(selectedPage);
          }}
        />
      )}

      {props.isList &&
        props.editable &&
        hasFilteredEntries &&
        localValues.length > 0 &&
        props.editModeDialogFormID &&
        props.createable &&
        addButton(true)}
      {props.isList &&
        props.formGridOptions?.showFilter &&
        (() => {
          const filterResult = applyFilter(transformedFilters, props.value);
          const filteredValues = filterResult.valueFiltered;

          if (filteredValues.length === 0) {
            return (
              <View
                style={styles.noEntriesContainer}
                key={'NoFilterLeft-' + props.jsonpath}
              >
                <AndpText style={styles.noEntriesText}>
                  {t('form_filter_no_entrys_left')}
                </AndpText>
              </View>
            );
          }
          return null;
        })()}

      {/* Aggregat-Elemente */}
      {
        <View
          key={'AggregateElementsView-' + props.jsonpath}
          style={{ gap: 20 }}
        >
          {elements}
        </View>
      }

      {/* show appended add button if there are no elements or if dialog form editing is enabled  */}
      {props.isList &&
        props.editable &&
        (localValues.length === 0 || props.editModeDialogFormID) &&
        props.createable &&
        addButton()}

      {/* Pagination unten */}
      {showPagination && (
        <Pagination
          previousLabel={t('form_aggregat_previous')}
          nextLabel={t('form_aggregat_next')}
          breakLabel={'...'}
          pageCount={Math.ceil(localValues.length / pageSize)}
          marginPagesDisplayed={2}
          pageRangeDisplayed={5}
          currentPage={page}
          onPageChange={(selectedPage) => setPage(selectedPage)}
        />
      )}
    </View>
  );
}

AggregateElement.validate = function (
  state: FormState,
  jsonPath: string,
  id: string,
  updateErrorMessages: boolean,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
): CommonValidation {
  return {
    isValid: true,
    errorMessage: '',
  };
};

AggregateElement.getSummaryValue = function (
  state: FormState,
  jsonPath: string,
  id: string,
  t: TFunction,
  formGridOptions?: IFormGridOptions,
) {
  return 'Debug';
};

function renderSummary(
  props: IFormComponentAttributes,
  elements: React.ReactElement[],
) {
  return (
    <SummaryElement
        initUrl={props.restService.initUrl}
        t={props.t}
        formState={props.formState}
        uiState={props.uiState}
        summaryOption={props.summary}
        label={props.label}
        value={elements?.length ? elements : null}
        aggregat={true}
        numberOfColumns={props.formGridOptions?.numberOfColumns}
      />
  );
}

const AggregateAddButton = ({
  id,
  className,
  onPress,
  showButtonText = false,
}: IAggregateAddButtonProps) => {
  const { t } = useTranslation();
  const isMobile = useWindowDimensions().width < 768;
  const styles = useMemo(
    () => mergeStyles(aggregateElementStyles, customAggregateElementStyles),
    [],
  );

  return (
    <Pressable
      //@ts-ignore
      dataSet={className ? { class: className } : undefined}
      id={id}
      onPress={onPress}
      style={({ hovered }) => [
        styles.aggAddButton,
        hovered && styles.aggAddButtonOnHover,
        isMobile && styles.aggAddButtonMobile,
      ]}
      accessible
      accessibilityRole="button"
      accessibilityLabel={t('form_add')}
      accessibilityHint={t('form_accessibility_aggregate_add_entry_hint')}
    >
      {({ hovered }) => {
        const contentStyle = [
          styles.aggAddButtonContent,
          hovered && styles.aggAddButtonContentOnHover,
        ];

        return (
          <>
            <AndpIcon name="plusSquare" style={contentStyle} />
            {showButtonText && (
              <AndpText style={contentStyle}>{t('form_add')}</AndpText>
            )}
          </>
        );
      }}
    </Pressable>
  );
};

export default AggregateElement;
