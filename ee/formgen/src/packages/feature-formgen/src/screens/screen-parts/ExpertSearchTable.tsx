import React, {
  useState,
  useRef,
  useEffect,
  useCallback,
  useMemo,
  PureComponent,
} from 'react';
import {
  View,
  Pressable,
  Platform,
  Linking,
  useWindowDimensions,
  ScrollView,
  NativeSyntheticEvent,
  NativeScrollEvent,
  FlatList,
} from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { expertSearchTableTheme } from '@andp/core/src/styles/theme';
import * as interfaces from '../../types/interfaces';
import * as overlayUtils from '../../utils/OverlayUtils';
import { StatesToUpdateEnum } from '../../constants/enums/StateToUpdateEnum';
import { OverlayTypeEnum } from '../../custom/OverlayTypes';
import AndpText from '@andp/core/src/components/AndpText';
import { endANRequest, startANRequest } from '../../utils/Utils';
import { expertSearchStyles } from '../../styles/common/common-style';
import { expertSearchStyles as customExpertSearchStyles } from '../../styles/custom/custom-style';
import {
  enhancedWidthFromColumNumber,
  mergeStyles,
  useBreakpoints,
} from '@andp/core/src/AndpUtils';
import CheckBox from './Table/Checkbox';
import Header from './Table/Header';
import Row from './Table/Row';
import Card from './Table/Card/Card';
import CustomAggregatePagination from '../../components/elements/helper/CustomAggregatePagination';
import { useMicroAppSDK } from '@andp/core/src/sdk/MicroAppSDK';

const MIN_W = 60;
const CHAR_PX = 8;
const MAX_W = 300;
const PADDING = expertSearchTableTheme.table.headerCell.padding * 2;
const CARD_PAGE_SIZE = 20;

/**
 * Props for the TableView component.
 *
 * @property {any[]} content - Array of row data to display in the table.
 * @property {any} styles - Style object for table container and cells.
 * @property {{ maximumMd: boolean; maximumLg: boolean }} breakpoints - Breakpoint flags for responsive layout.
 * @property {number} formWidth - The width factor of the form, used to calculate table width.
 * @property {number[]} visibleColumns - Zero-based indices of the columns currently visible.
 * @property {IReportResultHeaderColumn[]} headerColumns - Column definitions including names and types.
 * @property {string[]} sortDirection - Sort order ('asc' or 'desc') for each visible column.
 * @property {(idx: number) => void} sortTable - Callback to sort the table by the given column index.
 * @property {number[]} checkBoxesState - Indices of rows currently selected in multi-select mode.
 * @property {(buttonID: string, param: string) => Promise<void>} triggerFormEndpoint - Handler to invoke when a form action button is pressed.
 * @property {() => void} toggleAll - Callback to select or deselect all rows.
 * @property {(row: number) => void} toggleRow - Callback to toggle selection for a single row.
 * @property {(row: number) => boolean} isChecked - Function to determine if a given row is selected.
 * @property {boolean} multiSelect - Whether multi-select mode is active.
 */
interface TableViewProps {
  content: any[];
  styles: any;
  breakpoints: { maximumMd: boolean; maximumLg: boolean };
  formWidth: number;
  visibleColumns: number[];
  headerColumns: interfaces.IReportResultHeaderColumn[];
  sortDirection: string[];
  sortTable: (idx: number) => void;
  checkBoxesState: number[];
  triggerFormEndpoint: (buttonID: string, param: string) => Promise<void>;
  toggleAll: () => void;
  toggleRow: (row: number) => void;
  isChecked: (row: number) => boolean;
  multiSelect: boolean;
}

/**
 * TableView
 * ----------------------------------------
 * Renders a horizontally scrollable table with a fixed header and synchronized content rows.
 * Uses FlashList for performant row virtualization and calculates initial column widths
 * based on content length and header names.
 *
 * @component
 * @param {TableViewProps} props - Configuration and callbacks for rendering the table.
 * @returns {JSX.Element} The table view element.
 */
