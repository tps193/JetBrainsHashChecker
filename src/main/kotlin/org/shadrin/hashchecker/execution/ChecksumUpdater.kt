package org.shadrin.hashchecker.execution

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.project.LibraryPathType
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.shadrin.hashchecker.SettingsProvider
import org.shadrin.hashchecker.data.ChecksumCacheService
import org.shadrin.hashchecker.extensions.calculateChecksum
import org.shadrin.hashchecker.extensions.toTrimmerUrl
import org.shadrin.hashchecker.listener.ChecksumUpdateListener
import org.shadrin.hashchecker.model.ChecksumComparison
import org.shadrin.hashchecker.model.ChecksumComparisonStatus
import org.shadrin.hashchecker.model.json.ArtifactChecksum
import org.shadrin.hashchecker.model.json.ArtifactChecksumList
import org.shadrin.hashchecker.model.json.ArtifactIdList
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

class ChecksumUpdaterProjectImportListener(private val project: Project) : ProjectDataImportListener {
    override fun onImportFinished(projectPath: String?) {
        ProgressManager.getInstance().run(ChecksumUpdater(project))
    }
}

class ChecksumUpdater(project: Project) : Task.Backgroundable(
    project,
    "Analyze artifacts",
    false
) {
    private val logger = Logger.getLogger(this::class.java.name)

    override fun run(indicator: ProgressIndicator) {
        val artifactIds = mutableSetOf<String>()
        val checksumComparisonResult = mutableListOf<ChecksumComparison>()
        val projectData = ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
        indicator.text = "Collect artifact identifiers"
        projectData.forEach { projectInfo ->
            projectInfo.externalProjectStructure?.children
                ?.filter { it.key.dataType == LibraryData::class.qualifiedName }
                ?.forEach {
                    val data = it.data
                    if (data is LibraryData) {
                        if (!data.isUnresolved) {
                            val artifactId = "${data.groupId ?: ""}:${data.artifactId ?: ""}:${data.version ?: ""}"
                            artifactIds.add(artifactId)
                            // TODO: Why taking first of the paths?
                            data.getPaths(LibraryPathType.BINARY).first().let { path ->
                                try {
                                        File(path).calculateChecksum().also { checksum ->
                                            checksumComparisonResult.add(
                                                ChecksumComparison(
                                                artifactId = artifactId,
                                                localChecksum = checksum
                                            ))
                                    }
                                } catch (e: IOException) {
                                    logger.log(Level.WARNING, "Can't calculate checksum for $path", e)
                                    checksumComparisonResult.add(ChecksumComparison(
                                        artifactId = artifactId,
                                        status = ChecksumComparisonStatus.Skipped("Error getting file checksum")
                                    ))
                                }
                            }

                        }
                    }
                }
        }

        setCachedServerChecksums(checksumComparisonResult)

        indicator.text = "Retrieve artifacts checksum"
        setServerChecksums(checksumComparisonResult)

        indicator.text = "Analyze artifacts checksum"
        verifyChecksum(checksumComparisonResult)
    }

    private fun setServerChecksums(checksumComparisonResult: MutableList<ChecksumComparison>) {
        val itemsForUpdate = checksumComparisonResult
            .filter { it.status == ChecksumComparisonStatus.UNKNOWN }
            .filter { it.serverChecksum == null }.toList()

        val serverChecksumMap = getChunkSizeFromServer()?.let { chunk ->
            val serverChecksums = itemsForUpdate.map { it.artifactId }
                .chunked(chunk)
                .map { retrieveChecksumsFromServer(it) }
                .fold(listOf<ArtifactChecksum>()) { a, b -> a.toMutableList() + b }
                .toList()
            project.getService(ChecksumCacheService::class.java).put(serverChecksums)
            serverChecksums
        }?.associate { it.identifier to it.checksum }

        itemsForUpdate.forEach {
            it.serverChecksum = serverChecksumMap?.get(it.artifactId)
            if (it.serverChecksum == null) {
                it.status = ChecksumComparisonStatus.Skipped("No checksum retrieved from server")
            }
        }
    }

    private fun setCachedServerChecksums(checksumComparisonResult: MutableList<ChecksumComparison>) {
        val cachedChecksumService = project.getService(ChecksumCacheService::class.java)
        checksumComparisonResult.filter { it.status == ChecksumComparisonStatus.UNKNOWN }
            .forEach { comparison ->
                comparison.serverChecksum = cachedChecksumService.get(comparison.artifactId)?.checksum
            }
    }

    private fun verifyChecksum(checksumComparisonResult: List<ChecksumComparison>) {
        checksumComparisonResult.forEach { result ->
            if (result.status == ChecksumComparisonStatus.UNKNOWN) {
                    if (result.serverChecksum.equals(result.localChecksum, true)) {
                    result.status = ChecksumComparisonStatus.OK
                } else {
                    result.status = ChecksumComparisonStatus.Error("Incorrect artifact file checksum")
                }
            }
        }
        project.messageBus.syncPublisher(ChecksumUpdateListener.TOPIC).notify(checksumComparisonResult)
    }

    private fun getChunkSizeFromServer(): Int? {
        val client = OkHttpClient()
        val host = SettingsProvider.SERVER_URL.toTrimmerUrl()
        val request = Request.Builder()
            .url("$host/checksums/chunkSize")
            .get()
            .build()
        val call = client.newCall(request)
        try {
            val response = call.execute().body().string()
            return response.toInt()
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Error getting chunk size from server", e)
        } catch (e: NumberFormatException) {
            logger.log(Level.WARNING, "Server returned incorrect chunk size", e)
        }
        return null
    }

    private fun retrieveChecksumsFromServer(identifiers: List<String>): List<ArtifactChecksum> {
        val client = OkHttpClient()
        val artifacts = ArtifactIdList(identifiers.toList())
        val json = Json.encodeToString(artifacts)
        val body = RequestBody.create(
            MediaType.parse("application/json"), json
        )
        val host = SettingsProvider.SERVER_URL.toTrimmerUrl()
        val request = Request.Builder()
            .url("$host/checksums")
            .post(body)
            .build()
        val call = client.newCall(request)
        try {
            // TODO: Is it intended to use blocking API? Please suggest an alternative async approach would make more sense.
            val response = call.execute()
            if (response?.isSuccessful == true) {
                response.body()?.let { body -> // TODO: this `body` shadows the `body` above
                    try {
                        return Json.decodeFromString<ArtifactChecksumList>(body.string()).result
                    } catch (e: JsonProcessingException) {
                        logger.log(Level.WARNING, "Error creating request to server", e)
                    } catch (e: JsonMappingException) {
                        logger.log(Level.WARNING, "Error creating request to server", e)
                    }
                }
            } else {
                logger.warning("Error getting checksums from server")
            }
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Error getting checksums from server", e)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error getting checksums from server", e)
        }
        return listOf()
    }
}