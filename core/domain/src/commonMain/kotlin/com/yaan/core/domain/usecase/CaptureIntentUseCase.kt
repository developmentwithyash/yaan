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
        if (trimmed.length > MAX_INPUT_LENGTH) {
            return YaanResult.Failure(
                YaanError.ValidationError("rawInput", "Input exceeds maximum limit of $MAX_INPUT_LENGTH characters")
            )
        }

        // Sanitize control characters / null bytes
        val sanitized = trimmed.filter { !it.isISOControl() || it == '\n' || it == '\t' }

        // 1. Edge SLM Intent Classification (<15ms on device)
        val parsed = classifier.classifyIntent(sanitized)

        return when (parsed.type) {
            AiIntentType.TASK -> {
                val title = parsed.entities["title"] ?: sanitized
                val task = Task(
                    id = "task_${com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()}",
                    title = title.ifEmpty { "New Action Item" }
                )
                taskRepository.upsertTask(task).map { "Task captured: ${task.title}" }
            }

            AiIntentType.REMINDER -> {
                val task = Task(
                    id = "task_${com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()}",
                    title = sanitized,
                    notes = "Scheduled via quick capture"
                )
                taskRepository.upsertTask(task).map { "Reminder staged: ${task.title}" }
            }

            AiIntentType.ACTION_PROPOSAL -> {
                // Staged as action item awaiting confirmation
                val task = Task(
                    id = "task_${com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()}",
                    title = "Proposed: $sanitized",
                    notes = "Requires affirmative approval"
                )
                taskRepository.upsertTask(task).map { "Action proposal staged: ${task.title}" }
            }

            AiIntentType.BRIEFING_QUERY -> {
                YaanResult.Success("Briefing query recognized: Displaying today's commitments.")
            }

            AiIntentType.SEARCH -> {
                val query = parsed.entities["query"] ?: sanitized
                YaanResult.Success("Searching vault for: $query")
            }

            AiIntentType.NOTE -> {
                val note = Note(
                    id = "note_${com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()}",
                    title = sanitized.take(40),
                    contentCiphertext = sanitized
                )
                val suffix = if (parsed.requiresCloudCascade) " [Cloud cascade scheduled]" else ""
                noteRepository.upsertNote(note).map { "Note captured in vault: ${note.title}$suffix" }
            }
        }
    }

    companion object {
        const val MAX_INPUT_LENGTH = 8_192
    }
}
