/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.reactive.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.handler.SimpleHandlerResultHandler;

/**
 * @author Sebastien Deleuze
 */
public class SimpleHandlerResultHandlerTests {

	@Test
	public void supports() throws NoSuchMethodException {

		SimpleHandlerResultHandler resultHandler = new SimpleHandlerResultHandler();
		TestController controller = new TestController();

		HandlerMethod hm = new HandlerMethod(controller, TestController.class.getMethod("voidReturnValue"));
		ResolvableType type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertFalse(resultHandler.supports(new HandlerResult(hm, null, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherString"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertFalse(resultHandler.supports(new HandlerResult(hm, null, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(resultHandler.supports(new HandlerResult(hm, null, type)));
	}


	@SuppressWarnings("unused")
	private static class TestController {

		public Publisher<String> voidReturnValue() {
			return null;
		}

		public Publisher<String> publisherString() {
			return null;
		}

		public Publisher<Void> publisherVoid() {
			return null;
		}
	}

}