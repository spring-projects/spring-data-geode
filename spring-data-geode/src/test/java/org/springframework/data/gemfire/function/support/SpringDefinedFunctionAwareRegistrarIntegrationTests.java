/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire.function.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.gemfire.util.RuntimeExceptionFactory.newRuntimeException;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionFactory;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.server.CacheServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer;
import org.springframework.data.gemfire.function.config.EnableGemfireFunctionExecutions;
import org.springframework.data.gemfire.function.execution.GemfireOnServerFunctionTemplate;
import org.springframework.data.gemfire.process.ProcessWrapper;
import org.springframework.data.gemfire.support.ConnectionEndpoint;
import org.springframework.data.gemfire.test.support.ClientServerIntegrationTestsSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import example.app.geode.function.executions.Calculator;
import example.app.geode.function.executions.Echo;
import example.app.geode.function.impl.CalculatorFunctions;
import example.geode.function.executions.RegionCalculator;
import example.geode.function.impl.RegionCalculatorFunctions;

/**
 * Integration Tests for {@link SpringDefinedFunctionAwareRegistrar}.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.cache.execute.FunctionService
 * @see org.apache.geode.cache.server.CacheServer
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheApplication
 * @see org.springframework.data.gemfire.config.annotation.ClientCacheConfigurer
 * @see org.springframework.data.gemfire.function.config.EnableGemfireFunctionExecutions
 * @see org.springframework.data.gemfire.function.execution.GemfireOnServerFunctionTemplate
 * @see org.springframework.data.gemfire.function.support.SpringDefinedFunctionAwareRegistrar
 * @see org.springframework.data.gemfire.process.ProcessWrapper
 * @see org.springframework.data.gemfire.test.support.ClientServerIntegrationTestsSupport
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class SpringDefinedFunctionAwareRegistrarIntegrationTests extends ClientServerIntegrationTestsSupport {

	private static final AtomicBoolean registerFunctions = new AtomicBoolean(false);

	private static final SpringDefinedFunctionAwareRegistrar springDefinedFunctionAwareRegistrar =
		new SpringDefinedFunctionAwareRegistrar();

	private static final String CALCULATIONS_REGION_NAME = "Calculations";
	private static final String GEMFIRE_CACHE_SERVER_HOSTNAME = "localhost";
	private static final String GEMFIRE_CACHE_SERVER_PORT_PROPERTY = "gemfire.cache.server.port";
	private static final String GEMFIRE_LOG_LEVEL = "error";

	private static final String[] functionArguments = {
		CalculatorFunctions.class.getPackage().getName(),
		RegionCalculatorFunctions.class.getPackage().getName(),
	};

	private static ProcessWrapper gemfireServer;

	@BeforeClass
	public static void startGemFireServer() throws IOException {

		int cacheServerPort = findAvailablePort();

		gemfireServer = run(GemFireServerConfiguration.class,
			String.format("-D%s=%d", GEMFIRE_CACHE_SERVER_PORT_PROPERTY, cacheServerPort));

		waitForServerToStart(GEMFIRE_CACHE_SERVER_HOSTNAME, cacheServerPort);

		System.setProperty(GEMFIRE_CACHE_SERVER_PORT_PROPERTY, String.valueOf(cacheServerPort));
	}

	@AfterClass
	public static void stopGemFireServer() {
		System.clearProperty(GEMFIRE_CACHE_SERVER_PORT_PROPERTY);
		stop(gemfireServer);
	}

	@Autowired
	private GemFireCache gemfireCache;

	@Autowired
	private Calculator calculator;

	@Autowired
	private Echo echo;

	@Resource(name = CALCULATIONS_REGION_NAME)
	private Region<String, Number> calculations;

	@Autowired
	private RegionCalculator regionCalculator;

	@Before
	@SuppressWarnings("all")
	public void registerApplicationFunctionsOnServer() throws InterruptedException {

		if (registerFunctions.compareAndSet(false, true)) {

			GemfireOnServerFunctionTemplate functionTemplate =
				new GemfireOnServerFunctionTemplate(this.gemfireCache);

			SpringDefinedFunctionAwareRegistrar.ResultStatus resultStatus = functionTemplate
				.executeAndExtract(springDefinedFunctionAwareRegistrar, functionArguments);

			assertThat(resultStatus).isEqualTo(SpringDefinedFunctionAwareRegistrar.ResultStatus.SUCCESS);
		}
	}

	@Test
	public void calculatorCalculationsAreCorrect() {

		assertThat(this.calculator.add(4.0d, 4.0d)).isEqualTo(8.0d);
		assertThat(this.calculator.divide(16.0d, 4.0d)).isEqualTo(4.0d);
		assertThat(this.calculator.factorial(5L)).isEqualTo(120L);
		assertThat(this.calculator.identity(2.0d)).isEqualTo(2.0d);
		assertThat(this.calculator.multiply(4.0d, 4.0d)).isEqualTo(16.0d);
		assertThat(this.calculator.squared(4.0d)).isEqualTo(16.0d);
		assertThat(this.calculator.squareRoot(16.0d)).isEqualTo(4.0d);
		assertThat(this.calculator.subtract(8.0d, 4.0d)).isEqualTo(4.0d);
	}

	@Test
	public void echoEchoesMyMessage() {
		assertThat(this.echo.echo("TEST")).isEqualTo("TEST");
	}

	@Test
	public void regionCalculatorCalculationsAreCorrect() {

		this.calculations.put("number", 10);

		assertThat(this.calculations.get("number")).isEqualTo(10);

		this.regionCalculator.divideInHalf();
		this.calculations.put("test", 1);

		assertThat(this.calculations.get("number")).isEqualTo(5);

		this.regionCalculator.factorial();
		this.calculations.put("test", 2);

		assertThat(this.calculations.get("number")).isEqualTo(120L);

		this.regionCalculator.doubleInValue();
		this.calculations.put("test", 3);

		assertThat(this.calculations.get("number")).isEqualTo(240);
	}

	@ClientCacheApplication(logLevel = GEMFIRE_LOG_LEVEL)
	@EnableGemfireFunctionExecutions(basePackageClasses = { Calculator.class, RegionCalculator.class })
	static class GemFireClientConfiguration {

		@Bean
		ClientCacheConfigurer clientCachePoolPortConfigurer(
			@Value("${" + GEMFIRE_CACHE_SERVER_PORT_PROPERTY + ":" + CacheServer.DEFAULT_PORT + "}") int port) {

			return (beanName, clientCacheFactoryBean) -> clientCacheFactoryBean.setServers(
				Collections.singletonList(new ConnectionEndpoint(GEMFIRE_CACHE_SERVER_HOSTNAME, port)));
		}

		@Bean(CALCULATIONS_REGION_NAME)
		public ClientRegionFactoryBean<String, Number> calculationsRegion(GemFireCache gemfireCache) {

			ClientRegionFactoryBean<String, Number> calculationsRegion = new ClientRegionFactoryBean<>();

			calculationsRegion.setCache(gemfireCache);
			calculationsRegion.setClose(false);
			calculationsRegion.setKeyConstraint(String.class);
			calculationsRegion.setValueConstraint(Number.class);
			calculationsRegion.setShortcut(ClientRegionShortcut.PROXY);

			return calculationsRegion;
		}
	}

	static class GemFireServerConfiguration {

		public static void main(String[] args) {

			Cache gemfireCache = registerShutdownHook(gemfireCacheServer(gemfireCache(gemfireProperties())));

			Region<String, Number> calculationsRegion = calculationsRegion(gemfireCache);

			registerSpringDefinedFunctionAwareRegistrar(gemfireCache);
			//executeSpringDefinedFunctionAwareRegistrar(gemfireCache);
		}

		private static Properties gemfireProperties() {

			Properties gemfireProperties = new Properties();

			String memberName = SpringDefinedFunctionAwareRegistrarIntegrationTests.class.getSimpleName();

			gemfireProperties.setProperty("name", memberName);
			gemfireProperties.setProperty("log-level", GEMFIRE_LOG_LEVEL);
			//gemfireProperties.setProperty("locators", "localhost[10334]");
			//gemfireProperties.setProperty("jmx-manager", "true");
			//gemfireProperties.setProperty("jmx-manager-start", "true");
			//gemfireProperties.setProperty("member-timeout", "600000");
			//gemfireProperties.setProperty("start-locator", "localhost[10334]");

			return gemfireProperties;
		}

		private static Cache gemfireCache(Properties gemfireProperties) {
			return new CacheFactory(gemfireProperties).create();
		}

		private static Cache gemfireCacheServer(Cache gemfireCache) {

			CacheServer cacheServer = gemfireCache.addCacheServer();

			String host = GEMFIRE_CACHE_SERVER_HOSTNAME;

			int port = Optional.ofNullable(System.getProperty(GEMFIRE_CACHE_SERVER_PORT_PROPERTY))
				.map(GemFireServerConfiguration::getDigits)
				.filter(StringUtils::hasText)
				.map(Integer::valueOf)
				.orElse(CacheServer.DEFAULT_PORT);

			cacheServer.setBindAddress(host);
			cacheServer.setHostnameForClients(host);
			cacheServer.setPort(port);
			start(cacheServer, host, port);

			return gemfireCache;
		}

		private static void start(CacheServer cacheServer, String host, int port) {

			try {
				cacheServer.start();
			}
			catch (IOException cause) {
				throw newRuntimeException(cause, "Failed to start CacheServer on host [%s] and port [%d]",
					host, port);
			}
		}

		private static String getDigits(String value) {

			StringBuilder digits = new StringBuilder();

			for (char x : String.valueOf(value).toCharArray()) {
				if (Character.isDigit(x)) {
					digits.append(x);
				}
			}

			return digits.toString();
		}

		private static Region<String, Number> calculationsRegion(Cache gemfireCache) {

			RegionFactory<String, Number> calculationsRegion =
				gemfireCache.createRegionFactory(RegionShortcut.REPLICATE);

			calculationsRegion.setKeyConstraint(String.class);
			calculationsRegion.setValueConstraint(Number.class);

			return calculationsRegion.create(CALCULATIONS_REGION_NAME);
		}

		@SuppressWarnings("unchecked")
		private static void executeSpringDefinedFunctionAwareRegistrar(Cache gemfireCache) {

			FunctionService.onMember(gemfireCache.getDistributedSystem().getDistributedMember())
				.setArguments(functionArguments)
				.execute(springDefinedFunctionAwareRegistrar);
		}

		private static void registerSpringDefinedFunctionAwareRegistrar(Cache gemfireCache) {
			FunctionService.registerFunction(springDefinedFunctionAwareRegistrar);
		}

		private static Cache registerShutdownHook(Cache gemfireCache) {

			Runtime.getRuntime().addShutdownHook(new Thread(() ->
				GemfireUtils.close(gemfireCache), "Apache Geode/Pivotal GemFire Cache Shutdown Thread"));

			return gemfireCache;
		}
	}
}
