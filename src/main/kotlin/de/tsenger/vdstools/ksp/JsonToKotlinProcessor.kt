package de.tsenger.vdstools.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import java.io.File

data class Feature(
    val name: String,
    val tag: Int,
    val coding: String,
    val decodedLength: Int,
    val required: Boolean,
    val minLength: Int,
    val maxLength: Int
)

data class Document(
    val documentType: String,
    val documentRef: String,
    val version: Int,
    val features: List<Feature>
)

class JsonToKotlinProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {


    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Pfad zur JSON-Datei
        val jsonFile = File("src/main/resources/SealCodings.json")

        if (!jsonFile.exists()) {
            environment.logger.error("JSON file not found.")
            return emptyList()
        }

        // JSON einlesen
        val jsonContent = jsonFile.readText()

        // JSON in die Kotlin-Datenklassen parsen mit Gson
        val gson = Gson()
        val documentListType = object : TypeToken<List<Document>>() {}.type
        val documents: List<Document> = gson.fromJson(jsonContent, documentListType)

        // Erstelle eine Kotlin-Datei aus den JSON-Daten
        val fileSpec = FileSpec.builder("de.tsenger.vdstools.generated", "DocumentFeatures")
            .addType(
                TypeSpec.objectBuilder("DocumentFeatures")
                    .apply {
                        documents.forEach { document ->
                            // Generiere Properties für jedes Dokument
                            addProperty(
                                PropertySpec.builder(
                                    document.documentType,
                                    List::class.asClassName().parameterizedBy(Feature::class.asClassName())
                                )
                                    .initializer(
                                        "listOf(${
                                            document.features.joinToString(",\n") {
                                                "Feature(${it.name}, ${it.tag}, \"${it.coding}\", ${it.decodedLength}, ${it.required}, ${it.minLength}, ${it.maxLength})"
                                            }
                                        })"
                                    )
                                    .build()
                            )
                        }
                    }
                    .build()
            )
            .build()

        // Die generierte Datei speichern
        fileSpec.writeTo(File("build/generated/ksp/main/kotlin"))
        return emptyList()
    }
}

// KSP Processor Provider
class JsonToKotlinProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return JsonToKotlinProcessor(environment)
    }
}