const TableView: React.FC<TableViewProps> = (props) => {
  const scrollableExpersearchTable = expertSearchTableTheme.table.scrollable;
  const [containerWidthPx, setContainerWidthPx] = useState(0);
  const calculateInitialColumnWidth = () => {
    if (!props.visibleColumns.length) return;


    const tempWidths = props.visibleColumns.map((i) => {
      const col = props.headerColumns[i];
      const samples = [
        col.name,
        ...props.content.map((row) =>
          typeof row[i] === 'object' && !col.isMultiSelect
            ? col.name
            : String(row[i] ?? ''),
        ),
      ];
      const w = Math.min(
        MAX_W,
        Math.max(
          MIN_W,
          Math.max(...samples.map((s) => s.length)) * CHAR_PX + PADDING,
        ),
      );
      return w;
    });

    const columWidthSum = tempWidths.reduce((s, w) => s + w, 0) + (props.multiSelect ? 40 : 0);
    if (columWidthSum > containerWidthPx) return tempWidths;
    else {
      const remainingSpace = Math.max(0, containerWidthPx - 1 - columWidthSum);
      const base = Math.floor(remainingSpace / tempWidths.length);
      const remainder = remainingSpace % tempWidths.length;
      const widened = tempWidths.map((w, i) => w + base);
      return widened;
    }
  };

  // For Performance Reasons
  const initialWidths = useMemo(
    () => {
      return calculateInitialColumnWidth()
    },
    [props.visibleColumns, props.content, containerWidthPx],
  );

  const { colWidths, startResize } = useResizeHandlers(initialWidths);

  const totalTableWidth =
    colWidths.reduce((s, w) => s + w, 0) + (props.multiSelect ? 40 : 0);

  const checkBoxesState = props.checkBoxesState;

  // +-------------------------------------------------+
  // | Reference and Event Handler for the Scroll Bars |
  // +-------------------------------------------------+
  const headerRef = useRef<ScrollView | null>(null);
  const contentRef = useRef<ScrollView | null>(null);
  const onHeaderScroll = useCallback(
    (e: NativeSyntheticEvent<NativeScrollEvent>) => {
      contentRef.current?.scrollTo({
        x: e.nativeEvent.contentOffset.x,
        animated: false,
      });
    },
    [],
  );
  const onContentScroll = useCallback(
    (e: NativeSyntheticEvent<NativeScrollEvent>) => {
      headerRef.current?.scrollTo({
        x: e.nativeEvent.contentOffset.x,
        animated: false,
      });
    },
    [],
  );

  // stable keys for rows
  const rowKeyMap = useRef<WeakMap<any, string>>(new WeakMap());
  const getRowKey = useCallback((row: any, fallbackIndex: number) => {
    let k = rowKeyMap.current.get(row);
    if (!k) {
      k = `row-${fallbackIndex}-${Math.random().toString(36).slice(2, 8)}`;
      rowKeyMap.current.set(row, k);
    }
    return k;
  }, []);

  return (
    <View
      style={[
        props.styles.tableContainer,
        enhancedWidthFromColumNumber(
          props.formWidth || 8,
          props.breakpoints.maximumMd,
          !scrollableExpersearchTable
        ),
        !scrollableExpersearchTable && { width: 'min-content' },
      ]}
      onLayout={(e) => setContainerWidthPx(Math.round(e.nativeEvent.layout.width))}
    >
      {/* --------------------------------------------- */}
      {/* Dient nur für die obere scrollbar der Tabelle */}
      {/* --------------------------------------------- */}
      {expertSearchTableTheme.table.scrollable && (
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={props.content.length > 10}
          onScroll={onHeaderScroll}
          scrollEventThrottle={16}
        >
          <View style={{ width: totalTableWidth, height: 1 }}></View>
        </ScrollView>
      )}
      {/* --------------------------------------------- */}
      <ScrollView ref={headerRef} style={!scrollableExpersearchTable && { width: 'min-content' }}>
        <View style={{ width: totalTableWidth }}>
          <Header
            visibleColumns={props.visibleColumns}
            headerColumns={props.headerColumns}
            colWidths={colWidths}
            isMultiSelectChecked={
              checkBoxesState.length === props.content.length
            }
            sortDirection={props.sortDirection}
            isMultiSelect={props.multiSelect}
            styles={props.styles}
            toggleAll={props.toggleAll}
            sortTable={props.sortTable}
            startResize={startResize}
          />
        </View>
      </ScrollView>
      <ScrollView
        ref={contentRef}
        horizontal
        showsHorizontalScrollIndicator
        onScroll={onContentScroll}
        scrollEventThrottle={16}
        style={!scrollableExpersearchTable && { width: 'min-content' }}
      >
        <View style={{ width: totalTableWidth }}>
          <FlashList
            data={props.content}
            renderItem={({ item, index }) => (
              <Row
                item={item}
                index={index}
                styles={props.styles}
                colWidths={colWidths}
                headerColumns={props.headerColumns}
                visibleColumns={props.visibleColumns}
                isChecked={props.isChecked}
                toggleRow={props.toggleRow}
                triggerFormEndpoint={props.triggerFormEndpoint}
                isMultiSelect={props.multiSelect}
              />
            )}
            keyExtractor={(item, index) => getRowKey(item, index)}
            extraData={{ checkBoxesState, colWidths }}
          />
        </View>
      </ScrollView>
    </View>
  );
};

