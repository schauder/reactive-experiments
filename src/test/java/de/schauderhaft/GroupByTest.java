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
import java.util.Objects;
import java.util.function.Function;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;

/**
 * @author Jens Schauder
 */
public class GroupByTest {

	@Test
	public void test() {
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
	public void groupOnSwitch() {
		Flux<Long> longs = Flux
				.<Long>generate(s -> s.next(System.currentTimeMillis()))
				.delayElements(Duration.ofMillis(10))
				.take(Duration.ofSeconds(5));

		groupOnSwitch(longs, l -> l / 100)
				.flatMap(gf -> gf.count())
				.doOnNext(System.out::println)
				.blockLast();
	}

	private static Flux<GroupedFlux<Long, Long>> groupOnSwitch(Flux<Long> longs, Function<Long, Long> keyFunction) {
		ChangeTrigger changeTrigger = new ChangeTrigger(0);
		return longs.windowUntil(l -> changeTrigger.test(keyFunction.apply(l)));
	}

	private static class ChangeTrigger<T> {

		T last = null;

		ChangeTrigger(T initialValue) {
			last =initialValue;
		}

		boolean test(T value) {
			boolean result = !Objects.equals(last, value);
			last = value;
			return result;
		}
	}
}
