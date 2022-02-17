/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.gemfire.config.annotation.test.partition.entities;

import org.springframework.data.gemfire.mapping.annotation.PartitionRegion;

/**
 * Application entity type stored in an Apache Geode
 * {@link org.apache.geode.cache.DataPolicy#PARTITION {@link org.apache.geode.cache.Region}
 * collocated with the {@link BeforeEntity} application entity type.
 *
 * This entity type is deliberately named to alphabetically come before the {@link BeforeEntity} application entity type
 * in order to see how the entity component scan finds, defines, declares and registers
 * {@link org.apache.geode.cache.Region} bean definitions for these application entity types since Apache Geode expects
 * the {@link org.apache.geode.cache.DataPolicy#PARTITION} {@link org.apache.geode.cache.Region}
 * for the {@link BeforeEntity} to be created before the {@link org.apache.geode.cache.DataPolicy#PARTITION}
 * {@link org.apache.geode.cache.Region} for the {@literal AfterEntity} application entity type,
 * given this {@literal AfterEntity} is collocated with the {@link BeforeEntity}.
 *
 * @author John Blum
 * @see org.springframework.data.gemfire.mapping.annotation.PartitionRegion
 * @see org.springframework.data.gemfire.config.annotation.test.partition.entities.BeforeEntity
 * @since 2.7.0
 */
@PartitionRegion(name = "After", collocatedWith = "Before", redundantCopies = 1)
public class AfterEntity {

}
