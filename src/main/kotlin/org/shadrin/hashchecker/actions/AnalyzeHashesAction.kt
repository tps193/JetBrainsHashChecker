package org.shadrin.hashchecker.actions

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.shadrin.hashchecker.SettingsProvider
import org.shadrin.hashchecker.execution.ChecksumUpdater
import org.shadrin.hashchecker.model.json.ArtifactIdList
import org.shadrin.hashchecker.model.json.ArtifactChecksumList
import java.io.IOException
import java.util.logging.Logger

//https://github.com/JetBrains/intellij-community/tree/idea/222.4345.14/plugins/gradle/java/src

class AnalyzeHashesAction : AnAction() {

    private val logger = Logger.getLogger(this::class.java.name)

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            ProgressManager.getInstance().run(ChecksumUpdater(it))
        }
    }

    private fun retrieveChecksumsFromServer(externalNames: Collection<String>): Map<String,String> {
        val client = OkHttpClient()
        val artifacts = ArtifactIdList(externalNames.toList())
        val json = Json.encodeToString(artifacts) //ObjectMapper().writeValueAsString(artifacts) //Json.encodeToString(artifacts)
        val body = RequestBody.create(
            MediaType.parse("application/json"), json
        )
        val host = SettingsProvider.SERVER_URL.trimEnd('/')
        val request = Request.Builder()
            .url("$host/checksums")
            .post(body)
            .build()
        val call = client.newCall(request)
        try {
            val response = call.execute()
            if (response?.isSuccessful == true) {
                response.body()?.let { body ->
                    try {
                        val checksums = Json.decodeFromString<ArtifactChecksumList>(body.string())
                        return checksums.result.map { it.identifier to it.checksum }.toMap()
                    } catch (e: JsonProcessingException) {
                        e.printStackTrace()
                    } catch (e: JsonMappingException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            //TODO: process
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mapOf()
    }
}