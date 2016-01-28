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

package org.springframework.web.client;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.support.ByteBufferDecoder;
import org.springframework.core.codec.support.ByteBufferEncoder;
import org.springframework.core.codec.support.JacksonJsonDecoder;
import org.springframework.core.codec.support.JacksonJsonEncoder;
import org.springframework.core.codec.support.JsonObjectDecoder;
import org.springframework.core.codec.support.StringDecoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestFactory;
import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * Reactive HTTP client based on {@link reactor.core.publisher.Flux}
 * and {@link reactor.core.publisher.Mono} types.
 *
 * <p>Here is a simple example of a GET request:
 * <pre class="code">
 * HttpClient client = new HttpClient(new ReactorHttpClientRequestFactory());
 * Mono&lt;String&gt; result = client
 * 		.perform(RequestBuilders.get("http://example.org/resource")
 * 			.accept(MediaType.TEXT_PLAIN))
 * 		.asMonoOf(String.class);
 * </pre>
 *
 * @author Brian Clozel
 * @see RequestBuilders
 */
public final class HttpClient {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private ClientHttpRequestFactory requestFactory;

	private List<Encoder<?>> messageEncoders;

	private List<Decoder<?>> messageDecoders;

	/**
	 * Create a {@code ReactiveRestClient} instance, using the {@link ClientHttpRequestFactory}
	 * implementation given as an argument to drive the underlying HTTP client implementation.
	 *
	 * Register by default the following Encoders and Decoders:
	 * <ul>
	 *     <li>{@link ByteBufferEncoder} / {@link ByteBufferDecoder}</li>
	 *     <li>{@link StringEncoder} / {@link StringDecoder}</li>
	 *     <li>{@link JacksonJsonEncoder} / {@link JacksonJsonDecoder}</li>
	 * </ul>
	 *
	 * @param requestFactory the {@code ClientHttpRequestFactory} to use
	 */
	public HttpClient(ClientHttpRequestFactory requestFactory) {
		this.requestFactory = requestFactory;
		DataBufferAllocator allocator = new DefaultDataBufferAllocator();
		this.messageEncoders = Arrays.asList(new ByteBufferEncoder(allocator), new StringEncoder(allocator),
				new JacksonJsonEncoder(allocator));
		this.messageDecoders = Arrays.asList(new ByteBufferDecoder(), new StringDecoder(allocator),
				new JacksonJsonDecoder(new JsonObjectDecoder(allocator)));
	}

	/**
	 * Set the list of {@link Encoder}s to use for encoding messages
	 */
	public void setMessageEncoders(List<Encoder<?>> messageEncoders) {
		this.messageEncoders = messageEncoders;
	}

	/**
	 * Set the list of {@link Decoder}s to use for decoding messages
	 */
	public void setMessageDecoders(List<Decoder<?>> messageDecoders) {
		this.messageDecoders = messageDecoders;
	}

	/**
	 * Create a {@link HttpClientExecution} that will provide
	 * methods to consume the HTTP response
	 */
	public HttpClientExecution perform(RequestBuilder builder) {
		return new HttpClientExecution(builder);
	}

	/**
	 * Perform the actual HTTP request/response exchange by building the HTTP request
	 * using the provided {@code RequestBuilder} instance and decoding the response
	 * using the type information included in the given {@code ResolvableType} instance
	 */
	public class HttpClientExecution {

		private final RequestBuilder builder;

		public HttpClientExecution(RequestBuilder builder) {
			this.builder = builder;
		}

		/**
		 * Fetch the HTTP response as a reactive stream of a {@link FluxResponseEntity}, which
		 * contains the response headers and publishes the body.
		 */
		public <T> Mono<FluxResponseEntity<T>> asResponse(Class<?> sourceClass, Class<?>... generics) {

			return asResponse(ResolvableType.forClassWithGenerics(sourceClass, generics));
		}

		protected <T> Mono<FluxResponseEntity<T>> asResponse(ResolvableType resolvableType) {

			return perform(this.builder, resolvableType);
		}

		/**
		 * Fetch the HTTP response as a reactive stream emitting a single element
		 */
		public <T> Mono<T> asMonoOf(Class<?> sourceClass, Class<?>... generics) {

			return asMonoOf(ResolvableType.forClassWithGenerics(sourceClass, generics));
		}

		protected <T> Mono<T> asMonoOf(ResolvableType resolvableType) {

			return Mono.from(asFluxOf(resolvableType));
		}

		/**
		 * Fetch the HTTP response as a reactive stream emitting multiple elements
		 */
		public <T> Flux<T> asFluxOf(Class<?> sourceClass, Class<?>... generics) {

			return asFluxOf(ResolvableType.forClassWithGenerics(sourceClass, generics));
		}

		protected <T> Flux<T> asFluxOf(ResolvableType resolvableType) {

			return (Flux<T>) perform(this.builder, resolvableType).flatMap(clientResponseEntity -> clientResponseEntity);
		}

		private <T> Mono<FluxResponseEntity<T>> perform(RequestBuilder builder, ResolvableType expectedResponseType) {
			List<Object> hints = new ArrayList<>();
			hints.add(UTF_8);
			ClientHttpRequest request = builder.build(requestFactory);
			writeRequestBody(request, builder);

			return fetchAndDecodeResponse(request, expectedResponseType, hints);
		}

		private void writeRequestBody(ClientHttpRequest request, RequestBuilder builder) {

			if (builder.getContent() != null) {
				ResolvableType requestBodyType = ResolvableType.forInstance(builder.getContent());
				MediaType mediaType = request.getHeaders().getContentType();

				Publisher pub = Mono.just(builder.getContent());
				Optional<Encoder<?>> messageEncoder = resolveEncoder(requestBodyType, mediaType);

				if (messageEncoder.isPresent()) {
					request.setBody(messageEncoder.get().encode(pub, requestBodyType, mediaType));
				}
				else {
					throw new IllegalStateException("Body type '" + requestBodyType.toString() +
							"' with content type '" + mediaType.toString() + "' not supported");
				}
			}
		}

		protected <T> Mono<FluxResponseEntity<T>> fetchAndDecodeResponse(ClientHttpRequest request,
				ResolvableType expectedResponseType, List<Object> hints) {

			return request.execute()
					.log("org.springframework.http.client.reactive")
					.map(clientHttpResponse -> {
						MediaType contentType = resolveMediaType(clientHttpResponse);
						Optional<Decoder<?>> decoder = resolveDecoder(expectedResponseType, contentType, hints.toArray());
						if (decoder.isPresent()) {
							return new FluxResponseEntity<T>(clientHttpResponse.getStatusCode(), clientHttpResponse.getHeaders(),
									(Flux<T>) decoder.get().decode(clientHttpResponse.getBody(), expectedResponseType, contentType, hints.toArray()));
						}
						else {
							throw new IllegalStateException("Return value type '" + expectedResponseType.toString() +
									"' with content type '" + contentType + "' not supported");
						}
					});
		}

		private MediaType resolveMediaType(ClientHttpResponse response) {
			return response.getHeaders().getContentType();
		}

	}

	protected Optional<Decoder<?>> resolveDecoder(ResolvableType type, MediaType mediaType, Object[] hints) {
		return this.messageDecoders.stream().filter(e -> e.canDecode(type, mediaType)).findFirst();
	}

	protected Optional<Encoder<?>> resolveEncoder(ResolvableType type, MediaType mediaType) {
		return this.messageEncoders.stream()
				.filter(e -> e.canEncode(type, mediaType)).findFirst();
	}

}
