package it.unimi.dsi.sux4j.test;

import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.util.XorShift1024StarRandomGenerator;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

public class GenerateRandom64BitIntegers {
	public static final Logger LOGGER = LoggerFactory.getLogger( GenerateRandom64BitIntegers.class );
	
	public static void main( final String[] arg ) throws JSAPException, IOException {

		final SimpleJSAP jsap = new SimpleJSAP( GenerateRandom64BitIntegers.class.getName(), "Generates a list of sorted 64-bit random integers in DataOutput format.",
				new Parameter[] {
					new UnflaggedOption( "n", JSAP.LONG_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The number of strings." ),
					new UnflaggedOption( "output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The output file." )
		});
		
		JSAPResult jsapResult = jsap.parse( arg );
		if ( jsap.messagePrinted() ) return;
		
		final long n = jsapResult.getLong( "n" );
		final String output = jsapResult.getString( "output" );
		
		RandomGenerator r = new XorShift1024StarRandomGenerator();
	
		ProgressLogger pl = new ProgressLogger( LOGGER );
		pl.expectedUpdates = n;
		pl.start( "Generating... " );
		
		BigInteger l = BigInteger.ZERO;
		final BigInteger limit = BigInteger.valueOf( 256 ).pow( 8 );
		final BigInteger offset = BigInteger.valueOf( Long.MIN_VALUE );
		long incr = (long)Math.floor( 1.99 * ( limit.divide( BigInteger.valueOf( n ) ).longValue() ) ) - 1;
		
		@SuppressWarnings("resource")
		final DataOutputStream dos = new DataOutputStream( new FileOutputStream( output ) );
		
		LOGGER.info( "Increment: " + incr );
		
		for( long i = 0; i < n; i++ ) {
			l = l.add( BigInteger.valueOf( ( r.nextLong() & 0x7FFFFFFFFFFFFFFFL ) % incr + 1 ) );
			if ( l.compareTo( limit ) > 0 ) throw new AssertionError( Long.toString( i ) );
			
			assert l.add( offset ).compareTo( BigInteger.valueOf( Long.MAX_VALUE ) ) <= 0 : l.add( offset );
			assert l.add( offset ).compareTo( BigInteger.valueOf( Long.MIN_VALUE ) ) >= 0 : l.add( offset );
			dos.writeLong( l.add( offset ).longValue() );

			pl.lightUpdate();
		}
		
		
		pl.done();
		dos.close();
		
		LOGGER.info( "Last/limit: " + ( l.doubleValue() / limit.doubleValue() ) );
	}
}
