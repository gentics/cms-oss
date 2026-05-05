import React, { useEffect, useMemo, useRef } from 'react';
import { ScrollView } from 'react-native';
import { IErrorMessage } from '../../../../types/interfaces';
import { useSharedValue, withTiming, Easing, withDelay } from 'react-native-reanimated';
import ProgressStep from './ProgressStep.native';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { pagingStyles as customPagingStyles } from '../../../../styles/custom/custom-style';
import { pagingStyles } from '../../../../styles/common/common-style';
import { paginationTheme } from '@andp/core/src/styles/theme';
type ProgressItem = {
  kind: 'form' | 'flow';
  index: number;
  label: string;
  pageNumber?: number;
};

const ProgressBar: React.FC<{
  items: ProgressItem[];
  currentCombinedIndex: number;
  errorMsgs: IErrorMessage[];
  handlePageChange: (pageNumber: number) => void;
}> = ({
  items,
  currentCombinedIndex,
  errorMsgs,
  handlePageChange,
}) => {
    const progress = useSharedValue(currentCombinedIndex);
    const hasMounted = useRef(false);
    const styles = useMemo(() => mergeStyles(pagingStyles, customPagingStyles), []);

    useEffect(() => {
      if (!hasMounted.current) {
        hasMounted.current = true;
        if (currentCombinedIndex > 0) {
          const initialDuration = Math.min(
            paginationTheme.animation!.duration.introMax, 
            paginationTheme.animation!.duration.introBase + currentCombinedIndex * paginationTheme.animation!.duration.introStep
          );
          progress.value = withDelay(
            paginationTheme.animation!.delay.default,
            withTiming(currentCombinedIndex, {
              duration: initialDuration,
              easing: Easing.bezier(0.22, 1, 0.36, 1), // smooth ease-out (standard iOS-style)
            })
          );
        } else {
          progress.value = 0;
        }
      } else {
        progress.value = withTiming(currentCombinedIndex, {
          duration: paginationTheme.animation!.duration.default,
          easing: Easing.inOut(Easing.cubic),
        });
      }
    }, [currentCombinedIndex]);

    const isPageWithError = (pageNumber: number) =>
      errorMsgs.some((msg) => msg.pageNumber === pageNumber);

    return (
      <ScrollView contentContainerStyle={styles.pagination}
        horizontal={true}
        showsHorizontalScrollIndicator={false}>
        {items.map((item, idx) => {
          const isCurrent = currentCombinedIndex === item.index;
          const isLast = idx === items.length - 1;
          const isClickable = idx <= currentCombinedIndex;
          const hasError = item.pageNumber !== undefined && isPageWithError(item.pageNumber);

          return (
            <ProgressStep
              key={idx}
              item={item}
              index={idx}
              isCurrent={isCurrent}
              isLast={isLast}
              isClickable={isClickable}
              hasError={hasError}
              handlePageChange={handlePageChange}
              progressSV={progress}
              isIntro={!hasMounted.current}
            />
          );
        })}
      </ScrollView>
    );
  };

export default ProgressBar;