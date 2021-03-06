/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 * 
 * Copyright notice
 */

package de.triology.cas.ldap;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jasig.cas.authentication.principal.Principal;
import org.ldaptive.LdapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Combines the result of multiple {@link GroupResolver}.
 * 
 * @author Sebastian Sdorra
 */
public class CombinedGroupResolver implements GroupResolver {

    private static final Logger LOG = LoggerFactory.getLogger(CombinedGroupResolver.class);
    
    private final List<GroupResolver> groupResolvers;

    /**
     * Creates an new instance.
     * 
     * @param groupResolvers list of group resolvers
     */
    public CombinedGroupResolver(List<GroupResolver> groupResolvers) {
        this.groupResolvers = Collections.unmodifiableList(groupResolvers);
    }
    
    @Override
    public Set<String> resolveGroups(Principal principal, LdapEntry ldapEntry) {
        Set<String> groups = new HashSet<>();
        for ( GroupResolver resolver : groupResolvers ) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("resolve groups from {} with {}", principal.getId(), resolver.getClass());
            }
            groups.addAll(resolver.resolveGroups(principal, ldapEntry));
        }
        return groups;
    }

}
