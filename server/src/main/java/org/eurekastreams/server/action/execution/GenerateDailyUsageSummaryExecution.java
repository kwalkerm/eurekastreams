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
package org.eurekastreams.server.action.execution;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.eurekastreams.commons.actions.TaskHandlerExecutionStrategy;
import org.eurekastreams.commons.actions.context.ActionContext;
import org.eurekastreams.commons.actions.context.TaskHandlerActionContext;
import org.eurekastreams.commons.date.DateDayExtractor;
import org.eurekastreams.commons.date.DayOfWeekStrategy;
import org.eurekastreams.commons.date.GetDateFromDaysAgoStrategy;
import org.eurekastreams.commons.exceptions.ExecutionException;
import org.eurekastreams.commons.logging.LogFactory;
import org.eurekastreams.server.domain.DailyUsageSummary;
import org.eurekastreams.server.persistence.mappers.DomainMapper;
import org.eurekastreams.server.persistence.mappers.requests.PersistenceRequest;
import org.eurekastreams.server.service.actions.requests.UsageMetricDailyStreamInfoRequest;

/**
 * Execution strategy to generate the daily usage summary for the previous day.
 */
public class GenerateDailyUsageSummaryExecution implements TaskHandlerExecutionStrategy<ActionContext>
{
    /**
     * Logger.
     */
    private Log logger = LogFactory.make();

    /**
     * strategy to get a date from N days ago.
     */
    private GetDateFromDaysAgoStrategy daysAgoDateStrategy;

    /**
     * Number of days of data to generate.
     */
    private int daysToGenerate;

    // mappers that apply to the whole system

    /**
     * Mapper to get a day's page view count - for the whole system.
     */
    private DomainMapper<Date, Long> getDailyPageViewCountMapper;

    /**
     * Mapper to get a day's unique visitor count - for whole system.
     */
    private DomainMapper<Date, Long> getDailyUniqueVisitorCountMapper;

    // mappers that may be scoped to a particular thread

    /**
     * Mapper to get a single day's DailyUsageSummary - for a stream or the whole system.
     */
    private DomainMapper<UsageMetricDailyStreamInfoRequest, DailyUsageSummary> getDailyUsageSummaryByDateMapper;

    /**
     * Mapper to get a day's message count - for a stream or the whole system.
     */
    private DomainMapper<UsageMetricDailyStreamInfoRequest, Long> getDailyMessageCountMapper;

    /**
     * Mapper to get a day's stream contributor count - for a stream or the whole system.
     */
    private DomainMapper<UsageMetricDailyStreamInfoRequest, Long> getDailyStreamContributorCountMapper;

    /**
     * Mapper to get a day's stream view count - for a stream or the whole system.
     */
    private DomainMapper<UsageMetricDailyStreamInfoRequest, Long> getDailyStreamViewCountMapper;

    /**
     * Mapper to get a day's stream viewer count - for a stream or the whole system.
     */
    private DomainMapper<UsageMetricDailyStreamInfoRequest, Long> getDailyStreamViewerCountMapper;

    /**
     * Mapper to get the total number of activities posted to a stream.
     */
    private DomainMapper<Long, Long> getTotalActivityCountMapper;

    /**
     * Mapper to get the total number of comments posted to a stream.
     */
    private DomainMapper<Long, Long> getTotalCommentCountMapper;

    /**
     * Mapper to get the total number of contributors to a stream by stream scope id.
     */
    private DomainMapper<Long, Long> getTotalStreamContributorMapper;

    /**
     * Mapper to get day's average activity response time (for those that had responses) - for a stream or the whole
     * system.
     */
    private DomainMapper<Date, Long> getDailyMessageResponseTimeMapper;

    // helpers

    /**
     * Mapper to get all the ids for the stream scopes to generate data for.
     */
    private DomainMapper<Date, List<Long>> streamScopeIdsMapper;

    /**
     * Mapper to delete old UsageMetric data.
     */
    private DomainMapper<Serializable, Serializable> usageMetricDataCleanupMapper;

    /**
     * Mapper to insert the DailyUsageSummary entity.
     */
    private DomainMapper<PersistenceRequest<DailyUsageSummary>, Boolean> insertMapper;

    /**
     * Strategy to determine if a day is a weekday.
     */
    private DayOfWeekStrategy dayOfWeekStrategy;

