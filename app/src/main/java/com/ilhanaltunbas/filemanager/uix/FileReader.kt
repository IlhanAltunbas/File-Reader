package com.ilhanaltunbas.filemanager.uix

import android.content.Context
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DateUtil
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor

class FileReader(private val context: Context) {

    companion object {
        private const val MAX_PREVIEW_LINES = 500
        private const val MAX_FILE_SIZE_MB = 5
        private const val MAX_WORD_PREVIEW_CHARS = 10000
    }

    fun readFile(file: File): String {
        Log.d("FileReader", "readFile çağrıldı: ${file.name}, Uzantı: ${file.extension}")
        if (!file.exists()) {
            Log.w("FileReader", "Dosya bulunamadı: ${file.path}")
            return "Dosya bulunamadı"
        }
        if (file.length() > MAX_FILE_SIZE_MB * 1024 * 1024) {
            Log.w("FileReader", "Dosya boyutu çok büyük: ${file.length()} bytes")
            return "Dosya boyutu çok büyük (max ${MAX_FILE_SIZE_MB}MB)"
        }

        val fileExtension = file.extension.lowercase()
        Log.d("FileReader", "İşlenecek uzantı: $fileExtension")

        return when (fileExtension) {
            "txt","plain" -> {
                Log.d("FileReader", "txt dosyası okunuyor.")
                readTextFile(file)
            }
            "pdf" -> {
                Log.d("FileReader", "pdf dosyası okunuyor.")
                readPdfFile(file)
            }
            "xls", "ms-excel" -> {
                Log.d("FileReader", "xls dosyası okunuyor.")
                readXlsFile(file)
            }
            "doc","msword" -> {
                Log.d("FileReader", "doc dosyası okunuyor.")
                readDocFile(file)
            }
            else -> {
                Log.w("FileReader", "Desteklenmeyen format: $fileExtension")
                "Desteklenmeyen dosya formatı: $fileExtension"
            }
        }
    }

    private fun readTextFile(file: File): String {
        return try {
            file.bufferedReader().useLines { lines ->
                lines.take(MAX_PREVIEW_LINES).joinToString("\n")
            }
        } catch (e: Exception) {
            "Metin dosyası okunamadı: ${e.message}"
        }
    }

    private fun readPdfFile(file: File): String {
        var document: PDDocument? = null
        return try {
            document = PDDocument.load(file)
            val stripper = PDFTextStripper()
            stripper.getText(document)
        } catch (e: Exception) {
            "PDF okuma hatası: ${e.message}"
        } finally {
            document?.close()
        }
    }

    private fun readXlsFile(file: File): String {
        var inputStream: InputStream? = null
        var workbook: HSSFWorkbook? = null
        return try {
            inputStream = FileInputStream(file)
            workbook = HSSFWorkbook(inputStream)

            val text = StringBuilder()
            if (workbook.numberOfSheets == 0) {
                return "Excel dosyasında sayfa bulunamadı."
            }
            val sheet = workbook.getSheetAt(0)

            var lineCount = 0
            for (row in sheet) {
                if (lineCount >= MAX_PREVIEW_LINES) break
                var cellCountInRow = 0
                for (cell in row) {
                    when (cell.cellType) {
                        Cell.CELL_TYPE_STRING -> text.append(cell.stringCellValue)
                        Cell.CELL_TYPE_NUMERIC -> {
                            if (DateUtil.isCellDateFormatted(cell)) {
                                text.append(cell.dateCellValue?.toString() ?: "")
                            } else {
                                text.append(cell.numericCellValue.toString())
                            }
                        }
                        Cell.CELL_TYPE_BOOLEAN -> text.append(cell.booleanCellValue.toString())
                        Cell.CELL_TYPE_FORMULA -> {
                            when (cell.cachedFormulaResultType) {
                                Cell.CELL_TYPE_STRING -> text.append(cell.stringCellValue)
                                Cell.CELL_TYPE_NUMERIC -> {
                                    if (DateUtil.isCellDateFormatted(cell)) {
                                        text.append(cell.dateCellValue?.toString() ?: "")
                                    } else {
                                        text.append(cell.numericCellValue.toString())
                                    }
                                }
                                Cell.CELL_TYPE_BOOLEAN -> text.append(cell.booleanCellValue.toString())
                                Cell.CELL_TYPE_ERROR -> text.append("[Formül Hatası: ${cell.errorCellValue}]")
                                else -> text.append("[Formül: ${cell.cellFormula}]")
                            }
                        }
                        Cell.CELL_TYPE_BLANK -> { /* text.append("[BOŞ]") */ }
                        Cell.CELL_TYPE_ERROR -> text.append("[Hücre Hatası: ${cell.errorCellValue}]")
                        else -> { /* Diğer bilinmeyen hücre tipleri */ }
                    }
                    text.append("\t")
                    cellCountInRow++
                }
                if (cellCountInRow > 0) {
                    text.append("\n")
                }
                lineCount++
            }

            var result = text.toString()
            if (lineCount >= MAX_PREVIEW_LINES) {
                result += "\n\n... (devamı gösterilmiyor)"
            }
            result
        } catch (e: Exception) {
            "XLS okuma hatası: ${e.localizedMessage}"
        } catch (ome: OutOfMemoryError) {
            "XLS dosyası çok büyük veya karmaşık, işlenemedi (Bellek hatası)."
        } finally {
            try {
                workbook?.close()
            } catch (e: Exception) { /* Kapatma hatası */ }
            try {
                inputStream?.close()
            } catch (e: Exception) { /* Kapatma hatası */ }
        }
    }

    private fun readDocFile(file: File): String {
        return try {
            FileInputStream(file).use { inputStream ->
                val document = HWPFDocument(inputStream)
                val extractor = WordExtractor(document)
                val text = extractor.text ?: ""
                if (text.length > MAX_WORD_PREVIEW_CHARS) {
                    text.substring(0, MAX_WORD_PREVIEW_CHARS) + "\n\n... (devamı gösterilmiyor)"
                } else {
                    text
                }
            }
        } catch (e: Exception) {
            "DOC okuma hatası: ${e.localizedMessage}"
        } catch (ome: OutOfMemoryError) {
            "DOC dosyası çok büyük veya karmaşık, işlenemedi (Bellek hatası)."
        }
    }




    // .docx dosyalarını okumak için fonksiyon (XWPF)
    // Bu zaten dolaylı olarak vardı ama ayrı bir fonksiyon olarak daha net.

}