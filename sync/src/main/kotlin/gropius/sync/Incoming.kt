package gropius.sync

import gropius.model.architecture.IMSProject
import gropius.model.issue.Issue
import gropius.model.issue.timeline.TimelineItem

abstract class IncomingTimelineItem() {
    abstract suspend fun gropiusTimelineItem(
        imsProject: IMSProject,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<List<TimelineItem>, TimelineItemConversionInformation>;

    abstract suspend fun identification(): String;
}

abstract class IncomingIssue() {
    abstract suspend fun incomingTimelineItems(service: SyncDataService): List<IncomingTimelineItem>
    abstract suspend fun identification(): String;

    abstract suspend fun markDone(service: SyncDataService)

    abstract suspend fun createIssue(imsProject: IMSProject, service: SyncDataService): Issue
}

class Incoming {}