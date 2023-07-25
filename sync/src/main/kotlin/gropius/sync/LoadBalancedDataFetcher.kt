package gropius.sync

import gropius.model.architecture.IMSProject
import org.slf4j.LoggerFactory

interface LoadBalancedDataFetcherImplementation {
    suspend fun createBudget(): GeneralResourceWalkerBudget;
    open suspend fun balancedFetchData(
        imsProject: IMSProject, generalBudget: GeneralResourceWalkerBudget
    ): List<ResourceWalker>;
}

class LoadBalancedDataFetcher() : DataFetcher {
    var rawImplementation: LoadBalancedDataFetcherImplementation? = null

    val implementation get() = rawImplementation!!;

    /**
     * Logger used to print notifications
     */
    private val logger = LoggerFactory.getLogger(LoadBalancedDataFetcher::class.java)

    fun start(implementation: LoadBalancedDataFetcherImplementation) {
        rawImplementation = implementation
    }

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