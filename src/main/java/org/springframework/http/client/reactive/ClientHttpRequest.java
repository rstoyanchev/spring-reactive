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

package org.springframework.http.client.reactive;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpRequest;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * Represents a reactive client-side HTTP request.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 */
public interface ClientHttpRequest extends HttpRequest {

	/**
	 * Execute this request, resulting in a reactive stream of a single
	 * {@link org.springframework.http.client.ClientHttpResponse}.
	 *
	 * @return a {@code Mono<ClientHttpResponse>} that signals when the the response
	 * status and headers have been received. The response body is made available with
	 * a separate Publisher within the {@code ClientHttpResponse}.
	 */
	Mono<ClientHttpResponse> execute();

	/**
	 * Set the body of this message to the given publisher of {@link DataBuffer}s.
	 * The publisher will be used to write to the underlying HTTP layer
	 * asynchronously, given pull demand by this layer.
	 */
	void setBody(Publisher<DataBuffer> body);

	/**
	 * Set success and failure callbacks after the request headers and body
	 * have been written to the network.
	 *
	 * @param successCallback to be called once the request is sent
	 * @param failureCallback to be called if an error happens while sending the request
	 */
	void onSent(SuccessCallback<Void> successCallback, FailureCallback failureCallback);
}
