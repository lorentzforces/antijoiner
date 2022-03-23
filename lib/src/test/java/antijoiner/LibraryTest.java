// Copyright 2022 Lorentz Aberg subject to license details in {project root}/LICENSE.txt
package antijoiner;

import antijoiner.Library.AntijoinResult;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class LibraryTest {

	@Test
	public void sameBasicType() {
		List<Integer> left = new ArrayList<>();
		left.add(1);
		left.add(2);
		left.add(3);
		List<Integer> right = new ArrayList<>();
		right.add(2);
		right.add(3);
		right.add(4);

		AntijoinResult<Integer, Integer> result = Library.antijoin(left, right);

		// TODO: verify contents of these result collections
		assertEquals(result.leftComplement.size(), 1);
		assertEquals(result.joinedPairs.size(), 2);
		assertEquals(result.rightComplement.size(), 1);
	}

	// TODO: test disparate complex types

}
