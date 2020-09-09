// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.sqs.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.table.TableView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.Message
import software.aws.toolkits.core.utils.error
import software.aws.toolkits.core.utils.getLogger
import software.aws.toolkits.core.utils.info
import software.aws.toolkits.core.utils.warn
import software.aws.toolkits.jetbrains.utils.ApplicationThreadPoolScope
import software.aws.toolkits.jetbrains.utils.notifyError
import software.aws.toolkits.jetbrains.utils.notifyInfo
import software.aws.toolkits.resources.message

class DeleteMessageAction(
    private val project: Project,
    private val client: SqsClient,
    private val table: TableView<Message>,
    private val queueUrl: String
) : CoroutineScope by ApplicationThreadPoolScope("DeleteSQSMessageAction"),
    DumbAwareAction(message("sqs.delete.message.action", 1), null, AllIcons.Actions.Cancel) {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = table.selectedObjects.size > 0
        e.presentation.text = message("sqs.delete.message.action", table.selectedObjects.size)
    }

    override fun actionPerformed(e: AnActionEvent) {
        // get an immutable view of the selected items
        val messages = table.selectedObjects.toList()
        if (messages.isEmpty()) {
            return
        }
        launch {
            try {
                val requestEntries = messages.mapIndexed { index, item ->
                    // ID must be unique per request. Since we don't need to use it, just use index (other fields can be too long)
                    DeleteMessageBatchRequestEntry.builder().id(index.toString()).receiptHandle(item.receiptHandle()).build()
                }
                val response = client.deleteMessageBatch { it.queueUrl(queueUrl).entries(requestEntries) }
                // Remove all of the selected rows
                messages.forEach { _ ->
                    val selected = table.selectedRow
                    if (selected >= 0) {
                        table.listTableModel.removeRow(selected)
                    }
                }
                val title = if (response.successful().size == messages.size) {
                    LOG.info { "Successfully deleted ${messages.size} SQS messages" }
                    message("sqs.delete.message.succeeded", messages.size)
                } else {
                    LOG.warn { "Successfully deleted ${response.successful().size} SQS messages, ${response.failed().size} failed" }
                    message("sqs.delete.message.partial_successs", response.successful().size)
                }
                notifyInfo(
                    project = project,
                    title = title
                )
            } catch (e: Exception) {
                notifyError(
                    project = project,
                    title = message("sqs.delete.message.failed", messages.size)
                )
                LOG.error(e) { "Unable to delete SQS messages, request failed!" }
            }
        }
    }

    private companion object {
        private val LOG = getLogger<DeleteMessageAction>()
    }
}