package com.app.attops.features.reports.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.app.attops.features.reports.domain.model.IntegrityScorecard
import com.app.attops.features.reports.domain.model.MasterReport
import com.app.attops.features.reports.domain.repository.ReportFilter
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.app.attops.core.designsystem.R as DesignR

object PdfGenerator {

    private fun getLogoBitmap(context: Context): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, DesignR.drawable.logo)
        } catch (e: Exception) {
            null
        }
    }

    fun generateIntegrityReport(
        context: Context, 
        scorecards: List<IntegrityScorecard>,
        filter: ReportFilter
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 60f

        // Brand Header with Logo
        val logo = getLogoBitmap(context)
        if (logo != null) {
            val scaledLogo = Bitmap.createScaledBitmap(logo, 80, 80 * logo.height / logo.width, true)
            canvas.drawBitmap(scaledLogo, 40f, y - 20, paint)
            y += 40f
        } else {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 24f
            paint.color = Color.rgb(0, 102, 204)
            canvas.drawText("ATTOPS", 40f, y, paint)
            y += 20f
        }
        
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.GRAY
        canvas.drawText("High-Integrity Workforce Management", 40f, y, paint)
        
        y += 40f

        // Report Title & Meta
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("Integrity Scorecard Report", 40f, y, paint)
        y += 25f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        val current = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
        canvas.drawText("Generated: $current", 40f, y, paint)
        canvas.drawText("Period: ${filter.name.replace("_", " ")}", 400f, y, paint)
        y += 40f

        // Table Header Background
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawRect(40f, y - 20, 555f, y + 10, paint)

        // Table Header Text
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("NAME", 45f, y, paint)
        canvas.drawText("TASKS", 220f, y, paint)
        canvas.drawText("ATTENDANCE", 300f, y, paint)
        canvas.drawText("AVG DEV.", 420f, y, paint)
        canvas.drawText("FLAGS", 500f, y, paint)
        
        y += 30f

        // Table Content
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        scorecards.forEachIndexed { index, card ->
            if (index % 2 == 1) {
                paint.color = Color.rgb(250, 250, 250)
                canvas.drawRect(40f, y - 20, 555f, y + 5, paint)
            }
            
            paint.color = Color.BLACK
            canvas.drawText(card.fullName, 45f, y, paint)
            canvas.drawText("${card.completedTasks}/${card.totalTasks}", 220f, y, paint)
            
            // Attendance Rate with color coding
            val rate = (card.attendanceRate * 100).toInt()
            paint.color = when {
                rate >= 90 -> Color.rgb(0, 150, 0)
                rate >= 70 -> Color.rgb(200, 120, 0)
                else -> Color.RED
            }
            canvas.drawText("$rate%", 300f, y, paint)
            
            paint.color = Color.BLACK
            canvas.drawText("${card.averageIntegrityDistance.toInt()}m", 420f, y, paint)
            
            // Flags with color coding
            if (card.flagCount > 0) paint.color = Color.RED
            canvas.drawText(card.flagCount.toString(), 500f, y, paint)
            paint.color = Color.BLACK

            y += 25f
            
            if (y > 800) {
                // In a real app, we'd finish this page and start a new one.
                // For this MVP, we'll assume the staff list fits on one page (approx 25-30 employees).
            }
        }

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 9f
        canvas.drawText("Confidential - For Internal Organizational Use Only", 40f, 820f, paint)
        canvas.drawText("www.attops.app", 480f, 820f, paint)

        document.finishPage(page)

        val file = File(context.cacheDir, "AttOps_Integrity_Report_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun generateIntegrityCsv(
        context: Context, 
        scorecards: List<IntegrityScorecard>,
        filter: ReportFilter
    ): File {
        val file = File(context.cacheDir, "AttOps_Integrity_Report_${System.currentTimeMillis()}.csv")
        val writer = FileOutputStream(file).bufferedWriter()
        
        writer.write("ATTOPS INTEGRITY REPORT\n")
        writer.write("Period,${filter.name.replace("_", " ")}\n")
        writer.write("Generated,${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}\n\n")
        
        // Header
        writer.write("Employee ID,Full Name,Total Tasks,Completed Tasks,Avg Integrity Distance (m),Flag Count,Attendance Rate (%)\n")
        
        // Content
        scorecards.forEach { card ->
            writer.write("${card.employeeId},${card.fullName},${card.totalTasks},${card.completedTasks},${card.averageIntegrityDistance.toInt()},${card.flagCount},${(card.attendanceRate * 100).toInt()}\n")
        }
        
        writer.close()
        return file
    }

    fun generateEmployeeTimesheetPdf(
        context: Context,
        fullName: String,
        attendances: List<com.app.attops.core.network.model.TaskAttendance>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 60f

        // Brand Header with Logo
        val logo = getLogoBitmap(context)
        if (logo != null) {
            val scaledLogo = Bitmap.createScaledBitmap(logo, 80, 80 * logo.height / logo.width, true)
            canvas.drawBitmap(scaledLogo, 40f, y - 20, paint)
            y += 40f
        } else {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 24f
            paint.color = Color.rgb(0, 102, 204)
            canvas.drawText("ATTOPS", 40f, y, paint)
            y += 20f
        }

        // Title
        paint.color = Color.BLACK
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Employee Timesheet: $fullName", 40f, y, paint)
        y += 30f

        // Table Header
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATE", 40f, y, paint)
        canvas.drawText("CHECK IN", 180f, y, paint)
        canvas.drawText("CHECK OUT", 320f, y, paint)
        canvas.drawText("DEVIATION", 460f, y, paint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 25f

        // Content
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        attendances.forEach { log ->
            val date = log.checkInTime?.substring(0, 10) ?: "N/A"
            val inTime = log.checkInTime?.substring(11, 16) ?: "N/A"
            val outTime = log.checkOutTime?.substring(11, 16) ?: "--:--"
            val integrityDist = log.integrityDistance
            val dev = if (integrityDist != null) "${integrityDist.toInt()}m" else "-"

            canvas.drawText(date, 40f, y, paint)
            canvas.drawText(inTime, 180f, y, paint)
            canvas.drawText(outTime, 320f, y, paint)
            canvas.drawText(dev, 460f, y, paint)
            y += 20f
        }

        document.finishPage(page)
        val file = File(context.cacheDir, "Timesheet_${fullName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun generateMasterReportPdf(
        context: Context,
        data: List<MasterReport>,
        filter: ReportFilter
    ): File {
        val document = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }
        val textPaint = Paint().apply {
            textSize = 10f
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(1000, 1414, pageNumber).create() // Larger format for details
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = 60f

        // Header
        canvas.drawText("ATTOPS MASTER OPERATIONS REPORT", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Filter: ${filter.name} | Generated: ${LocalDateTime.now()}", 40f, y, textPaint)
        y += 40f

        // Table Headers
        val columns = listOf(
            "Task Title" to 40f,
            "Assigned To" to 200f,
            "Status" to 320f,
            "Check-In" to 420f,
            "Check-Out" to 650f,
            "Integrity (m)" to 880f
        )
        
        columns.forEach { (title, x) ->
            canvas.drawText(title, x, y, headerPaint)
        }
        y += 10f
        canvas.drawLine(40f, y, 960f, y, headerPaint)
        y += 20f

        data.forEach { report ->
            if (y > 1350) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(1000, 1414, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 60f
            }

            canvas.drawText(report.title.take(25), 40f, y, textPaint)
            canvas.drawText(report.assignedToName.take(15), 200f, y, textPaint)
            canvas.drawText(report.status.name, 320f, y, textPaint)
            
            val checkInStr = report.checkInTime?.let { formatDateTime(it) } ?: "N/A"
            canvas.drawText(checkInStr, 420f, y, textPaint)
            
            val checkOutStr = report.checkOutTime?.let { formatDateTime(it) } ?: "N/A"
            canvas.drawText(checkOutStr, 650f, y, textPaint)
            
            y += 15f
            // Sub-lines for location links if available
            if (report.checkInLat != null && report.checkInLng != null) {
                textPaint.color = Color.BLUE
                canvas.drawText("In: maps.google.com/?q=${report.checkInLat},${report.checkInLng}", 420f, y, textPaint)
                textPaint.color = Color.BLACK
            }
            if (report.checkOutLat != null && report.checkOutLng != null) {
                textPaint.color = Color.BLUE
                canvas.drawText("Out: maps.google.com/?q=${report.checkOutLat},${report.checkOutLng}", 650f, y, textPaint)
                textPaint.color = Color.BLACK
            }
            
            y += 25f
            canvas.drawLine(40f, y - 5, 960f, y - 5, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })
            y += 10f
        }

        document.finishPage(page)
        val file = File(context.cacheDir, "AttOps_Master_Report_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun generateMasterCsv(
        context: Context,
        data: List<MasterReport>,
        filter: ReportFilter
    ): File {
        val file = File(context.cacheDir, "AttOps_Master_Report_${System.currentTimeMillis()}.csv")
        val writer = FileOutputStream(file).bufferedWriter()

        // CSV Header
        writer.write("Task ID,Title,Description,Assigned To,Priority,Status,Deadline,Check-In Time,Check-In Lat,Check-In Lng,Check-In Map,Check-Out Time,Check-Out Lat,Check-Out Lng,Check-Out Map\n")

        data.forEach { r ->
            val checkInMap = if (r.checkInLat != null) "https://www.google.com/maps/search/?api=1&query=${r.checkInLat},${r.checkInLng}" else ""
            val checkOutMap = if (r.checkOutLat != null) "https://www.google.com/maps/search/?api=1&query=${r.checkOutLat},${r.checkOutLng}" else ""
            
            val line = listOf(
                r.taskId,
                r.title.replace(",", " "),
                (r.description ?: "").replace(",", " "),
                r.assignedToName,
                r.priority.name,
                r.status.name,
                r.deadline ?: "",
                r.checkInTime ?: "",
                r.checkInLat?.toString() ?: "",
                r.checkInLng?.toString() ?: "",
                checkInMap,
                r.checkOutTime ?: "",
                r.checkOutLat?.toString() ?: "",
                r.checkOutLng?.toString() ?: "",
                checkOutMap
            ).joinToString(",")
            writer.write("$line\n")
        }

        writer.close()
        return file
    }

    private fun formatDateTime(iso: String): String {
        return try {
            val odt = OffsetDateTime.parse(iso)
            odt.atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.getDefault()))
        } catch (e: Exception) {
            iso.take(16)
        }
    }
}
