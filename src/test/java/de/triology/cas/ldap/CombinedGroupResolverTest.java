/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.triology.cas.ldap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.jasig.cas.authentication.principal.Principal;
import org.junit.runner.RunWith;
import org.ldaptive.LdapEntry;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link CombinedGroupResolver}.
 * 
 * @author Sebastian Sdorra
 */
@RunWith(MockitoJUnitRunner.class)
public class CombinedGroupResolverTest {

  @Mock
  private GroupResolver resolverOne;
  
  @Mock
  private GroupResolver resolverTwo;
  
  @Mock
  private GroupResolver resolverThree;

  @Mock
  private Principal principal;
  
  @Mock
  private LdapEntry entry;
  
  /**
   * Tests {@link CombinedGroupResolver#resolveGroups(Principal, LdapEntry)}.
   */
  @Test
  public void testReolveGroups() {
    when(resolverOne.resolveGroups(principal, entry)).thenReturn(new HashSet<>(Arrays.asList("a", "b")));
    when(resolverTwo.resolveGroups(principal, entry)).thenReturn(new HashSet<>(Arrays.asList("c", "d", "b")));
    CombinedGroupResolver resolver = new CombinedGroupResolver(Arrays.asList(resolverOne, resolverTwo, resolverThree));
    assertThat(resolver.resolveGroups(principal, entry), containsInAnyOrder("a", "b", "c", "d"));
  }

}