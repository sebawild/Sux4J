package it.unimi.dsi.sux4j.mph;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;

import it.unimi.dsi.bits.TransformationStrategies;

public class PaCoTrieDistributorMonotoneMinimalPerfectHashFunctionSlowTest {

	@Test
	public void testBig() throws IOException {
		final Iterable<Long> p = LargeLongCollection.getInstance();
		final PaCoTrieDistributorMonotoneMinimalPerfectHashFunction<Long> f = new PaCoTrieDistributorMonotoneMinimalPerfectHashFunction<>(p, TransformationStrategies.fixedLong());

		long j = 0;
		for(final Iterator<Long> i = p.iterator(); i.hasNext();) {
			final Long s = i.next();
			assertEquals(j++, f.getLong(s));
		}
	}
}
