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

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

/**
 * @author Jens Schauder
 */
public class GroupByTest {

	@Test
	public void groupByDemo() {
		Flux<Long> longs = Flux
				.<Long>generate(s -> s.next(System.currentTimeMillis()))
				.delayElements(Duration.ofMillis(5))
				.take(Duration.ofMillis(500));
//		longs.doOnNext(System.out::println).blockLast();
		longs
				.groupBy(l -> l / 100)
				.flatMap(gf -> gf.count())
				.doOnNext(System.out::println)
				.blockLast();
	}


	@Test
	public void windowUntil() {

		Flux.range(0,10)
				.windowUntil(x -> x%4 ==0)
				.map(gf -> gf.materialize().doOnNext(System.out::println))
				.doOnNext(System.out::println)
				.blockLast();

	}


	@Test
	public void groupOnSwitchDemo() {
		Flux<Long> longs = Flux
				.<Long>generate(s -> s.next(System.currentTimeMillis()))
				.delayElements(Duration.ofMillis(10))
				.take(Duration.ofSeconds(5));

		groupOnSwitch(longs, l -> l / 100)
				.flatMap(gf -> gf.count())
				.doOnNext(System.out::println)
				.blockLast();
	}

	@Test
	public void groupOnSwitch() {
		StepVerifier
				.create(
						wordsByInitial()
				)
				.expectNext("one", "WINDOW CLOSED")// the first element gets swollowed, and I don't know how to fix that.
				.expectNext("two", "twenty", "tissue", "WINDOW CLOSED")
				.expectNext("berta", "blot", "WINDOW CLOSED")
				.expectNext("thousand", "WINDOW CLOSED")
				.verifyComplete();
	}

	@Test
	public void groupOnSwitchDebug(){
		wordsByInitial().blockLast();
	}

	private static Flux<String> wordsByInitial() {
		return groupOnSwitch(
				Flux.just("one", "two", "twenty", "tissue", "berta", "blot", "thousand"),
				s -> s.substring(0, 1))
				.flatMap(Flux::materialize)
				.doOnNext(System.out::println)
				.map(s -> s.isOnComplete() ? "WINDOW CLOSED" : s.get());
	}

	@Test
	public void rollingBuffer() {
		Flux<Integer> source = Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9);
		Flux<Integer> doubledFirst = source.compose(s -> source.take(1).concatWith(source));

		doubledFirst.buffer(2, 1).doOnNext(System.out::println).blockLast();
	}


	@Test
	public void groupOnSwitchKeys() {

		Flux<GroupedFlux<String, String>> fluxOfGroupedFluxes = groupOnSwitch(
				Flux.just("one", "two", "twenty", "tissue", "berta", "blot", "thousand"),
				s -> s.substring(0, 1));
		StepVerifier.create(
				fluxOfGroupedFluxes.map(gf -> gf.key())
		)
				.expectNext("o")// the first element gets swallowed, and I don't know how to fix that.
				.expectNext("t", "b", "t")
				.verifyComplete();
	}

	private static <T, X> Flux<GroupedFlux<X, T>> groupOnSwitch(Flux<T> source, Function<T, X> keyFunction) {
		return source
				.startWith((T) Long.valueOf(0L)) // add a dummy value, which will fly out again later in the processing
				.buffer(2, 1) // build lists with prevElemt + currentElement
				.filter(l -> l.size() == 2) // last list only has prevElement. We don't want that
				.windowUntil(l -> isNewGroup(keyFunction, l), true) // group as desired, just the key value is wrong now and we have still List<T> instead of T inside
				.flatMap(						gf -> gf
								.map(l -> l.get(1)) // just take the second element of the list. This removes  the artificial startelement again.
								.groupBy(t -> keyFunction.apply(t)));
	}

	private static <T, X> boolean isNewGroup(Function<T, X> keyFunction, List<T> l) {
		try {
			return !keyFunction.apply(l.get(1)).equals(keyFunction.apply(l.get(0)));
		} catch (ClassCastException cce) {
			return true;
		}
	}
}
