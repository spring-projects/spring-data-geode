/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.gemfire.listener;

import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeList;
import static org.springframework.data.gemfire.util.CollectionUtils.nullSafeSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.apache.geode.cache.RegionService;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.query.CqAttributes;
import org.apache.geode.cache.query.CqEvent;
import org.apache.geode.cache.query.CqException;
import org.apache.geode.cache.query.CqListener;
import org.apache.geode.cache.query.CqQuery;
import org.apache.geode.cache.query.QueryException;
import org.apache.geode.cache.query.QueryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.gemfire.GemfireQueryException;
import org.springframework.data.gemfire.GemfireUtils;
import org.springframework.data.gemfire.client.PoolResolver;
import org.springframework.data.gemfire.client.support.DefaultableDelegatingPoolAdapter;
import org.springframework.data.gemfire.client.support.DelegatingPoolAdapter;
import org.springframework.data.gemfire.client.support.PoolManagerPoolResolver;
import org.springframework.data.gemfire.config.annotation.ContinuousQueryListenerContainerConfigurer;
import org.springframework.data.gemfire.config.xml.SpringGemFireConstants;
import org.springframework.data.gemfire.util.ArrayUtils;
import org.springframework.data.gemfire.util.CollectionUtils;
import org.springframework.data.gemfire.util.SpringUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

/**
 * Container providing asynchronous processing/handling for Apache Geode Continuous Queries (CQ).
 *
 * @author Costin Leau
 * @author John Blum
 * @see java.util.concurrent.Executor
 * @see org.apache.geode.cache.RegionService
 * @see org.apache.geode.cache.client.Pool
 * @see org.apache.geode.cache.client.PoolManager
 * @see org.apache.geode.cache.query.CqAttributes
 * @see org.apache.geode.cache.query.CqEvent
 * @see org.apache.geode.cache.query.CqListener
 * @see org.apache.geode.cache.query.CqQuery
 * @see org.apache.geode.cache.query.QueryService
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.BeanFactoryAware
 * @see org.springframework.beans.factory.BeanNameAware
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.InitializingBean
 * @see org.springframework.context.SmartLifecycle
 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
 * @see org.springframework.core.task.TaskExecutor
 * @see org.springframework.data.gemfire.client.PoolResolver
 * @see org.springframework.data.gemfire.client.support.DefaultableDelegatingPoolAdapter
 * @see org.springframework.data.gemfire.client.support.DelegatingPoolAdapter
 * @see org.springframework.util.ErrorHandler
 * @since 1.1.0
 */
