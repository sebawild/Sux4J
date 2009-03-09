package it.unimi.dsi.sux4j.mph;

/*		 
 * Sux4J: Succinct data structures for Java
 *
 * Copyright (C) 2008 Sebastiano Vigna 
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 2.1 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

import it.unimi.dsi.Util;
import it.unimi.dsi.bits.BitVector;
import it.unimi.dsi.bits.BitVectors;
import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.bits.HuTuckerTransformationStrategy;
import it.unimi.dsi.bits.LongArrayBitVector;
import it.unimi.dsi.bits.TransformationStrategies;
import it.unimi.dsi.bits.TransformationStrategy;
import it.unimi.dsi.fastutil.ints.AbstractIntIterator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.FileLinesCollection;
import it.unimi.dsi.io.LineIterator;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.sux4j.bits.JacobsonBalancedParentheses;
import it.unimi.dsi.sux4j.util.EliasFanoLongBigList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import com.martiansoftware.jsap.stringparsers.ForNameStringParser;

/** A hollow trie, that is, a compacted trie recording just the length of the paths associated to the internal nodes.
 *
 * <p>Instances of this class can be used to compute a monotone minimal perfect hashing of the keys.
 */

public class HollowTrie<T> extends AbstractHashFunction<T> implements Serializable {
	private static final Logger LOGGER = Util.getLogger( HollowTrie.class );
	private static final long serialVersionUID = 1L;

	private static final boolean ASSERTS = false;
	private static final boolean DEBUG = false;
	
	private EliasFanoLongBigList skips;
	/** The bit vector containing Jacobson's representation of the trie. */
	private final BitVector trie;
	/** A balanced parentheses structure over {@link #trie}. */
	private JacobsonBalancedParentheses balParen;
	/** The transformation strategy. */
	private final TransformationStrategy<? super T> transform;
	/** The number of elements in this hollow trie. */
	private int size;
	
	private final static class Node {
		Node right;
		int skip;
		IntArrayList skips = new IntArrayList();
		LongArrayBitVector repr = LongArrayBitVector.getInstance();
		
