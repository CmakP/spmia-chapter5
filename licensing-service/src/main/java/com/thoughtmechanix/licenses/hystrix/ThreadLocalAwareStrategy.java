package com.thoughtmechanix.licenses.hystrix;

import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import com.netflix.hystrix.strategy.properties.HystrixProperty;
import com.thoughtmechanix.licenses.utils.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Several methods need to be overridden. Every method that could be overridden needs to check whether an
 * existing concurrency strategy is present and then either call the existing concurrency strategy’s method or
 * the base Hystrix concurrency strategy method.
 * You have to do this as a convention to ensure that you properly invoke the already-existing Spring Cloud’s
 * HystrixConcurrencyStrategy that deals with security. Otherwise, you can have nasty behavior when trying
 * to use Spring security context in your Hystrix protected code.
 *
 * All the methods that have been override, inherit same logic from:
 * org.springframework.cloud.netflix.hystrix.security.SecurityContextConcurrencyStrategy which extends HystrixConcurrencyStrategy
 *
 * NOTE: Spring Cloud already defines a HystrixConcurrencyStrategy
 */
public class ThreadLocalAwareStrategy extends HystrixConcurrencyStrategy{
    private HystrixConcurrencyStrategy existingConcurrencyStrategy;

    private static final Logger logger = LoggerFactory.getLogger(ThreadLocalAwareStrategy.class);

    //Spring Cloud already has a concurrency class defined. Pass the existing concurrency strategy into the class
    //constructor of your HystrixConcurrencyStrategy
    public ThreadLocalAwareStrategy(HystrixConcurrencyStrategy existingConcurrencyStrategy) {
        this.existingConcurrencyStrategy = existingConcurrencyStrategy;
    }

    @Override
    public BlockingQueue<Runnable> getBlockingQueue(int maxQueueSize) {
        return existingConcurrencyStrategy != null
                ? existingConcurrencyStrategy.getBlockingQueue(maxQueueSize)
                : super.getBlockingQueue(maxQueueSize);
    }

    @Override
    public <T> HystrixRequestVariable<T> getRequestVariable(HystrixRequestVariableLifecycle<T> rv) {
        return existingConcurrencyStrategy != null
                ? existingConcurrencyStrategy.getRequestVariable(rv)
                : super.getRequestVariable(rv);
    }

    @Override
    public ThreadPoolExecutor getThreadPool(HystrixThreadPoolKey threadPoolKey,
                                            HystrixProperty<Integer> corePoolSize,
                                            HystrixProperty<Integer> maximumPoolSize,
                                            HystrixProperty<Integer> keepAliveTime, TimeUnit unit,
                                            BlockingQueue<Runnable> workQueue) {
        return existingConcurrencyStrategy != null ?
               existingConcurrencyStrategy.getThreadPool(threadPoolKey, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue)
               : super.getThreadPool(threadPoolKey, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    // EXCEPT for this one - Inject your Callable implementation that will set the UserContext.
    @Override
    public <T> Callable<T> wrapCallable(Callable<T> callable) {
        logger.info("com.thoughtmechanix.licenses.hystrix.ThreadLocalAwareStrategy:wrapCallable - callable: " + callable.getClass());

        return existingConcurrencyStrategy != null ?
                existingConcurrencyStrategy.wrapCallable(new DelegatingUserContextCallable<T>(callable, UserContextHolder.getContext()))
                : super.wrapCallable(new DelegatingUserContextCallable<T>(callable, UserContextHolder.getContext()));
    }
}
