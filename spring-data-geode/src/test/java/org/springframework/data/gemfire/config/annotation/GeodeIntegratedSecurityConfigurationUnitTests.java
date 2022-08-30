/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.gemfire.config.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.springframework.data.gemfire.test.support.MapBuilder;

/**
 * Unit Tests for {@link GeodeIntegratedSecurityConfiguration}.
 *
 * @author John Blum
 * @see java.util.Properties
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.config.annotation.GeodeIntegratedSecurityConfiguration
 * @since 1.0.0
 */
public class GeodeIntegratedSecurityConfigurationUnitTests {

	@Test
	public void toGemFirePropertiesIsCorrect() {

		Map<String, Object> annotationAttributes = MapBuilder.<String, Object>newMapBuilder()
			.put("clientAuthenticationInitializer", "example.TestSecurityClientAuthenticationInitialization")
			.put("securityManagerClass", TestSecurityManager.class)
			.put("securityManagerClassName", "  ")
			.put("peerAuthenticationInitializer", "example.TestSecurityPeerAuthenticationInitializer")
			.put("securityPostProcessorClass", TestSecurityPostProcessor.class)
			.put("securityPostProcessorClassName", "")
			.build();

		GeodeIntegratedSecurityConfiguration configuration = new GeodeIntegratedSecurityConfiguration();

		Properties gemfireProperties = configuration.toGemFireProperties(annotationAttributes);

		assertThat(gemfireProperties).isNotNull();
		assertThat(gemfireProperties).isNotEmpty();
		assertThat(gemfireProperties.getProperty(GeodeIntegratedSecurityConfiguration.SECURITY_CLIENT_AUTH_INIT))
			.isEqualTo("example.TestSecurityClientAuthenticationInitialization");
		assertThat(gemfireProperties.getProperty(GeodeIntegratedSecurityConfiguration.SECURITY_MANAGER))
			.isEqualTo(TestSecurityManager.class.getName());
		assertThat(gemfireProperties).doesNotContainKey(GeodeIntegratedSecurityConfiguration.SECURITY_SHIRO_INIT);
		assertThat(gemfireProperties.getProperty(GeodeIntegratedSecurityConfiguration.SECURITY_PEER_AUTH_INIT))
			.isEqualTo("example.TestSecurityPeerAuthenticationInitializer");
		assertThat(gemfireProperties.getProperty(GeodeIntegratedSecurityConfiguration.SECURITY_POST_PROCESSOR))
			.isEqualTo(TestSecurityPostProcessor.class.getName());

	}

	private static class TestSecurityPostProcessor implements org.apache.geode.security.PostProcessor {

		@Override
		public Object processRegionValue(Object principal, String regionName, Object key, Object value) {
			return value;
		}
	}
}
