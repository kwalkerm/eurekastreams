/*
 * Copyright (c) 2010-2011 Lockheed Martin Corporation
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
package org.eurekastreams.server.action.execution.notification.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eurekastreams.server.action.execution.notification.NotificationBatch;
import org.eurekastreams.server.action.request.notification.ActivityNotificationsRequest;
import org.eurekastreams.server.action.request.notification.CreateNotificationsRequest;
import org.eurekastreams.server.domain.DomainGroup;
import org.eurekastreams.server.domain.NotificationType;
import org.eurekastreams.server.domain.PropertyMap;
import org.eurekastreams.server.domain.PropertyMapTestHelper;
import org.eurekastreams.server.domain.stream.ActivityDTO;
import org.eurekastreams.server.persistence.mappers.DomainMapper;
import org.eurekastreams.server.search.modelview.DomainGroupModelView;
import org.eurekastreams.server.search.modelview.PersonModelView;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the group stream post notification translator.
 */
@SuppressWarnings("unchecked")
public class GroupStreamPostTranslatorTest
{
    /** Test data. */
    private static final long ACTOR_ID = 1111L;

    /** Test data. */
    private static final long GROUP_ID = 1L;

    /** Test data. */
    private static final long COORDINATOR1_ID = 42;

    /** Test data. */
    private static final long COORDINATOR2_ID = 98;

    /** Test data. */
    private static final long MEMBER1_ID = 111;

    /** Test data. */
    private static final long MEMBER2_ID = 98;

    /** Test data. */
    private static final long ACTIVITY_ID = 3333L;

    /** Context for building mock objects. */
    private final Mockery context = new JUnit4Mockery()
    {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /** Fixture: Mapper to get list of members of a group. */
    private final DomainMapper<Long, List<Long>> memberMapper = context.mock(DomainMapper.class);

    /** System under test. */
    private NotificationTranslator sut;

    /** Test list of coordinators. */
    private final List<Long> coordinators = Arrays.asList(COORDINATOR1_ID, COORDINATOR2_ID);

    /** Test list of members. */
    private final List<Long> members = Arrays.asList(MEMBER1_ID, MEMBER2_ID);

    /** Test group. */
    private final DomainGroup group = context.mock(DomainGroup.class);

    /**
     * Setup test.
     */
    @Before
    public void setup()
    {
        sut = new GroupStreamPostTranslator(memberMapper);
    }

    /**
     * Test creating the notification for the event of posting to a group stream.
     */
    @Test
    public void testTranslateGroupStreamPost()
    {
        context.checking(new Expectations()
        {
            {
                oneOf(memberMapper).execute(GROUP_ID);
                will(returnValue(members));
            }
        });

        CreateNotificationsRequest request = new ActivityNotificationsRequest(null, ACTOR_ID, GROUP_ID, ACTIVITY_ID);
        NotificationBatch results = sut.translate(request);

        context.assertIsSatisfied();

        // check recipients
        assertEquals(1, results.getRecipients().size());
        TranslatorTestHelper.assertRecipients(results, NotificationType.POST_TO_JOINED_GROUP, members);

        // check properties
        PropertyMap<Object> props = results.getProperties();
        assertEquals(4, props.size());
        PropertyMapTestHelper.assertPlaceholder(props, "actor", PersonModelView.class, ACTOR_ID);
        PropertyMapTestHelper.assertPlaceholder(props, "stream", DomainGroupModelView.class, GROUP_ID);
        PropertyMapTestHelper.assertAlias(props, "source", "stream");
        PropertyMapTestHelper.assertPlaceholder(props, "activity", ActivityDTO.class, ACTIVITY_ID);
    }

    /**
     * Test creating the notification for the event of posting to a group stream when there are no members or none with
     * notifications enabled.
     */
    @Test
    public void testTranslateGroupStreamPostNoMembers()
    {
        context.checking(new Expectations()
        {
            {
                oneOf(memberMapper).execute(GROUP_ID);
                will(returnValue(Collections.EMPTY_LIST));
            }
        });

        CreateNotificationsRequest request = new ActivityNotificationsRequest(null, ACTOR_ID, GROUP_ID, ACTIVITY_ID);
        NotificationBatch results = sut.translate(request);

        context.assertIsSatisfied();
        assertNull(results);
    }
}
