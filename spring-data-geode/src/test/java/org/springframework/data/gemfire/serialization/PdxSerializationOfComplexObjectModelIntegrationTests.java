/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.gemfire.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.RegionService;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.PdxSerializer;
import org.apache.geode.pdx.PdxWriter;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;
import org.springframework.data.gemfire.mapping.MappingPdxSerializer;
import org.springframework.data.gemfire.mapping.annotation.Region;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Integration Tests testing and asserting the de/serialization of a complex application domain object model
 * using Apache Geode PDX serialization.
 *
 * @author John Blum
 * @see org.apache.geode.cache.GemFireCache
 * @see org.apache.geode.cache.Region
 * @see org.apache.geode.pdx.PdxInstance
 * @see org.apache.geode.pdx.PdxSerializer
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.config.annotation.EnableEntityDefinedRegions
 * @see org.springframework.data.gemfire.config.annotation.EnablePdx
 * @see org.springframework.data.gemfire.config.annotation.PeerCacheApplication
 * @see org.springframework.data.gemfire.mapping.MappingPdxSerializer
 * @see org.springframework.test.context.ActiveProfiles
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@ActiveProfiles("MappingPdxSerializer")
//@ActiveProfiles("ReflectionBasedAutoSerializer")
@SuppressWarnings("unused")
public class PdxSerializationOfComplexObjectModelIntegrationTests {

	@Autowired
	private Environment environment;

	@Autowired
	private GemfireTemplate ordersTemplate;

	@Before
	public void assertGeodeCacheAndRegionConfiguration() {

		assertThat(this.environment).isNotNull();
		assertThat(this.ordersTemplate).isNotNull();

		org.apache.geode.cache.Region<Object, Object> orders = this.ordersTemplate.getRegion();

		assertThat(orders).isNotNull();
		assertThat(orders.getName()).isEqualTo("Orders");
		assertThat(orders.getAttributes()).isNotNull();
		assertThat(orders.getAttributes().getDataPolicy()).isEqualTo(DataPolicy.PARTITION);

		RegionService regionService = orders.getRegionService();

		assertThat(regionService).isInstanceOf(GemFireCache.class);

		Class<? extends PdxSerializer> expectedPdxSerializerType =
			Arrays.asList(this.environment.getActiveProfiles()).contains("MappingPdxSerializer")
				? MappingPdxSerializer.class
				: ReflectionBasedAutoSerializer.class;

		GemFireCache cache = (GemFireCache) regionService;

		assertThat(cache).isNotNull();
		assertThat(cache.getPdxSerializer()).isInstanceOf(expectedPdxSerializerType);
	}

	@Test
	public void serializationUsingPdx() {

		Product productOne = Product.with("ONE", new BigDecimal("10.00"));
		Product productTwo = Product.with("TWO", new BigDecimal("20.00"));

		Item itemOne = Item.of(productOne, 5);
		Item itemTwo = Item.of(productTwo, 2);

		Customer jonDoe = Customer.as("Jon Doe");

		Order order = Order.identifiedAs(123)
			.add(itemOne)
			.add(itemTwo)
			.orderedBy(jonDoe);

		assertThat(order).isNotNull();
		assertThat(order.findBy("ONE").orElse(null)).isEqualTo(itemOne);
		assertThat(order.findBy("TWO").orElse(null)).isEqualTo(itemTwo);
		assertThat(order.getSize()).isEqualTo(7);
		assertThat(order.getTotal()).isEqualTo(new BigDecimal("90.00"));

		// Store (save) the Order in the Apache Geode cache "Orders" Region;
		// The Order should be stored as PDX using the SDG MappingPdxSerializer since the "Orders" Region
		// is a PARTITION Region and the cache was configured with SDG's MappingPdxSerializer.
		this.ordersTemplate.put(order.getNumber(), order);

		// Get (load) the Order from the Apache Geode cache "Orders" Region;
		// The Order object should be returned as PDX since the read-serialized bit was set to true.
		Object orderObject = this.ordersTemplate.get(order.getNumber());

		assertThat(orderObject).isInstanceOf(PdxInstance.class);

		PdxInstance orderPdx = (PdxInstance) orderObject;

		assertThat(orderPdx.getField("number")).isEqualTo(123);

		Object jonDoePdx = orderPdx.getField("customer");

		assertThat(jonDoePdx).isNotInstanceOf(Customer.class);
		assertThat(jonDoePdx).isInstanceOf(PdxInstance.class);
		assertThat(((PdxInstance) jonDoePdx).getField("name")).isEqualTo("Jon Doe");

		Object itemsObject = orderPdx.getField("items");

		assertThat(itemsObject).isNotInstanceOf(PdxInstance.class);
		assertThat(itemsObject).isInstanceOf(List.class);

		List<?> itemsList = (List<?>) itemsObject;

		assertThat(itemsList).hasSize(2);
		// The items List contents are PDX!
		assertThat(itemsList.get(0)).isInstanceOf(PdxInstance.class);

		PdxInstance itemOnePdx = (PdxInstance) itemsList.get(0);

		assertThat(itemOnePdx.getField("quantity")).isEqualTo(5);
		assertThat(itemOnePdx.getField("product")).isInstanceOf(PdxInstance.class);

		PdxInstance productOnePdx = (PdxInstance) itemOnePdx.getField("product");

		assertThat(productOnePdx.getField("name")).isEqualTo("ONE");
		assertThat(productOnePdx.getField("price")).isEqualTo(new BigDecimal("10.00"));

		Order deserializedOrder = (Order) orderPdx.getObject();

		assertThat(deserializedOrder).isNotNull();
		assertThat(deserializedOrder).isNotSameAs(order);
		assertThat(deserializedOrder).isEqualTo(order);
		assertThat(deserializedOrder.getNumber()).isEqualTo(123);

		PdxSerializer pdxSerializer = Optional.ofNullable(this.ordersTemplate)
			.map(GemfireTemplate::getRegion)
			.map(org.apache.geode.cache.Region::getRegionService)
			.filter(GemFireCache.class::isInstance)
			.map(GemFireCache.class::cast)
			.map(GemFireCache::getPdxSerializer)
			.orElse(null);

		assertThat(pdxSerializer).isNotNull();

		verify(pdxSerializer, atLeastOnce()).toData(eq(order), any(PdxWriter.class));
		verify(pdxSerializer, atLeastOnce()).toData(eq(jonDoe), any(PdxWriter.class));
		verify(pdxSerializer, atLeastOnce()).toData(eq(itemOne), any(PdxWriter.class));
		verify(pdxSerializer, atLeastOnce()).toData(eq(itemTwo), any(PdxWriter.class));
		verify(pdxSerializer, atLeastOnce()).toData(eq(productOne), any(PdxWriter.class));
		verify(pdxSerializer, atLeastOnce()).toData(eq(productTwo), any(PdxWriter.class));
	}

