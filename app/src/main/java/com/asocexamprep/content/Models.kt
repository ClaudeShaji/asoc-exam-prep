package com.asocexamprep.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Exam(
    val id: String,
    val title: String,
    val authority: String,
    val rulesNote: String,
    val sections: List<ExamSection>,
    val grades: List<ExamGrade>,
)

@Serializable
data class ExamSection(val id: String, val title: String, val questions: Int, val exemptionNote: String? = null)

@Serializable
data class ExamGrade(
    val id: String,
    val title: String,
    val minutes: Int,
    val maxMarks: Int,
    val minSectionPercent: Int,
    val minAggregatePercent: Int,
    val morse: MorseRequirement? = null,
)

@Serializable
data class MorseRequirement(
    val wpm: Int,
    val minutes: Int,
    val characters: Int,
    val maxErrors: Int,
    val errorFreeMinutePasses: Boolean,
    val receive: Boolean,
    val send: Boolean,
)

@Serializable
data class Curriculum(val syllabusSource: String, val units: List<SyllabusUnit>)

@Serializable
data class SyllabusUnit(val id: String, val section: String, val title: String, val topics: List<Topic>)

@Serializable
data class Topic(
    val id: String,
    val title: String,
    val grades: List<String>,
    val minutes: Int,
    val syllabus: List<String>,
)

@Serializable
data class Source(
    val id: String,
    val title: String,
    val publisher: String,
    val year: Int,
    val pages: Int,
    val url: String,
    val sha256: String,
    /** govt-public: may be bundled; download: fetched on device from [url]; user-imported. */
    val licence: String,
    val official: Boolean,
    val map: List<PageMap> = emptyList(),
)

@Serializable
data class PageMap(val topic: String, val pages: String) {
    /** 1-based inclusive page range. */
    val range: IntRange get() = pages.split("-").let { (a, b) -> a.trim().toInt()..b.trim().toInt() }
}

@Serializable
data class InfoPage(val id: String, val title: String, val icon: String, val verified: String, val source: String)

@Serializable
data class Reference(
    val phonetics: List<Spelled>,
    val figures: List<Spelled>,
    val qcodes: List<QCode>,
    val abbreviations: List<Abbreviation>,
    val bands: Bands,
)

@Serializable data class Spelled(val char: String, val word: String)
@Serializable data class QCode(val code: String, val question: String, val answer: String)
@Serializable data class Abbreviation(val code: String, val meaning: String)
@Serializable data class Bands(val source: String, val page: Int, val power: String, val rows: List<BandRow>)
@Serializable data class BandRow(val band: String, val range: String, val note: String)

/** A multiple-choice exam question. [answer] is the index of the correct option. */
@Serializable
data class Question(
    val id: String,
    val topic: String,
    val section: String,
    val stem: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String? = null,
    val source: String,
    val page: Int? = null,
    /** Official NCVEC pool questions whose key gives the same answer ("pool:ID"). */
    val verified: List<String> = emptyList(),
    /** The same question, with the same answer, in other sources. */
    val alsoIn: List<String> = emptyList(),
)

/** Study notes for a topic, converted from one source's pages. */
@Serializable
data class NoteRef(val topic: String, val source: String, val file: String, val pages: String)

/** A topic's pointer into one source: what to read for it. */
data class Reading(val source: Source, val range: IntRange)

/** Everything one exam's content pack holds. */
class ContentPack(
    val exam: Exam,
    val curriculum: Curriculum,
    val sources: List<Source>,
    val info: List<InfoPage>,
    private val infoText: Map<String, String>,
    val reference: Reference,
    val notes: List<NoteRef> = emptyList(),
    val training: com.asocexamprep.morse.TrainingData? = null,
    val questions: List<Question> = emptyList(),
    private val read: (String) -> String = { error("no reader") },
) {
    private val noteCache = HashMap<String, String>()

    /** Notes for a topic, official sources first. */
    fun notesFor(topicId: String): List<NoteRef> = notes.filter { it.topic == topicId }
        .sortedByDescending { n -> sources.firstOrNull { it.id == n.source }?.official == true }

    @Synchronized
    fun noteText(ref: NoteRef): String = noteCache.getOrPut(ref.file) { read(ref.file) }

    fun source(id: String): Source? = sources.firstOrNull { it.id == id }

    val topics: List<Topic> = curriculum.units.flatMap { it.topics }
    private val topicById = topics.associateBy { it.id }
    private val unitByTopic = curriculum.units.flatMap { u -> u.topics.map { it.id to u } }.toMap()

    fun topic(id: String): Topic? = topicById[id]
    fun unitOf(topicId: String): SyllabusUnit? = unitByTopic[topicId]
    fun infoText(id: String): String? = infoText[id]
    fun grade(id: String): ExamGrade? = exam.grades.firstOrNull { it.id == id }

    fun unitsFor(grade: String): List<SyllabusUnit> = curriculum.units
        .map { u -> u.copy(topics = u.topics.filter { grade in it.grades }) }
        .filter { it.topics.isNotEmpty() }

    /** Readings for a topic: official sources first, then newest. */
    fun readings(topicId: String): List<Reading> = sources
        .sortedWith(compareByDescending<Source> { it.official }.thenByDescending { it.year })
        .flatMap { s -> s.map.filter { it.topic == topicId }.map { Reading(s, it.range) } }

    companion object {
        val json = Json { ignoreUnknownKeys = true }

        /** Builds a pack from a file reader rooted at the pack directory (e.g. "content/asoc-in"). */
        fun load(read: (String) -> String): ContentPack {
            val info = json.decodeFromString<List<InfoPage>>(read("info/index.json"))
            return ContentPack(
                exam = json.decodeFromString(read("exam.json")),
                curriculum = json.decodeFromString(read("curriculum.json")),
                sources = json.decodeFromString(read("sources.json")),
                info = info,
                infoText = info.associate { it.id to read("info/${it.id}.md") },
                reference = json.decodeFromString(read("reference.json")),
                notes = json.decodeFromString(read("notes.json")),
                questions = runCatching { json.decodeFromString<List<Question>>(read("questions.json")) }.getOrDefault(emptyList()),
                training = runCatching { json.decodeFromString<com.asocexamprep.morse.TrainingData>(read("morse/training.json")) }.getOrNull(),
                read = read,
            )
        }
    }
}