    /**
     * Constructor.
     * 
     * @param inDaysToGenerate
     *            the number of days to generate data for
     * @param inDaysAgoDateStrategy
     *            strategy to get a date from yesterday
     * @param inGetDailyUsageSummaryByDateMapper
     *            Mapper to get a single day's DailyUsageSummary
     * @param inGetDailyMessageCountMapper
     *            Mapper to get a day's message count
     * @param inGetDailyPageViewCountMapper
     *            Mapper to get a day's page view count.
     * @param inGetDailyStreamContributorCountMapper
     *            Mapper to get a day's stream contributor count.
     * @param inGetDailyStreamViewCountMapper
     *            Mapper to get a day's stream view count.
     * @param inGetDailyStreamViewerCountMapper
     *            Mapper to get a day's stream viewer count.
     * @param inGetDailyUniqueVisitorCountMapper
     *            Mapper to get a day's unique visitor count.
     * @param inGetDailyMessageResponseTimeMapper
     *            Mapper to get day's average activity response time (for those that had responses).
     * @param inInsertMapper
     *            mapper to insert DailyUsageSummary
     * @param inUsageMetricDataCleanupMapper
     *            mapper to delete old UsageMetric data
     * @param inDayOfWeekStrategy
     *            dayOfWeekStrategy strategy to determine if a day is a weekday
     * @param inStreamScopeIdsMapper
     *            mapper to get all the ids of the stream scopes to generate data for
     * @param inGetTotalActivityCountMapper
     *            mapper to get the total activities for a stream
     * @param inGetTotalCommentCountMapper
     *            mapper to get the total comments for a stream
     * @param inGetTotalStreamContributorMapper
     *            mapper to get the total number of contributors to a stream
     */
    public GenerateDailyUsageSummaryExecution(
            final int inDaysToGenerate,
            final GetDateFromDaysAgoStrategy inDaysAgoDateStrategy,
            final DomainMapper<UsageMetricDailyStreamInfoRequest, DailyUsageSummary> inGetDailyUsageSummaryByDateMapper,
            final DomainMapper<UsageMetricDailyStreamInfoRequest, Long> inGetDailyMessageCountMapper,
            final DomainMapper<Date, Long> inGetDailyPageViewCountMapper,
            final DomainMapper<UsageMetricDailyStreamInfoRequest, Long> inGetDailyStreamContributorCountMapper,
            final DomainMapper<UsageMetricDailyStreamInfoRequest, Long> inGetDailyStreamViewCountMapper,
            final DomainMapper<UsageMetricDailyStreamInfoRequest, Long> inGetDailyStreamViewerCountMapper,
            final DomainMapper<Date, Long> inGetDailyUniqueVisitorCountMapper,
            final DomainMapper<Date, Long> inGetDailyMessageResponseTimeMapper,
            final DomainMapper<PersistenceRequest<DailyUsageSummary>, Boolean> inInsertMapper,
            final DomainMapper<Serializable, Serializable> inUsageMetricDataCleanupMapper,
            final DayOfWeekStrategy inDayOfWeekStrategy, final DomainMapper<Date, List<Long>> inStreamScopeIdsMapper,
            final DomainMapper<Long, Long> inGetTotalActivityCountMapper,
            final DomainMapper<Long, Long> inGetTotalCommentCountMapper,
            final DomainMapper<Long, Long> inGetTotalStreamContributorMapper)
    {
        daysToGenerate = inDaysToGenerate;
        daysAgoDateStrategy = inDaysAgoDateStrategy;
        getDailyUsageSummaryByDateMapper = inGetDailyUsageSummaryByDateMapper;
        getDailyMessageCountMapper = inGetDailyMessageCountMapper;
        getDailyPageViewCountMapper = inGetDailyPageViewCountMapper;
        getDailyStreamContributorCountMapper = inGetDailyStreamContributorCountMapper;
        getDailyStreamViewCountMapper = inGetDailyStreamViewCountMapper;
        getDailyStreamViewerCountMapper = inGetDailyStreamViewerCountMapper;
        getDailyUniqueVisitorCountMapper = inGetDailyUniqueVisitorCountMapper;
        getDailyMessageResponseTimeMapper = inGetDailyMessageResponseTimeMapper;
        insertMapper = inInsertMapper;
        usageMetricDataCleanupMapper = inUsageMetricDataCleanupMapper;
        dayOfWeekStrategy = inDayOfWeekStrategy;
        streamScopeIdsMapper = inStreamScopeIdsMapper;
        getTotalActivityCountMapper = inGetTotalActivityCountMapper;
        getTotalCommentCountMapper = inGetTotalCommentCountMapper;
        getTotalStreamContributorMapper = inGetTotalStreamContributorMapper;
    }

    /**
     * Generate the daily usage summary for the previous day.
     * 
     * @param inActionContext
     *            the action context
     * @return true if data was inserted, false if already existed
     * @throws ExecutionException
     *             when something really, really bad happens
     */
    @Override
    public Serializable execute(final TaskHandlerActionContext<ActionContext> inActionContext)
            throws ExecutionException
    {
        for (int i = daysToGenerate; i >= 1; i--)
        {
            logger.info("Generating metric data for " + i + " days ago.");
            generateDailyUsageSummaryForDay(i);
        }

        // delete old data
        logger.info("Deleting old daily usage metric data older than 2 days");
        usageMetricDataCleanupMapper.execute(null);

        return Boolean.TRUE;
    }

