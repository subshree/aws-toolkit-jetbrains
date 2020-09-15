// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.aws.toolkits.jetbrains.services.cloudwatch.logs.insights

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import software.amazon.awssdk.services.cloudwatchlogs.model.QueryDefinition
import software.aws.toolkits.jetbrains.core.credentials.ConnectionSettings
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.resources.CloudWatchResources
import software.aws.toolkits.jetbrains.ui.ResourceSelector
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

class SelectSavedQuery(
    private val project: Project,
    private val connectionSettings: ConnectionSettings
) {
    private lateinit var basePanel: JPanel
    lateinit var resourceSelector: ResourceSelector<QueryDefinition>
    private lateinit var logGroups: JTextArea
    private lateinit var queryString: JTextArea
    private lateinit var refreshButton: JButton

    private fun createUIComponents() {
        resourceSelector = ResourceSelector.builder(project)
            .resource { CloudWatchResources.DESCRIBE_QUERY_DEFINITIONS }
            .awsConnection { connectionSettings.region to connectionSettings.credentials }
            .customRenderer { entry, renderer -> renderer.append(entry.name()); renderer }
            .build()

        // select the first entry, if applicable
        resourceSelector.selectedItem { true }

        resourceSelector.addActionListener {
            resourceSelector.selected()?.let {
                logGroups.text = it.logGroupNames().joinToString("\n")
                queryString.text = it.queryString()
                logGroups.caretPosition = 0
                queryString.caretPosition = 0
            }
        }
    }

    init {
        refreshButton.icon = AllIcons.Actions.Refresh
        refreshButton.addActionListener {
            logGroups.text = ""
            queryString.text = ""
            resourceSelector.reload(forceFetch = true)
        }
    }

    fun getComponent(): JComponent = basePanel
}
