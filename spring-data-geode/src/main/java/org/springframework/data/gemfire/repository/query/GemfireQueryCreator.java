/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.data.gemfire.repository.query;

import java.util.Iterator;

import org.springframework.data.domain.Sort;
import org.springframework.data.gemfire.mapping.GemfirePersistentEntity;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractQueryCreator} to create {@link QueryString} instances.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see org.springframework.data.gemfire.repository.query.QueryBuilder
 * @see org.springframework.data.repository.query.parser.AbstractQueryCreator
 * @see org.springframework.data.repository.query.parser.Part
 * @see org.springframework.data.repository.query.parser.PartTree
 */
class GemfireQueryCreator extends AbstractQueryCreator<QueryString, Predicates> {

	private static final Logger logger = LoggerFactory.getLogger(GemfireQueryCreator.class);

	private Iterator<Integer> indexes;

	private final QueryBuilder queryBuilder;

	/**
	 * Creates a new {@link GemfireQueryCreator} using the given {@link PartTree} and domain class.
	 *
	 * @param tree must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 */
	public GemfireQueryCreator(PartTree tree, GemfirePersistentEntity<?> entity) {

		super(tree);

		this.queryBuilder = new QueryBuilder(entity, tree);
		this.indexes = new IndexProvider();
	}

	@Override
	public QueryString createQuery(Sort dynamicSort) {

		this.indexes = new IndexProvider();

		return super.createQuery(dynamicSort);
	}

	@Override
	protected Predicates create(Part part, Iterator<Object> iterator) {
		return Predicates.create(part, this.indexes);
	}

	@Override
	protected Predicates and(Part part, Predicates base, Iterator<Object> iterator) {
		return base.and(Predicates.create(part, this.indexes));
	}

	@Override
	protected Predicates or(Predicates base, Predicates criteria) {
		return base.or(criteria);
	}

	@Override
	protected QueryString complete(Predicates criteria, Sort sort) {

		QueryString query = this.queryBuilder.create(criteria).orderBy(sort);

		if (logger.isDebugEnabled()) {
			logger.debug("Created Query [{}]", query.toString());
		}

		return query;
	}

	/**
	 * {@link IndexProvider} is an {@link Iterator} providing sequentially numbered placeholders (starting at 1),
	 * in a generated GemFire OQL statement corresponding to all possible arguments passed to
	 * the query's indexed parameters.
	 *
	 * @see java.util.Iterator
	 */
	private static class IndexProvider implements Iterator<Integer> {

		private int index;

		public IndexProvider() {
			this.index = 1;
		}

		@Override
		public boolean hasNext() {
			return this.index <= Integer.MAX_VALUE;
		}

		@Override
		public Integer next() {
			return this.index++;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
