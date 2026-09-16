package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.IssueEntity
import com.example.data.ReportEntity
import com.example.util.DocxReportExporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Assured Capture", appName)
  }

  @Test
  fun `test docx report export produces valid zip with word content`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val report = ReportEntity(
      id = "test-rep-1",
      jobReference = "BBC-26-99",
      reportName = "Roof Inspection Test",
      clientName = "Acme Corp",
      address = "123 Main St",
      reportDate = "2026-09-15",
      status = "In Progress",
      inspectionDate = "2026-09-16",
      inspectionDateHistory = "[]",
      nextIssueNumber = 2,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )

    val issue = IssueEntity(
      id = "test-iss-1",
      reportId = report.id,
      issueNumber = 1,
      issueName = "Tile displacement",
      issueDescription = "Displaced ridge tiles near chimney stack.",
      createdAt = System.currentTimeMillis()
    )

    val file = DocxReportExporter.generateDocx(
      context = context,
      report = report,
      issues = listOf(issue),
      photos = emptyList()
    )

    assertTrue("Exported DOCX file must exist", file.exists())
    assertTrue("Exported file must have size > 0", file.length() > 0)

    // Verify it is a valid Zip containing Word OpenXML components
    val zip = ZipFile(file)
    val entries = zip.entries().toList().map { it.name }
    assertTrue("Must contain [Content_Types].xml", entries.contains("[Content_Types].xml"))
    assertTrue("Must contain _rels/.rels", entries.contains("_rels/.rels"))
    assertTrue("Must contain word/document.xml", entries.contains("word/document.xml"))
    assertTrue("Must contain word/styles.xml", entries.contains("word/styles.xml"))

    val docXml = zip.getInputStream(zip.getEntry("word/document.xml")).bufferedReader().readText()
    assertTrue("Document XML must include job reference", docXml.contains("BBC-26-99"))
    assertTrue("Document XML must include report name", docXml.contains("Roof Inspection Test"))
    assertTrue("Document XML must include issue description", docXml.contains("Tile displacement"))

    zip.close()
  }
}

