package com.thoughtmechanix.licenses.hystrix;

import com.thoughtmechanix.licenses.utils.UserContext;
import com.thoughtmechanix.licenses.utils.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * When a call is made to a Hystrix protected method, Hystrix and Spring Cloud will instantiate an instance of the
 * DelegatingUserContextCallable class, passing in the Callable class that would normally be invoked by a thread managed
 * by a Hystrix command pool. In the previous listing, this Callable class is stored in a Java property called delegate.
 */
public final class DelegatingUserContextCallable<V> implements Callable<V> {

    // Think of the delegate property as being the handle to the method protected by a @HystrixCommand annotation.
    private final Callable<V> delegate;
    //Spring Cloud is also passing along the UserContext object off the parent thread that initiated the call
    private UserContext originalUserContext;

    private static final Logger logger = LoggerFactory.getLogger(DelegatingUserContextCallable.class);

    // Custom Callable class will be passed the original Callable class that will invoke your Hystrix protected code and
    // UserContext coming in from the parent thread
    public DelegatingUserContextCallable(Callable<V> delegate, UserContext userContext) {
        this.delegate = delegate;
        this.originalUserContext = userContext;
    }

    // The call() function is invoked before the method protected by the @HystrixCommand annotation.
    public V call() throws Exception {
        UserContextHolder.setContext( originalUserContext );

        try {
            // Once the UserContext is set invoke the call() method on the Hystrix protected
            // method; for instance, your LicenseServer.getLicenseByOrg() method.
            logger.info("###DelegatingUserContextCallable.call() - UserContext is set invoke the call() method on the Hystrix protected method");
            return delegate.call();
        }
        finally {
            this.originalUserContext = null;
        }
    }

    public static <V> Callable<V> create(Callable<V> delegate, UserContext userContext) {
        return new DelegatingUserContextCallable<V>(delegate, userContext);
    }
}