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

package org.springframework.web.reactive.result.method.annotation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * Resolves method arguments annotated with {@code @RequestBody} by reading and
 * decoding the body of the request through a compatible
 * {@code HttpMessageConverter}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 */
public class RequestBodyArgumentResolver implements HandlerMethodArgumentResolver {

	private final List<HttpMessageConverter<?>> messageConverters;

	private final ConversionService conversionService;

	private final List<MediaType> supportedMediaTypes;


	/**
	 * Constructor with message converters and a ConversionService.
	 * @param converters converters for reading the request body with
	 * @param service for converting to other reactive types from Flux and Mono
	 */
	public RequestBodyArgumentResolver(List<HttpMessageConverter<?>> converters,
			ConversionService service) {

		Assert.notEmpty(converters, "At least one message converter is required.");
		Assert.notNull(service, "'conversionService' is required.");
		this.messageConverters = converters;
		this.conversionService = service;
		this.supportedMediaTypes = converters.stream()
				.flatMap(converter -> converter.getReadableMediaTypes().stream())
				.collect(Collectors.toList());
	}


	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Return the configured {@link ConversionService}.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, ModelMap model,
			ServerWebExchange exchange) {

		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		boolean isAsyncType = isAsyncType(type);
		ResolvableType elementType;

		if (isAsyncType || isListOrSet(type)) {
			elementType = type.getGeneric(0);
		}
		else if (type.getRawClass().isArray()) {
			elementType = type.getComponentType();
		}
		else {
			elementType = type;
		}

		MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
		if (mediaType == null) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}

		for (HttpMessageConverter<?> converter : getMessageConverters()) {
			if (converter.canRead(elementType, mediaType)) {
				Flux<?> elementFlux = converter.read(elementType, exchange.getRequest());
				if (Mono.class.equals(type.getRawClass())) {
					Object value = Mono.from(elementFlux);
					return Mono.just(value);
				}
				else if (Flux.class.equals(type.getRawClass())) {
					return Mono.just(elementFlux);
				}
				else if (isAsyncType) {
					Object value = getConversionService().convert(elementFlux, type.getRawClass());
					return Mono.just(value);
				}
				else if (List.class.isAssignableFrom(type.getRawClass())) {
					return elementFlux.collectList().map(o -> o);
				}
				else if (Set.class.isAssignableFrom(type.getRawClass())) {
					return elementFlux.collect(Collectors.toSet()).map(o -> o);
				}
				else if (type.getRawClass().isArray()) {
					return elementFlux.collectList().map(list ->
							getConversionService().convert(list,
									TypeDescriptor.forObject(list), new TypeDescriptor(parameter)));
				}
				else {
					// TODO Currently manage only "Foo" parameter, not "List<Foo>" parameters, Stéphane is going to add toIterable/toIterator to Flux to support that use case
					return elementFlux.next().map(o -> o);
				}
			}
		}

		return Mono.error(new UnsupportedMediaTypeStatusException(mediaType, this.supportedMediaTypes));
	}

	private boolean isAsyncType(ResolvableType type) {
		return (Mono.class.equals(type.getRawClass()) || Flux.class.equals(type.getRawClass()) ||
				getConversionService().canConvert(Publisher.class, type.getRawClass()));
	}

	private boolean isListOrSet(ResolvableType type) {
		return (List.class.equals(type.getRawClass()) || Set.class.equals(type.getRawClass()));
	}

}
