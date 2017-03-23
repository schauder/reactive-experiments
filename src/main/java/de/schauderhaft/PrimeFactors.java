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

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import reactor.core.publisher.Flux;

/**
 * @author Jens Schauder
 */
public class PrimeFactors {

	public static int firstPrime(int input) {
		int sqrt = (int) Math.sqrt(input);

		int factor = 2;
		while (factor <= sqrt) {
			if (input % factor == 0) {
				return factor;
			}
			factor++;
		}
		return input;
	}


	public static List<Integer> factorsList(int input) {

		List<Integer> primes = new ArrayList<>();

		while (true) {

			int prime = firstPrime(input);
			String.format("%s, %s", input, prime);
			primes.add(prime);

			if (prime == input) return primes;

			input = input / prime;
		}
	}

	static Pair<Integer, Integer> nextFactor(int input) {
		int prime = firstPrime(input);
		return new Pair<>(prime, input / prime);
	}

	public static Flux<Integer> factors(int input) {
		return Flux.generate(() -> input, (state, sink) -> {
			Pair<Integer, Integer> p = nextFactor(state);
			if (p.left <= 1) sink.complete();
			else
				sink.next(p.left);
			return p.right;
		});
	}

	@Data
	static class Pair<L, R> {

		private final L left;
		private final R right;
	}
}