@SuppressWarnings("unused")
public class ContinuousQueryListenerContainer implements BeanFactoryAware, BeanNameAware,
		InitializingBean, DisposableBean, SmartLifecycle {

	// Default Thread name prefix is "ContinuousQueryListenerContainer-"
	public static final String DEFAULT_THREAD_NAME_PREFIX =
		String.format("%s-", ContinuousQueryListenerContainer.class.getSimpleName());

	// Default PoolResolver uses Apache Geode's PoolManager
	protected static final PoolResolver DEFAULT_POOL_RESOLVER = new PoolManagerPoolResolver();

	private boolean autoStartup = true;

	private volatile boolean initialized = false;
	private volatile boolean manageExecutor = false;
	private volatile boolean running = false;

	private int phase = Integer.MAX_VALUE;

	private BeanFactory beanFactory;

	private ErrorHandler errorHandler;

	private Executor taskExecutor;

	private List<ContinuousQueryListenerContainerConfigurer> cqListenerContainerConfigurers = Collections.emptyList();

	private ContinuousQueryListenerContainerConfigurer compositeCqListenerContainerConfigurer =
		(beanName, container) -> nullSafeList(this.cqListenerContainerConfigurers).forEach(configurer ->
			configurer.configure(beanName, container));

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private PoolResolver poolResolver = DEFAULT_POOL_RESOLVER;

	private Queue<CqQuery> continuousQueries = new ConcurrentLinkedQueue<>();

	private QueryService queryService;

	private Set<ContinuousQueryDefinition> continuousQueryDefinitions = new LinkedHashSet<>();

	private String beanName;
	private String poolName;

	@Override
	public void afterPropertiesSet() {

		applyContinuousQueryListenerContainerConfigurers();
		validateQueryService(initQueryService(eagerlyInitializePool(resolvePoolName())));
		initExecutor();
		initContinuousQueries();

		this.initialized = true;
	}

	/**
	 * Applies configuration customizations to this {@link ContinuousQueryListenerContainer} from the registered
	 * composite {@link ContinuousQueryListenerContainerConfigurer} objects.
	 *
	 * @see #getCompositeContinuousQueryListenerContainerConfigurer()
	 * @see #applyContinuousQueryListenerContainerConfigurers(ContinuousQueryListenerContainerConfigurer...)
	 */
	private void applyContinuousQueryListenerContainerConfigurers() {
		applyContinuousQueryListenerContainerConfigurers(getCompositeContinuousQueryListenerContainerConfigurer());
	}

	/**
	 * Applies an array of {@link ContinuousQueryListenerContainerConfigurer} objects to customize the configuration
	 * of this {@link ContinuousQueryListenerContainer}.
	 *
	 * @param configurers array of {@link ContinuousQueryListenerContainerConfigurer} used to customize
	 * the configuration of this {@link ContinuousQueryListenerContainer}.
	 * @see org.springframework.data.gemfire.config.annotation.ContinuousQueryListenerContainerConfigurer
	 */
	protected void applyContinuousQueryListenerContainerConfigurers(
			ContinuousQueryListenerContainerConfigurer... configurers) {

		List<ContinuousQueryListenerContainerConfigurer> configurerList =
			Arrays.asList(ArrayUtils.nullSafeArray(configurers, ContinuousQueryListenerContainerConfigurer.class));

		applyContinuousQueryListenerContainerConfigurers(configurerList);
	}

	/**
	 * Applies an {@link Iterable} of {@link ContinuousQueryListenerContainerConfigurer} objects to customize
	 * the configuration of this {@link ContinuousQueryListenerContainer}.
	 *
	 * @param configurers {@link Iterable} of {@link ContinuousQueryListenerContainerConfigurer} used to customize
	 * the configuration of this {@link ContinuousQueryListenerContainer}.
	 * @see org.springframework.data.gemfire.config.annotation.ContinuousQueryListenerContainerConfigurer
	 */
	protected void applyContinuousQueryListenerContainerConfigurers(
			Iterable<ContinuousQueryListenerContainerConfigurer> configurers) {

		StreamSupport.stream(CollectionUtils.nullSafeIterable(configurers).spliterator(), false)
			.forEach(configurer -> configurer.configure(getBeanName(), this));
	}

	/**
	 * Resolves a {@link Pool} object with the given {@link String name} from the configured {@link PoolResolver}.
	 *
	 * @param poolName {@link String name} of the {@link Pool} to resolve.
	 * @return a resolved {@link Pool} object from the given {@link String name}.
	 * @see org.apache.geode.cache.client.Pool
	 * @see #getPoolResolver()
	 */
	@Nullable Pool resolvePool(String poolName) {
		return getPoolResolver().resolve(poolName);
	}

	/**
	 * Resolves the name of the {@link Pool} configured to handle the registered Continuous Queries.
	 *
	 * Note, the {@link Pool} must have subscription enabled.
	 *
	 * @return the {@link String name} of the {@link Pool} configured to handle the registered Continuous Queries.
	 */
	String resolvePoolName() {

		return Optional.ofNullable(getPoolName())
			.filter(StringUtils::hasText)
			.orElseGet(() ->
				Optional.ofNullable(getBeanFactory())
					.filter(it -> SpringUtils.isMatchingBean(it, SpringGemFireConstants.DEFAULT_GEMFIRE_POOL_NAME, Pool.class))
					.map(it -> SpringGemFireConstants.DEFAULT_GEMFIRE_POOL_NAME)
					.orElse(GemfireUtils.DEFAULT_POOL_NAME));
	}

	/**
	 * Eagerly initializes the {@link Pool} with the given {@link String name}.
	 *
	 * First, this method attempts to use the configured {@link BeanFactory}, if not {@literal null}, to find a bean
	 * in the Spring container of type {@link Pool} having the given {@link String name }and fetch the bean,
	 * thereby causing the {@link Pool} bean to be initialized.
	 *
	 * However, if the {@link BeanFactory} was not configured, or no bean exists in the Spring container
	 * with the given {@link String name} or of the {@link Pool} type, then the named {@link Pool} is looked up
	 * in GemFire/Geode's {@link org.apache.geode.cache.client.PoolManager}.
	 *
	 * @param poolName {@link String} containing the name of the {@link Pool} to initialize.
	 * @return the given {@link Pool} name.
	 */
	String eagerlyInitializePool(String poolName) {

		Supplier<String> poolNameResolver = () -> {
			Assert.notNull(resolvePool(poolName), String.format("No Pool with name [%s] was found", poolName));
			return poolName;
		};

		return Optional.ofNullable(getBeanFactory())
			.filter(it -> SpringUtils.isMatchingBean(it, poolName, Pool.class))
			.map(it -> {
				try {
					it.getBean(poolName, Pool.class);
					return poolName;
				}
				catch (BeansException ignore) {
					return poolNameResolver.get();
				}
			})
			.orElseGet(poolNameResolver);
	}

	/**
	 * Initializes the {@link QueryService} used to register Continuous Queries (CQ).
	 *
	 * @param poolName {@link String} containing the name of the {@link Pool} used obtain the {@link QueryService}
	 * if CQs are tied to a specific {@link Pool}.
	 * @return the initialized {@link QueryService}.
	 * @see org.apache.geode.cache.query.QueryService
	 */
	QueryService initQueryService(String poolName) {

		QueryService queryService = getQueryService();

		if (queryService == null || StringUtils.hasText(poolName)) {

			Pool resolvedPool = resolvePool(poolName);

			DefaultableDelegatingPoolAdapter poolAdapter =
				DefaultableDelegatingPoolAdapter.from(DelegatingPoolAdapter.from(resolvedPool));

			setQueryService(poolAdapter.preferPool().getQueryService(queryService));
		}

		return getQueryService();
	}

	/**
	 * Verifies the given {@link QueryService} is valid.
	 *
	 * @param queryService {@link QueryService} to validate.
	 * @throws IllegalStateException if the {@link QueryService} is {@literal null}.
	 * @return the given {@link QueryService}
	 * @see org.apache.geode.cache.query.QueryService
	 */
	private QueryService validateQueryService(QueryService queryService) {

		Assert.state(queryService != null, "QueryService is required");

		return queryService;
	}

	/**
	 * Initialize the {@link Executor} used to process CQ events asynchronously.
	 *
	 * @return a new isntance of {@link Executor} used to process CQ events asynchronously.
	 * @see java.util.concurrent.Executor
	 */
	Executor initExecutor() {

		if (getTaskExecutor() == null) {
			setTaskExecutor(createDefaultTaskExecutor());
			this.manageExecutor = true;
		}

		return getTaskExecutor();
	}

	/**
	 * Creates a default {@link TaskExecutor}.
	 *
	 * <p>Called if no explicit {@link TaskExecutor} has been configured.
	 *
	 * <p>The default implementation builds a {@link SimpleAsyncTaskExecutor} with the specified bean name
	 * (or the class name, if no bean name is specified) as the Thread name prefix.</p>
	 *
	 * @return an instance of the {@link TaskExecutor} used to process CQ events asynchronously.
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 */
	protected Executor createDefaultTaskExecutor() {

		String threadNamePrefix = Optional.ofNullable(getBeanName())
			.filter(StringUtils::hasText)
			.map(it -> String.format("%s-", it))
			.orElse(DEFAULT_THREAD_NAME_PREFIX);

		return new SimpleAsyncTaskExecutor(threadNamePrefix);
	}

	/**
	 * Initializes all the {@link CqQuery Continuous Queries} defined by
	 * the {@link ContinuousQueryDefinition Continuous Query Defintions}.
	 *
	 * @see #getContinuousQueryDefinitions()
	 * @see #initContinuousQueries(Set)
	 */
	private void initContinuousQueries() {
		initContinuousQueries(getContinuousQueryDefinitions());
	}

	private void initContinuousQueries(Set<ContinuousQueryDefinition> continuousQueryDefinitions) {

		// Stop the ContinuousQueryListenerContainer if currently running...
		stop();

		// Close any existing continuous queries...
		closeQueries();

		// Add current continuous queries based on the definitions from the configuration...
		continuousQueryDefinitions.forEach(this::addContinuousQuery);
	}

	/**
	 * Determines whether this container is currently active, i.e., whether it has been setup and initialized
	 * but not shutdown yet.
	 *
	 * @return a boolean indicating whether the container is active.
	 */
	public boolean isActive() {
		return this.initialized;
	}

	/**
	 * Sets whether the CQ listener container should automatically start on startup.
	 *
	 * @param autoStartup a boolean value indicating whether this CQ listener container should automatically start.
	 */
	public void setAutoStartup(final boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Determines whether this CQ listener container will automatically start on startup.
	 *
	 * @return a boolean value indicating whether this CQ listener container automatically starts.
	 * @see org.springframework.context.SmartLifecycle#isAutoStartup()
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Determines whether the container has be started and is currently running.
	 *
	 * @return a boolean value indicating whether the container has been started and is currently running.
	 */
	@Override
	public synchronized boolean isRunning() {
		return this.running;
	}

	/**
	 * Sets the {@link BeanFactory} containing this bean.
	 *
	 * @param beanFactory the Spring {@link BeanFactory} containing this bean.
	 * @throws BeansException if an initialization error occurs.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Returns a reference to the configured {@link BeanFactory}.
	 *
	 * @return a reference to the configured {@link BeanFactory}.
	 * @see org.springframework.beans.factory.BeanFactory
	 */
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Set the name of the bean in the bean factory that created this bean.
	 * <p>Invoked after population of normal bean properties but before an
	 * init callback such as {@link InitializingBean#afterPropertiesSet()}
	 * or a custom init-method.</p>
	 *
	 * @param name the name of the bean in the factory.
	 */
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * Returns the configured {@link String bean name} of this container.
	 *
	 * @return the configured {@link String bean name} of this container.
	 */
	protected String getBeanName() {
		return this.beanName;
	}

	/**
	 * Set the underlying RegionService (GemFire Cache) used for registering Queries.
	 *
	 * @param cache the RegionService (GemFire Cache) used for registering Queries.
	 * @see org.apache.geode.cache.RegionService
	 */
	public void setCache(RegionService cache) {
		setQueryService(cache.getQueryService());
	}

	/**
	 * Returns a reference to all the configured/registered {@link CqQuery Continuous Queries}.
	 *
	 * @return a reference to all the configured/registered {@link CqQuery Continuous Queries}.
	 * @see org.apache.geode.cache.query.CqQuery
	 * @see java.util.Queue
	 */
	protected Queue<CqQuery> getContinuousQueries() {
		return this.continuousQueries;
	}

	/**
	 * Returns a reference to all the configured {@link ContinuousQueryDefinition ContinuousQueryDefinitions}.
	 *
	 * @return a reference to all the configured {@link ContinuousQueryDefinition ContinuousQueryDefinitions}.
	 * @see org.springframework.data.gemfire.listener.ContinuousQueryDefinition
	 * @see java.util.Set
	 */
	protected Set<ContinuousQueryDefinition> getContinuousQueryDefinitions() {
		return this.continuousQueryDefinitions;
	}

	/**
	 * Null-safe operation setting an array of {@link ContinuousQueryListenerContainerConfigurer} objects used to
	 * customize the configuration of this {@link ContinuousQueryListenerContainer}.
	 *
	 * @param configurers array of {@link ContinuousQueryListenerContainerConfigurer} objects used to customize
	 * the configuration of this {@link ContinuousQueryListenerContainer}.
	 * @see org.springframework.data.gemfire.config.annotation.ContinuousQueryListenerContainerConfigurer
	 * @see #setContinuousQueryListenerContainerConfigurers(List)
	 */
	public void setContinuousQueryListenerContainerConfigurers(ContinuousQueryListenerContainerConfigurer... configurers) {

		List<ContinuousQueryListenerContainerConfigurer> configurerList =
			Arrays.asList(ArrayUtils.nullSafeArray(configurers, ContinuousQueryListenerContainerConfigurer.class));

		setContinuousQueryListenerContainerConfigurers(configurerList);
	}

	/**
	 * Null-safe operation setting an {@link Iterable} of {@link ContinuousQueryListenerContainerConfigurer} objects
	 * used to customize the configuration of this {@link ContinuousQueryListenerContainer}.
	 *
	 * @param configurers {@link Iterable} of {@link ContinuousQueryListenerContainerConfigurer} objects used to
	 * customize the configuration of this {@link ContinuousQueryListenerContainer}.
	 * @see org.springframework.data.gemfire.config.annotation.ContinuousQueryListenerContainerConfigurer
	 */
	public void setContinuousQueryListenerContainerConfigurers(List<ContinuousQueryListenerContainerConfigurer> configurers) {
		this.cqListenerContainerConfigurers = CollectionUtils.nullSafeList(configurers);
	}

	/**
	 * Returns a <a href="https://en.wikipedia.org/wiki/Composite_pattern">Composite</a> object containing
	 * the collection of {@link ContinuousQueryListenerContainerConfigurer} objects used to customize the configuration
	 * of this {@link ContinuousQueryListenerContainer}.
	 *
	 * @return a Composite object containing a collection of {@link ContinuousQueryListenerContainerConfigurer} objects
	 * used to customize the configuration of this {@link ContinuousQueryListenerContainer}.
	 * @see org.springframework.data.gemfire.config.annotation.ContinuousQueryListenerContainerConfigurer
	 */
	protected ContinuousQueryListenerContainerConfigurer getCompositeContinuousQueryListenerContainerConfigurer() {
		return this.compositeCqListenerContainerConfigurer;
	}

	/**
	 * Set an {@link ErrorHandler} to be invoked in case of any uncaught {@link Exception Exceptions} thrown
	 * while processing a CQ event.
	 *
	 * By default there is <b>no</b> {@link ErrorHandler} configured so error-level logging is the only result.
	 *
	 * @param errorHandler {@link ErrorHandler} invoked when uncaught {@link Exception Exceptions} are thrown
	 * while processing the CQ event.
	 * @see org.springframework.util.ErrorHandler
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Returns an {@link Optional} reference to the configured {@link ErrorHandler} invoked when
	 * any unhandled {@link Exception Exceptions} are thrown when invoking CQ listeners processing CQ events.
	 *
	 * @return an {@link Optional} reference to the configured {@link ErrorHandler}.
	 * @see org.springframework.util.ErrorHandler
	 */
	public Optional<ErrorHandler> getErrorHandler() {
		return Optional.ofNullable(this.errorHandler);
	}

	/**
	 * Sets the phase in which this CQ listener container will start in the Spring container.
	 *
	 * @param phase the phase value of this CQ listener container.
	 */
	public void setPhase(final int phase) {
		this.phase = phase;
	}

	/**
	 * Gets the phase in which this CQ listener container will start in the Spring container.
	 *
	 * @return the phase value of this CQ listener container.
	 * @see org.springframework.context.Phased#getPhase()
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * Set the name of the {@link Pool} used for performing the queries by this container.
	 *
	 * @param poolName the name of the pool to be used by the container
	 */
	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	/**
	 * Returns the configured {@link String pool name}.
	 *
	 * @return the configured {@link String pool name}.
	 */
	public String getPoolName() {
		return this.poolName;
	}

	/**
	 * Configures the {@link PoolResolver} to resolve {@link Pool} objects by {@link String name}
	 * from the Apache Geode cache.
	 *
	 * @param poolResolver the configured {@link PoolResolver} used to resolve {@link Pool} objects
	 * by {@link String name}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 */
	public void setPoolResolver(PoolResolver poolResolver) {
		this.poolResolver = poolResolver;
	}

	/**
	 * Returns the configured {@link PoolResolver} used to resolve {@link Pool} object by {@link String name}.
	 *
	 * @return the configured {@link PoolResolver}.
	 * @see org.springframework.data.gemfire.client.PoolResolver
	 */
	public PoolResolver getPoolResolver() {
		return this.poolResolver != null ? this.poolResolver : DEFAULT_POOL_RESOLVER;
	}

	/**
	 * Attaches the given query definitions.
	 *
	 * @param queries set of queries
	 */
	public void setQueryListeners(Set<ContinuousQueryDefinition> queries) {

		getContinuousQueryDefinitions().clear();
		getContinuousQueryDefinitions().addAll(nullSafeSet(queries));
	}

	/**
	 * Set the GemFire QueryService used by this container to create ContinuousQueries (CQ).
	 *
	 * @param queryService the GemFire QueryService object used by the container to create ContinuousQueries (CQ).
	 * @see org.apache.geode.cache.query.QueryService
	 */
	public void setQueryService(QueryService queryService) {
		this.queryService = queryService;
	}

	/**
	 * Returns a reference to the configured {@link QueryService}.
	 *
	 * @return a reference to the configured {@link QueryService}.
	 * @see org.apache.geode.cache.query.QueryService
	 */
	public QueryService getQueryService() {
		return this.queryService;
	}

	/**
	 * Sets the Task Executor used for running the event listeners when messages are received.
	 * If no task executor is set, an instance of {@link SimpleAsyncTaskExecutor} will be used by default.
	 * The task executor can be adjusted depending on the work done by the listeners and the number of
	 * messages coming in.
	 *
	 * @param taskExecutor The Task Executor used to run event listeners when query results messages are received.
	 * @see java.util.concurrent.Executor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Returns a reference to the configured {@link Executor TaskExecutor}.
	 *
	 * @return a reference to the configured {@link Executor TaskExecutor}.
	 * @see java.util.concurrent.Executor
	 */
	public Executor getTaskExecutor() {
		return this.taskExecutor;
	}

	/**
	 * Adds a {@link ContinuousQueryDefinition Continuous Query (CQ) definition} to the (potentially running) container.
	 *
	 * If the container is running, the listener starts receiving (matching) messages as soon as possible.
	 *
	 * @param definition {@link ContinuousQueryDefinition Continuous Query (CQ) definition} to register.
	 * @see org.springframework.data.gemfire.listener.ContinuousQueryDefinition
	 */
	public void addListener(ContinuousQueryDefinition definition) {

		CqQuery query = addContinuousQuery(definition);

		if (isRunning()) {
			execute(query);
		}
	}

	public boolean addContinuousQueryDefinition(ContinuousQueryDefinition definition) {

		return Optional.ofNullable(definition)
			.map(getContinuousQueryDefinitions()::add)
			.orElse(false);
	}

	CqQuery addContinuousQuery(ContinuousQueryDefinition definition) {

		try {

			CqAttributes attributes = definition.toCqAttributes(this::newCqListener);

			CqQuery query = definition.isNamed()
				? newNamedContinuousQuery(definition, attributes)
				: newUnnamedContinuousQuery(definition, attributes);

			return manage(query);
		}
		catch (QueryException cause) {
			throw new GemfireQueryException(String.format("Unable to create query [%s]", definition.getQuery()), cause);
		}
	}

	protected CqListener newCqListener(ContinuousQueryListener listener) {
		return new EventDispatcherAdapter(listener);
	}

	private CqQuery newNamedContinuousQuery(ContinuousQueryDefinition definition, CqAttributes attributes)
			throws QueryException {

		return getQueryService().newCq(definition.getName(), definition.getQuery(), attributes, definition.isDurable());
	}

	private CqQuery newUnnamedContinuousQuery(ContinuousQueryDefinition definition, CqAttributes attributes)
			throws CqException {

		return getQueryService().newCq(definition.getQuery(), attributes, definition.isDurable());
	}

	private CqQuery manage(CqQuery query) {

		getContinuousQueries().add(query);

		return query;
	}

	@Override
	public synchronized void start() {

		if (!isRunning()) {

			doStart();
			this.running = true;

			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Started ContinuousQueryListenerContainer");
			}
		}
	}

	void doStart() {
		getContinuousQueries().forEach(this::execute);
	}

	private void execute(CqQuery query) {

		try {
			query.execute();
		}
		catch (QueryException cause) {
			throw new GemfireQueryException(String.format("Could not execute query [%1$s]; state is [%2$s]",
				query.getName(), query.getState()), cause);
		}
	}

	/**
	 * Asynchronously dispatches the {@link CqEvent CQ event} to the targeted {@link ContinuousQueryListener}.
	 *
	 * @param listener {@link ContinuousQueryListener} which will process/handle the {@link CqEvent CQ event}.
	 * @param event {@link CqEvent CQ event} to process.
	 * @see org.springframework.data.gemfire.listener.ContinuousQueryListener
	 * @see org.apache.geode.cache.query.CqEvent
	 */
	protected void dispatchEvent(ContinuousQueryListener listener, CqEvent event) {
		getTaskExecutor().execute(() -> notify(listener, event));
	}

	/**
	 * Invoke the specified {@link ContinuousQueryListener listener} to process/handle the {@link CqEvent CQ event}.
	 *
	 * @param listener {@link ContinuousQueryListener} to notify of the {@link CqEvent CQ event}.
	 * @param event {@link CqEvent CQ event} to process/handle.
	 * @see #handleListenerError(Throwable)
	 */
	private void notify(ContinuousQueryListener listener, CqEvent event) {

		try {
			listener.onEvent(event);
		}
		catch (Throwable cause) {
			handleListenerError(cause);
		}
	}

	/**
	 * Invokes the configured {@link ErrorHandler} (if any) to handle the {@link Exception} thrown by the CQ listener.
	 *
	 * Logs at warning level if no {@link ErrorHandler} was configured.
	 *
	 * Logs at debug level if the CQ listener container was shutdown at the time when the CQ listener
	 * {@link Exception} was thrown.
	 *
	 * @param cause {@link Throwable uncaught error} thrown during normal CQ event processing.
	 * @see #setErrorHandler(ErrorHandler)
	 * @see #getErrorHandler()
	 */
	private void handleListenerError(Throwable cause) {

		getErrorHandler().filter(errorHandler -> {

				boolean active = this.isActive();

				if (!active && this.logger.isDebugEnabled()) {
					this.logger.debug("A CQ listener exception occurred after container shutdown; ErrorHandler will not be invoked", cause);
				}

				return active;
			})
			.ifPresent(errorHandler -> errorHandler.handleError(cause));

		if (!getErrorHandler().isPresent() && this.logger.isWarnEnabled()) {
			this.logger.warn("Execution of CQ listener failed; No ErrorHandler was configured", cause);
		}
	}

	@Override
	public void stop(Runnable callback) {

		stop();
		callback.run();
	}

	@Override
	public synchronized void stop() {

		if (isRunning()) {
			doStop();
			this.running = false;
		}

		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Stopped ContinuousQueryListenerContainer");
		}
	}

	void doStop() {

		getContinuousQueries().forEach(query -> {
			try {
				query.stop();
			}
			catch (Exception cause) {
				if (this.logger.isWarnEnabled()) {
					this.logger.warn(String.format("Cannot stop query [%1$s]; state is [%2$s]",
						query.getName(), query.getState()), cause);
				}
			}
		});
	}

	@Override
	public void destroy() {

		stop();
		closeQueries();
		destroyExecutor();

		this.initialized = false;
	}

	private void closeQueries() {

		getContinuousQueries().stream()
			.filter(query -> !query.isClosed())
			.forEach(query -> {
				try {
					query.close();
				}
				catch (Exception cause) {
					if (logger.isWarnEnabled()) {
						logger.warn(String.format("Cannot close query [%1$s]; state is [%2$s]",
							query.getName(), query.getState()), cause);
					}
				}
			});

		getContinuousQueries().clear();
	}

	private void destroyExecutor() {

		Optional.ofNullable(getTaskExecutor())
			.filter(it -> this.manageExecutor)
			.filter(DisposableBean.class::isInstance)
			.ifPresent(it -> {
				try {

					((DisposableBean) it).destroy();

					if (this.logger.isDebugEnabled()) {
						this.logger.debug("Stopped internally-managed TaskExecutor {}", it);
					}
				}
				catch (Exception cause) {
					this.logger.warn("Failed to properly destroy the managed TaskExecutor {}: {}",
						it, cause.getMessage());
				}
			});
	}

	protected class EventDispatcherAdapter implements CqListener {

		private final ContinuousQueryListener listener;

		protected EventDispatcherAdapter(ContinuousQueryListener listener) {

			Assert.notNull(listener, "ContinuousQueryListener is required");

			this.listener = listener;
		}

		protected ContinuousQueryListener getListener() {
			return this.listener;
		}

		public void onError(CqEvent event) {
			dispatchEvent(getListener(), event);
		}

		public void onEvent(CqEvent event) {
			dispatchEvent(getListener(), event);
		}

		public void close() { }

	}
}