    /**
     * Generate the daily usage summary for all streams for a specific day.
     * 
     * @param inDaysAgo
     *            the number of days ago to generate data for
     */
    private void generateDailyUsageSummaryForDay(final int inDaysAgo)
    {
        Date reportDate = DateDayExtractor.getStartOfDay(daysAgoDateStrategy.execute(inDaysAgo));
        Date priorDate = DateDayExtractor.getStartOfDay(daysAgoDateStrategy.execute(inDaysAgo + 1));

        logger.info("Checking id daily usage summary data exists for " + reportDate);
        DailyUsageSummary existingSummary = getDailyUsageSummaryByDateMapper
                .execute(new UsageMetricDailyStreamInfoRequest(reportDate, null));

        if (existingSummary != null)
        {
            logger.info("Data already exists for " + reportDate);
            return;
        }

        // generate data for all streams
        generateDailyUsageSummaryForStreamScope(reportDate, priorDate, null);

        // now generate data for each stream
        List<Long> streamScopeIds = streamScopeIdsMapper.execute(reportDate);
        for (Long streamScopeId : streamScopeIds)
        {
            generateDailyUsageSummaryForStreamScope(reportDate, priorDate, streamScopeId);
        }
        logger.info("Inserted Daily Summary metrics for " + reportDate);
    }

    /**
     * Generate the daily usage summary for the stream scope with the input id, and for the given date.
     * 
     * @param inDate
     *            the date to generate for
     * @param inPriorDate
     *            the date of the day before inDate
     * @param inStreamScopeId
     *            the streamscope id to generate stats for
     */
    private void generateDailyUsageSummaryForStreamScope(final Date inDate, final Date inPriorDate,
            final Long inStreamScopeId)
    {
        UsageMetricDailyStreamInfoRequest streamInfoRequest = new UsageMetricDailyStreamInfoRequest(inDate,
                inStreamScopeId);

        long uniqueVisitorCount = 0;
        long pageViewCount = 0;
        long streamViewCount = 0;
        long streamViewerCount = 0;
        long streamContributorCount = 0;
        long messageCount = 0;
        long avgActvityResponeTime = 0;

        Long totalStreamViewCount = null;
        Long totalActivityCount = null;
        Long totalCommentCount = null;
        Long totalContributorCount = null;

        // get the stream view count -
        logger.info("Generating number of stream views for " + inDate);
        streamViewCount = getDailyStreamViewCountMapper.execute(streamInfoRequest);

        if (inStreamScopeId == null)
        {
            // doesn't make sense on a per-stream basis
            logger.info("Generating number of unique visitors for " + inDate);
            uniqueVisitorCount = getDailyUniqueVisitorCountMapper.execute(inDate);

            logger.info("Generating number of page views for " + inDate);
            pageViewCount = getDailyPageViewCountMapper.execute(inDate);

            logger.info("Generating average activity comment time (for those with comments on the same day) for "
                    + inDate);
            avgActvityResponeTime = getDailyMessageResponseTimeMapper.execute(inDate);
        }
        else
        {
            // these are only generated for individual streams
            totalActivityCount = getTotalActivityCountMapper.execute(inStreamScopeId);
            totalCommentCount = getTotalCommentCountMapper.execute(inStreamScopeId);
            totalContributorCount = getTotalStreamContributorMapper.execute(inStreamScopeId);

            DailyUsageSummary priorDayData = getDailyUsageSummaryByDateMapper
                    .execute(new UsageMetricDailyStreamInfoRequest(inPriorDate, inStreamScopeId));

            if (priorDayData != null)
            {
                totalStreamViewCount = priorDayData.getTotalStreamViewCount() + streamViewCount;
            }
            else
            {
                totalStreamViewCount = streamViewCount;
            }
        }

        logger.info("Generating number of stream viewers for " + inDate);
        streamViewerCount = getDailyStreamViewerCountMapper.execute(streamInfoRequest);

        logger.info("Generating number of stream contributors for " + inDate);
        streamContributorCount = getDailyStreamContributorCountMapper.execute(streamInfoRequest);

        logger.info("Generating number of messages (activities and comments) for " + inDate);
        messageCount = getDailyMessageCountMapper.execute(streamInfoRequest);

        boolean isWeekday = dayOfWeekStrategy.isWeekday(inDate);

        DailyUsageSummary data = new DailyUsageSummary(uniqueVisitorCount, pageViewCount, streamViewerCount,
                streamViewCount, streamContributorCount, messageCount, avgActvityResponeTime, inDate, isWeekday,
                inStreamScopeId, totalActivityCount, totalCommentCount, totalStreamViewCount, totalContributorCount);

        // store this
        logger.info("Inserting daily usage metric data for " + inDate);
        insertMapper.execute(new PersistenceRequest<DailyUsageSummary>(data));
    }
}
