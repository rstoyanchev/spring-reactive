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

import reactor.core.publisher.Flux;

/**
 * Example use:
 * <pre>{@code
 * import static org.springframework.http.ReactorEntities.*;
 *
 * Flux<Integer> flux = Flux.just(1, 2, 3);
 * ResponseFlux<Integer> response = ResponseEntity2.ok(flux).as(responseFlux());
 * }
 * </pre>
 *
 * @author Rossen Stoyanchev
 */
public class ResponseFlux<T> extends ResponseEntity2<Flux<T>> {


	ResponseFlux(ResponseEntity2<Flux<T>> entity) {
		super(entity.getBody(), entity.getHeaders(), entity.getStatusCode());
	}

}
