package gropius.sync.github

import gropius.sync.SyncDataService
import gropius.sync.user.UserMapper
import org.springframework.stereotype.Component

@Component
class GithubDataService(val issuePileService: IssuePileService, val userMapper: UserMapper) : SyncDataService {}