/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.data.gemfire.function.config.three;

import java.util.Set;

import org.springframework.data.gemfire.function.annotation.Filter;
import org.springframework.data.gemfire.function.annotation.FunctionId;
import org.springframework.data.gemfire.function.annotation.OnServer;

/**
 * @author David Turanski
 *
 */
@OnServer(id="testClientOnServerFunction",pool="gemfirePool",resultCollector="myResultCollector")
public interface TestClientOnServerFunction {
	   @FunctionId("f1")
	   //TODO: @Filter is not valid here since not @OnRegion
	   public String getString(Object arg1, @Filter Set<Object> keys) ;

	   @FunctionId("f2")
	   public String getString(Object arg1) ;
}
