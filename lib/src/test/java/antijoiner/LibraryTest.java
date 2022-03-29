package antijoiner;

import antijoiner.Library.AntijoinResult;
import antijoiner.Library.NullValuePolicy;
import antijoiner.testutils.NonBasicType;
import antijoiner.testutils.TestData;
import java.util.function.Function;
import org.junit.Test;

import static org.junit.Assert.*;

public class LibraryTest {

	/**
	 * Calculating the [nametbd] with plain {@Link Integer} objects produces the expected results.
	 */
	@Test
	public void sameBasicType() {
		TestData<Integer> testData = buildBasicSets(10, 4);

		AntijoinResult<Integer, Integer> results =
			Library.antijoin(testData.left, testData.right);

		testData.verifyExactResultsSize(results);

		for (int i = 0; i < testData.unmatchedLeftCount; i++) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(results.leftComplement.stream().anyMatch(x -> x.equals(objectValue)));
		}

		for (
			int i = testData.unmatchedLeftCount;
			i < testData.unmatchedLeftCount + testData.overlap;
			i++
		) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(
				results.joinedPairs.stream()
					.anyMatch(x -> x.left.equals(objectValue) && x.right.equals(objectValue))
			);
		}

		for (int i = testData.unmatchedLeftCount + testData.overlap; i < testData.totalItems; i++) {
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
			Library.antijoin(
				testData.left,
				testData.right,
				accessor,
				accessor,
				NullValuePolicy.RETAIN_NONE
			);

		testData.verifyExactResultsSize(results);

		for (int i = 0; i < testData.unmatchedLeftCount; i++) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(
				results.leftComplement.stream().map(accessor).anyMatch(x -> x.equals(objectValue))
			);
		}

		for (
			int i = testData.unmatchedLeftCount;
			i < testData.unmatchedLeftCount + testData.overlap;
			i++
		) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(
				results.joinedPairs.stream().anyMatch(x ->
					objectValue.equals(x.left.getValue())
					&& objectValue.equals(x.right.getValue())
				));
		}

		for (int i = testData.unmatchedLeftCount + testData.overlap; i < testData.totalItems; i++) {
			Integer objectValue = Integer.valueOf(i);
			assertTrue(
				results.rightComplement.stream().anyMatch(x -> objectValue.equals(x.getValue()))
			);
		}
	}

	/**
	 * Calculating the [nametbd] retains {@code null} values as specified
	 */
	@Test
	public void nullValueRetainment() {
		TestData<NonBasicType> testData = buildNonBasicSets(10, 4);
		testData.left.add(new NonBasicType(null));
		testData.left.add(new NonBasicType(null));
		testData.right.add(new NonBasicType(null));
		testData.right.add(new NonBasicType(null));
		testData.right.add(new NonBasicType(null));

		AntijoinResult<NonBasicType, NonBasicType> resultsRetainLeft =
			Library.antijoin(
				testData.left,
				testData.right,
				NonBasicType::getValue,
				NonBasicType::getValue,
				NullValuePolicy.RETAIN_LEFT
			);
		assertEquals(testData.unmatchedLeftCount + 2, resultsRetainLeft.leftComplement.size());
		assertEquals(testData.unmatchedRightCount, resultsRetainLeft.rightComplement.size());

		AntijoinResult<NonBasicType, NonBasicType> resultsRetainRight =
			Library.antijoin(
				testData.left,
				testData.right,
				NonBasicType::getValue,
				NonBasicType::getValue,
				NullValuePolicy.RETAIN_RIGHT
			);
		assertEquals(testData.unmatchedLeftCount, resultsRetainRight.leftComplement.size());
		assertEquals(testData.unmatchedRightCount + 3, resultsRetainRight.rightComplement.size());

		AntijoinResult<NonBasicType, NonBasicType> resultsRetainFull =
			Library.antijoin(
				testData.left,
				testData.right,
				NonBasicType::getValue,
				NonBasicType::getValue,
				NullValuePolicy.RETAIN_FULL
			);
		assertEquals(testData.unmatchedLeftCount + 2, resultsRetainFull.leftComplement.size());
		assertEquals(testData.unmatchedRightCount + 3, resultsRetainFull.rightComplement.size());
	}

	/**
	 * Passing a {@code null} object (not an object with a {@code null} accessed value) in one of
	 * the collections throws a {@link NullPointerException}.
	 */
	@Test
	public void nullObjectsThrowExceptions() {
		TestData<NonBasicType> hasNullOnLeft = buildNonBasicSets(3, 1);
		hasNullOnLeft.left.add(null);

		NullPointerException expectedExceptionFromLeft = null;
		try {
			Library.antijoin(hasNullOnLeft.left, hasNullOnLeft.right, NonBasicType::getValue);
		} catch (NullPointerException e) {
			expectedExceptionFromLeft = e;
		}
		assertNotNull(expectedExceptionFromLeft);

		TestData<NonBasicType> hasNullOnRight = buildNonBasicSets(3, 1);
		hasNullOnRight.right.add(null);

		NullPointerException expectedExceptionFromRight = null;
		try {
			Library.antijoin(hasNullOnRight.left, hasNullOnRight.right, NonBasicType::getValue);
		} catch (NullPointerException e) {
			expectedExceptionFromRight = e;
		}
		assertNotNull(expectedExceptionFromRight);
	}

	private static TestData<Integer> buildBasicSets(int numberOfItems, int overlap) {
		return new TestData<>(numberOfItems, overlap, x -> Integer.valueOf(x));
	}

	private static TestData<NonBasicType> buildNonBasicSets(int numberOfItems, int overlap) {
		return new TestData<>(numberOfItems, overlap, x -> new NonBasicType(x));
	}

}
