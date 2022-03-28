// Copyright 2022 Lorentz Aberg subject to license details in {project root}/LICENSE.txt
package antijoiner;

import antijoiner.Library.AntijoinResult;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class LibraryTest {

	/**
	 * Calculating the [nametbd] with plain {@Link java.lang.Integer} objects produces the
	 * expected results.
	 */
	@Test
	public void sameBasicType() {
		TestData<Integer> testData = buildBasicSets(10, 4);

		AntijoinResult<Integer, Integer> results =
			Library.antijoin(testData.left, testData.right);

		verifyExactResultsSize(results, testData);

		for (int i = 0; i < testData.overlapOffset; i++) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(results.leftComplement.stream().anyMatch(x -> x.equals(objectValue)));
		}

		for (int i = testData.overlapOffset; i < testData.overlapOffset + testData.overlap; i++) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(
				results.joinedPairs.stream()
					.anyMatch(x -> x.left.equals(objectValue) && x.right.equals(objectValue))
			);
		}

		for (int i = testData.overlapOffset + testData.overlap; i < testData.totalItems; i++) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(results.rightComplement.stream().anyMatch(x -> x.equals(objectValue)));
		}
	}

	/**
	 * Calculating the [nametbd] with nontrivial objects produces the expected results.
	 */
	@Test
	public void sameNonBasicType() {
		TestData<NonBasicType> testData = buildNonBasicSets(10, 4);
		Function<NonBasicType, Integer> accessor = NonBasicType::getValue;

		AntijoinResult<NonBasicType, NonBasicType> results =
			Library.antijoin(testData.left, testData.right, accessor, accessor);

		verifyExactResultsSize(results, testData);

		for (int i = 0; i < testData.overlapOffset; i++) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(
				results.leftComplement.stream().map(accessor).anyMatch(x -> x.equals(objectValue))
			);
		}

		for (int i = testData.overlapOffset; i < testData.overlapOffset + testData.overlap; i++) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(
				results.joinedPairs.stream().anyMatch(x ->
					objectValue.equals(x.left.getValue())
					&& objectValue.equals(x.right.getValue())
				));
		}

		for (int i = testData.overlapOffset + testData.overlap; i < testData.totalItems; i++) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(
				results.rightComplement.stream().anyMatch(x -> objectValue.equals(x.getValue()))
			);
		}
	}

	private static <T> void verifyExactResultsSize(
		AntijoinResult<T, T> results,
		TestData<T> testData
	) {
		assertEquals(testData.overlapOffset, results.leftComplement.size());
		assertEquals(testData.overlap, results.joinedPairs.size());
		assertEquals(
			testData.totalItems - testData.overlapOffset - testData.overlap,
			results.rightComplement.size()
		);
	}

	private static TestData<Integer> buildBasicSets(int numberOfItems, int overlap) {
		return new TestData<>(numberOfItems, overlap, x -> Integer.valueOf(x));
	}

	private static TestData<NonBasicType> buildNonBasicSets(int numberOfItems, int overlap) {
		return new TestData<>(numberOfItems, overlap, x -> new NonBasicType(x));
	}

	private static class TestData<T> {

		List<T> left;
		List<T> right;
		int totalItems;
		int overlap;
		int overlapOffset;

		public TestData(int totalItems, int overlap, IntFunction<T> generator) {
			assert(overlap <= totalItems);

			Random rng = new Random();

			this.totalItems = totalItems;
			this.overlap = overlap;
			this.overlapOffset = rng.nextInt(totalItems - overlap);

			this.left = IntStream.range(0, overlapOffset).mapToObj(generator).collect(toList());
			this.right = IntStream.range(overlapOffset + overlap, totalItems).mapToObj(generator).collect(toList());

			IntStream.range(overlapOffset, overlapOffset + overlap).forEach(x -> {
				left.add(generator.apply(x));
				right.add(generator.apply(x));
			});
		}

	}

	private static class NonBasicType {

		private final int value;

		public NonBasicType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

	}

}
