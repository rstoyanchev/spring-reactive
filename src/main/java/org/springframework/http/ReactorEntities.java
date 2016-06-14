/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.http;

import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author Rossen Stoyanchev
 */
public class ReactorEntities {

	public static <W> Function<ResponseEntity2<Mono<W>>, ResponseMono<W>> responseMono() {
		return ResponseMono::new;
	}

	public static <W> Function<ResponseEntity2<Flux<W>>, ResponseFlux<W>> responseFlux() {
		return ResponseFlux::new;
	}

}
