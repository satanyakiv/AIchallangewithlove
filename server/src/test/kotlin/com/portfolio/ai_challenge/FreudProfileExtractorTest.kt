package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.freud_agent.FreudProfileExtractor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FreudProfileExtractorTest {

    private val extractor = FreudProfileExtractor()

    @Test
    fun testExtract_dreamMention_addsDreamSymbol() {
        val result = extractor.extract("I had a strange dream last night about flying")
        assertTrue(result.newDreamSymbols.contains("dream_content"))
    }

    @Test
    fun testExtract_nightmareMention_addsDreamSymbol() {
        val result = extractor.extract("I keep having nightmares about falling")
        assertTrue(result.newDreamSymbols.contains("dream_content"))
    }

    @Test
    fun testExtract_motherReference_addsChildhoodTheme() {
        val result = extractor.extract("My mother always told me I was not good enough")
        assertTrue(result.newChildhoodThemes.contains("mother"))
    }

    @Test
    fun testExtract_fatherReference_addsChildhoodTheme() {
        val result = extractor.extract("My father was never around when I needed him")
        assertTrue(result.newChildhoodThemes.contains("father"))
    }

    @Test
    fun testExtract_denialKeywords_addsDefenseMechanism() {
        val result = extractor.extract("I deny that this has anything to do with my past")
        assertTrue(result.newDefenseMechanisms.contains("denial"))
    }

    @Test
    fun testExtract_angerKeywords_addsDefenseMechanism() {
        val result = extractor.extract("I feel so angry all the time for no reason")
        assertTrue(result.newDefenseMechanisms.contains("displacement"))
    }

    @Test
    fun testExtract_foodReference_detectsOralFixation() {
        val result = extractor.extract("I can not stop eating junk food when stressed")
        assertEquals("oral", result.detectedFixation)
    }

    @Test
    fun testExtract_cleanlinessReference_detectsAnalFixation() {
        val result = extractor.extract("I must keep everything perfectly clean and organized")
        assertEquals("anal", result.detectedFixation)
    }

    @Test
    fun testExtract_competitiveReference_detectsPhallic() {
        val result = extractor.extract("I always need attention and to show-off")
        assertEquals("phallic", result.detectedFixation)
    }

    @Test
    fun testExtract_bossReference_addsRelationshipPattern() {
        val result = extractor.extract("My boss reminds me of my father")
        assertTrue(result.newRelationshipPatterns.contains("authority_figure"))
    }

    @Test
    fun testExtract_normalMessage_noMarkers() {
        val result = extractor.extract("The weather is nice today")
        assertTrue(result.newDreamSymbols.isEmpty())
        assertTrue(result.newDefenseMechanisms.isEmpty())
        assertTrue(result.newChildhoodThemes.isEmpty())
        assertNull(result.detectedFixation)
        assertTrue(result.newRelationshipPatterns.isEmpty())
        assertNull(result.patientName)
    }

    @Test
    fun testExtract_nameExtraction_setsPatientName() {
        val result = extractor.extract("My name is Heinrich and I have been anxious")
        assertEquals("Heinrich", result.patientName)
    }

    @Test
    fun testExtract_callMePattern_setsPatientName() {
        val result = extractor.extract("Please call me Anna")
        assertEquals("Anna", result.patientName)
    }

    @Test
    fun testExtract_multipleMarkers_detectsAll() {
        val result = extractor.extract("I dreamed about my mother and I deny it matters")
        assertTrue(result.newDreamSymbols.isNotEmpty())
        assertTrue(result.newChildhoodThemes.contains("mother"))
        assertTrue(result.newDefenseMechanisms.contains("denial"))
    }

    // --- Language detection tests ---

    @Test
    fun testExtract_ukrainianMessage_detectsUkrainian() {
        val result = extractor.extract("Мені сниться один і той самий сон")
        assertEquals("uk", result.detectedLanguage)
    }

    @Test
    fun testExtract_russianMessage_detectsRussian() {
        val result = extractor.extract("Мне снится один и тот же сон")
        assertEquals("ru", result.detectedLanguage)
    }

    @Test
    fun testExtract_englishMessage_detectsEnglish() {
        val result = extractor.extract("I keep having the same dream over and over")
        assertEquals("en", result.detectedLanguage)
    }

    @Test
    fun testExtract_germanMessage_detectsGerman() {
        val result = extractor.extract("Ich habe immer den gleichen Traum")
        assertEquals("de", result.detectedLanguage)
    }

    @Test
    fun testExtract_mixedCyrillicEnglish_detectsCyrillic() {
        val result = extractor.extract("Мій boss мене дратує")
        assertEquals("uk", result.detectedLanguage)
    }
}
