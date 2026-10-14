package com.yaan.core.domain.usecase

import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.ai.AiIntentClassifier
import com.yaan.core.domain.ai.AiIntentType
import com.yaan.core.domain.ai.HybridEdgeIntentClassifier
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.repository.NoteRepository
import com.yaan.core.domain.repository.TaskRepository

/**
 * Parses user quick capture input using the Edge SLM Classifier (ADR-0006)
 * and routes to 0ms local storage with zero cloud latency.
 */
class CaptureIntentUseCase(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val classifier: AiIntentClassifier = HybridEdgeIntentClassifier()
) {
    suspend operator fun invoke(rawInput: String): YaanResult<String> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return YaanResult.Failure(
                YaanError.ValidationError("rawInput", "Input cannot be empty")
            )
        }

        // 1. Edge SLM Intent Classification (<15ms on device)
        val parsed = classifier.classifyIntent(trimmed)

        return when (parsed.type) {
            AiIntentType.TASK -> {
                val title = parsed.entities["title"] ?: trimmed
                val task = Task(
                    id = "task_${com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()}",
                    title = title.ifEmpty { "New Action Item" }
                )
                taskRepository.upsertTask(task).map { "Task captured: ${task.title}" }
            }

            AiIntentType.REMINDER -> {
                val task = Task(
                    id = "task_${com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()}",
                    title = trimmed,
                    notes = "Scheduled via quick capture"
                )
                taskRepository.upsertTask(task).map { "Reminder staged: ${task.title}" }
            }

            AiIntentType.ACTION_PROPOSAL -> {
                // Staged as action item awaiting confirmation
                val task = Task(
                    id = "task_${com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()}",
                    title = "Proposed: $trimmed",
                    notes = "Requires affirmative approval"
                )
                taskRepository.upsertTask(task).map { "Action proposal staged: ${task.title}" }
            }

            AiIntentType.BRIEFING_QUERY -> {
                YaanResult.Success("Briefing query recognized: Displaying today's commitments.")
            }

            AiIntentType.SEARCH -> {
                val query = parsed.entities["query"] ?: trimmed
                YaanResult.Success("Searching vault for: $query")
            }

            AiIntentType.NOTE -> {
                val note = Note(
                    id = "note_${com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()}",
                    title = trimmed.take(40),
                    contentCiphertext = trimmed
                )
                val suffix = if (parsed.requiresCloudCascade) " [Cloud cascade scheduled]" else ""
                noteRepository.upsertNote(note).map { "Note captured in vault: ${note.title}$suffix" }
            }
        }
    }
}
