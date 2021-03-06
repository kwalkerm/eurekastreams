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
package org.eurekastreams.web.client.ui.pages.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eurekastreams.server.domain.EntityType;
import org.eurekastreams.server.domain.Page;
import org.eurekastreams.server.domain.PagedSet;
import org.eurekastreams.server.domain.stream.ActivityDTO;
import org.eurekastreams.server.domain.stream.StreamFilter;
import org.eurekastreams.server.domain.stream.StreamScope;
import org.eurekastreams.server.domain.stream.StreamScope.ScopeType;
import org.eurekastreams.server.search.modelview.DomainGroupModelView;
import org.eurekastreams.server.search.modelview.PersonModelView;
import org.eurekastreams.web.client.events.EventBus;
import org.eurekastreams.web.client.events.HistoryViewsChangedEvent;
import org.eurekastreams.web.client.events.MessageStreamAppendEvent;
import org.eurekastreams.web.client.events.Observer;
import org.eurekastreams.web.client.events.UpdatedHistoryParametersEvent;
import org.eurekastreams.web.client.events.data.GotActivityResponseEvent;
import org.eurekastreams.web.client.events.data.GotCurrentUserCustomStreamsResponseEvent;
import org.eurekastreams.web.client.events.data.GotGroupModelViewInformationResponseEvent;
import org.eurekastreams.web.client.events.data.GotPersonalInformationResponseEvent;
import org.eurekastreams.web.client.events.data.GotStreamResponseEvent;
import org.eurekastreams.web.client.events.data.PostableStreamScopeChangeEvent;
import org.eurekastreams.web.client.history.CreateUrlRequest;
import org.eurekastreams.web.client.jsni.EffectsFacade;
import org.eurekastreams.web.client.model.ActivityModel;
import org.eurekastreams.web.client.model.CustomStreamModel;
import org.eurekastreams.web.client.model.GroupModel;
import org.eurekastreams.web.client.model.PersonalInformationModel;
import org.eurekastreams.web.client.model.StreamModel;
import org.eurekastreams.web.client.ui.Session;
import org.eurekastreams.web.client.ui.common.animation.ExpandCollapseAnimation;
import org.eurekastreams.web.client.ui.common.avatar.AvatarWidget.Size;
import org.eurekastreams.web.client.ui.common.charts.StreamAnalyticsChart;
import org.eurekastreams.web.client.ui.common.stream.ActivityDetailPanel;
import org.eurekastreams.web.client.ui.common.stream.StreamJsonRequestFactory;
import org.eurekastreams.web.client.ui.common.stream.renderers.AvatarRenderer;
import org.eurekastreams.web.client.ui.common.stream.renderers.ShowRecipient;
import org.eurekastreams.web.client.ui.common.stream.renderers.StreamMessageItemRenderer;
import org.eurekastreams.web.client.ui.pages.master.StaticResourceBundle;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Activity Page.
 */
public class ActivityContent extends Composite
{
    /** Binder for building UI. */
    private static LocalUiBinder binder = GWT.create(LocalUiBinder.class);

    /**
     * CSS resource.
     */
    interface ActivityStyle extends CssResource
    {
        /**
         * Active sort style.
         * 
         * @return Active sort style
         */
        String activeSort();

        /**
         * Active stream style.
         * 
         * @return Active stream style.
         */
        String activeStream();
    }

    /**
     * CSS style.
     */
    @UiField
    ActivityStyle style;

    /**
     * UI element for streams.
     */
    @UiField
    FlowPanel streamPanel;

    /**
     * UI element for bookmarks.
     */
    @UiField
    UListElement bookmarkList;

    /**
     * UI element for filters.
     */
    @UiField
    UListElement filterList;

    /**
     * UI element for default streams.
     */
    @UiField
    UListElement defaultList;

    /**
     * UI element for stream name.
     */
    @UiField
    SpanElement streamName;

    /**
     * UI element for stream meta info.
     */
    @UiField
    SpanElement streamMeta;

    /**
     * UI element for stream avatar.
     */
    @UiField
    HTMLPanel streamAvatar;

    /**
     * UI element for stream description.
     */
    @UiField
    DivElement streamDescription;

    /**
     * UI element for stream interests.
     */
    @UiField
    DivElement streamInterests;

    /**
     * UI element for stream hash tags.
     */
    @UiField
    DivElement streamHashtags;

