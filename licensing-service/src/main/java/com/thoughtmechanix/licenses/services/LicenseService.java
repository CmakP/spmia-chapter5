package com.thoughtmechanix.licenses.services;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.thoughtmechanix.licenses.clients.OrganizationRestTemplateClient;
import com.thoughtmechanix.licenses.config.ServiceConfig;
import com.thoughtmechanix.licenses.model.License;
import com.thoughtmechanix.licenses.model.Organization;
import com.thoughtmechanix.licenses.repository.LicenseRepository;
import com.thoughtmechanix.licenses.utils.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Hystrix does let you set default parameters at the class level so that all Hystrix commands within a specific class
 * share the same configurations.
 * For example, if you wanted all the resources within a specific class to have a timeout of 10 seconds,
 * you could set the @DefaultProperties in the following manner:
 */
//@DefaultProperties(
//        commandProperties = {
//                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000")})
@Service
public class LicenseService {
    private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);
    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    ServiceConfig config;

    @Autowired
    OrganizationRestTemplateClient organizationRestClient;


    public License getLicense(String organizationId,String licenseId) {
        License license = licenseRepository.findByOrganizationIdAndLicenseId(organizationId, licenseId);

        logger.info("(###LicenseService.getLicense) invoking organizationservice getOrganization service");
        Organization org = getOrganization(organizationId);

        return license
                .withOrganizationName( org.getName())
                .withContactName( org.getContactName())
                .withContactEmail( org.getContactEmail() )
                .withContactPhone( org.getContactPhone() )
                .withComment(config.getExampleProperty());
    }

    @HystrixCommand
    private Organization getOrganization(String organizationId) {
        return organizationRestClient.getOrganization(organizationId);
    }

    private void randomlyRunLong(){
      Random rand = new Random();

      int randomNum = rand.nextInt((3 - 1) + 1) + 1;

      if (randomNum==3) sleep();
    }

    private void sleep(){
        try {
            logger.info("###randomlyRunLong() - Sleeping for 11 secs...");
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // With the use of the @HystrixCommand annotation, any time the getLicensesByOrg() method is called, the call will be wrapped
    // with a Hystrix circuit breaker. The circuit breaker will interrupt any call to the getLicensesByOrg() method any time
    // the call takes longer than 1,000 milliseconds. (To config this time, uncomment execution.isolation.thread.timeoutInMilliseconds)
    @HystrixCommand(
            //The fallbackMethod attribute defines a single function in your class that will be called if the call from Hystrix fails
            fallbackMethod = "buildFallbackLicenseList",
            threadPoolKey = "licenseByOrgThreadPool",
            threadPoolProperties =
                    {@HystrixProperty(name = "coreSize",value="30"),
                     @HystrixProperty(name="maxQueueSize", value="10")},
            commandProperties={
                    // controls the amount of consecutive calls that must occur within a 10-second window
                    // before Hystrix will consider tripping the circuit breaker for the call
                     @HystrixProperty(name="circuitBreaker.requestVolumeThreshold", value="10"),
                    // percentage of calls that must fail after this value has been passed before the circuit
                    // breaker is tripped
                     @HystrixProperty(name="circuitBreaker.errorThresholdPercentage", value="75"),
                    // amount of time Hystrix will sleep once the circuit breaker is tripped before Hystrix
                    // will allow another call through to see if the service is healthy again
                     @HystrixProperty(name="circuitBreaker.sleepWindowInMilliseconds", value="7000"),
                    // used to control the size of the window that will be used by Hystrix to monitor for
                    // problems with a service call
                     @HystrixProperty(name="metrics.rollingStats.timeInMilliseconds", value="15000"),
                    // controls the number of times statistics are collected in the window you’ve defined
                     @HystrixProperty(name="metrics.rollingStats.numBuckets", value="5")}
                    // To avoid a timeout error, set the maximum timeout a Hystrix call will wait before failing to be 12 seconds
                    // Because your artificial timeout on the call is 11 seconds while now its configured to only time out after 12 seconds
                    // @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds", value="12000")}
    )
//    @HystrixCommand
    public List<License> getLicensesByOrg(String organizationId){
        logger.debug("LicenseService.getLicensesByOrg Correlation id: {}", UserContextHolder.getContext().getCorrelationId());
        randomlyRunLong();

        return licenseRepository.findByOrganizationId(organizationId);
    }

    /**
     * This fallback method must reside in the same class as the original method that was protected by the @HystrixCommand.
     * The fallback method must have the exact same method signature as the originating function as all of the parameters
     * passed into the original method protected by the @HystrixCommand will be passed to the fallback.
     */
    private List<License> buildFallbackLicenseList(String organizationId){
        List<License> fallbackList = new ArrayList<>();
        logger.info("(###LicenseService.buildFallbackLicenseList) - Fallback method for getLicensesByOrg triggered!");
        License license = new License()
                .withId("0000000-00-00000")
                .withOrganizationId( organizationId )
                .withProductName("Sorry no licensing information currently available");

        fallbackList.add(license);
        return fallbackList;
    }

    public void saveLicense(License license){
        license.withId( UUID.randomUUID().toString());

        licenseRepository.save(license);
    }

    public void updateLicense(License license){
      licenseRepository.save(license);
    }

    public void deleteLicense(License license){
        licenseRepository.delete( license.getLicenseId());
    }
}
