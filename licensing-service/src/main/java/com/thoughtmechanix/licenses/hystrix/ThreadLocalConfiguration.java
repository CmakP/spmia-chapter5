package com.thoughtmechanix.licenses.hystrix;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Now that you have your HystrixConcurrencyStrategy via the ThreadLocalAwareStrategy class and your
 * Callable class defined via the DelegatingUserContextCallable class, you need to hook them in Spring Cloud and Hystrix.
 *
 * This Spring configuration class basically rebuilds the Hystrix plugin that manages all the different components
 * running within your service.
 */
@Configuration
public class ThreadLocalConfiguration {
        //When the configuration object is constructed it will autowire in the existing HystrixConcurrencyStrategy
        @Autowired(required = false)
        private HystrixConcurrencyStrategy existingConcurrencyStrategy;

        // Grab references to all the Hystrix components used by the plugin.
        // Then register your custom HystrixConcurrencyStrategy (ThreadLocalAwareStrategy)
        @PostConstruct
        public void init() {

            HystrixPlugins hystrixPluginsInstance = HystrixPlugins.getInstance();

            /**
               Because you’re registering a new concurrency strategy, you’re going to grab all the
               other Hystrix components and then reset the Hystrix plugin.
             */

            // Keeps references of existing Hystrix plugins.
            HystrixEventNotifier eventNotifier = hystrixPluginsInstance.getEventNotifier();
            HystrixMetricsPublisher metricsPublisher = hystrixPluginsInstance.getMetricsPublisher();
            HystrixPropertiesStrategy propertiesStrategy = hystrixPluginsInstance.getPropertiesStrategy();
            HystrixCommandExecutionHook commandExecutionHook = hystrixPluginsInstance.getCommandExecutionHook();

            HystrixPlugins.reset();

            // You now register your HystrixConcurrencyStrategy (ThreadLocalAwareStrategy) with the Hystrix plugin.
            hystrixPluginsInstance.registerConcurrencyStrategy(new ThreadLocalAwareStrategy(existingConcurrencyStrategy));
            // Then re-register all the Hystrix components used by the Hystrix plugin
            hystrixPluginsInstance.registerEventNotifier(eventNotifier);
            hystrixPluginsInstance.registerMetricsPublisher(metricsPublisher);
            hystrixPluginsInstance.registerPropertiesStrategy(propertiesStrategy);
            hystrixPluginsInstance.registerCommandExecutionHook(commandExecutionHook);
        }
}