    /**
     * UI element for stream connections.
     */
    @UiField
    HTMLPanel streamConnections;

    /**
     * UI element for recent sort.
     */
    @UiField
    Anchor recentSort;

    /**
     * UI element for popular sort.
     */
    @UiField
    Anchor popularSort;

    /**
     * UI element for active sort.
     */
    @UiField
    Anchor activeSort;

    /**
     * UI element for toggling details.
     */
    @UiField
    Anchor toggleDetails;

    /**
     * UI element for stream about panel.
     */
    @UiField
    HTMLPanel streamAbout;

    /**
     * UI element for follower count.
     */
    @UiField
    SpanElement followerCount;

    /**
     * UI element for following count.
     */
    @UiField
    SpanElement followingCount;

    /**
     * UI element for stream details.
     */
    @UiField
    DivElement streamDetailsContainer;

    /**
     * UI element for chart.
     */
    @UiField
    HTMLPanel analyticsChartContainer;

    /**
     * UI element for about link.
     */
    @UiField
    Anchor aboutLink;

    /**
     * UI element for followers link.
     */
    @UiField
    Anchor followersLink;

    /**
     * UI element for activity loading spinner.
     */
    @UiField
    DivElement activitySpinner;

    /**
     * UI element for more spinner.
     */
    @UiField
    DivElement moreSpinner;

    /**
     * UI element for more link.
     */
    @UiField
    Label moreLink;

    /**
     * Expand/Collapse animation.
     */
    private ExpandCollapseAnimation detailsContainerAnimation;

    /**
     * Message Renderer.
     */
    StreamMessageItemRenderer renderer = new StreamMessageItemRenderer(ShowRecipient.ALL);

    /**
     * Newest activity ID.
     */
    private long longNewestActivityId = 0L;

    /**
     * Oldest Activity ID.
     */
    private long longOldestActivityId = 0;

    /**
     * Avatar Renderer.
     */
    private AvatarRenderer avatarRenderer = new AvatarRenderer();

    /**
     * Current Request.
     */
    private JSONObject currentRequestObj = null;

    /**
     * Default stream details container size.
     */
    private static final int DEFAULT_STREAM_DETAILS_CONTAINER_SIZE = 330;

    /**
     * Expand animation duration.
     */
    private static final int EXPAND_ANIMATION_DURATION = 500;

    /**
     * New activity polling.
     */
    private static final int NEW_ACTIVITY_POLLING_DELAY = 1200000;

    /**
     * Default constructor.
     */
    public ActivityContent()
    {
        initWidget(binder.createAndBindUi(this));
        buildPage();
    }

    /**
     * Build the page.
     */
    private void buildPage()
    {
        detailsContainerAnimation = new ExpandCollapseAnimation(streamDetailsContainer,
                DEFAULT_STREAM_DETAILS_CONTAINER_SIZE, EXPAND_ANIMATION_DURATION);
        final StreamAnalyticsChart chart = new StreamAnalyticsChart();

        addEventHandlers();

        defaultList.appendChild(createLI("Following", "following"));
        defaultList.appendChild(createLI("Everyone", "everyone"));

        bookmarkList.appendChild(createLI(Session.getInstance().getCurrentPerson().getPreferredName(), "person/"
                + Session.getInstance().getCurrentPerson().getAccountId()));

        streamAvatar.add(avatarRenderer.render(0L, null, EntityType.PERSON, Size.Normal));

        CustomStreamModel.getInstance().fetch(null, true);

        moreSpinner.addClassName(StaticResourceBundle.INSTANCE.coreCss().displayNone());
        analyticsChartContainer.add(chart);
        chart.update();
        streamConnections.setVisible(false);
    }

