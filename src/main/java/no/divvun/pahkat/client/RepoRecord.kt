package no.divvun.pahkat.client

import java.net.URI

data class RepoRecord(
    val url: URI,
    val channel: Repository.Channel
)