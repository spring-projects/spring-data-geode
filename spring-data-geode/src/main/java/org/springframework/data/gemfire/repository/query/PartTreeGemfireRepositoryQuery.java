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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * {@link GemfireRepositoryQuery} backed by a {@link PartTree}, deriving an OQL query
 * from the backing {@link QueryMethod QueryMethod's} name/signature.
 *
 * @author Oliver Gierke
 * @author John Blum
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.repository.query.GemfireRepositoryQuery
 * @see org.springframework.data.repository.query.QueryMethod
 * @see org.springframework.data.repository.query.RepositoryQuery
 * @see org.springframework.data.repository.query.parser.Part
 * @see org.springframework.data.repository.query.parser.PartTree
 */
public class PartTreeGemfireRepositoryQuery extends GemfireRepositoryQuery {

	private final GemfireTemplate template;

	private final PartTree tree;

	/**
	 * Constructs a new instance of {@link PartTreeGemfireRepositoryQuery} initialized with
	 * the given {@link GemfireQueryMethod} and {@link GemfireTemplate}.
	 *
	 * @param queryMethod {@link GemfireQueryMethod} implementing the {@link RepositoryQuery};
	 * must not be {@literal null}.
	 * @param template {@link GemfireTemplate} used to execute {@literal QOL queries};
	 * must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link GemfireQueryMethod} or {@link GemfireTemplate} are {@literal null}.
	 * @see org.springframework.data.gemfire.repository.query.GemfireQueryMethod
	 * @see org.springframework.data.gemfire.GemfireTemplate
	 */
	public PartTreeGemfireRepositoryQuery(GemfireQueryMethod queryMethod, GemfireTemplate template) {

		super(queryMethod);

		Assert.notNull(template, "GemfireTemplate must not be null");

		this.template = template;
		this.tree = new PartTree(queryMethod.getName(), queryMethod.getEntityInformation().getJavaType());
	}

	/**
	 * Returns a {@link PartTree} object consisting of the parts of the (OQL) query.
	 *
	 * @return a {@link PartTree} object consisting of the parts of the (OQL) query.
	 * @see org.springframework.data.repository.query.parser.PartTree
	 */
	protected @NonNull PartTree getPartTree() {
		return this.tree;
	}

	/**
	 * Returns a reference to the {@link GemfireTemplate} used to perform all data access and query operations.
	 *
	 * @return a reference to the {@link GemfireTemplate} used to perform all data access and query operations.
	 * @see org.springframework.data.gemfire.GemfireTemplate
	 */
	protected @NonNull GemfireTemplate getTemplate() {
		return this.template;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Object execute(Object[] arguments) {

		GemfireQueryMethod queryMethod = getGemfireQueryMethod();

		QueryString query = newQueryString(queryMethod, getPartTree(), arguments);

		GemfireRepositoryQuery repositoryQuery = newRepositoryQuery(queryMethod, query, getTemplate());

		return repositoryQuery.execute(prepareStringParameters(arguments));
	}

	private QueryString newQueryString(GemfireQueryMethod queryMethod, PartTree tree, Object[] arguments) {

		ParametersParameterAccessor parameterAccessor =
			new ParametersParameterAccessor(queryMethod.getParameters(), arguments);

		GemfireQueryCreator queryCreator = new GemfireQueryCreator(tree, queryMethod.getPersistentEntity());

		return queryCreator.createQuery(parameterAccessor.getSort());
	}

	private GemfireRepositoryQuery newRepositoryQuery(GemfireQueryMethod queryMethod,
			QueryString query, GemfireTemplate template) {

		StringBasedGemfireRepositoryQuery repositoryQuery =
			new StringBasedGemfireRepositoryQuery(query.toString(), queryMethod, template);

		repositoryQuery.register(getQueryPostProcessor());
		repositoryQuery.asDerivedQuery();

		return repositoryQuery;
	}

	private Object[] prepareStringParameters(Object[] parameters) {

		Iterator<Part> partsIterator = getPartTree().getParts().iterator();

		List<Object> stringParameters = new ArrayList<>(parameters.length);

		for (Object parameter : parameters) {
			if (parameter == null || parameter instanceof Sort) {
				stringParameters.add(parameter);
			}
			else {
				switch (partsIterator.next().getType()) {
					case CONTAINING:
						stringParameters.add(String.format("%%%s%%", parameter.toString()));
						break;
					case STARTING_WITH:
						stringParameters.add(String.format("%s%%", parameter.toString()));
						break;
					case ENDING_WITH:
						stringParameters.add(String.format("%%%s", parameter.toString()));
						break;
					default:
						stringParameters.add(parameter);
				}
			}
		}

		return stringParameters.toArray();
	}
}
