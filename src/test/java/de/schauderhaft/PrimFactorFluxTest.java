/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schauderhaft;

import org.junit.Test;

import reactor.core.publisher.Flux;

/**
 * @author Jens Schauder
 */
public class PrimFactorFluxTest {

	@Test
	public void test() {
		PrimeFactors.factors(60).subscribe(System.out::println,System.err::println,()->System.out.println("Done"));
	}
}