		public Node( final Node right, final int skip ) {
			this.right = right;
			this.skip = skip;
			if ( ASSERTS ) assert skip >= 0;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public long getLong( final Object object ) {
		//System.err.println( "Hashing " + object + "..." );
		if ( size <= 1 ) return size - 1;
		final BitVector bitVector = transform.toBitVector( (T)object ).fast();
		long p = 1, length = bitVector.length(), index = 0;
		int s = 0, r = 0;
		
		for(;;) {
			if ( ( s += (int)skips.getLong( r ) ) >= length ) return defRetValue;
			//System.err.println( "Skipping " + rank9.rank( p ) + " bits..." );
			
			//System.err.print( "Turning " + ( bitVector.getBoolean( s ) ? "right" : "left" ) + " at bit " + s + "... \n" );
			if ( bitVector.getBoolean( s ) ) {
				final long q = balParen.findClose( p ) + 1;
				r += ( q - p ) / 2 ;
				index += ( q - p ) / 2;
				//System.err.println( "Increasing index by " + ( q - p + 1 ) / 2 + " to " + index + "..." );
				if ( ! trie.getBoolean( q ) ) return index;
				p = q;
			}
			else {
				if ( ! trie.getBoolean( ++p ) ) return index;
				r++;
			}
			
			s++;
		}
	}
	
	public HollowTrie( final Iterable<? extends T> iterable, final TransformationStrategy<? super T> transform ) {
		this( iterable.iterator(), transform );
	}
		
	public HollowTrie( final Iterator<? extends T> iterator, final TransformationStrategy<? super T> transform ) {

		this.transform = transform;
		defRetValue = -1; // For the very few cases in which we can decide

		int size = 0;
		long maxLength = 0;
		
		Node root = null, node, parent;
		int prefix, numNodes = 0, cmp;

		ProgressLogger pl = new ProgressLogger( LOGGER );
		pl.displayFreeMemory = true;
		
		pl.start( "Generating hollow trie..." );

		if ( iterator.hasNext() ) {
			LongArrayBitVector prev = LongArrayBitVector.copy( transform.toBitVector( iterator.next() ) );
			BitVector curr;
			if ( DEBUG ) System.err.println( prev );
			pl.lightUpdate();
			maxLength = prev.length();
			size++;

			while( iterator.hasNext() ) {
				size++;
				curr = transform.toBitVector( iterator.next() );
				if ( DEBUG ) System.err.println( curr );
				pl.lightUpdate();
				if ( maxLength < curr.length() ) maxLength = curr.length();
				cmp = prev.compareTo( curr );
				if ( cmp == 0 ) throw new IllegalArgumentException( "The input bit vectors are not distinct" );
				if ( cmp > 0 ) throw new IllegalArgumentException( "The input bit vectors are not lexicographically sorted" );
				prefix = (int)curr.longestCommonPrefixLength( prev );
				if ( prefix == prev.length() ) throw new IllegalArgumentException( "The input bit vectors are not prefix-free" );

				node = root;
				parent = null;
				Node newNode = null;
				while( node != null ) {
					if ( prefix < node.skip ) {
						newNode = new Node( null, prefix );
						numNodes++;
						if ( parent == null ) {
							root.skip -= prefix + 1;
							if ( ASSERTS ) assert root.skip >= 0;


							root = newNode;
						}
						else {
							parent.right = newNode;
							node.skip -= prefix + 1;
							
							if ( ASSERTS ) assert node.skip >= 0;
						}

						Node n = node;
						long reprSize = 0;
						int skipSize = 0;
						do {
							reprSize += n.repr.length() + 2;
							skipSize += n.skips.size() + 1;
						} while( ( n = n.right ) != null );

						n = node;
						newNode.repr.ensureCapacity( reprSize );
						newNode.skips.ensureCapacity( skipSize );
						
						do {
							newNode.repr.add( true );
							newNode.repr.append( n.repr );
							newNode.repr.add( false );
							newNode.skips.add( n.skip );
							newNode.skips.addAll( n.skips );
						} while( ( n = n.right ) != null );
						

						break;
					}

					prefix -= node.skip + 1;
					parent = node;
					node = node.right;
				}

				if ( node == null ) {
					if ( parent == null ) root = new Node( null, prefix );
					else parent.right = new Node( null, prefix );
					numNodes++;
				}

				prev.replace( curr );
			}
		}
		
		this.size = size;

		pl.done();
		
		if ( size <= 1 ) {
			balParen = new JacobsonBalancedParentheses( BitVectors.EMPTY_VECTOR );
			trie = BitVectors.EMPTY_VECTOR;
			return;
		}

		final LongArrayBitVector bitVector = LongArrayBitVector.getInstance( 2 * numNodes + 2 );
		
		bitVector.add( true ); // Fake open parenthesis

		Node m = root;
		do {
			bitVector.add( true );
			bitVector.append( m.repr );
			bitVector.add( false );
		} while( ( m = m.right ) != null );
		
		bitVector.add( false );
		
		if ( ASSERTS ) assert bitVector.length() == 2 * numNodes + 2;
		
		LOGGER.debug( "Generating succinct representations..." );
		
		trie = bitVector;
		balParen = new JacobsonBalancedParentheses( bitVector, false, true, false );

		final Node finalRoot = root;
		
		final IntIterable skipIterable = new IntIterable() {
			public IntIterator iterator() {
				return new AbstractIntIterator() {
					Node curr = finalRoot;
					int currElem = -1;
					
					public int nextInt() {
						if ( currElem == -1 ) {
							currElem++;
							return curr.skip;
						}
						else {
							if ( currElem < curr.skips.size() ) return curr.skips.getInt( currElem++ );
							curr = curr.right;
							currElem = 0;
							return curr.skip;
						}
					}
					
					public boolean hasNext() {
						return curr != null && ( currElem < curr.skips.size() || currElem == curr.skips.size() && curr.right != null );
					}
				};
			}
		};
		
		long maxSkip = 0, sumOfSkips = 0;
		int s;
		for( IntIterator i = skipIterable.iterator(); i.hasNext(); ) {
			s = i.nextInt();
			maxSkip = Math.max( s, maxSkip );
			sumOfSkips += s;
		}
		
		final int skipWidth = Fast.ceilLog2( maxSkip );

		LOGGER.debug( "Max skip: " + maxSkip );
		LOGGER.debug( "Max skip width: " + skipWidth );

		this.skips = new EliasFanoLongBigList( skipIterable );
		
		if ( DEBUG ) {
			System.err.println( skips );
			System.err.println( this.skips );
		}
		
		final long numBits = numBits();
		LOGGER.debug( "Bits: " + numBits );
		LOGGER.debug( "Bits per open parenthesis: " + (double)balParen.numBits() / size );
		final double avgSkip = (double)sumOfSkips / skips.size();
		LOGGER.info( "Forecast bit cost per element: " + ( 4 + Fast.log2( avgSkip ) + Fast.log2( 1 + Fast.log2( avgSkip ) ) ) );
		LOGGER.info( "Actual bit cost per element: " + (double)numBits / size );
	}
	
	
	public int size() {
		return size;
	}

	public long numBits() {
		return balParen.numBits() + trie.length() + this.skips.numBits() + transform.numBits();
	}
		
	public static void main( final String[] arg ) throws NoSuchMethodException, IOException, JSAPException {

		final SimpleJSAP jsap = new SimpleJSAP( HollowTrie.class.getName(), "Builds a hollow trie reading a newline-separated list of strings.",
				new Parameter[] {
			new FlaggedOption( "encoding", ForNameStringParser.getParser( Charset.class ), "UTF-8", JSAP.NOT_REQUIRED, 'e', "encoding", "The string file encoding." ),
			new Switch( "huTucker", 'h', "hu-tucker", "Use Hu-Tucker coding to reduce string length." ),
			new Switch( "iso", 'i', "iso", "Use ISO-8859-1 coding (i.e., just use the lower eight bits of each character)." ),
			new Switch( "zipped", 'z', "zipped", "The string list is compressed in gzip format." ),
			new UnflaggedOption( "trie", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The filename for the serialised hollow trie." ),
			new UnflaggedOption( "stringFile", JSAP.STRING_PARSER, "-", JSAP.NOT_REQUIRED, JSAP.NOT_GREEDY, "The name of a file containing a newline-separated list of strings, or - for standard input; in the first case, strings will not be loaded into core memory." ),
		});

		JSAPResult jsapResult = jsap.parse( arg );
		if ( jsap.messagePrinted() ) return;

		final String trieName = jsapResult.getString( "trie" );
		final String stringFile = jsapResult.getString( "stringFile" );
		final Charset encoding = (Charset)jsapResult.getObject( "encoding" );
		final boolean zipped = jsapResult.getBoolean( "zipped" );
		final boolean iso = jsapResult.getBoolean( "iso" );
		final boolean huTucker = jsapResult.getBoolean( "huTucker" );

		final Collection<MutableString> collection;
		if ( "-".equals( stringFile ) ) {
			final ProgressLogger pl = new ProgressLogger( LOGGER );
			pl.start( "Loading strings..." );
			collection = new LineIterator( new FastBufferedReader( new InputStreamReader( zipped ? new GZIPInputStream( System.in ) : System.in, encoding ) ), pl ).allLines();
			pl.done();
		}
		else collection = new FileLinesCollection( stringFile, encoding.toString(), zipped );
		final TransformationStrategy<CharSequence> transformationStrategy = huTucker 
			? new HuTuckerTransformationStrategy( collection, true )
			: iso
				? TransformationStrategies.prefixFreeIso() 
				: TransformationStrategies.prefixFreeUtf16();

		BinIO.storeObject( new HollowTrie<CharSequence>( collection, transformationStrategy ), trieName );
		LOGGER.info( "Completed." );
	}
}
