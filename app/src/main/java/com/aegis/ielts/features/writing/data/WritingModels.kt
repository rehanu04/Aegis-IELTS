package com.aegis.ielts.features.writing.data

import kotlinx.serialization.Serializable

/**
 * Data profile representing an IELTS Writing Task (Academic Task 1 or Task 2).
 */
data class WritingTask(
    val id: String,
    val taskType: Int, // 1 = Academic Task 1 (Report), 2 = Task 2 (Essay)
    val minWords: Int, // 150 for Task 1, 250 for Task 2
    val title: String,
    val prompt: String,
    val detailsText: String? = null // Multi-view markdown/text data metrics for Task 1
)

/**
 * Sample mock task database for the Writing module.
 */
object WritingMockTasks {
    val task1 = WritingTask(
        id = "writing_task_1",
        taskType = 1,
        minWords = 150,
        title = "Global Energy Consumption",
        prompt = "The table below shows the global consumption of energy by source in 2015 and 2025. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.",
        detailsText = """
            | Energy Source | 2015 Share (%) | 2025 Share (%) |
            | :--- | :---: | :---: |
            | Coal | 30% | 22% |
            | Gas | 23% | 25% |
            | Oil | 33% | 28% |
            | Renewables | 14% | 25% |
        """.trimIndent()
    )

    val task2 = WritingTask(
        id = "writing_task_2",
        taskType = 2,
        minWords = 250,
        title = "Tertiary Education Funding",
        prompt = "Some people believe that university education should be free for all students, while others argue that students should pay for their tertiary studies because they will benefit personally from higher salaries later in life. Discuss both views and give your opinion.",
        detailsText = null
    )
}
