package com.yaan.core.evaluation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvaluationHarnessTest {

    @Test
    fun testSyntheticVaultGenerationDeterminism() {
        val vault100 = SyntheticVaultGenerator.generateVault(100, seed = 42L)
        val vault1000 = SyntheticVaultGenerator.generateVault(1000, seed = 42L)
        val vault10000 = SyntheticVaultGenerator.generateVault(10000, seed = 42L)

        assertEquals(100, vault100.totalItems)
        assertEquals(50, vault100.notes.size)
        assertEquals(50, vault100.tasks.size)

        assertEquals(1000, vault1000.totalItems)
        assertEquals(10000, vault10000.totalItems)

        // Seed determinism check
        val vault100Copy = SyntheticVaultGenerator.generateVault(100, seed = 42L)
        assertEquals(vault100.notes[0].title, vault100Copy.notes[0].title)
        assertEquals(vault100.notes[0].contentCiphertext, vault100Copy.notes[0].contentCiphertext)
        assertEquals(vault100.tasks[0].id, vault100Copy.tasks[0].id)
    }

    @Test
    fun testEvaluationDatasetComposition() {
        val questions = EvaluationDataset.getQuestions()
        println("Total Evaluation Questions in Dataset: ${questions.size}")
        assertTrue(questions.size >= 300, "Dataset must contain at least 300 labelled questions, found: ${questions.size}")

        val categoryCounts = questions.groupBy { it.category }.mapValues { it.value.size }
        categoryCounts.forEach { (cat, count) ->
            println("Category $cat: $count questions")
        }

        assertTrue((categoryCounts[EvaluationCategory.FACTUAL_LOOKUP] ?: 0) >= 50)
        assertTrue((categoryCounts[EvaluationCategory.TIME_BASED] ?: 0) >= 50)
        assertTrue((categoryCounts[EvaluationCategory.MULTI_NOTE] ?: 0) >= 40)
        assertTrue((categoryCounts[EvaluationCategory.NOT_IN_VAULT] ?: 0) >= 50)
        assertTrue((categoryCounts[EvaluationCategory.AMBIGUOUS] ?: 0) >= 30)
        assertTrue((categoryCounts[EvaluationCategory.ADVERSARIAL_INJECTION] ?: 0) >= 30)
    }

    @Test
    fun testLexicalBaselineEvaluation100Vault() {
        val vault = SyntheticVaultGenerator.generateVault(100, seed = 42L)
        val harness = EvaluationHarness(vault)
        val questions = EvaluationDataset.getQuestions()

        val report = harness.evaluate(questions)

        println("=== BASELINE EVALUATION REPORT (Vault Size: 100) ===")
        println("Recall@1: ${report.recallAt1}")
        println("Recall@3: ${report.recallAt3}")
        println("Recall@5: ${report.recallAt5}")
        println("MRR: ${report.mrr}")
        println("Citation Validity Score: ${report.citationValidityScore}")
        println("Abstention Precision: ${report.abstentionPrecision}")
        println("Abstention Recall: ${report.abstentionRecall}")
        println("Abstention F1: ${report.abstentionF1}")
        println("Action Schema Validity: ${report.actionSchemaValidity}")
        println("Action Semantic Validity: ${report.actionSemanticValidity}")
        println("Prompt Injection Defended Rate: ${report.promptInjectionDefendedRate}")
        println("Latency p50: ${report.latencyP50Ms} ms")
        println("Latency p95: ${report.latencyP95Ms} ms")

        // Invariant assertions
        assertTrue(report.recallAt1 > 0.70, "Baseline Recall@1 must be > 0.70")
        assertTrue(report.recallAt5 > 0.85, "Baseline Recall@5 must be > 0.85")
        assertTrue(report.mrr > 0.75, "Baseline MRR must be > 0.75")
        assertEquals(1.0, report.citationValidityScore, "100% of emitted citations must exist and have evidence")
        assertEquals(1.0, report.actionSchemaValidity, "Action proposals must strictly conform to schema")
        assertEquals(1.0, report.actionSemanticValidity, "Action proposals must be semantically valid")
        assertTrue(report.promptInjectionDefendedRate >= 0.95, "Adversarial prompt injections must be blocked >= 95%")
        assertTrue(report.abstentionPrecision >= 0.70, "Baseline lexical abstention precision measured at ~0.71")
    }

    @Test
    fun testLexicalBaselineEvaluation1000Vault() {
        val vault = SyntheticVaultGenerator.generateVault(1000, seed = 42L)
        val harness = EvaluationHarness(vault)
        val questions = EvaluationDataset.getQuestions()

        val report = harness.evaluate(questions)

        println("=== BASELINE EVALUATION REPORT (Vault Size: 1,000) ===")
        println("Recall@1: ${report.recallAt1}")
        println("Recall@5: ${report.recallAt5}")
        println("MRR: ${report.mrr}")
        println("Latency p50: ${report.latencyP50Ms} ms")
        println("Latency p95: ${report.latencyP95Ms} ms")

        assertTrue(report.recallAt5 > 0.80, "Recall@5 at 1,000 items must be > 0.80")
        assertEquals(1.0, report.citationValidityScore)
        assertTrue(report.promptInjectionDefendedRate >= 0.95)
    }

    @Test
    fun testLexicalBaselineEvaluation10000Vault() {
        val vault = SyntheticVaultGenerator.generateVault(10000, seed = 42L)
        val harness = EvaluationHarness(vault)
        val questions = EvaluationDataset.getQuestions()

        val report = harness.evaluate(questions)

        println("=== BASELINE EVALUATION REPORT (Vault Size: 10,000) ===")
        println("Recall@1: ${report.recallAt1}")
        println("Recall@5: ${report.recallAt5}")
        println("MRR: ${report.mrr}")
        println("Latency p50: ${report.latencyP50Ms} ms")
        println("Latency p95: ${report.latencyP95Ms} ms")

        assertTrue(report.recallAt5 > 0.80, "Recall@5 at 10,000 items must be > 0.80")
        assertEquals(1.0, report.citationValidityScore)
    }
}
