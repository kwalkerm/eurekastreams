/*
 * Copyright (c) 2009-2010 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eurekastreams.server.service.actions.strategies.ldap.filters;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.ldap.filter.AbstractFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;

/**
 * Tests LDAP CN filter.
 */
public class FindByCnTest
{
    /**
     * System under test.
     */
    private FindByCn sut;

    /**
     * Tests creating the filter.
     */
    @Test
    public final void testCreateFilter()
    {
        List<Filter> filterList = new ArrayList<Filter>();
        filterList.add(new EqualsFilter("objectclass", "person"));
        filterList.add(new EqualsFilter("objectclass", "user"));
        FindByAttrib findByAttribFilterCreator = new FindByAttrib(filterList);

        sut = new FindByCn(findByAttribFilterCreator);

        AbstractFilter filter = sut.getFilter("something");

        assertEquals("(&(objectclass=person)(objectclass=user)(cn=something*))", filter.encode());
    }
}