package dk.acto.gradle.i18nplugin

import com.google.api.client.json.jackson2.JacksonFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class I18nPluginConfiguration {
    @get:InputFile
    abstract val driveCredentialsFile: RegularFileProperty
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    abstract val spreadsheetId: Property<String>
    abstract val sheetId: Property<String>
    abstract val localeColumns: ListProperty<String>
    abstract val keyColumn: Property<String>
}

abstract class GenerateI18nTask @Inject constructor(config: I18nPluginConfiguration) : DefaultTask() {
    init {
        val requiredParams = mapOf(
            "driveConfigurationsFile" to config.driveCredentialsFile,
            "outputFile" to config.outputFile,
            "spreadsheetId" to config.spreadsheetId,
            "localeColumns" to config.localeColumns,
            "keyColumn" to config.keyColumn
        )

        requiredParams.forEach {
            if (!it.value.isPresent) throw GradleException("Configuration error: ${it.key} is required")
        }
    }

    private val jsonFactory = JacksonFactory.getDefaultInstance()

    private val credentialsFile = config.driveCredentialsFile.get().asFile
    private val outputFile = config.outputFile.get().asFile
    private val spreadsheetId = config.spreadsheetId.get()
    private val sheetId = config.sheetId.getOrElse("Sheet1")
    private val localeColumns = config.localeColumns.getOrElse(emptyList())
    private val keyColumn = config.keyColumn.get()

    @TaskAction
    fun run () {
        val sheetFacade = SheetFacade(jsonFactory)
        val service = sheetFacade.createSheetsService(sheetFacade.getCredentialsFromFile(credentialsFile))

        val localeRanges = localeColumns.map { "$sheetId!$it:$it" }
        val keyRange = "$sheetId!$keyColumn:$keyColumn"

        val locales = sheetFacade.bulkGet(localeRanges, spreadsheetId, service)
        val keys = sheetFacade.get(keyRange, spreadsheetId, service)

        val translations = generateTranslations(locales, keys)

        if (outputFile.exists()) outputFile.delete()
        outputFile.writeText(jsonFactory.toString(translations), Charsets.UTF_8)
    }

    private fun generateTranslations (locales: List<List<String>>, keys: List<String>): Map<String, Map<String, String>> {
        val keysWithoutHeader = keys.drop(1)
        val translationMap = locales.associateBy({ it.first() }, { it.drop(1) })
        return translationMap.mapValues { keysWithoutHeader.zip(it.value).toMap() }
    }
}

class I18nPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val config = project.extensions.create("i18n", I18nPluginConfiguration::class.java)
        project.tasks.register("generateI18n", GenerateI18nTask::class.java, config)
    }
}
