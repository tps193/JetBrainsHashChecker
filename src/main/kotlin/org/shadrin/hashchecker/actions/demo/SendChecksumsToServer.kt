package org.shadrin.hashchecker.actions.demo

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.project.LibraryPathType
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.ui.Messages
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.shadrin.hashchecker.SettingsProvider
import org.shadrin.hashchecker.extensions.calculateChecksum
import org.shadrin.hashchecker.extensions.toTrimmerUrl
import org.shadrin.hashchecker.model.json.ArtifactChecksum
import org.shadrin.hashchecker.model.json.ArtifactChecksumList
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

/*
XXX: demo action for test purposes
Just copied-pasted from ChecksumUpdater class
 */
class SendChecksumsToServer : AnAction() {

    private val logger = Logger.getLogger(this::class.java.name)

    override fun actionPerformed(e: AnActionEvent) {
        val selection = Messages.showYesNoDialog(
            "Do you really want to upload your local artifact checksums to server?",
            "Sync Checksums",
            AllIcons.General.BalloonWarning
        )
        if (selection != Messages.YES) {
            return
        }

        val project = e.project!!
        // Q: Is the current usage reasonable and safe memory-wise?
        // A: No, because this class was added just for test purposes. I've not paid any attention to code quality for it.
        // Let me redesign it a little bit (as well as server).
        runBackgroundableTask("Send checksums to server", project, false) {
            val projectData = ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
            val artifactChecksums = mutableSetOf<ArtifactChecksum>()
            projectData.forEach { projectInfo ->
                projectInfo.externalProjectStructure?.children
                    ?.filter { it.key.dataType == LibraryData::class.qualifiedName }
                    ?.forEach {
                        // Q: If you would want to optimize this part, how would you change it? It may involve change of a server either.
                        // A: I will just change it to show possible optimization :)
                        val data = it.data
                        if (data is LibraryData) {
                            if (!data.isUnresolved) {
                                val artifactId =
                                    "${data.groupId ?: ""}:${data.artifactId ?: ""}:${data.version ?: ""}"
                                data.getPaths(LibraryPathType.BINARY).first().let { path ->
                                    try {
                                        File(path).calculateChecksum().also { checksum ->
                                            artifactChecksums.add(ArtifactChecksum(artifactId, checksum))
                                        }
                                    } catch (e: IOException) {
                                        logger.log(Level.WARNING, "Can't calculate checksum for $path", e)
                                    }
                                }
                            }
                        }
                    }
            }
            val json = Json.encodeToString(ArtifactChecksumList(artifactChecksums.toList()))
            val body = RequestBody.create(
                MediaType.parse("application/json"), json
            )
            val host = SettingsProvider.SERVER_URL.toTrimmerUrl()
            val request = Request.Builder()
                .url("$host/checksums/add")
                .post(body)
                .build()
            val client = OkHttpClient()
            client.newCall(request).execute()
        }
    }
}