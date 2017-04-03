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
import java.util.function.Function;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Jens Schauder
 */
public class ErrorTest {

	@Test
	public void test() {
		Flux<Integer> source = Flux.range(1, 100)
				.doOnNext(i -> System.out.println("in source " + i));
		Function<Integer, Integer> f = i -> 100 * i + i / (i % 13);

		source
				.flatMap(i -> Flux.just(i)
						.map(j -> f.apply(j))
						.onErrorResumeWith(t -> Mono.just(9999))
				)
				.doOnNext(System.out::println)
				.doOnError(t -> t.printStackTrace())
				.doOnComplete(() -> System.out.println("done"))
				.take(Duration.ofSeconds(1))
				.blockLast(Duration.ofSeconds(2));
	}
}
