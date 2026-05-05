import AndpText from '@andp/core/src/components/AndpText';
import React from 'react';
import { View, TouchableOpacity, useWindowDimensions } from 'react-native';
import { aggregateElementStyles as styles } from '../../../styles/common/common-style';

interface CustomPaginationProps {
  currentPage: number;
  pageCount: number;
  onPageChange: (selectedPage: number) => void;

  previousLabel?: string;
  nextLabel?: string;
  // Label für Trenner zwischen Seiten (z. B. "...")
  breakLabel?: string;
  marginPagesDisplayed?: number;
  pageRangeDisplayed?: number;
}

const CustomAggregatePagination: React.FC<CustomPaginationProps> = ({
  currentPage,
  pageCount,
  onPageChange,
  previousLabel = 'Previous',
  nextLabel = 'Next',
  breakLabel = '...',
  marginPagesDisplayed = 2,
  pageRangeDisplayed = 5,
}) => {
  const { width } = useWindowDimensions();
  const effectiveMarginPagesDisplayed = width < 400 ? 1 : marginPagesDisplayed;
  const effectivePageRangeDisplayed = width < 400 ? 1 : (width < 550 ? 2 : pageRangeDisplayed);

  function generatePageNumbers(
    currentPage1Based: number,
    total: number,
    margin: number,
    range: number
  ): number[] {
    const pagesToShow: number[] = [];
    
    function addPages(start: number, end: number) {
      for (let i = start; i <= end; i++) {
        pagesToShow.push(i);
      }
    }

    if (total <= margin * 2 + range) {
      addPages(1, total);
      return pagesToShow;
    }
  
    // Links
    addPages(1, margin);
  
    // Berechne Kernbereich
    let left = Math.max(currentPage1Based - Math.floor(range / 2), margin + 1);
    let right = left + range - 1;
    if (right > total - margin) {
      right = total - margin;
      left = Math.max(right - range + 1, margin + 1);
    }
  
    if (left > margin + 1) {
      pagesToShow.push(-1); // -1 als Platzhalter für "…"
    }
  
    addPages(left, right);
    if (right < total - margin) {
      pagesToShow.push(-1);
    }
  
    // Rechts
    addPages(total - margin + 1, total);
  
    return pagesToShow;
  }
  
  // Beachte: Da currentPage in unserem Component 0-basiert ist, übergeben wir currentPage+1
  const pageNumbers = generatePageNumbers(
    currentPage + 1,
    pageCount,
    effectiveMarginPagesDisplayed,
    effectivePageRangeDisplayed
  );
  
  return (
    <View style={styles.paginationContainer}
          accessibilityRole="adjustable"
          accessibilityLabel="Pagination" //TODO translation
          accessibilityHint="Swipe left or right to adjust the page">
      {/* Prev-Button */}
      <TouchableOpacity
        //@ts-ignore
        dataSet={{ class: 'agg-pagination-previous-page-btn' }}
        style={[styles.pageItem, currentPage === 0 && styles.disabledButton]}
        disabled={currentPage === 0}
        onPress={() => onPageChange(currentPage - 1)}
        accessibilityRole="button"
        accessibilityLabel={`Previous page. ${previousLabel}`}
        accessibilityHint="Navigates to the previous page"
      >
        <AndpText style={styles.pageItemText}>{previousLabel}</AndpText>
      </TouchableOpacity>
  
      {/* Seitenzahlen */}
      {pageNumbers.map((p, index) => {
        if (p === -1) {
          return (
            <View key={`break-${index}`} style={styles.breakContainer}
                  accessibilityRole="text"
                  accessibilityLabel="More pages"
                  accessibilityHint="Indicates skipped pages">
              <AndpText style={styles.breakText}>{breakLabel}</AndpText>
            </View>
          );
        }
        const zeroBasedPage = p - 1; 
        const isActive = zeroBasedPage === currentPage;
        return (
          <TouchableOpacity
            //@ts-ignore
            dataSet={{ class: `agg-pagination-page-${p}-btn` }}
            key={`page-${p}`}
            onPress={() => onPageChange(zeroBasedPage)}
            style={[
              styles.pageItem,
              isActive && styles.activePageItem,
            ]}
            accessibilityRole="button"
            accessibilityLabel={`Page ${p}`}
            accessibilityHint={`Navigates to page ${p}`}
          >
            <AndpText style={isActive ? styles.activePageItemText : styles.pageItemText}>
              {p}
            </AndpText>
          </TouchableOpacity>
        );
      })}
  
      {/* Next-Button */}
      <TouchableOpacity
        //@ts-ignore
        dataSet={{ class: 'agg-pagination-next-page-btn' }}
        style={[
          styles.pageItem,
          currentPage === pageCount - 1 && styles.disabledButton,
        ]}
        disabled={currentPage === pageCount - 1}
        onPress={() => onPageChange(currentPage + 1)}
        accessibilityRole="button"
        accessibilityLabel={`Next page. ${nextLabel}`}
        accessibilityHint="Navigates to the next page"
      >
        <AndpText style={styles.pageItemText}>{nextLabel}</AndpText>
      </TouchableOpacity>
    </View>
  );
};

export default CustomAggregatePagination;