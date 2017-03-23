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

import static java.util.Arrays.asList;

import java.time.Duration;

import org.junit.Test;

import reactor.core.publisher.Flux;

/**
 * @author Jens Schauder
 */
public class ZipWithTest {

	@Test
	public void zipWith() throws InterruptedException {
		Flux<Integer> ints = Flux.fromIterable(asList(1, 2, 3, 4, 5, 6, 7, 8)).delayElements(Duration.ofMillis(30));
		Flux<String> letters = Flux.fromIterable(asList("a", "b", "c", "d", "e", "f", "g", "h")).delayElements(Duration.ofMillis(100));

		ints.zipWith(letters).map(t -> {
			System.out.println(t);
			return t;
		}).blockLast();
	}

}