    /**
     * Add events.
     */
    private void addEventHandlers()
    {
        toggleDetails.addClickHandler(new ClickHandler()
        {
            public void onClick(final ClickEvent event)
            {
                detailsContainerAnimation.toggle();
            }
        });

        EventBus.getInstance().addObserver(GotActivityResponseEvent.class, new Observer<GotActivityResponseEvent>()
        {

            public void update(final GotActivityResponseEvent event)
            {
                streamPanel.clear();
                activitySpinner.addClassName(StaticResourceBundle.INSTANCE.coreCss().displayNone());
                streamPanel.add(new ActivityDetailPanel(event.getResponse(), ShowRecipient.ALL));
                streamPanel.removeStyleName(StaticResourceBundle.INSTANCE.coreCss().hidden());
            }
        });

        EventBus.getInstance().addObserver(GotStreamResponseEvent.class, new Observer<GotStreamResponseEvent>()
        {
            public void update(final GotStreamResponseEvent event)
            {
                final PagedSet<ActivityDTO> activitySet = event.getStream();

                if (activitySet.getPagedSet().size() > 0)
                {
                    longNewestActivityId = activitySet.getPagedSet().get(0).getEntityId();
                    longOldestActivityId = activitySet.getPagedSet().get(activitySet.getPagedSet().size() - 1)
                            .getEntityId();
                }

                if (StreamJsonRequestFactory.getJSONRequest(event.getJsonRequest()).containsKey("minId"))
                {
                    for (int i = activitySet.getPagedSet().size(); i > 0; i--)
                    {
                        appendActivity(activitySet.getPagedSet().get(i - 1));
                    }
                }
                else if (StreamJsonRequestFactory.getJSONRequest(event.getJsonRequest()).containsKey("maxId"))
                {
                    moreSpinner.addClassName(StaticResourceBundle.INSTANCE.coreCss().displayNone());

                    for (ActivityDTO activity : activitySet.getPagedSet())
                    {
                        streamPanel.add(renderer.render(activity));
                    }

                    moreLink.setVisible(activitySet.getTotal() > activitySet.getPagedSet().size());
                }
                else
                {
                    streamPanel.clear();
                    activitySpinner.addClassName(StaticResourceBundle.INSTANCE.coreCss().displayNone());
                    streamPanel.removeStyleName(StaticResourceBundle.INSTANCE.coreCss().hidden());

                    for (ActivityDTO activity : activitySet.getPagedSet())
                    {
                        streamPanel.add(renderer.render(activity));
                    }

                    moreLink.setVisible(activitySet.getTotal() > activitySet.getPagedSet().size());
                }

            }
        });

        EventBus.getInstance().addObserver(GotCurrentUserCustomStreamsResponseEvent.class,
                new Observer<GotCurrentUserCustomStreamsResponseEvent>()
                {
                    public void update(final GotCurrentUserCustomStreamsResponseEvent event)
                    {
                        for (StreamFilter filter : event.getResponse().getStreamFilters())
                        {

                            filterList.appendChild(createLI(filter.getName(), "custom/"
                                    + filter.getId()
                                    + "/"
                                    + URL.encodeQueryString(filter.getRequest().replace("%%CURRENT_USER_ACCOUNT_ID%%",
                                            Session.getInstance().getCurrentPerson().getAccountId()))));
                        }
                    }
                });

        moreLink.addClickHandler(new ClickHandler()
        {
            public void onClick(final ClickEvent event)
            {
                moreSpinner.removeClassName(StaticResourceBundle.INSTANCE.coreCss().displayNone());

                JSONObject moreItemsRequest = StreamJsonRequestFactory.setMaxId(longOldestActivityId,
                        StreamJsonRequestFactory.getJSONRequest(currentRequestObj.toString()));

                StreamModel.getInstance().fetch(moreItemsRequest.toString(), false);
            }
        });
        EventBus.getInstance().addObserver(UpdatedHistoryParametersEvent.class,
                new Observer<UpdatedHistoryParametersEvent>()
                {
                    public void update(final UpdatedHistoryParametersEvent event)
                    {
                        if (event.getParameters().containsKey("details"))
                        {
                            streamConnections.setVisible("connections".equals(event.getParameters().get("details")));
                            streamAbout.setVisible("about".equals(event.getParameters().get("details")));
                        }
                    }
                });

        EventBus.getInstance().addObserver(HistoryViewsChangedEvent.class, new Observer<HistoryViewsChangedEvent>()
        {
            public void update(final HistoryViewsChangedEvent event)
            {
                loadStream(event.getViews());
                List<String> views = new ArrayList<String>(event.getViews());

                HashMap<String, String> params = new HashMap<String, String>();
                params.put("details", "about");
                aboutLink.setHref("#"
                        + Session.getInstance().generateUrl(new CreateUrlRequest(Page.ACTIVITY, views, params)));

                params.put("details", "connections");
                followersLink.setHref("#"
                        + Session.getInstance().generateUrl(new CreateUrlRequest(Page.ACTIVITY, views, params)));

                if (views.size() < 2 || !"sort".equals(views.get(views.size() - 2)))
                {
                    views.add("sort");
                    views.add("recent");
                }

                views.set(views.size() - 1, "recent");
                recentSort.setHref("#" + Session.getInstance().generateUrl(new CreateUrlRequest(Page.ACTIVITY, views)));

                views.set(views.size() - 1, "popular");
                popularSort
                        .setHref("#" + Session.getInstance().generateUrl(new CreateUrlRequest(Page.ACTIVITY, views)));

                views.set(views.size() - 1, "active");
                activeSort.setHref("#" + Session.getInstance().generateUrl(new CreateUrlRequest(Page.ACTIVITY, views)));

            }
        }, true);

        EventBus.getInstance().addObserver(GotPersonalInformationResponseEvent.class,
                new Observer<GotPersonalInformationResponseEvent>()
                {
                    public void update(final GotPersonalInformationResponseEvent event)
                    {
                        PersonModelView person = event.getResponse();
                        streamName.setInnerText(person.getDisplayName());
                        streamMeta.setInnerText(person.getTitle());
                        streamAvatar.clear();
                        streamAvatar.add(avatarRenderer.render(person.getEntityId(), person.getAvatarId(),
                                EntityType.PERSON, Size.Normal));

                        followerCount.setInnerText(Integer.toString(person.getFollowersCount()));
                        followingCount.setInnerText(Integer.toString(person.getFollowingCount()));
                        streamDescription.setInnerText(person.getJobDescription());
                        String interestString = "";
                        for (String interest : person.getInterests())
                        {
                            interestString += "<a href='#" + interest + "'>" + interest + "</a>";
                        }
                        streamInterests.setInnerHTML(interestString);
                        streamHashtags.setInnerHTML("<a href='#something'>#something</a>");

                        streamConnections.clear();
                        streamConnections.add(GetPersonFollowingTab.getFollowingTab(person));
                    }
                });

        EventBus.getInstance().addObserver(GotGroupModelViewInformationResponseEvent.class,
                new Observer<GotGroupModelViewInformationResponseEvent>()
                {
                    public void update(final GotGroupModelViewInformationResponseEvent event)
                    {
                        DomainGroupModelView group = event.getResponse();
                        streamName.setInnerText(group.getName());
                        // streamMeta.setInnerText(group.get);
                        streamAvatar.clear();
                        streamAvatar.add(avatarRenderer.render(group.getEntityId(), group.getAvatarId(),
                                EntityType.GROUP, Size.Normal));

                        followerCount.setInnerText(Integer.toString(group.getFollowersCount()));
                        streamDescription.setInnerText(group.getDescription());
                        String interestString = "";
                        for (String interest : group.getCapabilities())
                        {
                            interestString += "<a href='#" + interest + "'>" + interest + "</a>";
                        }
                        streamInterests.setInnerHTML(interestString);
                        streamHashtags.setInnerHTML("<a href='#something'>#something</a>");

                        streamConnections.clear();
                        // streamConnections.add(GetPersonFollowingTab.getFollowingTab(person));
                    }
                });

        EventBus.getInstance().addObserver(MessageStreamAppendEvent.class, new Observer<MessageStreamAppendEvent>()
        {
            public void update(final MessageStreamAppendEvent event)
            {
                longNewestActivityId = event.getMessage().getId();
                appendActivity(event.getMessage());

            }
        });

        Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
        {
            public boolean execute()
            {
                if (null != currentRequestObj
                        && "date".equals(currentRequestObj.get("query").isObject().get("sortBy").isString()
                                .stringValue()))
                {
                    if (Document.get().getScrollTop() < streamDetailsContainer.getAbsoluteTop())
                    {
                        JSONObject newItemsRequest = StreamJsonRequestFactory.setMinId(longNewestActivityId,
                                StreamJsonRequestFactory.getJSONRequest(currentRequestObj.toString()));

                        StreamModel.getInstance().fetch(newItemsRequest.toString(), false);
                    }
                }

                return Session.getInstance().getUrlPage().equals(Page.ACTIVITY);
            }
        }, NEW_ACTIVITY_POLLING_DELAY);

    }

    /**
     * Append a new message.
     * 
     * @param message
     *            the messa.ge
     */
    private void appendActivity(final ActivityDTO message)
    {
        Panel newActivity = renderer.render(message);
        newActivity.setVisible(false);
        streamPanel.insert(newActivity, 0);
        EffectsFacade.nativeFadeIn(newActivity.getElement(), true);
    }

    /**
     * Load a stream.
     * 
     * @param views
     *            the stream history link.
     */
    private void loadStream(final List<String> views)
    {
        boolean singleActivityMode = false;
        activitySpinner.removeClassName(StaticResourceBundle.INSTANCE.coreCss().displayNone());
        moreLink.setVisible(false);
        streamPanel.addStyleName(StaticResourceBundle.INSTANCE.coreCss().hidden());
        Session.getInstance().getActionProcessor().setQueueRequests(true);
        currentRequestObj = StreamJsonRequestFactory.getEmptyRequest();
        StreamScope currentStream = new StreamScope(ScopeType.PERSON, Session.getInstance().getCurrentPerson()
                .getAccountId());

        if (views == null || views.size() == 0 || views.get(0).equals("following"))
        {
            currentRequestObj = StreamJsonRequestFactory.setSourceAsFollowing(currentRequestObj);
            streamName.setInnerHTML("Following");
        }
        else if (views.get(0).equals("person") && views.size() >= 2)
        {
            String accountId = views.get(1);
            currentRequestObj = StreamJsonRequestFactory.addRecipient(EntityType.PERSON, accountId, currentRequestObj);
            PersonalInformationModel.getInstance().fetch(accountId, false);
            currentStream.setScopeType(ScopeType.PERSON);
            currentStream.setUniqueKey(accountId);
        }
        else if (views.get(0).equals("group") && views.size() >= 2)
        {
            String shortName = views.get(1);
            currentRequestObj = StreamJsonRequestFactory.addRecipient(EntityType.GROUP, shortName, currentRequestObj);
            GroupModel.getInstance().fetch(shortName, false);
            currentStream.setScopeType(ScopeType.GROUP);
            currentStream.setUniqueKey(shortName);
        }
        else if (views.get(0).equals("custom") && views.size() >= 3)
        {
            currentRequestObj = StreamJsonRequestFactory.getJSONRequest(views.get(2));
            streamName.setInnerHTML("Custom Stream");
            currentStream.setScopeType(null);
        }
        else if (views.get(0).equals("everyone"))
        {
            streamName.setInnerHTML("Everyone");
        }
        else if (views.size() == 1)
        {
            singleActivityMode = true;
        }

        if (!singleActivityMode)
        {
            String sortBy = "recent";

            if (views != null && views.size() >= 2 && "sort".equals(views.get(views.size() - 2)))
            {
                sortBy = views.get(views.size() - 1);
            }

            recentSort.removeStyleName(style.activeSort());
            popularSort.removeStyleName(style.activeSort());
            activeSort.removeStyleName(style.activeSort());

            String sortKeyword = "date";

            if ("recent".equals(sortBy))
            {
                recentSort.addStyleName(style.activeSort());
                sortKeyword = "date";
            }
            else if ("popular".equals(sortBy))
            {
                popularSort.addStyleName(style.activeSort());
                sortKeyword = "interesting";
            }
            else if ("active".equals(sortBy))
            {
                activeSort.addStyleName(style.activeSort());
                sortKeyword = "commentdate";
            }

            currentRequestObj = StreamJsonRequestFactory.setSort(sortKeyword, currentRequestObj);

            StreamModel.getInstance().fetch(currentRequestObj.toString(), false);
            EventBus.getInstance().notifyObservers(new PostableStreamScopeChangeEvent(currentStream));
        }
        else
        {
            ActivityModel.getInstance().fetch(Long.parseLong(views.get(0)), true);
        }

        Session.getInstance().getActionProcessor().fireQueuedRequests();
        Session.getInstance().getActionProcessor().setQueueRequests(false);
    }

    /**
     * Create LI Element for stream lists.
     * 
     * @param name
     *            the name of the item.
     * @param view
     *            the history token to load.
     * @return the LI.
     */
    private LIElement createLI(final String name, final String view)
    {
        AnchorElement aElem = Document.get().createAnchorElement();
        aElem.setInnerHTML(name);
        aElem.setHref("#" + Session.getInstance().generateUrl(new CreateUrlRequest(Page.ACTIVITY, view)));

        LIElement filterElem = Document.get().createLIElement();
        filterElem.appendChild(aElem);

        return filterElem;
    }

    /**
     * Binder for building UI.
     */
    interface LocalUiBinder extends UiBinder<Widget, ActivityContent>
    {
    }
}
