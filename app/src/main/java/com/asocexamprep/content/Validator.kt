package com.asocexamprep.content

/** Consistency checks for a content pack; returns human-readable problems (empty = valid). */
fun validate(pack: ContentPack): List<String> = buildList {
    val gradeIds = pack.exam.grades.map { it.id }.toSet()
    val sectionIds = pack.exam.sections.map { it.id }.toSet() + "M"
    val topicIds = pack.topics.map { it.id }
    val sourceIds = pack.sources.map { it.id }

    topicIds.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { add("duplicate topic id $it") }
    pack.curriculum.units.map { it.id }.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { add("duplicate unit id $it") }
    sourceIds.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { add("duplicate source id $it") }

    pack.curriculum.units.forEach { u ->
        if (u.section !in sectionIds) add("unit ${u.id}: unknown section ${u.section}")
        u.topics.forEach { t ->
            if (t.grades.isEmpty()) add("topic ${t.id}: no grades")
            t.grades.filter { it !in gradeIds }.forEach { add("topic ${t.id}: unknown grade $it") }
            if (t.syllabus.isEmpty()) add("topic ${t.id}: no syllabus points")
            if (t.minutes <= 0) add("topic ${t.id}: minutes must be positive")
        }
    }

    pack.sources.forEach { s ->
        if (!Regex("[0-9a-f]{64}").matches(s.sha256)) add("source ${s.id}: bad sha256")
        if (!s.url.startsWith("http")) add("source ${s.id}: bad url")
        s.map.forEach { m ->
            if (m.topic !in topicIds) add("source ${s.id}: unknown topic ${m.topic}")
            val r = runCatching { m.range }.getOrNull()
            if (r == null || r.first < 1 || r.last > s.pages || r.first > r.last)
                add("source ${s.id}: bad page range ${m.pages} for ${m.topic} (pages ${s.pages})")
        }
    }

    pack.info.forEach { p ->
        if (pack.infoText(p.id).isNullOrBlank()) add("info ${p.id}: empty page")
        if (p.source !in sourceIds) add("info ${p.id}: unknown source ${p.source}")
    }
    if (pack.reference.bands.source !in sourceIds) add("reference bands: unknown source")

    // Every Section A/B topic should point to at least one reading.
    pack.topics.filter { pack.readings(it.id).isEmpty() }.forEach { add("topic ${it.id}: no reading") }
}
