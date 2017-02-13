/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apereo.cas.services.ServicesManager;

/**
 * Manages the Dogus that are accessible via CAS within the Cloudogu Ecosystem.
 * Depending on the {@link CesServicesManagerStage} ({@link CesServicesManagerStageDevelopment} or
 * {@link CesServicesManagerStageProductive}), a number of {@link RegisteredService}s is returned.
 */
public class CesServicesManager implements ServicesManager {

    /**
     * This triggers operation in development stage.
     */
    static final String STAGE_DEVELOPMENT = "development";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CesServicesManagerStage serviceStage;

    public CesServicesManager(List<String> allowedAttributes, String stage, Registry registry) {
        serviceStage = createStage(stage, allowedAttributes, registry);
    }

    @Override
    public Collection<RegisteredService> getAllServices() {
        return Collections.unmodifiableCollection(serviceStage.getRegisteredServices().values());
    }

    @Override
    public RegisteredService findServiceBy(final Service service) {
        final Collection<RegisteredService> registeredServices = serviceStage.getRegisteredServices().values();

        for (final RegisteredService registeredService : registeredServices) {
            if (registeredService.matches(service)) {
                return registeredService;
            }
        }

        return null;
    }

    @Override
    public RegisteredService findServiceBy(final long id) {
        final RegisteredService r = serviceStage.getRegisteredServices().get(id);

        try {
            return r == null ? null : r.clone();
        } catch (final CloneNotSupportedException e) {
            return r;
        }
    }

    @Override
    public boolean matchesExistingService(final Service service) {
        return findServiceBy(service) != null;
    }
    
    @Override
    public RegisteredService findServiceBy(String serviceId) {
        return findServiceBy(Long.parseLong(serviceId));
    }

    @Override
    public Collection<RegisteredService> findServiceBy(Predicate<RegisteredService> predicate) {
        return Collections2.filter(getAllServices(), predicate);
    }

    @Override
    public boolean matchesExistingService(String service) {
        return findServiceBy(service) != null;
    }

    @Override
    public void load() {
        logger.info("Cas wants to load registered services.");
    }


    @Override
    public RegisteredService save(final RegisteredService registeredService) {
        throw new UnsupportedOperationException("Operation save is not supported.");
    }

    @Override
    public RegisteredService delete(final long id) {
        throw new UnsupportedOperationException("Operation delete is not supported.");
    }

    /**
     * @return a new instance of the {@link CesServicesManagerStage}, depending on the <code>stageString</code> parameter.
     */
    protected CesServicesManagerStage createStage(String stageString, List<String> allowedAttributes, Registry registry) {
        if (!STAGE_DEVELOPMENT.equals(stageString)) {
            return new CesServicesManagerStageProductive(allowedAttributes, registry);
        } else {
            return new CesServicesManagerStageDevelopment(allowedAttributes);
        }
    }
}
