package com.myai.app.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * خواندن محتوای فایل‌های متنی انتخاب‌شده توسط کاربر
 */
class FileHelper(private val context: Context) {

    data class FileInfo(
        val name: String,
        val size: Long,
        val content: String,
        val mimeType: String?
    )

    /**
     * خواندن محتوای فایل از Uri
     * فعلاً از فایل‌های متنی پشتیبانی می‌کند (txt, md, json, csv, ...)
     */
    fun readTextFile(uri: Uri): Result<FileInfo> {
        return try {
            val name = getFileName(uri) ?: "فایل ناشناس"
            val size = getFileSize(uri)
            val mimeType = context.contentResolver.getType(uri)

            // محدودیت اندازه برای جلوگیری از مشکل حافظه (حداکثر ۲ مگابایت متن)
            if (size > 2 * 1024 * 1024) {
                return Result.failure(Exception("حجم فایل بیش از حد مجاز است (حداکثر ۲ مگابایت)"))
            }

            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return Result.failure(Exception("نتوانستم فایل را بخوانم"))

            if (content.isBlank()) {
                return Result.failure(Exception("فایل خالی است"))
            }

            Result.success(
                FileInfo(
                    name = name,
                    size = size,
                    content = content,
                    mimeType = mimeType
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("خطا در خواندن فایل: ${e.message}"))
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }

    /**
     * ساخت پرامپت برای خلاصه‌سازی فایل
     */
    fun buildSummarizePrompt(fileInfo: FileInfo): String {
        val preview = if (fileInfo.content.length > 12000) {
            fileInfo.content.take(12000) + "\n\n[... ادامه فایل بریده شد ...]"
        } else {
            fileInfo.content
        }

        return """
            لطفاً این فایل را خلاصه کن و نکات مهم آن را به زبان فارسی و واضح بگو.
            
            نام فایل: ${fileInfo.name}
            
            محتوای فایل:
            $preview
        """.trimIndent()
    }
}
