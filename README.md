# Gradle-i18n-plugin

Gradle-i18n-plugin is meant for generating a JSON file with internationalized texts, based on a Google Sheet.

The plugin assumes that you have a series of columns, each representing a locale, as well as a column for the translation key.  
The plugin also assumes that the first row of the given column, contains the language code of the locale. See the below image for an example of this setup.

![image](https://user-images.githubusercontent.com/3519438/136553409-ad9d5611-0275-44c2-847a-24b0955fb0f6.png)

Considering the above example, a JSON file of the following structure would be generated:
```json
{
  "da-DK": {
    "booking.cancel.bookingDoesNotExist": "Bookingen kan ikke findes.",
    "booking.cancel.button.cancelBooking": "Aflys booking",
    ...
  },
  "en-US": {
    "booking.cancel.bookingDoesNotExist": "The booking could not be found.",
    "booking.cancel.button.cancelBooking": "Cancel booking",
    ...
  }
}
```

NOTE: Blank cells are being filtered out, so make sure that you don't have only partially filled rows.

## Setup
To integrate the plugin into your setup, simply add it as a plugin in your `build.gradle` file.
```groovy
plugins {
    id "dk.acto.gradle.i18nplugin" version "1.0"
}
``` 

This will include the `generateI18n` task, as well as the `i18n` configuration block into your setup.

The plugin accepts the following parameters for configuration:  
|          Key         |                             Value                            |         Required         |
|:--------------------:|:------------------------------------------------------------:|:------------------------:|
| driveCredentialsFile |    The JSON file containing your Google Cloud credentials    |            Yes           |
|      outputFile      | The file to which the plugin should write the generated JSON |            Yes           |
|     spreadsheetId    |  The ID of the Google Spreadsheet (can be found in the URL)  |            Yes           |
|        sheetId       |        The name of the sheet, on the given spreadsheet       | No, defaults to "Sheet1" |
|     localeColumns    |          A list of the columns to include as locales         |            Yes           |
|       keyColumn      |          The column containing the translation keys          |            Yes           |

Example configuration:
```groovy
i18n {
  driveCredentialsFile = layout.projectDirectory.file("driveCredentials.json")
  outputFile = layout.projectDirectory.file("languageCache.json")
  spreadsheetId = "abcdefgh"
  localeColumns = ["C", "D"]
  keyColumn = "E"
}

build.dependsOn(generateI18n)
```
