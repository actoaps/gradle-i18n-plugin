package dk.acto.gradle.i18nplugin

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.Credentials
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import java.io.File
import java.io.FileInputStream

class SheetFacade (private val jsonFactory: JsonFactory) {
    private fun sanitizeValues (values: ValueRange) : List<String> {
        return values.getValues()
            .filter { it.isNotEmpty() }
            .map { it[0] as String }
    }

    fun getCredentialsFromFile (file: File): Credentials {
        return ServiceAccountCredentials.fromStream(FileInputStream(file))
            .createScoped(listOf(SheetsScopes.SPREADSHEETS_READONLY))
    }

    fun createSheetsService (credentials: Credentials): Sheets {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        val reqInitializer = HttpCredentialsAdapter(credentials)

        return Sheets.Builder(transport, jsonFactory, reqInitializer)
            .setApplicationName("I18n Plugin")
            .build()
    }

    fun bulkGet (ranges: List<String>, spreadsheetId: String, service: Sheets): List<List<String>> {
        return service.spreadsheets().values().batchGet(spreadsheetId).setRanges(ranges)
            .setValueRenderOption("UNFORMATTED_VALUE").execute().valueRanges.map { sanitizeValues(it) }
    }

    fun get (range: String, spreadsheetId: String, service: Sheets): List<String> {
        return service.spreadsheets().values().get(spreadsheetId, range)
            .setValueRenderOption("UNFORMATTED_VALUE").execute().let { sanitizeValues(it) }
    }
}
