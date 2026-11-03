package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link Slice}
 */
public class SliceTest {
	@Test
	public void testSliceFirstPage() {
		Slice slice = Slice.of(0, 2, 5);

		assertThat(slice.fromIndex()).isEqualTo(0);
		assertThat(slice.toIndex()).isEqualTo(2);
		assertThat(slice.truncated()).isTrue();
		assertThat(slice.nextFrom()).isEqualTo(2);
	}

	@Test
	public void testSliceMiddlePageNotAlignedToSize() {
		Slice slice = Slice.of(3, 2, 10);

		assertThat(slice.fromIndex()).isEqualTo(3);
		assertThat(slice.toIndex()).isEqualTo(5);
		assertThat(slice.nextFrom()).isEqualTo(5);
	}

	@Test
	public void testSliceLastPartialPage() {
		Slice slice = Slice.of(4, 2, 5);

		assertThat(slice.fromIndex()).isEqualTo(4);
		assertThat(slice.toIndex()).isEqualTo(5);
		assertThat(slice.truncated()).isFalse();
		assertThat(slice.nextFrom()).isNull();
	}

	@Test
	public void testSliceExactlyAtEnd() {
		Slice slice = Slice.of(3, 2, 5);

		assertThat(slice.toIndex()).isEqualTo(5);
		assertThat(slice.truncated()).isFalse();
		assertThat(slice.nextFrom()).isNull();
	}

	@Test
	public void testSliceFromBeyondTotal() {
		Slice slice = Slice.of(10, 2, 5);

		assertThat(slice.fromIndex()).isEqualTo(5);
		assertThat(slice.toIndex()).isEqualTo(5);
		assertThat(slice.truncated()).isFalse();
		assertThat(slice.nextFrom()).isNull();
	}

	@Test
	public void testSliceEmptyList() {
		Slice slice = Slice.of(0, 25, 0);

		assertThat(slice.fromIndex()).isEqualTo(0);
		assertThat(slice.toIndex()).isEqualTo(0);
		assertThat(slice.truncated()).isFalse();
		assertThat(slice.nextFrom()).isNull();
	}

	@Test
	public void testSliceNoOverflow() {
		Slice slice = Slice.of(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, Integer.MAX_VALUE);

		assertThat(slice.toIndex()).isEqualTo(Integer.MAX_VALUE);
		assertThat(slice.truncated()).isFalse();
	}

	@Test
	public void testSlicesCoverEveryItemExactlyOnce() {
		int total = 7;
		int size = 3;
		int covered = 0;
		Integer from = 0;
		while (from != null) {
			Slice slice = Slice.of(from, size, total);
			assertThat(slice.fromIndex()).isEqualTo(covered);
			covered = slice.toIndex();
			from = slice.nextFrom();
		}
		assertThat(covered).isEqualTo(total);
	}

	@Test
	public void testApply() {
		assertThat(Slice.of(1, 2, 5).apply(List.of("a", "b", "c", "d", "e"))).containsExactly("b", "c");
		assertThat(Slice.of(4, 2, 5).apply(List.of("a", "b", "c", "d", "e"))).containsExactly("e");
		assertThat(Slice.of(10, 2, 5).apply(List.of("a", "b", "c", "d", "e"))).isEmpty();
		assertThat(Slice.of(0, 25, 0).apply(List.of())).isEmpty();
	}

	@Test
	public void testApplyRejectsListOfOtherSize() {
		assertThatThrownBy(() -> Slice.of(0, 2, 5).apply(List.of("a", "b")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testInvalidArgumentsAreRejected() {
		assertThatThrownBy(() -> Slice.of(-1, 2, 5)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Slice.of(0, 0, 5)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Slice.of(0, 2, -1)).isInstanceOf(IllegalArgumentException.class);
	}
}
