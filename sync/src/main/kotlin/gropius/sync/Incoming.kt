package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.TimelineItem

/**
 * This class is used to store information about the conversion of a timeline item.
 */
abstract class IncomingTimelineItem() {

    /**
     * This function converts the timeline item to a gropius timeline item.
     * @param imsProject The project the timeline item belongs to.
     * @param service The service used to sync the data.
     * @param timelineItemConversionInformation The information about the conversion of the timeline item.
     * @return The gropius timeline item and the information about the conversion of the timeline item.
     */
    abstract suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?,
        issue: Issue
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation>;

    /**
     * This function returns the identification of the timeline item.
     * @return The identification of the timeline item.
     */
    abstract suspend fun identification(): String;
}

/**
 * This class is used to store information about the conversion of an issue.
 */
abstract class IncomingIssue() {

    /**
     * This function converts the issue to a gropius issue.
     * @param imsProject The project the issue belongs to.
     * @param service The service used to sync the data.
     * @param issueConversionInformation The information about the conversion of the issue.
     * @return The gropius issue and the information about the conversion of the issue.
     */
    abstract suspend fun incomingTimelineItems(service: SyncDataService): List<IncomingTimelineItem>

    /**
     * This function returns the identification of the issue
     * @return The identification of the issue.
     */
    abstract suspend fun identification(): String;

    /**
     * This function marks the issue as done.
     * @param service The service used to sync the data.
     * @return The gropius issue.
     */
    abstract suspend fun markDone(service: SyncDataService)

    /**
     * This function creates the gropius issue.
     * @param imsProject The project the issue belongs to.
     * @param service The service used to sync the data.
     * @return The gropius issue.
     */
    abstract suspend fun createIssue(imsProject: IMSProject, service: SyncDataService): Issue

    /**
     * This function sets the templated fields on an IMSIssue.
     * @param service The service used to sync the data.
     */
    abstract suspend fun fillImsIssueTemplatedFields(
        templatedFields: MutableMap<String, String>, service: SyncDataService
    )
}
