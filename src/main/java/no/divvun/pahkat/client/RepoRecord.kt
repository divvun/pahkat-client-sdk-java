package no.divvun.pahkat.client

import java.net.URL

data class RepoRecord(
    val url: URL,
    val channel: Repository.Channel
)