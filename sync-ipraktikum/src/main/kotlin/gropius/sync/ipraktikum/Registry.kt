package gropius.sync.ipraktikum

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.neo4j.core.ReactiveNeo4jClient
import org.springframework.stereotype.Component

/** Canonical identities, receipts and outbox live in Neo4j alongside domain transactions. */
@Component
class Registry(private val client: ReactiveNeo4jClient) {
    val owner: String = UUID.randomUUID().toString()

    suspend fun initialize() {
        client
            .query(
                "CREATE CONSTRAINT ipraktikum_record_key IF NOT EXISTS FOR (n:IPraktikumRecord) REQUIRE n.key IS UNIQUE"
            )
            .run()
            .awaitSingle()
    }

    suspend fun get(key: String): String? =
        client
            .query("MATCH (n:IPraktikumRecord {key: \$key}) RETURN n.json AS json")
            .bind(key)
            .to("key")
            .fetch()
            .one()
            .awaitSingleOrNull()
            ?.get("json") as? String

    suspend fun put(key: String, json: String) {
        client
            .query("MERGE (n:IPraktikumRecord {key: \$key}) SET n.json = \$json")
            .bind(key)
            .to("key")
            .bind(json)
            .to("json")
            .run()
            .awaitSingle()
    }

    suspend fun claim(key: String, value: String) {
        val result =
            client
                .query(
                    "MERGE (n:IPraktikumRecord {key: \$key}) ON CREATE SET n.json = \$value RETURN n.json AS value"
                )
                .bind(key)
                .to("key")
                .bind(value)
                .to("value")
                .fetch()
                .one()
                .awaitSingle()
        require(result["value"] == value) { "Identity already belongs to another binding or issue" }
    }

    suspend fun lock(key: String) {
        client
            .query("MERGE (n:IPraktikumRecord {key: \$key}) SET n.lock = coalesce(n.lock, 0) + 1")
            .bind("lock:$key")
            .to("key")
            .run()
            .awaitSingle()
    }

    suspend fun records(binding: String): List<IssueRecord> =
        client
            .query(
                "MATCH (n:IPraktikumRecord) WHERE n.key STARTS WITH \$prefix RETURN n.json AS json"
            )
            .bind("issue:$binding:")
            .to("prefix")
            .fetch()
            .all()
            .collectList()
            .awaitSingle()
            .map { syncJson.decodeFromString<IssueRecord>(it["json"] as String) }

    suspend fun save(record: IssueRecord) =
        put("issue:${record.binding}:${record.syncId}", syncJson.encodeToString(record))

    suspend fun record(binding: String, id: String) =
        get("issue:$binding:$id")?.let { syncJson.decodeFromString<IssueRecord>(it) }

    suspend fun lease(binding: String): Boolean {
        val now = Instant.now().toEpochMilli()
        return client
            .query(
                """
                MERGE (n:IPraktikumRecord {key: ${'$'}key})
                ON CREATE SET n.expires = 0
                WITH n WHERE n.owner = ${'$'}owner OR n.expires < ${'$'}now
                SET n.owner = ${'$'}owner, n.expires = ${'$'}expires RETURN n.owner AS owner
                """
                    .trimIndent()
            )
            .bind("lease:$binding")
            .to("key")
            .bind(owner)
            .to("owner")
            .bind(now)
            .to("now")
            .bind(now + 300_000)
            .to("expires")
            .fetch()
            .one()
            .awaitSingleOrNull() != null
    }

    suspend fun fence(binding: String) {
        val row =
            client
                .query(
                    """
                    MATCH (n:IPraktikumRecord {key: ${'$'}key})
                    WHERE n.owner = ${'$'}owner AND n.expires > ${'$'}now
                    SET n.expires = ${'$'}expires RETURN n.owner AS owner
                    """
                        .trimIndent()
                )
                .bind("lease:$binding")
                .to("key")
                .bind(owner)
                .to("owner")
                .bind(Instant.now().toEpochMilli())
                .to("now")
                .bind(Instant.now().toEpochMilli() + 300_000)
                .to("expires")
                .fetch()
                .one()
                .awaitSingleOrNull()
        check(row != null) { "Worker lease lost" }
    }

    suspend fun release(binding: String) {
        client
            .query(
                "MATCH (n:IPraktikumRecord {key: \$key}) WHERE n.owner = \$owner SET n.expires = 0"
            )
            .bind("lease:$binding")
            .to("key")
            .bind(owner)
            .to("owner")
            .run()
            .awaitSingle()
    }

    suspend fun key(kind: String, id: String, name: String): String {
        val record = "key:$kind:$id"
        get(record)?.let {
            return it
        }
        val generated = stableKey(kind, id, name)
        claim(record, generated)
        return generated
    }
}
