import {
  widthFromColumNumber,
  useBreakpoints,
  mergeStyles,
} from '@andp/core/src/AndpUtils';
import React, { useMemo } from 'react';
import { View, Platform, useWindowDimensions } from 'react-native';
import { baseStyles } from '../../../styles/common/common-style';
import { baseStyles as customBaseStyles } from '../../../styles/custom/custom-style';
import { standardElement } from '@andp/core/src/styles/theme';
import AndpText from '@andp/core/src/components/AndpText';

interface RowProps {
  children: React.ReactNode;
  rowGap?: number;
  columnGap?: number;
  summary?: boolean;
  isAggregate?: boolean;
  elementsNum?: number;
  expertSearchTable?: React.ReactNode;
  formWidth: number | undefined;
}

const MemoGridItem = React.memo(
  ({ el, width, cGap }: { el: React.ReactNode; width: any; cGap: number }) => (
    <View style={[width, { paddingHorizontal: cGap }]}>{el}</View>
  ),
  (prev, next) =>
    prev.el === next.el && prev.width === next.width && prev.cGap === next.cGap,
);

export const Row = ({
  children,
  rowGap = standardElement.verticalElementGap,
  columnGap = 8,
  summary = false,
  isAggregate = false,
  formWidth,
}: RowProps) => {
  const breakpoints = useBreakpoints();
  const rGap = rowGap;
  const cGap = Platform.OS === 'web' ? columnGap / 2 : columnGap;
  const childArray = useMemo(
    () => React.Children.toArray(children),
    [children],
  );
  const { width } = useWindowDimensions();
  const isMobile = width < 768;
  const styles = useMemo(() => mergeStyles(baseStyles, customBaseStyles), []);

  if (isAggregate || summary) {
    const rows: React.ReactNode[][] = [];
    let currentRow: React.ReactNode[] = [];
    let currentSum = 0;

    childArray.forEach((child: any) => {
      const cols =
        isAggregate && isMobile
          ? 12
          : Number(child.props.numberOfColumns) || 12;

      if (currentSum + cols > 12) {
        rows.push(currentRow);
        currentRow = [child];
        currentSum = cols;
      } else {
        currentRow.push(child);
        currentSum += cols;
      }
    });
    if (currentRow.length) rows.push(currentRow);

    return (
      <>
        {rows.map((rowEls, rowIdx) => {
          const hasProps = rowEls.some(
            (el: any) => el.props && Object.keys(el.props).length > 0,
          );

          if (hasProps) {
            return (
              <View
                key={'agg-row-' + rowIdx}
                style={[
                  styles.row,
                  { marginHorizontal: -cGap, marginBottom: rGap },
                ]}
                shouldRasterizeIOS
                renderToHardwareTextureAndroid
              >
                {rowEls.map((el: any, i: number) => {
                  // Prüft ob ein Summary Element leer ist oder nicht -> Falls leer dann wird gar kein Row gerendert
                  if (
                    !summary ||
                    (el.props && Object.keys(el.props).length > 0)
                  ) {
                    return (
                      <View
                        key={'agg-row-' + rowIdx + '-' + i}
                        style={[
                          widthFromColumNumber(
                            el.props.numberOfColumns,
                            breakpoints,
                          ),
                          { paddingHorizontal: cGap },
                        ]}
                      >
                        {el}
                      </View>
                    );
                  }
                })}
              </View>
            );
          }
        })}
      </>
    );
  }

  const rows = useMemo(() => {
    const out: React.ReactNode[][] = [];
    let cur: React.ReactNode[] = [];
    let sum = 0;

    childArray.forEach((child: any) => {
      const cols = Number(child.props.numberOfColumns) || 12;
      if (breakpoints.maximumMd) {
        out.push([child]);
        return;
      }
      if (sum + cols > 12) {
        out.push(cur);
        cur = [child];
        sum = cols;
      } else {
        cur.push(child);
        sum += cols;
      }
    });
    out.push(cur);
    return out;
  }, [childArray, breakpoints.maximumMd]);

  return (
    <View
      style={[
        !summary && !isAggregate ? { marginBottom: 10 } : { marginBottom: 0 },
      ]}
      key={'Row-RenderView-' + rows.length}
    >
      {rows.map((item, index) => (
        <React.Fragment key={'Row-RenderView-' + index}>
          <View
            style={[
              styles.formContainer,
              widthFromColumNumber(formWidth, breakpoints),
            ]}
            shouldRasterizeIOS
            renderToHardwareTextureAndroid
          >
            <View style={[styles.row, { marginHorizontal: -cGap }]}>
              {item.map((el: any, i: number) => (
                <MemoGridItem
                  key={'renderItem-View-View-memoGrid-' + (el.key ?? i)}
                  el={el}
                  width={widthFromColumNumber(
                    el.props.numberOfColumns,
                    breakpoints,
                  )}
                  cGap={cGap}
                />
              ))}
            </View>
          </View>
          {index < rows.length - 1 && <View style={{ height: rGap }} />}
        </React.Fragment>
      ))}
    </View>
  );
};
