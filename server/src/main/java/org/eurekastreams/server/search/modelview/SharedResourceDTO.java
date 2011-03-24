/*
 * Copyright (c) 2011 Lockheed Martin Corporation
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
package org.eurekastreams.server.search.modelview;

import java.util.Map;

import org.eurekastreams.commons.search.modelview.ModelView;

/**
 * DTO for SharedResource entity.
 * 
 */
public class SharedResourceDTO extends ModelView
{

    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 784907611156885886L;

    /**
     * Stream id for this person.
     */
    private long streamId = UNINITIALIZED_LONG_VALUE;

    /**
     * SharedResource key.
     */
    private String key = UNINITIALIZED_STRING_VALUE;

    /**
     * Load this object's properties from the input Map.
     * 
     * @param properties
     *            the Map of the properties to load
     */
    @Override
    public void loadProperties(final Map<String, Object> properties)
    {
        // let the parent class get its properties first
        super.loadProperties(properties);

        if (properties.containsKey("streamId"))
        {
            setStreamId((Long) properties.get("streamId"));
        }
        if (properties.containsKey("key"))
        {
            setKey((String) properties.get("key"));
        }

    }

    @Override
    protected String getEntityName()
    {
        return "SharedResource";
    }

    /**
     * @return id of SharedResource.
     */
    public long getId()
    {
        return super.getEntityId();
    }

    /**
     * @return the streamId
     */
    public long getStreamId()
    {
        return streamId;
    }

    /**
     * @param inStreamId
     *            the streamId to set
     */
    public void setStreamId(final long inStreamId)
    {
        streamId = inStreamId;
    }

    /**
     * @return the key
     */
    public String getKey()
    {
        return key;
    }

    /**
     * @param inKey
     *            the key to set
     */
    public void setKey(final String inKey)
    {
        key = inKey;
    }

}