/**
 * Props for the CardView component.
 *
 * @property {any} styles - Style object for card container and elements.
 * @property {IReportResultHeaderColumn[]} headerColumns - Column definitions used for labeling cards.
 * @property {number[]} checkBoxesState - Selected row indices in multi-select mode.
 * @property {any[]} content - Array of row data to render as cards.
 * @property {boolean} multiSelect - Whether multi-select mode is active.
 * @property {(row: number) => boolean} isChecked - Function to check if a row is selected.
 * @property {() => void} toggleAll - Callback to select or deselect all rows.
 * @property {(row: number) => void} toggleRow - Callback to toggle selection for a single row.
 * @property {(buttonID: string, param: string) => Promise<void>} triggerFormEndpoint - Handler for action buttons in cards.
 * @property {(key: string) => string} t - Translation function for localized text.
 */
interface CardViewProps {
  styles: any;
  headerColumns: interfaces.IReportResultHeaderColumn[];
  checkBoxesState: number[];
  content: any[];
  multiSelect: boolean;
  isChecked: (row: number) => boolean;
  toggleAll: () => void;
  toggleRow: (row: number) => void;
  triggerFormEndpoint: (buttonID: string, param: string) => Promise<void>;
  t: (key: string) => string;
}

/**
 * CardView
 * ----------------------------------------
 * Renders a list of cards representing table rows when screen is small or on mobile.
 * Each card shows a primary field, optional multi-select checkbox, and action buttons.
 * Uses FlashList for virtualization of card items.
 *
 * @component
 * @param {CardViewProps} props - Configuration and callbacks for rendering cards.
 * @returns {JSX.Element} The card view element.
 */
