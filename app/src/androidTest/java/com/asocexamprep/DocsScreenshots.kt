package com.asocexamprep

import android.graphics.Bitmap
import android.view.WindowInsets
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.asocexamprep.data.db.TopicProgress
import com.asocexamprep.data.settings.Grade
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Screenshots for the user guide (docs/ site). Run explicitly:
 *   adb shell am instrument -w -e class com.asocexamprep.DocsScreenshots <runner>
 * Images land in the app's files/docs-shots folder, status bar cropped, 540 px wide.
 */
@RunWith(AndroidJUnit4::class)
class DocsScreenshots {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val pack get() = app.container.content.pack
    private lateinit var player: FakeMorsePlayer

    private fun shot(name: String) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_500) // let tap ripples fade
        rule.waitForIdle()
        val full = rule.onRoot().captureToImage().asAndroidBitmap()
        val top = rule.activity.window.decorView.rootWindowInsets?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
        val bottom = rule.activity.window.decorView.rootWindowInsets?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
        val cropped = Bitmap.createBitmap(full, 0, top, full.width, full.height - top - bottom)
        val scaled = Bitmap.createScaledBitmap(cropped, 540, cropped.height * 540 / cropped.width, true)
        val dir = File(app.filesDir, "docs-shots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun text(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()
    private fun waitFor(tag: String) = rule.waitUntil(5_000) { exists(tag) }
    private fun click(tag: String) {
        runCatching { rule.onNodeWithTag(tag).performScrollTo() }
        rule.onNodeWithTag(tag).performClick()
    }

    /** A learner a week in: a few topics read, the first Morse letters learned, a streak going. */
    private fun seedProgress() = runBlocking {
        val c = app.container
        val now = System.currentTimeMillis()
        c.settings.setGrade(Grade.GENERAL)
        c.settings.setDailyMinutes(30)
        c.settings.setLiveResults(false)
        c.settings.setExamDay(LocalDate.now().plusDays(45).toEpochDay())
        val topics = pack.curriculum.units.flatMap { it.topics }.map { it.id }
        topics.take(7).forEach {
            c.db.progress().upsertTopic(TopicProgress(it, studied = true, notesRead = 1f, lastSeenAt = now))
            c.game.topicStudied(it, 4)
        }
        for (step in 0 until 12) {
            c.game.learnResult(step, if (step % 3 == 0) 1f else 0.95f)
            c.game.stepUnlocked(step + 1)
        }
        c.settings.setTrainerStep(12)
        val today = LocalDate.now().toEpochDay()
        for (d in 1..5) c.db.activity().add(today - d, "morse", 25)
        c.db.activity().add(today, "morse", 10)
        c.game.refresh()
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    private fun home() { rule.onNodeWithTag("tab_home").performClick(); waitFor("screen_home") }

    @Test fun guide() {
        player = resetForTest()
        seedProgress()
        rule.activityRule.scenario.recreate()

        // Home and grade
        home()
        shot("home")

        // Study
        rule.onNodeWithTag("tab_study").performClick()
        shot("study")
        click("topic_a-resistors")
        waitFor("screen_topic")
        shot("topic")
        back()
        click("open_info")
        shot("info")
        click("info_fees")
        shot("info-page")
        back(); back()

        // Reference: phonetics with a word being spelled
        app.container.voicePlayer = object : com.asocexamprep.voice.VoicePlayer {
            override suspend fun say(items: List<com.asocexamprep.voice.Phonetics.Spoken>, onItem: (Int) -> Unit) {
                onItem(2); kotlinx.coroutines.awaitCancellation()
            }
            override fun release() {}
        }
        click("open_reference")
        rule.onNodeWithTag("phon_input").performTextInput("VU2ABC")
        click("phon_spell")
        shot("reference-phonetics")
        click("phon_spell")
        click("ref_tab_1")
        shot("reference-qcodes")
        click("ref_tab_3")
        shot("reference-bands")
        back()

        // Morse
        rule.onNodeWithTag("tab_morse").performClick()
        waitFor("screen_morse")
        shot("morse")
        click("morse_learn")
        waitFor("screen_learn")
        shot("learn")
        rule.onNodeWithTag("scroll_learn").performScrollToNode(hasTestTag("char_start"))
        click("char_start")
        rule.waitUntil(5_000) { exists("char_input") && player.played.isNotEmpty() }
        rule.onNodeWithTag("char_input").performTextInput(player.played.last())
        rule.waitUntil(5_000) { text("char_count") == "2 of 10" }
        shot("learn-drill")
        back()
        click("morse_copy")
        waitFor("screen_copy")
        shot("copy")
        click("copy_mode_CALLSIGNS")
        click("copy_start")
        waitFor("copy_check")
        val calls = player.played.last().split(" ")
        rule.onNodeWithTag("copy_input").performTextInput((calls.dropLast(1) + "VU2XY").joinToString(" "))
        click("copy_check")
        shot("copy-result")
        back()
        click("morse_send")
        waitFor("screen_send")
        val target = text("send_target")
        rule.onNodeWithTag("send_key").performTouchInput {
            target.take(1).forEach { ch ->
                com.asocexamprep.morse.MorseCode.pattern(ch)!!.forEachIndexed { si, sym ->
                    if (si > 0) advanceEventTime(200)
                    down(center); advanceEventTime(if (sym == '.') 150 else 450); up()
                }
            }
        }
        rule.waitUntil(5_000) { text("send_decoded").isNotBlank() }
        shot("send")
        back()
        click("morse_alphabet")
        waitFor("screen_alphabet")
        shot("alphabet")
        back()

        // Exams
        rule.onNodeWithTag("tab_exam").performClick()
        waitFor("screen_exam")
        shot("exam")
        click("exam_paper_B")
        shot("paper-intro")
        click("mock_start")
        waitFor("paper_list")
        click("live_toggle")
        fun paperQ(n: Int): com.asocexamprep.content.Question {
            rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_stem_$n"))
            val id = rule.onNodeWithTag("mq_stem_$n").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            return pack.questions.first { it.id == id }
        }
        fun pick(n: Int, o: Int) {
            rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_${n}_opt_$o"))
            rule.onNodeWithTag("mq_${n}_opt_$o").performClick()
        }
        val answers = (0 until 50).map { paperQ(it) }
        pick(0, answers[0].answer)
        pick(1, (answers[1].answer + 1) % 4)
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_stem_0"))
        shot("paper-live")
        val withHint = (0 until 50).first { answers[it].id in pack.hints }
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_hint_$withHint"))
        rule.onNodeWithTag("mq_hint_$withHint").performClick()
        rule.onNodeWithTag("paper_list").performScrollToNode(hasTestTag("mq_stem_${withHint + 1}"))
        shot("paper-hint")
        // Finish with about 4 in 5 right.
        for (n in 2 until 50) pick(n, if (n % 5 == 0) (answers[n].answer + 1) % 4 else answers[n].answer)
        rule.onNodeWithTag("mock_submit").performClick()
        rule.onNodeWithTag("mock_confirm").performClick()
        waitFor("mock_result")
        shot("paper-results")
        click("mock_done")
    }

    @Test fun guideExamExtras() {
        player = resetForTest()
        seedProgress()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("tab_exam").performClick()
        waitFor("screen_exam")
        click("exam_practice")
        waitFor("screen_practice_topics")
        shot("practice-topics")
        rule.onNodeWithTag("scroll_practice_topics").performScrollToNode(hasTestTag("practice_b-phonetics-codes"))
        click("practice_b-phonetics-codes")
        waitFor("quiz_stem")
        val id = rule.onNodeWithTag("quiz_stem").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        val q = pack.questions.first { it.id == id }
        click("opt_${q.answer}")
        shot("practice-question")
        back()
        back()
        click("exam_morse")
        waitFor("screen_morse_test")
        shot("morse-test")
        back()

        // Progress, settings, about
        rule.onNodeWithTag("tab_progress").performClick()
        waitFor("screen_progress")
        shot("progress")
        rule.onNodeWithTag("tab_home").performClick()
        click("open_settings")
        waitFor("screen_settings")
        shot("settings")
        rule.onNodeWithTag("scroll_settings").performScrollToNode(hasTestTag("open_about"))
        click("open_about")
        waitFor("screen_about")
        shot("about")
    }
}
