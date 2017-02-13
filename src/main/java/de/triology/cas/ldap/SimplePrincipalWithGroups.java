/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 * 
 * Copyright notice
 */

package de.triology.cas.ldap;

import java.util.Map;
import org.apereo.cas.authentication.principal.SimplePrincipal;

/**
 *
 * @author Sebastian Sdorra
 */
public class SimplePrincipalWithGroups extends SimplePrincipal {

    public SimplePrincipalWithGroups(String id, Map<String, Object> attributes) {
        super(id, attributes);
    }    
    
}