const CardView: React.FC<CardViewProps> = React.memo((props) => {
  /**
   * Current page index used for card pagination.
   * The index is zero-based to align with CustomAggregatePagination.
   */
  const [currentPage, setCurrentPage] = useState(0);

  /**
   * Total number of pages based on a fixed page size.
   * We always return at least 1 page to keep pagination stable.
   */
  const pageCount = useMemo(
    () =>
      props.content.length
        ? Math.ceil(props.content.length / CARD_PAGE_SIZE)
        : 1,
    [props.content.length],
  );

  /**
   * Reset to the first page whenever the underlying content array changes.
   * This ensures new searches or filters always start at page 1.
   */
  useEffect(() => {
    setCurrentPage(0);
  }, [props.content]);

  /**
   * Clamp the current page when the result set shrinks (e.g. after a new search),
   * so we never point to a page index that no longer exists.
   */
  useEffect(() => {
    if (currentPage > pageCount - 1) {
      setCurrentPage(pageCount - 1);
    }
  }, [pageCount, currentPage]);

  /**
   * Starting index within the full content array for the current page.
   */
  const pageOffset = currentPage * CARD_PAGE_SIZE;

  /**
   * Slice of the full result set that should be rendered as cards on
   * the current page. This keeps the rendered list small and predictable,
   * even if the total result set contains thousands of entries.
   */
  const pageContent = useMemo(
    () =>
      props.content.slice(
        pageOffset,
        pageOffset + CARD_PAGE_SIZE,
      ),
    [props.content, pageOffset],
  );

  return (
    <View style={props.styles.cardView}>
      <View style={props.styles.cardViewHeaderContainer}>
        <AndpText style={props.styles.cardViewListTitle}>
          {props.headerColumns[0].name}
        </AndpText>
        {props.multiSelect ? (
          <View style={props.styles.cardSelectAllHeader}>
            <Pressable onPress={props.toggleAll}>
              {CheckBox(props.checkBoxesState.length === props.content.length)}
            </Pressable>
            <AndpText style={props.styles.cardSelectAllHeaderText}>
              {props.t('form_expertSearch_checkMarksChooseAll')}
            </AndpText>
          </View>
        ) : null}
      </View>
      {pageCount > 1 && (
        <CustomAggregatePagination
          currentPage={currentPage}
          pageCount={pageCount}
          onPageChange={setCurrentPage}
          marginPagesDisplayed={1}
          key={'ExpertSearchPaginationOben-' + props.content.length}
          previousLabel={props.t('form_aggregat_previous')}
          nextLabel={props.t('form_aggregat_next')}
        />
      )}
      <FlashList
        data={pageContent}
        renderItem={({ item, index }) => {
          // Translate the index within the current page into the
          // global row index of the full content array so that
          // selection and actions continue to work as before.
          const globalIndex = pageOffset + index;
          return (
            <Card
              item={item}
              index={globalIndex}
              styles={props.styles}
              isMultiSelect={props.multiSelect}
              headerColumns={props.headerColumns}
              isChecked={props.multiSelect ? props.isChecked : undefined}
              toggleRow={props.multiSelect ? props.toggleRow : undefined}
              triggerFormEndpoint={props.triggerFormEndpoint}
            />
          );
        }}
        keyExtractor={(_: any, i: number) => (pageOffset + i).toString()}
        extraData={props.checkBoxesState}
        ItemSeparatorComponent={() => (
          <View style={props.styles.cardSeparator} />
        )}
      />
      {pageCount > 1 && (
        <CustomAggregatePagination
          currentPage={currentPage}
          pageCount={pageCount}
          onPageChange={setCurrentPage}
          marginPagesDisplayed={1}
          key={'ExpertSearchPaginationUnten-' + props.content.length}
          previousLabel={props.t('form_aggregat_previous')}
          nextLabel={props.t('form_aggregat_next')}

        />
      )}
    </View>
  );
});
/**
 * Props for the SearchTable component.
 *
 * @property {any} styles - Style object for both table and card views.
 * @property {boolean} isSmall - Flag indicating if the current window width is small.
 * @property {{ maximumMd: boolean; maximumLg: boolean }} breakpoints - Breakpoint flags for responsive layout.
 * @property {any[]} content - Array of row data to display.
 * @property {number} formWidth - Width factor for table layout.
 * @property {number[]} visibleColumns - Indices of columns currently visible.
 * @property {IReportResultHeaderColumn[]} headerColumns - Definitions for all header columns.
 * @property {string[]} sortDirection - Sort order for visible columns.
 * @property {number[]} checkBoxesState - Indices of rows selected in multi-select.
 * @property {(idx: number) => void} sortTable - Callback to sort by column.
 * @property {(buttonID: string, param: string) => Promise<void>} triggerFormEndpoint - Handler for table action buttons.
 * @property {() => void} toggleAll - Callback to toggle all row selections.
 * @property {(row: number) => void} toggleRow - Callback to toggle a single row selection.
 * @property {(row: number) => boolean} isChecked - Function to check if a row is selected.
 * @property {(key: string) => string} t - Translation function.
 */
