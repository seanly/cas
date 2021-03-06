package de.triology.cas.services;

import org.jasig.cas.services.RegisteredService;

import java.util.List;
import java.util.Map;

/**
 * Special stage in which a {@link CesServiceManager} operates during development.
 *
 * <p><b>Never use in production</b>, as it accepts <b>all</b> requests from https imaps.</p>
 */
class CesServicesManagerStageDevelopment extends CesServicesManagerStage {

    CesServicesManagerStageDevelopment(List<String> allowedAttributes) {
        super(allowedAttributes);
    }

    @Override
    protected void initRegisteredServices(Map<Long, RegisteredService> registeredServices) {
        logger.debug("Cas started in development stage. All services can get an ST.");
        addDevService();
    }

    /**
     * The dev service accepts all services
     */
    private void addDevService() {
        addNewService("10000001", "^(https?|imaps?)://.*");
    }
}