	@PeerCacheApplication(name = "PdxSerializationOfComplexObjectModelIntegrationTests", copyOnRead = true)
	@EnableEntityDefinedRegions(basePackageClasses = Order.class)
	@EnablePdx(serializerBeanName = "OrderPdxSerializer", readSerialized = true)
	static class TestConfiguration {

		@Bean("OrderPdxSerializer")
		@Profile("MappingPdxSerializer")
		MappingPdxSerializer customMappingPdxSerializer() {

			Set<Class<?>> includedTypes = CollectionUtils.asSet(Customer.class, Order.class, Item.class, Product.class);

			MappingPdxSerializer customMappingPdxSerializer = spy(MappingPdxSerializer.newMappingPdxSerializer());

			customMappingPdxSerializer.setIncludeTypeFilters(type -> includedTypes.contains(type));

			return customMappingPdxSerializer;
		}

		@Bean("OrderPdxSerializer")
		@Profile("ReflectionBasedAutoSerializer")
		PdxSerializer reflectionAutoSerializer() {
			return new ReflectionBasedAutoSerializer(".*");
		}

		@Bean
		@DependsOn("Orders")
		GemfireTemplate ordersTemplate(GemFireCache cache) {
			return new GemfireTemplate(cache.getRegion("/Orders"));
		}
	}

	@Getter
	@EqualsAndHashCode
	@ToString(of = "name")
	@RequiredArgsConstructor(staticName = "as")
	static class Customer {

		@lombok.NonNull
		private final String name;

	}

	@Getter
	@Region("Orders")
	@EqualsAndHashCode
	@ToString(of = { "number", "customer" })
	@RequiredArgsConstructor(staticName = "identifiedAs")
	static class Order implements Iterable<Item> {

		@Setter(AccessLevel.PRIVATE)
		private Customer customer;

		@Id
		@lombok.NonNull
		private final Integer number;

		@AccessType(AccessType.Type.PROPERTY)
		@Getter(AccessLevel.PROTECTED)
		private final List<Item> items = new ArrayList<>();

		public void setItems(List<Item> items) {

			CollectionUtils.nullSafeList(items).stream()
				.filter(Objects::nonNull)
				.forEach(getItems()::add);
		}

		@Transient
		public int getSize() {

			return getItems().stream()
				.map(Item::getQuantity)
				.reduce((quantityOne, quantityTwo) -> quantityOne + quantityTwo)
				.orElse(0);
		}

		@Transient
		public @NonNull BigDecimal getTotal() {

			return getItems().stream()
				.map(Item::getSubTotal)
				.reduce((subTotalOne, subTotalTwo) -> subTotalOne.add(subTotalTwo))
				.orElse(BigDecimal.ZERO);
		}

		public @NonNull Order add(@NonNull Item item) {

			if (item != null) {

				findBy(item.getProduct().getName())
					.map(existingItem -> existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity()))
					.orElseGet(() -> doAdd(item));
			}

			return this;
		}

		private @NonNull Item doAdd(@NonNull Item item) {
			getItems().add(item);
			return item;
		}

		public Optional<Item> findBy(String productName) {

			return getItems().stream()
				.filter(item -> item.getProduct().getName().equals(productName))
				.findFirst();
		}

		@Override
		public Iterator<Item> iterator() {
			return Collections.unmodifiableList(getItems()).iterator();
		}

		public @NonNull Order orderedBy(Customer customer) {
			setCustomer(customer);
			return this;
		}
	}

	@ToString
	@EqualsAndHashCode(of = "product")
	@RequiredArgsConstructor(staticName = "of")
	static class Item {

		@Getter
		@lombok.NonNull
		private final Product product;

		@lombok.NonNull
		private Integer quantity;

		private synchronized @NonNull Integer getQuantity() {
			Integer quantity = this.quantity;
			return quantity != null ? quantity : 0;
		}

		private synchronized @NonNull Item setQuantity(Integer quantity) {
			this.quantity = quantity;
			return this;
		}

		@Transient
		private synchronized @NonNull BigDecimal getSubTotal() {
			return BigDecimal.valueOf(getQuantity()).multiply(getProduct().getPrice());
		}
	}

	@Getter
	@ToString(of = "name")
	@EqualsAndHashCode(of = "name")
	@RequiredArgsConstructor(staticName = "with")
	static class Product {

		@lombok.NonNull
		private final String name;

		@lombok.NonNull
		private final BigDecimal price;

	}
}