interface SearchTableProps {
  styles: any;
  isSmall: boolean;
  breakpoints: { maximumMd: boolean; maximumLg: boolean };
  content: any[];
  formWidth: number;
  visibleColumns: number[];
  headerColumns: interfaces.IReportResultHeaderColumn[];
  sortDirection: string[];
  checkBoxesState: number[];
  sortTable: (idx: number) => void;
  triggerFormEndpoint: (buttonID: string, param: string) => Promise<void>;
  toggleAll: () => void;
  toggleRow: (row: number) => void;
  isChecked: (row: number) => boolean;
  t: (key: string) => string;
}

/**
 * SearchTable
 * ----------------------------------------
 * ! This Component was added in order to increase Performance and reduce re-renders !
 * It's a PureComponent which will only re-renderes if the shallow comparison is not equal.
 * 
 * Chooses between rendering a TableView or CardView based on screen size or platform.
 * Provides synchronized scrolling and multi-select capabilities.
 * Wraps main table and card presentations inside a PureComponent for optimized re-renders.
 *
 * @extends PureComponent<SearchTableProps>
 */
class SearchTable extends PureComponent<SearchTableProps> {
  private get multiSelect(): boolean {
    return this.props.headerColumns.some((c) => c.isMultiSelect);
  }
  render() {
    /**
      Whether to force card view on web when small or not.
      If true, the table will switch to card view on web when the window width is less than 768px.
    **/
    const forceCardViewOnWebWhenSmall = true;

    const isSmall = this.props.isSmall;
    const styles = this.props.styles;
    const breakpoints = this.props.breakpoints;
    const content = this.props.content;
    const formWidth = this.props.formWidth;
    const visibleColumns = this.props.visibleColumns;
    const headerColumns = this.props.headerColumns;
    const sortDirection = this.props.sortDirection;
    const sortTable = this.props.sortTable;
    const checkBoxesState = this.props.checkBoxesState;
    const triggerFormEndpoint = this.props.triggerFormEndpoint;
    const toggleAll = this.props.toggleAll;
    const toggleRow = this.props.toggleRow;
    const isChecked = this.props.isChecked;
    const t = this.props.t;

    const useCardView =
      Platform.OS !== 'web' || (forceCardViewOnWebWhenSmall && isSmall);

    return useCardView ? (
      <CardView
        styles={styles}
        headerColumns={headerColumns}
        checkBoxesState={checkBoxesState}
        content={content}
        multiSelect={this.multiSelect}
        isChecked={isChecked}
        toggleAll={toggleAll}
        toggleRow={toggleRow}
        triggerFormEndpoint={triggerFormEndpoint}
        t={t}
      />
    ) : (
      <TableView
        content={content}
        styles={styles}
        breakpoints={breakpoints}
        formWidth={formWidth}
        visibleColumns={visibleColumns}
        headerColumns={headerColumns}
        sortDirection={sortDirection}
        sortTable={sortTable}
        checkBoxesState={checkBoxesState}
        triggerFormEndpoint={triggerFormEndpoint}
        toggleAll={toggleAll}
        toggleRow={toggleRow}
        isChecked={isChecked}
        multiSelect={this.multiSelect}
      />
    );
  }
}

/**
 * ExpertSearchTable
 * ----------------------------------------
 * Main entry component for expert search functionality.
 * Manages search state, sorting, selection, and invokes data fetching callbacks.
 * Calculates responsive layout breakpoints and assembles header/row props.
 *
 * @component
 * @param {interfaces.IExpertSearch} props - Contains formState, handlers, and translation function.
 * @returns {JSX.Element | null} The rendered expert search table or card container.
 */
