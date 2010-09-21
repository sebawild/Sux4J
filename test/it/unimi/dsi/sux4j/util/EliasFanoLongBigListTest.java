package it.unimi.dsi.sux4j.util;

import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;
import junit.framework.TestCase;

public class EliasFanoLongBigListTest extends TestCase {

	public void testSmall() {
		for( boolean offline : new boolean[] { false, true } ) {
			LongBigArrayBigList l;
			l = new LongBigArrayBigList( new long[][] { { 0, 0, 0 } } );
			assertEquals( l, new EliasFanoLongBigList( l.iterator(), 0, offline ) );

			l = new LongBigArrayBigList( new long[][] { { 0, 1, 0 } } );
			assertEquals( l, new EliasFanoLongBigList( l.iterator(), 0, offline ) );

			l = new LongBigArrayBigList( new long[][] { { 1, 1, 1 } } );
			assertEquals( l, new EliasFanoLongBigList( l.iterator(), 0, offline ) );

			l = new LongBigArrayBigList( new long[][] { { 4, 3, 2 } } );
			assertEquals( l, new EliasFanoLongBigList( l.iterator(), 0, offline ) );

			l = new LongBigArrayBigList( new long[][] { { 128, 2000, 50000000, 200, 10 } } );
			assertEquals( l, new EliasFanoLongBigList( l.iterator(), 0, offline ) );
		}
	}
}
