package gropius.sync

import gropius.model.architecture.IMSProject
import org.slf4j.LoggerFactory

/**
 * Budget for a resource walker
 */
interface LoadBalancedDataFetcherImplementation {
    /**
     * Create a new budget
     * @return the budget
     */
    suspend fun createBudget(): GeneralResourceWalkerBudget;

    /**
     * Fetch data for a specific project
     * @param imsProject the project to fetch data for
     * @param generalBudget the budget to use
     * @return the walkers to execute
     */
    open suspend fun balancedFetchData(
        imsProject: IMSProject, generalBudget: GeneralResourceWalkerBudget
    ): List<ResourceWalker>;
}

/**
 * Fetches data from the GitHub API
 */
class LoadBalancedDataFetcher() : DataFetcher {

    /**
     * The implementation to use
     */
    var rawImplementation: LoadBalancedDataFetcherImplementation? = null

    /**
     * The implementation to use
     */
    val implementation get() = rawImplementation!!;

    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(LoadBalancedDataFetcher::class.java)

    /**
     * Start the data fetcher
     * @param implementation the implementation to use
     */
    fun start(implementation: LoadBalancedDataFetcherImplementation) {
        rawImplementation = implementation
    }

    /**
     * Fetch data for a list of projects
     * @param imsProjects the projects to fetch data for
     */
    override suspend fun fetchData(imsProjects: List<IMSProject>) {
        val budget = implementation.createBudget()
        val walkerPairs = mutableListOf<Pair<Double, Pair<ResourceWalker, IMSProject>>>()
        for (imsProject in imsProjects) {
            logger.info("Collecting walkers for ${imsProject.rawId!!}")
            for (walker in implementation.balancedFetchData(imsProject, budget)) {
                walkerPairs += walker.getPriority() to (walker to imsProject)
            }
            logger.info("Collected walkers for ${imsProject.rawId!!}")
        }
        val walkers = walkerPairs.sortedBy { it.first }.map { it.second }
        for ((walker, imsProject) in walkers) {
            logger.info("Executing walker for ${imsProject.rawId!!}")
            walker.process()
            logger.info("Executed walker for ${imsProject.rawId!!}")
        }
    }
}