export function ExpertSearchTable(props: interfaces.IExpertSearch) {
  const t = props.t;
  const { width: windowWidth } = useWindowDimensions();
  const isSmall = windowWidth < 768;
  const rawBp = useBreakpoints();
  const reportResult = props.reportResult;
  const headerColumns =
    reportResult?.headerColumns ||
    ([] as interfaces.IReportResultHeaderColumn[]);
  const content = reportResult?.content || [];
  const [checkBoxesState, setCheckBoxesState] = useState<number[]>([]);
  const [sortDirection, setSortDirection] = useState<string[]>([]);

  const styles = useMemo(
    () => mergeStyles(expertSearchStyles, customExpertSearchStyles),
    [],
  );
  const breakpoints = useMemo(() => rawBp, [rawBp.maximumMd, rawBp.maximumLg]);
  const visibleColumns = useMemo(
    () =>
      headerColumns
        .map((c, i) => i)
        .filter(
          (i) =>
            !(
              headerColumns[i].type === 'button' &&
              headerColumns[i].isMultiSelect
            ),
        ),
    [headerColumns],
  );

  // initialSearch
  const formHasVisibleProperties = Object.values(
    props.formState.data?.properties || {},
  ).some((p) => p?.visible);
  const [hasSearched, setHasSearched] = useState(props.hasSearched);

  useEffect(() => {
    if (!hasSearched && !formHasVisibleProperties) {
      props.handleExpertSearch && props.handleExpertSearch();
    }
  }, []);

  useEffect(() => {
    if (props.hasSearched != hasSearched) setHasSearched(props.hasSearched);
  }, [props.hasSearched]);

  useEffect(() => {
    if (props.formState.expertMultipleHandler != triggerFormEndpointMultiple) {
      props.formState.expertMultipleHandler = triggerFormEndpointMultiple;
      if (
        props.formState.expertSearchCheckMarkCount == null ||
        props.formState.expertSearchCheckMarkCount != checkBoxesState.length
      ) {
        props.formState.expertSearchCheckMarkCount = checkBoxesState.length;
      }
      props.updateState(StatesToUpdateEnum.FormState);
    }
  }, [checkBoxesState.length]);

  const isChecked = useCallback(
    (row: number) => checkBoxesState.includes(row),
    [checkBoxesState],
  );

  const toggleRow = useCallback(
    (row: number) =>
      setCheckBoxesState((p) =>
        p.includes(row) ? p.filter((i) => i !== row) : [...p, row],
      ),
    [],
  );

  const toggleAll = useCallback(
    () =>
      setCheckBoxesState((p) =>
        p.length === content.length
          ? []
          : Array.from({ length: content.length }, (_, i) => i),
      ),
    [content.length],
  );

  const sortTable = useCallback(
    (idx: number) => {
      const dir = sortDirection[idx] === 'desc' ? 'asc' : 'desc';
      setSortDirection((prev) => {
        const copy = [...prev];
        copy[idx] = dir;
        return copy;
      });
      const data = [...content];
      const selectedRows = checkBoxesState.map((i) => data[i]);
      data.sort((a, b) => {
        const A: any = a[visibleColumns[idx]] ?? '';
        const B: any = b[visibleColumns[idx]] ?? '';
        const c =
          typeof A === 'string' && typeof B === 'string'
            ? A.toLowerCase().localeCompare(B.toLowerCase())
            : 0;
        return dir === 'asc' ? c : -c;
      });
      props.reportResult!.content = data;
      props.updateState(StatesToUpdateEnum.ALL);
      setCheckBoxesState(
        data.reduce<number[]>((acc, row, i) => {
          if (selectedRows.includes(row)) acc.push(i);
          return acc;
        }, []),
      );
    },
    [sortDirection, content, visibleColumns, checkBoxesState],
  );

  const { andpSharedFunctions, andpSharedProps, andpNavigation } = useMicroAppSDK();


  const triggerFormEndpoint = async (buttonID: string, param: string) => {
    startANRequest(() => {
      overlayUtils.setOverlay(
        props.uiState,
        props.formState,
        OverlayTypeEnum.FULL_SCREEN_LOADING,
        props.updateState,
      );
    }, true);
    await props.restService.ANTriggerForm(
      props.formState,
      buttonID,
      [param],
      (res: any) => {
        if (Platform.OS == "web") {
          if (res.targetFormID) {
            const url = `/form/${res.targetFormID}`;
            localStorage.setItem('formID', res.targetFormID);
            localStorage.setItem(
              'initializeData',
              JSON.stringify(res.initializeData),
            );
            res.action === 'openWindow'
              ? window.open(url, '_blank')
              : (window.location.href = url);

          } else if (res.targetUrl) {
            const tgt = res.targetUrl.startsWith('http')
              ? res.targetUrl
              : `https://${res.targetUrl}`;
            res.action === 'openWindow'
              ? window.open(tgt, '_blank')
              : (window.location.href = tgt);

          }
        } else {

          if (res.targetFormID) {
            andpSharedProps.set('formgen.formID', res.targetFormID);
            andpSharedProps.set('formgen.initializeData', JSON.stringify(res.initializeData));

            andpSharedFunctions.get("services.linkCallback")?.(res.targetFormID, true)
          } else if (res.targetUrl) {
            const tgt = res.targetUrl.startsWith('http')
              ? res.targetUrl
              : `https://${res.targetUrl}`;

            Linking.openURL(tgt);
          }

        }
        endANRequest(props.uiState, props.updateState);
      },
      errorTriggerFormEndpoint,
    );
  }

  const triggerFormEndpointMultiple = useCallback(
    async (buttonID?: string) => {
      startANRequest(() => {
        overlayUtils.setOverlay(
          props.uiState,
          props.formState,
          OverlayTypeEnum.FULL_SCREEN_LOADING,
          props.updateState,
        );
      }, true);
      let parameters: string[] = [];
      if (
        typeof props.reportResult === 'object' &&
        typeof buttonID !== 'undefined'
      ) {
        props.reportResult['content'].forEach(
          (rowElement: any, indexParent: any) => {
            if (checkBoxesState.includes(indexParent)) {
              rowElement.forEach((element: any) => {
                if (
                  element['buttonID'] === buttonID &&
                  element['parameter'] !== null
                ) {
                  parameters.push(element['parameter']);
                }
              });
            }
          },
        );
        await props.restService.ANTriggerForm(
          props.formState,
          buttonID,
          parameters,
          callbackTriggerFormEndpoint,
          errorTriggerFormEndpoint,
        );
        endANRequest(props.uiState, props.updateState);
      }
    },
    [checkBoxesState],
  );

  const startsWith = useCallback((str: string, word: string) => {
    return str.lastIndexOf(word, 0) === 0;
  }, []);

  const callbackTriggerFormEndpoint = useCallback(
    (response: interfaces.ITriggerFormEndpointResponse) => {
      if (Platform.OS == "web") {
        if (response.targetFormID && response.targetFormID !== '') {
          let targetUrl = '/form/' + response.targetFormID;
          localStorage.setItem('formID', response.targetFormID);
          localStorage.setItem(
            'initializeData',
            JSON.stringify(response.initializeData),
          );
          if (response.action === 'openWindow') {
            let windowName =
              'w_' + Date.now() + Math.floor(Math.random() * 100000).toString();
            window.open(targetUrl, windowName);
          } else {
            window.location.href = targetUrl;
          }
        } else if (response.targetUrl && response.targetUrl !== '') {
          if (
            !startsWith(response.targetUrl, 'https://') &&
            !startsWith(response.targetUrl, 'http://')
          ) {
            response.targetUrl = 'https://' + response.targetUrl;
          }
          if (response.action === 'openWindow') {
            window.open(response.targetUrl, '_blank');
          } else {
            window.location.href = response.targetUrl;
          }
        }
      } else {
        if (response.targetFormID) {
          andpSharedProps.set('formgen.formID', response.targetFormID);
          andpSharedProps.set('formgen.initializeData', JSON.stringify(response.initializeData));

          andpSharedFunctions.get("services.linkCallback")?.(response.targetFormID, true)
        } else if (response.targetUrl) {
          const tgt = response.targetUrl.startsWith('http')
            ? response.targetUrl
            : `https://${response.targetUrl}`;

          Linking.openURL(tgt);
        }
      }
    },
    [],
  );

  const errorTriggerFormEndpoint = useCallback(
    (request?: interfaces.ApiResponse | Error) => {
      endANRequest(props.uiState, props.updateState);
      console.error('Failure on triggerFormEndpoint');
      overlayUtils.setOverlay(
        props.uiState,
        props.formState,
        OverlayTypeEnum.EXPERT_SEARCH_TRIGGER_ENDPOINT,
        props.updateState,
        { response: request },
      );
    },
    [],
  );

  if (!hasSearched) {
    return null;
  }

  if (content.length === 0) {
    return (
      <AndpText style={styles.emptyText}>
        {t('form_expertSearchEmpty')}
      </AndpText>
    );
  }

  return (
    <View>
      {content.length ? (
        <SearchTable
          styles={styles}
          isSmall={isSmall}
          breakpoints={breakpoints}
          content={content}
          formWidth={props.formState.form.formwidth ?? 8}
          visibleColumns={visibleColumns}
          headerColumns={headerColumns}
          sortDirection={sortDirection}
          sortTable={sortTable}
          checkBoxesState={checkBoxesState}
          triggerFormEndpoint={triggerFormEndpoint}
          toggleAll={toggleAll}
          toggleRow={toggleRow}
          isChecked={isChecked}
          t={t}
        />
      ) : !props.uiState.loading &&
        (global as any).anRequestRunning == false &&
        content.length === 0 &&
        hasSearched ? (
        <AndpText style={styles.emptyText}>
          {t('form_expertSearchEmpty')}
        </AndpText>
      ) : null}
    </View>
  );
}

