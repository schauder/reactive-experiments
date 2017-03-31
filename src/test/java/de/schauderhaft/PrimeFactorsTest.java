package de.schauderhaft;

import static de.schauderhaft.PrimeFactors.*;
import static java.util.Arrays.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Jens Schauder
 */
public class PrimeFactorsTest {

	@Test
	public void smalestPrime() {

		assertEquals(1, firstPrime(1));
		assertEquals(2, firstPrime(2));
		assertEquals(3, firstPrime(3));
		assertEquals(2, firstPrime(4));
		assertEquals(5, firstPrime(5));
		assertEquals(3, firstPrime(15));
		assertEquals(23, firstPrime(23));
	}

	@Test
	public void primes() {

		assertEquals(asList(), factors(1).collectList().block());
		assertEquals(asList(2), factors(2).collectList().block());
		assertEquals(asList(3), factors(3).collectList().block());
		assertEquals(asList(2, 2), factors(4).collectList().block());
		assertEquals(asList(5), factors(5).collectList().block());
		assertEquals(asList(3, 5), factors(15).collectList().block());
		assertEquals(asList(23), factors(23).collectList().block());
		assertEquals(asList(2, 2, 2, 3), factors(24).collectList().block());
	}
}