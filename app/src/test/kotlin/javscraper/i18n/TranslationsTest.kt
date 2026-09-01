package javscraper.i18n

import java.lang.reflect.Field
import kotlin.test.Test
import kotlin.test.assertTrue

class TranslationsTest {
    @Test
    fun `static texts duplicated in both locales use shared keys`() {
        val english = stringFields(TranslationEn())
        val chinese = stringFields(TranslationZh())
        val duplicatedPairs = english.keys.flatMap { first ->
            english.keys.filter { second ->
                second > first &&
                    english.getValue(first) == english.getValue(second) &&
                    chinese.getValue(first) == chinese.getValue(second)
            }.map { second -> "$first/$second" }
        }

        assertTrue(
            duplicatedPairs.isEmpty(),
            "Duplicated texts should use shared translation keys: $duplicatedPairs"
        )
    }

    private fun stringFields(translation: TranslationEn): Map<String, String> =
        translation.javaClass.declaredFields
            .filter { field: Field -> field.type == String::class.java }
            .associate { field: Field ->
                field.isAccessible = true
                field.name to field.get(translation) as String
            }
}