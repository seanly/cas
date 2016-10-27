/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import org.apache.commons.lang.StringUtils;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.RegexRegisteredService;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ReloadableServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adds for each dogu, which needs cas, a service to registeredServices. These
 * dogus getting identified with etcd: Installed dogus have a directory
 * '/dogu/${name of dogu}/current' with their used version. Further 'cas' has to
 * be in the dependencies of the dogu. Changes of the '/dogu' directory will be
 * noticed and registeredServices updated. Every service will be accepted if the
 * ecosystem is in development stage.
 *
 * @author Michael Behlendorf
 */
public final class EtcdServicesManager implements ReloadableServicesManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Map to store all registeredServices.
     */
    private ConcurrentHashMap<Long, RegisteredService> registeredServices = new ConcurrentHashMap<>();

    private String fqdn;

    private String stage;

    private final List<String> allowedAttributes;

    private CloudoguRegistry cloudoguRegistry;

    public EtcdServicesManager(List<String> allowedAttributes, String stage, CloudoguRegistry cloudoguRegistry) {
        this.allowedAttributes = allowedAttributes;
        this.stage = stage;
        this.cloudoguRegistry = cloudoguRegistry;
    }

    @Override
    public Collection<RegisteredService> getAllServices() {
        return Collections.unmodifiableCollection(convertToTreeSet());
    }

    @Override
    public RegisteredService findServiceBy(final Service service) {
        final Collection<RegisteredService> c = convertToTreeSet();

        for (final RegisteredService r : c) {
            if (r.matches(service)) {
                return r;
            }
        }

        return null;
    }

    @Override
    public RegisteredService findServiceBy(final long id) {
        final RegisteredService r = getRegisteredServices().get(id);

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
    public void reload() {
        logger.info("Cas wants to reload registered services.");
    }

    @Override
    public RegisteredService save(final RegisteredService registeredService) {
        throw new UnsupportedOperationException("Operation save is not supported.");
    }

    @Override
    public RegisteredService delete(final long id) {
        throw new UnsupportedOperationException("Operation delete is not supported.");
    }

    private TreeSet<RegisteredService> convertToTreeSet() {
        return new TreeSet<>(getRegisteredServices().values());
    }

    private void load() {
        try {
            addServices(cloudoguRegistry.getDogus());
        } catch (CloudoguRegistryException ex) {
            logger.warn("failed to update servicesManager", ex);
        }
        logger.info(String.format("Loaded %s services.", getRegisteredServices().size()));
    }

    private ConcurrentHashMap<Long, RegisteredService> getRegisteredServices() {
        if (this.registeredServices.isEmpty()) {
            initRegisteredServices();
        }
        return this.registeredServices;
    }

    private void initRegisteredServices() {
        // depends on stage configured in cas.properties
        if (!"development".equals(stage)) {
            initProductionMode();
        } else {
            initDevelopmentMode();
        }
    }

    private void initProductionMode() {
        try {
            logger.debug("cas started in production stage");
            logger.debug("only installed dogus can get a ST");
            fqdn = cloudoguRegistry.getFqdn();
            addServices(cloudoguRegistry.getDogus());
            addCasService(registeredServices);
            changeLoop();
        } catch (CloudoguRegistryException ex) {
            logger.warn("failed to data from cloudoguRegistry", ex);
        }
    }


    private void initDevelopmentMode() {
        logger.debug("cas started in development stage");
        logger.debug("all services can get a ST");
        addDevService(registeredServices);
    }

    /**
     * ChangeLoop detects when a new dogu is installed
     */
    private void changeLoop() {
        logger.debug("entered changeLoop");
        try {
            cloudoguRegistry.addDoguChangeListener(()-> {
                logger.debug("registered change in /dogu");
                load();
                changeLoop();
            });
        } catch (CloudoguRegistryException ex) {
            logger.error("failed to load service", ex);
        }
    }

    /**
     * Creates a RegexRegisteredService for an given name
     */
    private RegexRegisteredService createService(String name) {
        RegexRegisteredService service = new RegexRegisteredService();
        String[] nameArray = StringUtils.split(name, "/");
        service.setAllowedToProxy(true);
        service.setName(name);
        service.setServiceId("https://" + fqdn + "(:443)?/" + nameArray[nameArray.length - 1] + "(/.*)?");
        service.setEvaluationOrder((int) service.getId());
        service.setAllowedAttributes(allowedAttributes);
        service.setId(EtcdRegistryUtils.findHighestId(registeredServices) + 1);
        return service;
    }

    /**
     * Adds for each installed dogu an entry to registeredServices
     *
     * @param nodesFromEtcd  all dogu nodes
     */
    private void addServices(List<String> nodesFromEtcd) {
        synchronize(nodesFromEtcd, registeredServices);
    }

    /**
     * Destination becomes a Map with a RegisteredService for each String in source
     */
    private void synchronize(List<String> source, ConcurrentHashMap<Long, RegisteredService> destination) {
        for (Iterator<Map.Entry<Long, RegisteredService>> entries = destination.entrySet().iterator(); entries
                .hasNext(); ) {
            Map.Entry<Long, RegisteredService> entry = entries.next();
            int index = EtcdRegistryUtils.containsService(source, entry.getValue());
            if (index < 0) {
                entries.remove();
            } else {
                source.remove(index);
            }
        }

        for (String entry : source) {
            RegexRegisteredService newService = createService(entry);
            destination.put(newService.getId(), newService);
        }
    }

    /**
     * This is necessary since cas needs a ST in clearPass workflow
     */
    private void addCasService(ConcurrentHashMap<Long, RegisteredService> localServices) {
        RegexRegisteredService service = new RegexRegisteredService();
        service.setAllowedToProxy(true);
        service.setName("cas");
        service.setServiceId("https://" + fqdn + "/cas/.*");
        service.setEvaluationOrder((int) service.getId());
        service.setAllowedAttributes(allowedAttributes);
        service.setId(EtcdRegistryUtils.findHighestId(localServices) + 1);
        localServices.put(service.getId(), service);
    }

    /**
     * The dev service accepts all services
     */
    private void addDevService(ConcurrentHashMap<Long, RegisteredService> localServices) {
        RegexRegisteredService service = new RegexRegisteredService();
        service.setServiceId("^(https?|imaps?)://.*");
        service.setId(0);
        service.setName("10000001");
        service.setAllowedToProxy(true);
        service.setAllowedAttributes(allowedAttributes);
        localServices.put(service.getId(), service);
    }
}
