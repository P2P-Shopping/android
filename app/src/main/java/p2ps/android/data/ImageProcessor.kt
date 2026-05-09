package p2ps.android.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageProcessor {

    private const val MAX_DIMENSION = 1920
    private const val COMPRESSION_QUALITY = 80

    fun processAndCompressImage(context: Context, imageUri: Uri): String? {
        return try {
            // 1. Aflăm dimensiunile originale fără a încărca imaginea completă în memorie (evităm OOM)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // 2. Calculăm factorul de scalare (inSampleSize)
            options.inSampleSize = calculateInSampleSize(options, MAX_DIMENSION, MAX_DIMENSION)
            options.inJustDecodeBounds = false

            // 3. Încărcăm imaginea scalată
            var bitmap = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return null

            // 4. Corectăm rotația folosind EXIF
            bitmap = rotateImageIfRequired(context, bitmap, imageUri)

            // 5. Mai facem un resize exact la MAX_DIMENSION dacă e necesar (inSampleSize e putere a lui 2)
            bitmap = scaleBitmap(bitmap, MAX_DIMENSION)

            // 6. Comprimăm în JPEG (sau WEBP) și convertim în Base64
            val outputStream = ByteArrayOutputStream()
            // Folosim WEBP dacă ești pe API 30+, altfel JPEG. Aici folosim JPEG pentru compatibilitate maximă.
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            val byteArray = outputStream.toByteArray()

            bitmap.recycle() // Eliberăm memoria

            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
        val input: InputStream? = context.contentResolver.openInputStream(selectedImage)
        val ei = input?.let { ExifInterface(it) }
        val orientation = ei?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val finalWidth: Int
        val finalHeight: Int

        if (width > height) {
            finalWidth = maxDimension
            finalHeight = (maxDimension / ratio).toInt()
        } else {
            finalHeight = maxDimension
            finalWidth = (maxDimension * ratio).toInt()
        }

        val scaled = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }
}