export default ExpertSearchTable;

interface UseResizeHandlers {
  colWidths: number[];
  startResize: (colIndex: number, clientX: number) => void;
}

/**
 * Manages column-resize state and mouse listeners for resizing.
 *
 * @param initialWidths  array of initial widths
 */

export function useResizeHandlers(
  initialWidths: number[] | undefined,
): UseResizeHandlers {
  /** A Set containing the widths of all columns in the table */
  const [colWidths, setColWidths] = useState<number[]>(
    initialWidths ? initialWidths : [],
  );
  /** The column, which is currently beeing resized */
  const activeCol = useRef<number | null>(null);
  /** The position of the mouse when starting to resize a Column */
  const startX = useRef(0);
  /** A set reference of the starting value of all column widths of the table */
  const startWidths = useRef<number[]>([]);

  // mousemove callback
  const onMouseMove = useCallback((e: MouseEvent) => {
    if (activeCol.current === null) return;
    const dx = e.clientX - startX.current;
    const i = activeCol.current;
    const next = i + 1;
    const newW = [...startWidths.current];
    if (newW[i] + dx < MIN_W || newW[next] - dx < MIN_W) return;
    newW[i] += dx;
    newW[next] -= dx;
    setColWidths(newW);
  }, []);

  // mouseup callback (stop resizing)
  const stopResize = useCallback(() => {
    activeCol.current = null;
    if (Platform.OS === 'web') {
      document.body.style.cursor = 'default';
      document.body.style.userSelect = 'auto';
    }
    window.removeEventListener('mousemove', onMouseMove);
    window.removeEventListener('mouseup', stopResize);
  }, [onMouseMove]);

  // function to kick off resizing
  const startResize = useCallback(
    (colIndex: number, clientX: number) => {
      activeCol.current = colIndex;
      startX.current = clientX;
      startWidths.current = [...colWidths];
      if (Platform.OS === 'web') {
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
      }
      window.addEventListener('mousemove', onMouseMove);
      window.addEventListener('mouseup', stopResize);
    },
    [colWidths, onMouseMove, stopResize],
  );

  // cleanup on unmount
  useEffect(() => {
    return () => {
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', stopResize);
    };
  }, [onMouseMove, stopResize]);

  // sync when the computed initial widths change (e.g., container resize, columns/content change)
  useEffect(() => {
    if (!initialWidths || !initialWidths.length) return;
    const same =
      colWidths.length === initialWidths.length &&
      colWidths.every((w, i) => w === initialWidths[i]);
    if (!same) {
      setColWidths(initialWidths);
    }
  }, [initialWidths]);

  return { colWidths, startResize };
}
