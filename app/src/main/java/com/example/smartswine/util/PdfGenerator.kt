package com.example.smartswine.util

import android.content.Context
import android.util.Log
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.bibiniitech.smartswine.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.GlobalNotice
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.getIngredientNameKey
import com.example.smartswine.utils.getCategoryKey
import com.example.smartswine.model.FeedInventoryItem
import com.example.smartswine.model.FeedInventoryTransaction
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.ui.feed.FeedNutrientProfile
import com.example.smartswine.ui.feed.IngredientSafetyAlert
import com.itextpdf.io.image.ImageData
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

object PdfGenerator {
    private var baseFontName: String? = null
    private var cachedWatermarkData: ImageData? = null

    private fun createFontProvider(): com.itextpdf.layout.font.FontProvider {
        val provider = com.itextpdf.layout.font.FontProvider()
        provider.addStandardPdfFonts()
        try {
            val systemFontsDir = File("/system/fonts")
            if (systemFontsDir.exists() && systemFontsDir.isDirectory) {
                val files = systemFontsDir.listFiles()
                if (files != null) {
                    val candidates = mutableListOf<File>()
                    
                    // Filter system fonts to key candidates to prevent high memory usage and parsing slowness
                    files.firstOrNull { it.name.contains("Roboto-Regular", ignoreCase = true) }?.let { candidates.add(it) }
                        ?: files.firstOrNull { it.name.contains("Roboto", ignoreCase = true) && !it.name.contains("Italic", ignoreCase = true) }?.let { candidates.add(it) }
                    
                    files.firstOrNull { it.name.contains("NotoSansCJK", ignoreCase = true) }?.let { candidates.add(it) }
                        ?: files.firstOrNull { it.name.contains("DroidSansFallback", ignoreCase = true) }?.let { candidates.add(it) }
                        ?: files.firstOrNull { it.name.contains("Hans", ignoreCase = true) || it.name.contains("Hant", ignoreCase = true) }?.let { candidates.add(it) }
                    
                    files.firstOrNull { it.name.contains("Thai", ignoreCase = true) && !it.name.contains("Italic", ignoreCase = true) }?.let { candidates.add(it) }
                    
                    files.firstOrNull { it.name.contains("Devanagari", ignoreCase = true) && !it.name.contains("Italic", ignoreCase = true) }?.let { candidates.add(it) }
                        ?: files.firstOrNull { it.name.contains("Hindi", ignoreCase = true) }?.let { candidates.add(it) }
                    
                    candidates.forEach { file ->
                        val path = file.absolutePath
                        try {
                            if (path.endsWith(".ttc", ignoreCase = true)) {
                                try {
                                    // TrueType Collection needs index 0 for iText
                                    provider.addFont("$path,0", com.itextpdf.io.font.PdfEncodings.IDENTITY_H)
                                } catch (e: Exception) {
                                    provider.addFont(path, com.itextpdf.io.font.PdfEncodings.IDENTITY_H)
                                }
                            } else if (path.endsWith(".ttf", ignoreCase = true) || path.endsWith(".otf", ignoreCase = true)) {
                                provider.addFont(path, com.itextpdf.io.font.PdfEncodings.IDENTITY_H)
                            }
                        } catch (e: Exception) {
                            Log.e("PdfGenerator", "Failed to register font: $path", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error scanning system fonts", e)
        }
        
        try {
            val fonts = provider.fontSet.fonts
            val roboto = fonts.firstOrNull { it.fontName.contains("Roboto", ignoreCase = true) && !it.fontName.contains("Italic", ignoreCase = true) }
            baseFontName = roboto?.fontName ?: fonts.firstOrNull { !it.fontName.contains("Helvetica", ignoreCase = true) }?.fontName
        } catch (e: Exception) {
            Log.e("PdfGenerator", "Error resolving base font name", e)
        }
        
        return provider
    }

    private class WatermarkHandler(watermarkData: ImageData) : IEventHandler {
        private val xObject = PdfImageXObject(watermarkData)

        override fun handleEvent(event: Event?) {
            val docEvent = event as PdfDocumentEvent
            val pdfDoc = docEvent.document
            val page = docEvent.page
            val pdfCanvas = PdfCanvas(page.newContentStreamBefore(), page.resources, pdfDoc)
            
            try {
                val pageSize = page.pageSize
                val x = pageSize.left + pageSize.width / 2
                val y = pageSize.bottom + pageSize.height / 2
                
                // 300x300 watermark centered at 0.08f opacity
                val gs = PdfExtGState().setFillOpacity(0.08f).setStrokeOpacity(0.08f)
                pdfCanvas.saveState()
                pdfCanvas.setExtGState(gs)
                pdfCanvas.addXObjectAt(xObject, x - 150f, y - 150f)
                pdfCanvas.restoreState()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Footer at the bottom of the page: "SmartSwine App"
            try {
                val pageSize = page.pageSize
                val canvas = com.itextpdf.layout.Canvas(
                    PdfCanvas(page.newContentStreamAfter(), page.resources, pdfDoc),
                    pageSize
                )
                canvas.showTextAligned(
                    Paragraph("SmartSwine App")
                        .setFontSize(9f)
                        .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY),
                    pageSize.left + pageSize.width / 2,
                    pageSize.bottom + 15f,
                    TextAlignment.CENTER
                )
                canvas.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getWatermarkData(context: Context): ImageData? {
        synchronized(this) {
            if (cachedWatermarkData != null) return cachedWatermarkData
            return try {
                val options = BitmapFactory.Options()
                options.inScaled = false
                options.inJustDecodeBounds = true
                BitmapFactory.decodeResource(context.resources, R.drawable.app_logo, options)
                
                var inSampleSize = 1
                if (options.outHeight > 300 || options.outWidth > 300) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while (halfHeight / inSampleSize >= 300 || halfWidth / inSampleSize >= 300) {
                        inSampleSize *= 2
                    }
                }
                
                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                
                val rawBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo, options)
                val bitmap = if (rawBitmap != null && (rawBitmap.width != 300 || rawBitmap.height != 300)) {
                    val scaled = Bitmap.createScaledBitmap(rawBitmap, 300, 300, true)
                    rawBitmap.recycle()
                    scaled
                } else {
                    rawBitmap
                }
                val stream = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.PNG, 70, stream)
                bitmap?.recycle()
                val data = stream.toByteArray().let { ImageDataFactory.create(it) }
                cachedWatermarkData = data
                data
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun tr(key: String, lang: String, vararg args: Any) = Translator.getString(key, lang, *args)

    private fun Document.addSection(text: String) {
        add(Paragraph("\n$text").setFontSize(16f).setBold())
    }

    private fun Table.addHeaderRow(lang: String, vararg keys: String) {
        val bg = com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY
        keys.forEach { key ->
            addHeaderCell(com.itextpdf.layout.element.Cell().add(Paragraph(tr(key, lang)).setBold()).setBackgroundColor(bg))
        }
    }

    private fun resolvePigIds(text: String, allPigs: List<com.example.smartswine.model.Pig>): String {
        if (!text.contains("Pig ")) return text
        val parts = text.split("Pig ")
        val result = StringBuilder(parts[0])
        for (i in 1 until parts.size) {
            val remaining = parts[i]
            val idCandidate = remaining.takeWhile { it.isLetterOrDigit() || (it == '-') || (it == '_') }
            val rest = remaining.substring(idCandidate.length)
            val tag = allPigs.find { it.id == idCandidate }?.tagNumber ?: idCandidate
            result.append("Pig ").append(tag).append(rest)
        }
        return result.toString()
    }

    private fun runPdfTask(
        context: Context,
        lang: String,
        noticeKey: String,
        fileNamePrefix: String,
        reportTitle: String,
        onBuild: Document.(PdfDocument) -> Unit
    ) {
        GlobalNotice.show(tr(noticeKey, lang))
        CoroutineScope(Dispatchers.IO).launch {
            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val watermarkData = getWatermarkData(context)

            try {
                val writer = PdfWriter(file)
                val pdf = PdfDocument(writer)
                if (watermarkData != null) {
                    pdf.addEventHandler(PdfDocumentEvent.END_PAGE, WatermarkHandler(watermarkData))
                }
                val document = Document(pdf)
                document.setFontProvider(createFontProvider())
                baseFontName?.let { document.setFontFamily(it) }

                addCommonHeader(context, document, pdf, reportTitle, lang, watermarkData)
                document.onBuild(pdf)
                document.close()

                withContext(Dispatchers.Main) {
                    GlobalNotice.clear()
                    sharePdf(context, file, lang)
                }
            } catch (e: Exception) {
                Log.e("PdfGenerator", "PDF task failed", e)
                withContext(Dispatchers.Main) {
                    GlobalNotice.show("Error: ${e.localizedMessage ?: e.message ?: "Unknown error"}")
                }
            }
        }
    }

    fun generateFeedRequirementPdf(
        context: Context,
        requirements: Map<String, Double>,
        lang: String = "en"
    ) {
        runPdfTask(
            context, lang, "generating_feed_requirements_pdf", "Feed_Requirements",
            tr("feed_requirements_report", lang)
        ) {
            val days = requirements["__days"]?.toInt() ?: 1
            val durationLabel = if (days > 1) {
                val daysWord = tr("days", lang).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                "$days $daysWord"
            } else {
                tr("daily", lang).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + " (kg)"
            }

            val table = Table(UnitValue.createPointArray(floatArrayOf(3f, 1f, 1f))).useAllAvailableWidth()

            table.addHeaderRow(lang, "growth_stage", "daily")
            table.addHeaderCell(com.itextpdf.layout.element.Cell()
                .add(Paragraph(durationLabel).setBold())
                .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY))

            requirements.filter { !it.key.startsWith("__") && !it.key.contains("Total") }.forEach { (label, daily) ->
                val localizedLabel = if (label.contains("(")) {
                    val category = label.substringBefore(" (")
                    val count = label.substringAfter("(").substringBefore(")")
                    "${tr(category.lowercase().replace(" ", "_"), lang)} ($count)"
                } else {
                    tr(label.lowercase().replace(" ", "_"), lang)
                }
                table.addCell(Paragraph(localizedLabel))
                table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", daily)))
                table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", daily * days)))
            }

            val totalDaily = requirements["Total Daily Requirement"] ?: requirements["Daily Total"] ?: 0.0
            table.addCell(Paragraph(tr("total", lang)).setBold())
            table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", totalDaily)).setBold())
            table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", totalDaily * days)).setBold())

            add(table)
            add(Paragraph("\n${tr("disclaimer_vet", lang)}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
                .setBold()
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED))
        }
    }

    fun generateFormulationPdf(
        context: Context,
        title: String,
        ingredients: List<Pair<String, Double>>,
        additives: List<Triple<String, Double, String>>,
        total: Double,
        nutritionalComparison: List<Map<String, Any>>,
        lang: String = "en",
        costPerKg: Double = 0.0,
        costPer50kgBag: Double = 0.0,
        currencySymbol: String = "$"
    ) {
        runPdfTask(
            context, lang, "generating_feed_formulation_pdf", "Feed_Formulation",
            "${tr("feed_formulation_report", lang)} - $title"
        ) {
            addSection(tr("ingredients_composition", lang))

            val table = Table(UnitValue.createPointArray(floatArrayOf(3f, 1f, 2f))).useAllAvailableWidth()
            table.addHeaderRow(lang, "ingredient", "percentage_pct", "qty_batch_size")

            ingredients.forEach { (name, percent) ->
                table.addCell(Paragraph(name))
                table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f%%", percent)))
                table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f kg", (percent / 100.0) * 1000)))
            }

            additives.forEach { (name, percent, note) ->
                table.addCell(Paragraph(name))
                table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f%%", percent)))
                table.addCell(Paragraph(note))
            }

            table.addCell(Paragraph(tr("total", lang)).setBold())
            table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f%%", total)).setBold())
            table.addCell(Paragraph("1000.0 kg").setBold())
            add(table)

            if (costPerKg > 0.0) {
                addSection(tr("cost_analysis", lang))
                val costTable = Table(UnitValue.createPointArray(floatArrayOf(2f, 2f))).useAllAvailableWidth()
                costTable.addHeaderRow(lang, "metric", "cost")
                costTable.addCell(Paragraph(tr("cost_per_kg", lang)))
                costTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", costPerKg)}"))
                costTable.addCell(Paragraph(tr("cost_per_50kg_bag", lang)))
                costTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", costPer50kgBag)}"))
                add(costTable)
            }

            if (total < 99.9) {
                add(Paragraph("\n${tr("formula_incomplete", lang).format(String.format(Locale.getDefault(), "%.1f", total))}")
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED)
                    .setBold()
                    .setFontSize(12f))
            }

            addSection(tr("nutritional_analysis", lang))

            val analysisTable = Table(UnitValue.createPointArray(floatArrayOf(2f, 1f, 1f, 1f))).useAllAvailableWidth()
            analysisTable.addHeaderRow(lang, "nutrient", "target", "actual", "status")

            nutritionalComparison.forEach { data ->
                analysisTable.addCell(Paragraph(data["label"].toString()))
                analysisTable.addCell(Paragraph(String.format(Locale.getDefault(), "%.2f", data["target"])))

                val actualVal = data["actual"] as? Double ?: 0.0
                val isDeficient = data["isDeficient"] as? Boolean ?: false

                val actualCell = Paragraph(String.format(Locale.getDefault(), "%.2f", actualVal))
                actualCell.setFontColor(if (isDeficient) com.itextpdf.kernel.colors.ColorConstants.RED else com.itextpdf.kernel.colors.ColorConstants.GREEN)
                analysisTable.addCell(actualCell)

                val statusKey = if (isDeficient) "deficient" else "ok_status"
                val statusCell = Paragraph(tr(statusKey, lang))
                statusCell.setFontColor(if (isDeficient) com.itextpdf.kernel.colors.ColorConstants.RED else com.itextpdf.kernel.colors.ColorConstants.GREEN)
                analysisTable.addCell(statusCell)
            }
            add(analysisTable)

            add(Paragraph("\n${tr("disclaimer_vet", lang)}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
                .setBold()
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED))
        }
    }

    fun generateAnalyzedFeedPdf(
        context: Context,
        targetStage: String,
        ingredients: List<Pair<String, Double>>,
        isPercentage: Boolean,
        totalWeight: Double,
        profile: com.example.smartswine.ui.feed.FeedNutrientProfile,
        target: com.example.smartswine.model.NutritionalRequirement,
        alerts: List<com.example.smartswine.ui.feed.InclusionSafetyAlert>,
        lang: String = "en",
        currencySymbol: String = "$"
    ) {
        runPdfTask(
            context, lang, "generating_analyzed_feed_pdf", "Feed_Analysis",
            "${tr("feed_analysis_report", lang)} - $targetStage"
        ) {
            addSection(tr("ration_composition", lang))

            val unitLabel = if (isPercentage) "%" else "kg"
            val compTable = Table(UnitValue.createPointArray(floatArrayOf(3f, 1.5f, 1.5f))).useAllAvailableWidth()
            compTable.addHeaderRow(lang, "ingredient", "amount", "inclusion_rate")

            val sumQty = totalWeight.coerceAtLeast(0.0001)
            ingredients.forEach { (name, qty) ->
                compTable.addCell(Paragraph(name))
                compTable.addCell(Paragraph(String.format(Locale.getDefault(), "%.2f %s", qty, unitLabel)))
                val pct = if (isPercentage) qty else (qty / sumQty) * 100.0
                compTable.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f%%", pct)))
            }
            compTable.addCell(Paragraph(tr("total", lang)).setBold())
            compTable.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f %s", totalWeight, unitLabel)).setBold())
            compTable.addCell(Paragraph("100.0%").setBold())
            add(compTable)

            if (profile.costPerKg > 0.0) {
                addSection(tr("cost_analysis", lang))
                val costTable = Table(UnitValue.createPointArray(floatArrayOf(2f, 2f))).useAllAvailableWidth()
                costTable.addHeaderRow(lang, "metric", "cost")
                costTable.addCell(Paragraph(tr("cost_per_kg", lang)))
                costTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", profile.costPerKg)}"))
                costTable.addCell(Paragraph(tr("cost_per_50kg_bag", lang)))
                costTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", profile.costPer50kgBag)}"))
                if (!isPercentage && totalWeight > 0.0) {
                    costTable.addCell(Paragraph(tr("batch_total_cost", lang)).setBold())
                    costTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", profile.totalCost)}").setBold())
                }
                add(costTable)
            }

            addSection("${tr("nutritional_analysis", lang)} ($targetStage)")
            val analysisTable = Table(UnitValue.createPointArray(floatArrayOf(2.5f, 1.2f, 1.2f, 1.2f))).useAllAvailableWidth()
            analysisTable.addHeaderRow(lang, "nutrient", "target", "actual", "status")

            val comparisons = listOf(
                Triple("Crude Protein (%)", target.getTargetCrudeProtein(), profile.crudeProtein),
                Triple("Digestible Protein (%)", target.getTargetDigestibleProtein(), profile.digestibleProtein),
                Triple("ME (kcal/kg)", target.metabolizableEnergy, profile.metabolizableEnergy),
                Triple("Crude Fiber (%)", target.crudeFiber, profile.crudeFiber),
                Triple("Calcium (%)", target.calcium, profile.calcium),
                Triple("Phosphorus (%)", target.phosphorus, profile.phosphorus),
                Triple("Lysine (% diet)", target.getTargetDietaryLysine(), profile.lysine),
                Triple("Methionine+Cys (% diet)", target.getTargetDietaryMethionine(), profile.methionine)
            )

            comparisons.forEach { (nutrientName, tgt, act) ->
                analysisTable.addCell(Paragraph(nutrientName))
                analysisTable.addCell(Paragraph(String.format(Locale.getDefault(), "%.2f", tgt)))
                
                val isFiber = nutrientName.contains("Fiber", ignoreCase = true)
                val isAdequate = if (isFiber) act <= (tgt * 1.05) else act >= (tgt * 0.95)
                
                val actCell = Paragraph(String.format(Locale.getDefault(), "%.2f", act))
                actCell.setFontColor(if (isAdequate) com.itextpdf.kernel.colors.ColorConstants.GREEN else com.itextpdf.kernel.colors.ColorConstants.RED)
                analysisTable.addCell(actCell)

                val statusText = if (act <= 0.001) "-" else if (isAdequate) "OK" else if (isFiber) "Excess" else "Deficient"
                val statusCell = Paragraph(statusText)
                statusCell.setFontColor(if (isAdequate) com.itextpdf.kernel.colors.ColorConstants.GREEN else com.itextpdf.kernel.colors.ColorConstants.RED)
                analysisTable.addCell(statusCell)
            }
            add(analysisTable)

            val ratio = profile.caPRatio
            val ratioStatus = if (ratio in 1.2..1.5) "Optimal (1.2:1 - 1.5:1)" 
                              else if (ratio in 1.0..2.0) "Acceptable (1.0:1 - 2.0:1)" 
                              else if (ratio < 1.0) "Deficient (Inverted Ca:P Risk)" 
                              else "Excessive (Mineral Binding Risk)"
            val ratioColor = if (ratio in 1.2..1.5) com.itextpdf.kernel.colors.ColorConstants.GREEN 
                             else if (ratio in 1.0..2.0) com.itextpdf.kernel.colors.ColorConstants.ORANGE 
                             else com.itextpdf.kernel.colors.ColorConstants.RED
            add(Paragraph("\n${tr("calcium_phosphorus_ratio", lang)}: ${String.format(Locale.getDefault(), "%.2f:1", ratio)} — $ratioStatus")
                .setFontColor(ratioColor)
                .setBold()
                .setFontSize(11f))

            if (alerts.isNotEmpty()) {
                addSection(tr("veterinary_safety_alerts", lang))
                val alertTable = Table(UnitValue.createPointArray(floatArrayOf(2f, 1f, 1f, 3f))).useAllAvailableWidth()
                alertTable.addHeaderRow(lang, "ingredient", "actual", "max_limit", "risk_warning")
                alerts.forEach { alert ->
                    alertTable.addCell(Paragraph(alert.ingredientName).setBold())
                    alertTable.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f%%", alert.currentPercent)))
                    alertTable.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f%%", alert.maxAllowedPercent)))
                    val riskCell = Paragraph(alert.riskDescription).setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED)
                    alertTable.addCell(riskCell)
                }
                add(alertTable)
            }

            add(Paragraph("\n${tr("disclaimer_vet", lang)}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(11f)
                .setBold()
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED))
        }
    }

    fun generateFinancialReportPdf(
        context: Context,
        records: List<com.example.smartswine.model.FinancialRecord>,
        allPigs: List<com.example.smartswine.model.Pig> = emptyList(),
        lang: String = "en",
        reportTitle: String = tr("financial_summary_report", lang),
    ) {
        runPdfTask(context, lang, "generating_financial_report_pdf", "Financial_Report", reportTitle) {
            val totalIncome = records.asSequence().filter { it.type == "Income" }.sumOf { it.amount }
            val totalExpense = records.asSequence().filter { it.type == "Expense" }.sumOf { it.amount }
            val netProfit = totalIncome - totalExpense

            addSection(tr("summary", lang))
            add(Paragraph("${tr("total_income", lang)}: ${String.format(Locale.getDefault(), "%.2f", totalIncome)}"))
            add(Paragraph("${tr("total_expense", lang)}: ${String.format(Locale.getDefault(), "%.2f", totalExpense)}"))
            add(Paragraph("${tr("net_profit_loss", lang)}: ${String.format(Locale.getDefault(), "%.2f", netProfit)}")
                .setFontColor(if (netProfit >= 0) com.itextpdf.kernel.colors.ColorConstants.GREEN else com.itextpdf.kernel.colors.ColorConstants.RED))

            addSection(tr("detailed_transaction_history", lang))

            val table = Table(UnitValue.createPointArray(floatArrayOf(2f, 2f, 3f, 2f, 2f, 2f))).useAllAvailableWidth()
            table.addHeaderRow(lang, "date", "narration", "description", "income", "expense", "balance")

            var runningBalance = 0.0
            records.sortedBy { DateUtils.parseAnyDateNonNull(it.date) }.forEach { record ->
                val displayDescription = resolvePigIds(record.description, allPigs)
                val isIncome = record.type == "Income"
                val incomeVal = if (isIncome) record.amount else 0.0
                val expenseVal = if (!isIncome) record.amount else 0.0
                runningBalance += (incomeVal - expenseVal)

                table.addCell(Paragraph(record.date))
                val rawCategory = record.category
                val translatedCategory = when (rawCategory) {
                    "Pig Sale" -> when (lang) {
                        "fr" -> "Vente de porcs"
                        "zh" -> "猪只销售"
                        "es" -> "Venta de cerdos"
                        "tl" -> "Benta ng Baboy"
                        "vi" -> "Bán lợn"
                        "th" -> "ขายหมู"
                        "pt" -> "Venda de porcos"
                        "hi" -> "सुअर की बिक्री"
                        else -> "Pig Sale"
                    }
                    "Manure Sale" -> when (lang) {
                        "fr" -> "Vente de fumier"
                        "zh" -> "粪便销售"
                        "es" -> "Venta de estiércol"
                        "tl" -> "Benta ng Pataba"
                        "vi" -> "Bán phân"
                        "th" -> "ขายปุ๋ยคอก"
                        "pt" -> "Venda de esterco"
                        "hi" -> "खाद की बिक्री"
                        else -> "Manure Sale"
                    }
                    "Breeding Service" -> when (lang) {
                        "fr" -> "Service d'élevage"
                        "zh" -> "配种服务"
                        "es" -> "Servicio de cría"
                        "tl" -> "Serbisyo sa Pagpaparami"
                        "vi" -> "Dịch vụ phối giống"
                        "th" -> "บริการผสมพันธุ์"
                        "pt" -> "Serviço de reprodução"
                        "hi" -> "प्रजनन सेवा"
                        else -> "Breeding Service"
                    }
                    "Equipment Sale" -> when (lang) {
                        "fr" -> "Vente d'équipement"
                        "zh" -> "设备销售"
                        "es" -> "Venta de equipos"
                        "tl" -> "Benta ng Kagamitan"
                        "vi" -> "Bán thiết bị"
                        "th" -> "ขายอุปกรณ์"
                        "pt" -> "Venda de equipamentos"
                        "hi" -> "उपकरण की बिक्री"
                        else -> "Equipment Sale"
                    }
                    "Vet/Medication" -> when (lang) {
                        "fr" -> "Vétérinaire/Médicaments"
                        "zh" -> "兽医/药物"
                        "es" -> "Veterinario/Medicamentos"
                        "tl" -> "Vet/Gamot"
                        "vi" -> "Thú y/Thuốc"
                        "th" -> "สัตวแพทย์/ยารักษาโรค"
                        "pt" -> "Veterinário/Medicamento"
                        "hi" -> "पशु चिकित्सक/दवा"
                        else -> "Vet/Medication"
                    }
                    "Labor/Salary" -> when (lang) {
                        "fr" -> "Main d'œuvre/Salaire"
                        "zh" -> "人工/工资"
                        "es" -> "Mano de obra/Salario"
                        "tl" -> "Labor/Sahod"
                        "vi" -> "Nhân công/Lương"
                        "th" -> "แรงงาน/เงินเดือน"
                        "pt" -> "Mão de obra/Salário"
                        "hi" -> "श्रम/वेतन"
                        else -> "Labor/Salary"
                    }
                    "Transport" -> when (lang) {
                        "fr" -> "Transport"
                        "zh" -> "运输"
                        "es" -> "Transporte"
                        "tl" -> "Transportasyon"
                        "vi" -> "Vận chuyển"
                        "th" -> "การขนส่ง"
                        "pt" -> "Transporte"
                        "hi" -> "परिवहन"
                        else -> "Transport"
                    }
                    "Rent" -> when (lang) {
                        "fr" -> "Loyer"
                        "zh" -> "租金"
                        "es" -> "Alquiler"
                        "tl" -> "Upa"
                        "vi" -> "Thuê"
                        "th" -> "ค่าเช่า"
                        "pt" -> "Aluguel"
                        "hi" -> "किराया"
                        else -> "Rent"
                    }
                    "Utility" -> when (lang) {
                        "fr" -> "Services publics"
                        "zh" -> "水电费"
                        "es" -> "Servicios públicos"
                        "tl" -> "Kuryente at Tubig"
                        "vi" -> "Tiện ích"
                        "th" -> "สาธารณูปโภค"
                        "pt" -> "Serviços públicos"
                        "hi" -> "उपयोगिता"
                        else -> "Utility"
                    }
                    "Sale" -> tr("cat_sale", lang)
                    "Other" -> tr("cat_other", lang)
                    "Feed" -> tr("cat_feed", lang)
                    "Medicine" -> tr("cat_medicine", lang)
                    "Equipment" -> tr("cat_equipment", lang)
                    "Labor" -> tr("cat_labor", lang)
                    else -> rawCategory
                }
                table.addCell(Paragraph(translatedCategory))
                table.addCell(Paragraph(displayDescription))
                table.addCell(Paragraph(if (incomeVal > 0) String.format(Locale.getDefault(), "%.2f", incomeVal) else ""))
                table.addCell(Paragraph(if (expenseVal > 0) String.format(Locale.getDefault(), "%.2f", expenseVal) else ""))
                table.addCell(Paragraph(String.format(Locale.getDefault(), "%.2f", runningBalance)))
            }
            add(table)
        }
    }

    fun generateHerdReportPdf(
        context: Context,
        pigs: List<com.example.smartswine.model.Pig>,
        allPigs: List<com.example.smartswine.model.Pig> = emptyList(),
        healthRecords: Map<String, List<com.example.smartswine.model.HealthRecord>> = emptyMap(),
        lang: String = "en",
        reportTitle: String = tr("herd_data_report", lang),
    ) {
        runPdfTask(context, lang, "generating_herd_report_pdf", "Herd_Report", reportTitle) {
            if (!reportTitle.contains("Profile")) {
                val breeders = pigs.filter { it.purpose == "Breeder" }
                val porkers = pigs.filter { it.purpose == "Porker" }
                val m = tr("males", lang); val f = tr("females", lang)

                add(Paragraph(tr("herd_summary_label", lang)).setBold().setFontSize(16f))
                add(Paragraph(tr("herd_total_pigs_label", lang, pigs.size)))
                add(Paragraph(tr("herd_total_breeders_label", lang, breeders.size)))
                add(Paragraph("   ${tr("herd_breakdown_breeders", lang)}: $m: ${breeders.count { it.gender == "Male" }}, $f: ${breeders.count { it.gender == "Female" }}"))
                add(Paragraph(tr("herd_total_porkers_label", lang, porkers.size)))
                add(Paragraph("   ${tr("herd_breakdown_porkers", lang)}: $m: ${porkers.count { it.gender == "Male" }}, $f: ${porkers.count { it.gender == "Female" }}"))
                add(Paragraph("\n"))
            }

            val table = Table(UnitValue.createPointArray(floatArrayOf(2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f))).useAllAvailableWidth()
            table.addHeaderRow(lang, "tag_hash", "gender", "breed", "purpose", "weight_kg", "pen_hash", "dob", "age")

            pigs.forEach { pig ->
                table.addCell(Paragraph(pig.tagNumber))
                table.addCell(Paragraph(tr(pig.gender.lowercase(), lang)))
                table.addCell(Paragraph(pig.breed.ifEmpty { tr("not_specified", lang) }))
                table.addCell(Paragraph(tr(pig.purpose.lowercase(), lang)))
                table.addCell(Paragraph(pig.weight.toString()))
                table.addCell(Paragraph(pig.location))
                table.addCell(Paragraph(pig.birthDate))

                val ageText = com.example.smartswine.utils.DateUtils.formatSwineAge(pig.birthDate, compact = true, lang = lang)
                table.addCell(Paragraph(ageText))
            }
            add(table)

            if (healthRecords.isNotEmpty()) {
                addSection(tr("history", lang))
                healthRecords.forEach { (pigId, records) ->
                    val pigTag = pigs.find { it.id == pigId }?.tagNumber ?: allPigs.find { it.id == pigId }?.tagNumber ?: pigId
                    add(Paragraph("${tr("target_animal", lang)}: $pigTag").setBold().setFontSize(14f))

                    val hTable = Table(UnitValue.createPointArray(floatArrayOf(3f, 2f, 5f))).useAllAvailableWidth()
                    hTable.addHeaderRow(lang, "activity_type", "date", "notes")

                    records.forEach { record ->
                        hTable.addCell(Paragraph(tr(record.type.lowercase().replace(" ", "_").replace("/", "_"), lang)))
                        hTable.addCell(Paragraph(record.date))

                        var notes = resolvePigIds(record.description, allPigs)
                        if (record.medication.isNotEmpty()) notes += "\n${tr("medication", lang)}: ${record.medication}"
                        hTable.addCell(Paragraph(notes))
                    }
                    add(hTable)
                    add(Paragraph("\n"))
                }
            }
        }
    }

    fun generateFeedReportPdf(
        context: Context,
        feedItems: List<com.example.smartswine.model.FeedInventoryItem>,
        transactions: List<com.example.smartswine.model.FeedInventoryTransaction>,
        reportTitle: String? = null,
        lang: String = "en"
    ) {
        val finalTitle = reportTitle ?: tr("feed_inventory_report", lang)
        runPdfTask(context, lang, "generating_feed_report_pdf", "Feed_Report", finalTitle) {
            val table = Table(UnitValue.createPointArray(floatArrayOf(3f, 2f, 2f, 2f, 2f, 2f))).useAllAvailableWidth()
            
            val unitHeader = when (lang) {
                "fr" -> "Unité"
                "es" -> "Unidad"
                "zh" -> "单位"
                "tl" -> "Yunit"
                "vi" -> "Đơn vị"
                "th" -> "หน่วย"
                "pt" -> "Unidade"
                "hi" -> "इकाई"
                else -> "Unit"
            }
            
            val bg = com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY
            table.addHeaderCell(com.itextpdf.layout.element.Cell().add(Paragraph(tr("feed_name", lang)).setBold()).setBackgroundColor(bg))
            table.addHeaderCell(com.itextpdf.layout.element.Cell().add(Paragraph(tr("feed_type", lang)).setBold()).setBackgroundColor(bg))
            table.addHeaderCell(com.itextpdf.layout.element.Cell().add(Paragraph(unitHeader).setBold()).setBackgroundColor(bg))
            table.addHeaderCell(com.itextpdf.layout.element.Cell().add(Paragraph(tr("addition", lang)).setBold()).setBackgroundColor(bg))
            table.addHeaderCell(com.itextpdf.layout.element.Cell().add(Paragraph(tr("usage", lang)).setBold()).setBackgroundColor(bg))
            table.addHeaderCell(com.itextpdf.layout.element.Cell().add(Paragraph(tr("in_stock", lang)).setBold()).setBackgroundColor(bg))

            var totalAdditionInKg = 0.0
            var totalUsageInKg = 0.0
            var totalInStockInKg = 0.0

            feedItems.forEach { item ->
                val itemTransactions = transactions.filter { it.itemId == item.id }
                
                val getConvertedQty = { tx: com.example.smartswine.model.FeedInventoryTransaction ->
                    when (tx.unit) {
                        item.unit -> tx.quantity
                        "bags" -> if (item.unit == "kg") tx.quantity * item.unitWeight else tx.quantity
                        "kg" -> if (item.unit == "bags" && item.unitWeight > 0.0) tx.quantity / item.unitWeight else tx.quantity
                        else -> tx.quantity
                    }
                }
                
                val addition = itemTransactions.filter { it.type == "Restock" }.sumOf { getConvertedQty(it) }
                val usage = itemTransactions.filter { it.type == "Usage" }.sumOf { getConvertedQty(it) }
                val inStock = item.quantity

                val additionInKg = addition * (if (item.unit == "bags") item.unitWeight else 1.0)
                val usageInKg = usage * (if (item.unit == "bags") item.unitWeight else 1.0)
                val inStockInKg = inStock * (if (item.unit == "bags") item.unitWeight else 1.0)

                totalAdditionInKg += additionInKg
                totalUsageInKg += usageInKg
                totalInStockInKg += inStockInKg

                val feedTypeKey = item.feedType.lowercase().replace(" ", "_")
                val translatedFeedType = tr(feedTypeKey, lang)
                val displayFeedType = if (translatedFeedType == feedTypeKey) item.feedType else translatedFeedType
                
                val unitTranslation = when (lang) {
                    "fr" -> if (item.unit == "bags") "Sacs" else "kg"
                    "es" -> if (item.unit == "bags") "Sacos" else "kg"
                    "zh" -> if (item.unit == "bags") "袋" else "公斤"
                    "tl" -> if (item.unit == "bags") "Bags" else "kg"
                    "vi" -> if (item.unit == "bags") "Bao" else "kg"
                    "th" -> if (item.unit == "bags") "กระสอบ" else "กิโลกรัม"
                    "pt" -> if (item.unit == "bags") "Sacos" else "kg"
                    "hi" -> if (item.unit == "bags") "बोरी" else "किग्रा"
                    else -> if (item.unit == "bags") "Bags" else "kg"
                }

                table.addCell(Paragraph(item.name))
                table.addCell(Paragraph(displayFeedType))
                table.addCell(Paragraph(unitTranslation))
                table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", addition)))
                table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", usage)))
                table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", inStock)))
            }

            table.addCell(Paragraph(tr("total", lang) + " (kg)").setBold())
            table.addCell(Paragraph(""))
            table.addCell(Paragraph(""))
            table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", totalAdditionInKg)).setBold())
            table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", totalUsageInKg)).setBold())
            table.addCell(Paragraph(String.format(Locale.getDefault(), "%.1f", totalInStockInKg)).setBold())
            add(table)
        }
    }

    fun generateHRReportPdf(
        context: Context,
        staff: List<com.example.smartswine.model.StaffMember>,
        currencySymbol: String,
        financialRecords: List<com.example.smartswine.model.FinancialRecord>,
        languageCode: String = "en"
    ) {
        runPdfTask(context, languageCode, "generating_hr_report_pdf", "HR_Report", tr("hr_report", languageCode)) {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
            val totalStaff = staff.size
            val activeStaff = staff.count { !it.status.equals("Inactive", ignoreCase = true) && !it.status.equals("Archived", ignoreCase = true) }
            val totalMonthlyPayroll = staff.filter { !it.status.equals("Archived", ignoreCase = true) }.sumOf { it.salary }

            val staffWithYtd = staff.map { member ->
                val memberPayments = financialRecords.filter {
                    (it.category.equals("Salary", ignoreCase = true) || it.category.equals("Labor/Salary", ignoreCase = true)) &&
                    it.description.contains(member.name, ignoreCase = true)
                }
                val ytd = memberPayments.filter { it.date.startsWith(currentYear) }.sumOf { it.amount }
                Triple(member, ytd, memberPayments)
            }
            val totalYtdPayroll = staffWithYtd.sumOf { it.second }

            addSection(tr("summary", languageCode))
            add(Paragraph("${tr("total_staff", languageCode)}: $totalStaff"))
            add(Paragraph("${tr("active_staff", languageCode)}: $activeStaff"))
            add(Paragraph("${tr("monthly_payroll", languageCode)}: $currencySymbol${String.format(Locale.getDefault(), "%.2f", totalMonthlyPayroll)}"))
            add(Paragraph("${tr("total_paid_ytd", languageCode)} ($currentYear): $currencySymbol${String.format(Locale.getDefault(), "%.2f", totalYtdPayroll)}").setBold())

            addSection(tr("staff_details", languageCode))
            val staffTable = Table(UnitValue.createPointArray(floatArrayOf(120f, 90f, 70f, 90f, 80f, 80f))).useAllAvailableWidth()
            staffTable.addHeaderRow(languageCode, "staff_name", "role", "status", "phone", "monthly_salary", "ytd_salary")

            staffWithYtd.forEach { (member, ytd, _) ->
                staffTable.addCell(Paragraph(member.name))
                staffTable.addCell(Paragraph(member.role))
                staffTable.addCell(Paragraph(member.status))
                staffTable.addCell(Paragraph(member.phone.ifBlank { "—" }))
                staffTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", member.salary)}"))
                staffTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", ytd)}"))
            }
            add(staffTable)

            addSection(tr("payroll_history", languageCode))
            val historyTable = Table(UnitValue.createPointArray(floatArrayOf(90f, 180f, 90f, 80f))).useAllAvailableWidth()
            historyTable.addHeaderRow(languageCode, "activity_date", "description", "amount", "category_label")

            val salaryPayments = financialRecords.filter { 
                it.category.equals("Salary", ignoreCase = true) || it.category.equals("Labor/Salary", ignoreCase = true)
            }.sortedByDescending { DateUtils.parseAnyDateNonNull(it.date) }

            var totalPeriod = 0.0
            salaryPayments.forEach { record ->
                historyTable.addCell(Paragraph(record.date))
                historyTable.addCell(Paragraph(record.description))
                historyTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", record.amount)}"))
                historyTable.addCell(Paragraph(tr(record.category.lowercase().replace(" ", "_"), languageCode)))
                totalPeriod += record.amount
            }
            add(historyTable)
            add(Paragraph("\n${tr("total_payments_period", languageCode, "$currencySymbol${String.format(Locale.getDefault(), "%.2f", totalPeriod)}")}").setBold())
        }
    }

    fun generateStaffDetailPdf(
        context: Context,
        member: com.example.smartswine.model.StaffMember,
        currencySymbol: String,
        financialRecords: List<com.example.smartswine.model.FinancialRecord>,
        languageCode: String = "en"
    ) {
        val sanitizedName = member.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Staff" }
        runPdfTask(
            context, languageCode, "generating_staff_report_pdf", "Staff_${sanitizedName}",
            "${tr("employee_profile_report", languageCode)} - ${member.name}"
        ) {
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
            val memberSalaryPayments = financialRecords.filter {
                (it.category.equals("Salary", ignoreCase = true) || it.category.equals("Labor/Salary", ignoreCase = true)) &&
                it.description.contains(member.name, ignoreCase = true)
            }.sortedByDescending { DateUtils.parseAnyDateNonNull(it.date) }

            val ytdSalary = memberSalaryPayments.filter { record ->
                val d = DateUtils.parseAnyDate(record.date)
                if (d != null) {
                    val cal = java.util.Calendar.getInstance().apply { time = d }
                    cal.get(java.util.Calendar.YEAR).toString() == currentYear
                } else {
                    record.date.contains(currentYear)
                }
            }.sumOf { it.amount }
            val lifetimeSalary = memberSalaryPayments.sumOf { it.amount }

            // Profile Information
            addSection(tr("staff_profile", languageCode))
            val infoTable = Table(UnitValue.createPointArray(floatArrayOf(150f, 350f))).useAllAvailableWidth()
            fun addInfoRow(labelKey: String, value: String) {
                infoTable.addCell(Paragraph(tr(labelKey, languageCode)).setBold())
                infoTable.addCell(Paragraph(value.ifBlank { "—" }))
            }
            addInfoRow("staff_name", member.name)
            addInfoRow("role", member.role)
            addInfoRow("status", member.status)
            addInfoRow("phone_number", member.phone)
            addInfoRow("staff_email", member.email)
            addInfoRow("residential_address", member.residentialAddress)
            addInfoRow("date_of_birth", member.dateOfBirth)
            addInfoRow("gender", member.gender)
            addInfoRow("join_date", member.joinDate)
            addInfoRow("system_access", if (member.allowAppAccess) "${tr("enabled", languageCode)} (${member.inviteStatus})" else tr("disabled", languageCode))
            add(infoTable)

            // Emergency Contact
            if (member.emergencyContactName.isNotBlank() || member.emergencyContactPhone.isNotBlank()) {
                addSection(tr("emergency_contact_details", languageCode))
                val emTable = Table(UnitValue.createPointArray(floatArrayOf(150f, 350f))).useAllAvailableWidth()
                fun addEmRow(label: String, value: String) {
                    emTable.addCell(Paragraph(label).setBold())
                    emTable.addCell(Paragraph(value.ifBlank { "—" }))
                }
                addEmRow(tr("name", languageCode), member.emergencyContactName)
                addEmRow(tr("relationship", languageCode), member.emergencyContactRelation)
                addEmRow(tr("phone_number", languageCode), member.emergencyContactPhone)
                if (member.emergencyContactAddress.isNotBlank()) {
                    addEmRow(tr("residential_address", languageCode), member.emergencyContactAddress)
                }
                add(emTable)
            }

            // Compensation Summary
            addSection(tr("compensation_summary", languageCode))
            val compTable = Table(UnitValue.createPointArray(floatArrayOf(150f, 350f))).useAllAvailableWidth()
            compTable.addCell(Paragraph(tr("monthly_salary", languageCode)).setBold())
            compTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", member.salary)}"))
            compTable.addCell(Paragraph("${tr("total_paid_ytd", languageCode)} ($currentYear)").setBold())
            compTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", ytdSalary)}").setBold())
            compTable.addCell(Paragraph(tr("total_lifetime_salary", languageCode)).setBold())
            compTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", lifetimeSalary)}").setBold())
            add(compTable)

            // Salary Payment History
            addSection(tr("salary_payment_history", languageCode))
            if (memberSalaryPayments.isEmpty()) {
                add(Paragraph(tr("no_salary_records_found", languageCode)).setItalic())
            } else {
                val histTable = Table(UnitValue.createPointArray(floatArrayOf(90f, 320f, 100f))).useAllAvailableWidth()
                histTable.addHeaderRow(languageCode, "activity_date", "description", "amount")
                memberSalaryPayments.forEach { record ->
                    histTable.addCell(Paragraph(record.date))
                    histTable.addCell(Paragraph(record.description))
                    histTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", record.amount)}"))
                }
                histTable.addCell(Paragraph(tr("total", languageCode)).setBold())
                histTable.addCell(Paragraph(""))
                histTable.addCell(Paragraph("$currencySymbol${String.format(Locale.getDefault(), "%.2f", lifetimeSalary)}").setBold())
                add(histTable)
            }
        }
    }

    fun generateAnalyzedFeedPdf(
        context: Context,
        targetStage: String,
        ingredients: List<Pair<String, Double>>,
        isPercentage: Boolean,
        totalWeight: Double,
        profile: FeedNutrientProfile,
        target: NutritionalRequirement,
        alerts: List<IngredientSafetyAlert>,
        lang: String = "en"
    ) {
        runPdfTask(
            context, lang, "generating_feed_analysis_pdf", "Analyzed_Feed_Ration",
            "${tr("feed_analysis_report", lang)} - $targetStage"
        ) {
            addSection(tr("ingredients_composition", lang))

            val table = Table(UnitValue.createPointArray(floatArrayOf(3f, 2f, 2f))).useAllAvailableWidth()
            val col2Header = if (isPercentage) "percentage_pct" else "weight_kg"
            table.addHeaderRow(lang, "ingredient", col2Header, "calculated_inclusion")

            ingredients.forEach { (name, qty) ->
                table.addCell(Paragraph(name))
                val qtyStr = if (isPercentage) String.format(Locale.getDefault(), "%.1f%%", qty) else String.format(Locale.getDefault(), "%.1f kg", qty)
                val calcStr = if (isPercentage) String.format(Locale.getDefault(), "%.1f kg (per 100kg)", qty) else String.format(Locale.getDefault(), "%.1f%%", if (totalWeight > 0) (qty / totalWeight) * 100.0 else 0.0)
                table.addCell(Paragraph(qtyStr))
                table.addCell(Paragraph(calcStr))
            }

            table.addCell(Paragraph(tr("total", lang)).setBold())
            val totalStr = if (isPercentage) String.format(Locale.getDefault(), "%.1f%%", totalWeight) else String.format(Locale.getDefault(), "%.1f kg", totalWeight)
            table.addCell(Paragraph(totalStr).setBold())
            table.addCell(Paragraph("—").setBold())
            add(table)

            addSection(tr("nutritional_analysis", lang))

            val analysisTable = Table(UnitValue.createPointArray(floatArrayOf(2f, 1f, 1f, 1f))).useAllAvailableWidth()
            analysisTable.addHeaderRow(lang, "nutrient", "target", "actual", "status")

            val comparisons = listOf(
                Triple("Crude Protein (CP)", target.getTargetCrudeProtein(), profile.crudeProtein),
                Triple("Digestible Protein", target.getTargetDigestibleProtein(), profile.digestibleProtein),
                Triple("Metabolizable Energy (kcal/kg)", target.metabolizableEnergy, profile.metabolizableEnergy),
                Triple("Calcium (Ca %)", target.calcium, profile.calcium),
                Triple("Phosphorus (P %)", target.phosphorus, profile.phosphorus),
                Triple("Dietary Lysine (%)", target.getTargetDietaryLysine(), profile.lysine),
                Triple("Dietary Methionine (%)", target.getTargetDietaryMethionine(), profile.methionine)
            )

            comparisons.forEach { (label, tgtVal, actVal) ->
                analysisTable.addCell(Paragraph(label))
                analysisTable.addCell(Paragraph(String.format(Locale.getDefault(), "%.2f", tgtVal)))

                val isOK = actVal >= tgtVal * 0.95
                val actCell = Paragraph(String.format(Locale.getDefault(), "%.2f", actVal))
                actCell.setFontColor(if (isOK) ColorConstants.GREEN else ColorConstants.RED)
                analysisTable.addCell(actCell)

                val statusCell = Paragraph(if (isOK) "OK" else "Deficient")
                statusCell.setFontColor(if (isOK) ColorConstants.GREEN else ColorConstants.RED)
                analysisTable.addCell(statusCell)
            }
            add(analysisTable)

            if (alerts.isNotEmpty()) {
                addSection("Veterinary Safety Alerts")
                alerts.forEach { alert ->
                    add(Paragraph("${alert.ingredientName}: ${alert.riskDescription}")
                        .setFontColor(ColorConstants.RED)
                        .setFontSize(10f))
                }
            }

            add(Paragraph("\n${tr("disclaimer_vet", lang)}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10f)
                .setBold()
                .setFontColor(ColorConstants.RED))
        }
    }

    private fun sharePdf(context: Context, file: File, lang: String = "en") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chooser = Intent.createChooser(intent, Translator.getString("open_pdf", lang))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            GlobalNotice.show(Translator.getString("no_pdf_app_found", lang))
        }
    }

    private data class FarmHeaderInfo(val logo: ImageData?, val farmName: String?)

    private fun getFarmHeaderInfo(context: Context): FarmHeaderInfo {
        var logoData: ImageData? = null
        var farmName: String? = null
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val task = FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid).get()
                val snapshot = Tasks.await(task, 3, TimeUnit.SECONDS)
                farmName = snapshot.getString("farmName") ?: snapshot.getString("name")
                val farmLogo = snapshot.getString("farmLogo")
                if (!farmLogo.isNullOrBlank()) {
                    val cleanBase64 = if (farmLogo.contains(",")) farmLogo.substringAfter(",") else farmLogo
                    val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bmp != null) {
                        val stream = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.PNG, 85, stream)
                        bmp.recycle()
                        logoData = ImageDataFactory.create(stream.toByteArray())
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("PdfGenerator", "Using fallback logo/name: ${e.message}")
        }
        if (logoData == null) {
            logoData = getWatermarkData(context)
        }
        return FarmHeaderInfo(logoData, farmName)
    }

    private fun addCommonHeader(
        context: Context,
        document: Document,
        pdfDoc: PdfDocument,
        reportTitle: String,
        lang: String,
        logoData: ImageData?
    ) {
        val farmInfo = getFarmHeaderInfo(context)
        val rightLogo = farmInfo.logo ?: logoData
        if (rightLogo != null) {
            try {
                val logo = Image(rightLogo)
                logo.scaleToFit(50f, 50f)
                val pageSize = pdfDoc.defaultPageSize
                logo.setFixedPosition(1, pageSize.right - 70f, pageSize.top - 70f)
                document.add(logo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val headerFarmName = farmInfo.farmName?.takeIf { it.isNotBlank() } ?: "SmartSwine Farm"
        document.add(Paragraph(headerFarmName)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(22f)
            .setBold()
            .setFontColor(com.itextpdf.kernel.colors.ColorConstants.BLACK))

        document.add(Paragraph(reportTitle)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(16f)
            .setBold()
            .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY)
            .setMarginTop(4f))

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateString = dateFormat.format(java.util.Date())
        val generatedOn = Translator.getString("generated_on", lang, "Generated on: %s").format(dateString)
        document.add(Paragraph(generatedOn)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10f))

        document.add(Paragraph("Exported with the SmartSwine App")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(9f)
            .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY))

        document.add(Paragraph("\n"))
    }
}




