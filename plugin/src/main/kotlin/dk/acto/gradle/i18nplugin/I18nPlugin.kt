package dk.acto.gradle.i18nplugin

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.Credentials
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
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
    private val jsonFactory = JacksonFactory.getDefaultInstance()

    private val credentialsFile = config.driveCredentialsFile.get().asFile
    private val outputFile = config.outputFile.get().asFile
    private val spreadsheetId = config.spreadsheetId.get()
    private val sheetId = config.sheetId.getOrElse("Sheet1")
    private val localeColumns = config.localeColumns.getOrElse(emptyList())
    private val keyColumn = config.keyColumn.get()

    @TaskAction
    fun run () {
        val service = createSheetsService(getCredentialsFromFile(credentialsFile))

        val localeRanges = localeColumns.map { "$sheetId!$it:$it" }
        val keyRange = "$sheetId!$keyColumn:$keyColumn"

        val locales = service.spreadsheets().values().batchGet(spreadsheetId).setRanges(localeRanges)
            .setValueRenderOption("UNFORMATTED_VALUE").execute().valueRanges.map { sanitizeValues(it) }

        val keys = service.spreadsheets().values().get(spreadsheetId, keyRange)
            .setValueRenderOption("UNFORMATTED_VALUE").execute().let { sanitizeValues(it) }

        val translations = generateTranslations(locales, keys)

        if (outputFile.exists()) outputFile.delete()
        outputFile.writeText(jsonFactory.toPrettyString(translations))
    }

    private fun generateTranslations (locales: List<List<String>>, keys: List<String>): Map<String, Map<String, String>> {
        val keysWithoutHeader = keys.drop(1)
        val translationMap = locales.associateBy({ it.first() }, { it.drop(1) })
        return translationMap.mapValues { keysWithoutHeader.zip(it.value).toMap() }
    }

    private fun sanitizeValues (values: ValueRange) : List<String> {
        return values.getValues()
            .filter { it.isNotEmpty() }
            .map { it[0] as String }
    }

    private fun getCredentialsFromFile (file: File): Credentials {
        return ServiceAccountCredentials.fromStream(FileInputStream(file))
            .createScoped(listOf(SheetsScopes.SPREADSHEETS_READONLY))
    }

    private fun createSheetsService (credentials: Credentials): Sheets {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        val reqInitializer = HttpCredentialsAdapter(credentials)

        return Sheets.Builder(transport, jsonFactory, reqInitializer)
            .setApplicationName("I18n Plugin")
            .build()
    }
}

class I18nPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val config = project.extensions.create("i18n", I18nPluginConfiguration::class.java)
        project.tasks.register("generateI18n", GenerateI18nTask::class.java, config)
    }
}
