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

import static de.schauderhaft.Result.*;

import java.time.Duration;
import java.util.Random;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Jens Schauder
 */
public class BlockingSimulator {


	public static void main(String[] args) {
		Random random = new Random(0);
		Flux<Integer> events = Flux.generate(s -> s.next(random.nextInt()));
		Flux<Result> results = events.flatMap(l -> PrimeFactors.factors(l).map(f -> new Result(l, String.format("result<%s>", f))).concatWith(Mono.just(finalResult(l))));

		results.doOnNext(System.out::println).blockLast(Duration.ofSeconds(10));
	}
}
