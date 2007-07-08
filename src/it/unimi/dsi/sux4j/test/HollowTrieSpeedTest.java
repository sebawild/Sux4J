package it.unimi.dsi.sux4j.test;

import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.mg4j.io.FastBufferedReader;
import it.unimi.dsi.mg4j.io.LineIterator;
import it.unimi.dsi.sux4j.bits.BitVector;
import it.unimi.dsi.sux4j.mph.HollowTrie;
import it.unimi.dsi.sux4j.mph.HollowTrie.CharSequenceBitVectorIterator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import com.martiansoftware.jsap.stringparsers.ForNameStringParser;

public class HollowTrieSpeedTest extends TestCase {

	public static void main( final String[] arg ) throws NoSuchMethodException, IOException, JSAPException, ClassNotFoundException {

		final SimpleJSAP jsap = new SimpleJSAP( HollowTrie.class.getName(), "Builds a hollow trie reading a newline-separated list of terms.",
				new Parameter[] {
					new FlaggedOption( "bufferSize", JSAP.INTSIZE_PARSER, "64Ki", JSAP.NOT_REQUIRED, 'b',  "buffer-size", "The size of the I/O buffer used to read terms." ),
					//new FlaggedOption( "class", MG4JClassParser.getParser(), klass.getName(), JSAP.NOT_REQUIRED, 'c', "class", "A subclass of MinimalPerfectHash to be used when creating the table." ),
					new FlaggedOption( "encoding", ForNameStringParser.getParser( Charset.class ), "UTF-8", JSAP.NOT_REQUIRED, 'e', "encoding", "The term file encoding." ),
					new Switch( "zipped", 'z', "zipped", "The term list is compressed in gzip format." ),
					new FlaggedOption( "termFile", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', "offline", "Read terms from this file (without loading them into core memory) instead of standard input." ),
					new UnflaggedOption( "trie", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The filename for the serialised hollow trie." )
		});
		
		JSAPResult jsapResult = jsap.parse( arg );
		if ( jsap.messagePrinted() ) return;
		
		final int bufferSize = jsapResult.getInt( "bufferSize" );
		final String trieName = jsapResult.getString( "trie" );
		final String termFile = jsapResult.getString( "termFile" );
		//final Class<?> tableClass = jsapResult.getClass( "class" );
		final Charset encoding = (Charset)jsapResult.getObject( "encoding" );
		final boolean zipped = jsapResult.getBoolean( "zipped" );
		
		final HollowTrie hollowTrie = (HollowTrie)BinIO.loadObject( trieName );
		
		CharSequenceBitVectorIterator i;
		if ( termFile == null ) i = new CharSequenceBitVectorIterator( new LineIterator( new FastBufferedReader( new InputStreamReader( System.in, encoding ), bufferSize ) ) );
		else i = new CharSequenceBitVectorIterator( new LineIterator( new FastBufferedReader( new InputStreamReader( zipped ? new GZIPInputStream( new FileInputStream( termFile ) ) : new FileInputStream( termFile ), encoding ), bufferSize ) ) );
		ObjectArrayList<BitVector> bitVectors = new ObjectArrayList<BitVector>();
		while( i.hasNext() ) bitVectors.add( i.next().copy() );
		
		long time = -System.currentTimeMillis();
		for( int j = Math.min( 200000, bitVectors.size() ); j-- != 0; ) {
			hollowTrie.getLeafIndex( bitVectors.get( j ) );
			if ( j % 1000 == 0 ) System.err.print('.');
		}
		System.err.println();
		time += System.currentTimeMillis();
		System.err.println( time + "ms, " + ( Math.min( 200000, bitVectors.size() ) * 1000.0 ) / time + " vectors/s" );
	}
}