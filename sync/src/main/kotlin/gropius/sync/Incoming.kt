package gropius.sync

import gropius.model.issue.Issue
import gropius.model.issue.timeline.TimelineItem

abstract class IncomingTimelineItem() {
    abstract suspend fun gropiusTimelineItem(
        imsProject: String,
        service: SyncDataService,
        timelineItemConversionInformation: TimelineItemConversionInformation?
    ): Pair<TimelineItem?, TimelineItemConversionInformation>;

    abstract suspend fun identification(): String;
}

abstract class IncomingIssue() {
    abstract suspend fun incomingTimelineItems(): List<IncomingTimelineItem>
    abstract suspend fun identification(): String;

    abstract suspend fun markDone(service: SyncDataService)

    abstract suspend fun createIssue(): Issue
}

class Incoming {}