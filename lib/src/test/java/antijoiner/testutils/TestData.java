package antijoiner.testutils;

import antijoiner.Library.AntijoinResult;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class TestData<T> {

	public final List<T> left;
	public final List<T> right;
	public final int totalItems;
	public final int overlap;
	public final int unmatchedLeftCount;
	public final int unmatchedRightCount;

	public TestData(int totalItems, int overlap, IntFunction<T> generator) {
		assert (overlap <= totalItems);

		Random rng = new Random();

		this.totalItems = totalItems;
		this.overlap = overlap;

		int overlapOffset = rng.nextInt(totalItems - overlap);
		this.unmatchedLeftCount = overlapOffset;
		this.unmatchedRightCount = this.totalItems - (unmatchedLeftCount + overlap);

		this.left = IntStream.range(0, overlapOffset).mapToObj(generator).collect(toList());
		this.right =
			IntStream.range(overlapOffset + overlap, totalItems)
			.mapToObj(generator).collect(toList());

		IntStream.range(overlapOffset, overlapOffset + overlap).forEach(x -> {
			left.add(generator.apply(x));
			right.add(generator.apply(x));
		});
	}

	public <U> void verifyExactResultsSize(
		AntijoinResult<U, U> results
	) {
		assertEquals(unmatchedLeftCount, results.leftComplement.size());
		assertEquals(overlap, results.joinedPairs.size());
		assertEquals(unmatchedRightCount, results.rightComplement.size());
	}

}
