package com.mama.views.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.graphics.Shader.TileMode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.media.ExifInterface
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import androidx.annotation.RawRes
import java.io.*
import java.nio.ByteBuffer

/**
 * The BitmapUtil apply a series of methods to create bitmap or change bitmap
 * shape or scale bitmap or mirror bitmap or save the bitmap~~;
 *
 * @author mazongkun
 */
object BitmapUtil {
    private val mMatrix = Matrix()
    private const val TAG = "BitmapUtil"
    private const val DEBUG = false
    private val matrix: Matrix
        private get() {
            mMatrix.reset()
            return mMatrix
        }

    fun converYUVtoJPG(
        data: ByteArray,
        outName: String,
        width: Int,
        height: Int,
        dir: String
    ) {
        if (data.size == 0) {
            Log.d(TAG, "data is null!")
            return
        }
        val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
        val bos = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, bos)
        val outJpgName = outName.replace("yuv", "jpg")
        try {
            val save = File(dir)
            if (!save.exists()) {
                save.mkdir()
            }
            val file = File(dir + outJpgName)
            val fos = FileOutputStream(file)
            fos.write(bos.toByteArray())
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun resizeBitmap(bitmap: Bitmap): Bitmap {
        var w = bitmap.width
        var h = bitmap.height
        if (bitmap.width % 2 == 1) {
            w = bitmap.width - 1
        }
        if (bitmap.height % 2 == 1) {
            h = bitmap.height - 1
        }
        return zoomBitmap(bitmap, w, h)
    }

    fun zoomBitmapLimit(bm: Bitmap, limitShort: Int, limitLong: Int): Bitmap? {
        if (isEmty(bm)) return null
        val width = bm.width
        val height = bm.height
        val longSide = if (width > height) width else height
        val shortSide = if (width > height) height else width
        if (longSide <= limitLong && shortSide <= limitShort) {
            // no need to resize
            return bm
        }

        // calculate ratio
        val longRatio = 1.0f * longSide / limitLong
        val shortRatio = 1.0f * shortSide / limitShort
        val baseRatio = if (longRatio > shortRatio) longRatio else shortRatio
        val resizeLong = (longSide / baseRatio).toInt()
        val resizeShort = (shortSide / baseRatio).toInt()
        Log.d(
            TAG,
            "zoomBitmapLimit: resize: " + resizeLong + "x" + resizeShort
        )
        return if (width > height) zoomBitmap(
            bm,
            resizeLong,
            resizeShort
        ) else zoomBitmap(bm, resizeShort, resizeLong)
    }

    fun zoomBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        // 获得图片的宽高
        val width = bm.width
        val height = bm.height
        // 计算缩放比例
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // 取得想要缩放的matrix参数
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        // 得到新的图片 www.2cto.com
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true)
    }

    /**
     * create bitmap by path
     *
     * @param path
     * @return Bitmap
     */
    fun createBitmapFromPath(path: String?): Bitmap? {
        val imgFile = File(path)
        if (imgFile.exists()) {
            return BitmapFactory.decodeFile(imgFile.absolutePath)
        } else {
            printLog("  createBitmapFromPath imgFile is not exist")
        }
        return null
    }

    /**
     * scale bitmap which parameter x or y is 1080
     *
     * @param data
     * @return
     */
    fun createScaleBitmapFromData(data: ByteArray?, width: Int, height: Int): Bitmap? {
        if (data == null || data.size == 0) {
            printLog("createScaleBitmapFromData data==null||data.length==0")
            return null
        }
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(data, 0, data.size, opts)
        val scaleX = opts.outWidth / width
        val scaleY = opts.outHeight / height
        var scale = if (scaleX > scaleY) scaleX else scaleY
        if (scale < 1) scale = 1
        opts.inJustDecodeBounds = false
        opts.inSampleSize = scale
        return BitmapFactory.decodeByteArray(data, 0, data.size, opts)
    }

    /**
     * create bitmap by resources but the density will same as resources
     *
     * @param resources
     * @param id
     * @return
     */
    fun creatBitmapFromRawResource(resources: Resources, id: Int): Bitmap {
        val value = TypedValue()
        resources.openRawResource(id, value)
        val opts = BitmapFactory.Options()
        opts.inTargetDensity = value.density
        printLog("  creatBitmapFromResource value.density=" + value.density)
        return BitmapFactory.decodeResource(resources, id, opts)
    }

    /**
     * create source bitmap by Resource which size is specified(thumb nail
     * bitmap)
     *
     * @param id
     * Resources id
     * @param width
     * bitmap width
     * @param height
     * bitmap height
     * @return Bitmap
     */
    fun createImageThumbnail(
        resources: Resources?,
        id: Int,
        width: Int,
        height: Int
    ): Bitmap? {
        var bitmap: Bitmap? = null
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        bitmap = BitmapFactory.decodeResource(resources, id)
        val h = options.outHeight
        val w = options.outWidth
        val beWidth = w / width
        val beHeight = h / height
        var be = 1
        be = if (beWidth < beHeight) {
            beWidth
        } else {
            beHeight
        }
        if (be <= 0) {
            be = 1
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = be
        BitmapFactory.decodeResource(resources, id, options)
        bitmap = ThumbnailUtils.extractThumbnail(
            bitmap,
            width,
            height,
            ThumbnailUtils.OPTIONS_RECYCLE_INPUT
        )
        return bitmap
    }

    /**
     * load source bitmap by image path which size is specified
     *
     * @param imagePath
     * image path
     * @param width
     * bitmap width
     * @param height
     * bitmap height
     * @return Bitmap
     */
    fun createImageThumbnail(imagePath: String?, width: Int, height: Int): Bitmap? {
        return try {
            if (TextUtils.isEmpty(imagePath)) {
                printLog("createImageThumbnail imagePath cannot be empty")
                return null
            }
            var bitmap: Bitmap? = null
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            bitmap = BitmapFactory.decodeFile(imagePath, options)
            val h = options.outHeight
            val w = options.outWidth
            val beWidth = w / width
            val beHeight = h / height
            var be = 1
            be = if (beWidth < beHeight) {
                beWidth
            } else {
                beHeight
            }
            if (be <= 0) {
                be = 1
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = be
            bitmap = BitmapFactory.decodeStream(FileInputStream(imagePath), null, options)
            // bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
            // ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            bitmap
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create reflection images
     *
     * @param bitmap
     * @return Bitmap
     */
    fun createReflectionImage(bitmap: Bitmap): Bitmap? {
        if (isEmty(bitmap)) {
            printLog("createReflectionImageWithOrigin bitmap is null")
            return null
        }
        val reflectionGap = 4
        val w = bitmap.width
        val h = bitmap.height
        val matrix = Matrix()
        matrix.preScale(1f, -1f)
        val reflectionImage = Bitmap.createBitmap(bitmap, 0, h / 2, w, h / 2, matrix, false)
        val bitmapWithReflection =
            Bitmap.createBitmap(w, h + h / 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapWithReflection)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val deafalutPaint = Paint()
        canvas.drawRect(0f, h.toFloat(), w.toFloat(), h + reflectionGap.toFloat(), deafalutPaint)
        canvas.drawBitmap(reflectionImage, 0f, h + reflectionGap.toFloat(), null)
        val paint = Paint()
        val shader = LinearGradient(
            0f, bitmap.height.toFloat(), 0f, (bitmapWithReflection.height+ reflectionGap).toFloat()
                    , 0x70ffffff, 0x00ffffff, TileMode.CLAMP
        )
        paint.shader = shader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawRect(
            0f,
            h.toFloat(),
            w.toFloat(),
            bitmapWithReflection.height + reflectionGap.toFloat(),
            paint
        )
        return bitmapWithReflection
    }

    /**
     * 压缩图片
     */
    fun compressImageFromFile(srcPath: String?, isCompress: Boolean): Bitmap? {
        val angle = getPictureDegree(srcPath)
        val options = BitmapFactory.Options()
        if (isCompress) {
            options.inJustDecodeBounds = true // 只读边,不读内容
            BitmapFactory.decodeFile(srcPath, options)
            var sampleSize = 1
            val w = options.outWidth
            val h = options.outHeight
            if (w < 0 || h < 0) { // 不是图片文件直接返回
                return null
            }
            val requestH = 1000 // 需求最大高度
            val requestW = 1000 // 需求最大宽度
            while (h / sampleSize > requestH || w / sampleSize > requestW) {
                sampleSize = sampleSize shl 1
            }
            options.inJustDecodeBounds = false // 不再只读边
            options.inSampleSize = sampleSize // 设置采样率大小
        }
        var bitmap = BitmapFactory.decodeFile(srcPath, options)
        bitmap = rotateImageView(angle, bitmap)
        return bitmap
    }

    /**
     * 压缩图片
     */
    fun compressImageFromFile(srcPath: String?, width: Int, height: Int): Bitmap? {
        val angle = getPictureDegree(srcPath)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true // 只读边,不读内容
        BitmapFactory.decodeFile(srcPath, options)
        var sampleSize = 1
        val w = options.outWidth
        val h = options.outHeight
        if (w < 0 || h < 0) { // 不是图片文件直接返回
            return null
        }
        while (h / sampleSize > width || w / sampleSize > height) {
            sampleSize = sampleSize shl 1
        }
        options.inJustDecodeBounds = false // 不再只读边
        options.inSampleSize = sampleSize // 设置采样率大小
        var bitmap = BitmapFactory.decodeFile(srcPath, options)
        bitmap = rotateImageView(angle, bitmap)
        return bitmap
    }

    /**
     * 旋转图片
     *
     * @param angle
     * @param bitmap
     * @return Bitmap
     */
    fun rotateImageView(angle: Int, bitmap: Bitmap): Bitmap {
        // 旋转图片 动作
        var bitmap = bitmap
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        // 创建新的图片
        bitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        return bitmap
    }

    /**
     * Decode sourcePath file,and save it to outFilePath after rotated and
     * scaled;
     *
     * @param sourcePath
     * @param outFilePath
     */
    fun compressImage(sourcePath: String?, outFilePath: String?) {
        if (TextUtils.isEmpty(sourcePath)) {
            printLog("compressImage srcPath cannot be null")
            return
        }
        val degree = getPictureDegree(sourcePath)
        val DEF_MIN = 480
        val DEF_MAX = 1280
        val newOpts = BitmapFactory.Options()
        newOpts.inJustDecodeBounds = true
        var bitmap: Bitmap?
        BitmapFactory.decodeFile(sourcePath, newOpts)
        val picMin = Math.min(newOpts.outWidth, newOpts.outHeight)
        val picMax = Math.max(newOpts.outWidth, newOpts.outHeight)
        val picShap = 1.0f * picMin / picMax
        val defShap = 1.0f * DEF_MIN / DEF_MAX
        var scale = 1f
        if (picMax < DEF_MAX) {
        } else if (picShap > defShap) {
            scale = 1.0f * DEF_MAX / picMax
        } else if (picMin > DEF_MIN) {
            scale = 1.0f * DEF_MIN / picMin
        }
        val scaledWidth = (scale * newOpts.outWidth).toInt()
        val scaledHeight = (scale * newOpts.outHeight).toInt()
        newOpts.inSampleSize = ((if (scale == 1f) 1 else 2) / scale).toInt()
        newOpts.inJustDecodeBounds = false
        bitmap = BitmapFactory.decodeFile(sourcePath, newOpts)
        if (scale != 1f) {
            val matrix = Matrix()
            scale = scaledWidth.toFloat() / bitmap.width
            matrix.setScale(scale, scale)
            val bitmapNew = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                false
            )
            bitmap.recycle()
            bitmap = bitmapNew
        }
        val quality: Int
        quality = if (scaledHeight * scaledWidth > 1280 * 720) {
            30
        } else {
            80 - (scaledHeight.toFloat() * scaledWidth / (1280 * 720) * 50).toInt()
        }
        try {
            bitmap = getRotateBitmap(bitmap, degree.toFloat())
            bitmap!!.compress(
                CompressFormat.JPEG,
                quality,
                FileOutputStream(File(outFilePath))
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        bitmap!!.recycle()
        System.gc()
    }

    /**
     * Used to calculate inSimpleSize value of BitmapFactpry;
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return int
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        if (reqWidth == 0 || reqHeight == 0) {
            return 1
        }
        val height = options.outHeight
        val width = options.outWidth
        Log.d(TAG, "origin, w= $width h=$height")
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        printLog("sampleSize:$inSampleSize")
        return inSampleSize
    }

    /**
     * Used to load bitmap from path
     *
     * @param path
     * @param reqWidth
     * @param reqHeight
     * @return Bitmap
     */
    fun decodeSampledBitmapFromPath(path: String?, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeStream(FileInputStream(path), null, options)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Used to load bitmap from resource
     *
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return Bitmap
     */
    fun decodeSampledBitmapFromResource(
        res: Resources?,
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(res, resId, options)
    }

    fun decodeResource(resources: Resources, id: Int): Bitmap {
        val value = TypedValue()
        resources.openRawResource(id, value)
        val opts = BitmapFactory.Options()
        opts.inTargetDensity = value.density
        return BitmapFactory.decodeResource(resources, id, opts)
    }

    fun getBitmapFromRGBA(data: ByteArray?, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data))
        return bitmap
    }

    fun getRGBAFromBitmap(bitmap: Bitmap): ByteArray? {
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            Log.e(TAG, "getRGBAFromBitmap bitmap format must ARGB_8888!")
            return null
        }
        val rgba = ByteArray(bitmap.width * bitmap.height * 4)
        val buffer = ByteBuffer.wrap(rgba)
        bitmap.copyPixelsToBuffer(buffer)
        return rgba
    }

    /**
     * Used to get bitmap with uri
     *
     * @param imageUri
     * @param context
     * @param imageWidth
     * @return Bitmap
     */
    fun getBitmapFromUri(
        imageUri: Uri?,
        context: Context,
        imageWidth: Int
    ): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val resolver = context.contentResolver
            val `is` = resolver.openInputStream(imageUri!!)
            bitmap = BitmapFactory.decodeStream(`is`)
            val width = bitmap.width
            val height = bitmap.height
            val dstWidth: Int
            val dstHeight: Int
            if (width > height) {
                dstWidth = imageWidth
                dstHeight = imageWidth * height / width
            } else {
                dstHeight = imageWidth
                dstWidth = imageWidth * width / height
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, true)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return bitmap
    }

    @Throws(IOException::class)
    fun getBitmapFromRaw(context: Context?, @RawRes resourceId: Int): Bitmap {
        val bytes =
            FileUtils.readRawFile(context, resourceId)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size)
    }

    /**
     * The Drawable resource files into Bitmap images
     *
     * @param drawable
     * @return Bitmap
     */
    fun getBitmapWithDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            printLog("getBitmapWithDrawable drawable is null")
            return null
        }
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * crop capture the middle part of the bitmap
     *
     * @param sourceBitmap
     * sourceBitmap
     * @param edgeLength
     * Hope to get part of the side length of square,To a minimum
     * length is edgeLength bitmap
     * @return Bitmap
     */
    fun getCenterSquareScaleBitmap(sourceBitmap: Bitmap?, edgeLength: Int): Bitmap? {
        if (null == sourceBitmap || edgeLength <= 0) {
            printLog("getCenterSquareScaleBitmap sourceBitmap is null or edgeLength<=0")
            return null
        }
        var result = sourceBitmap
        val widthOrg = sourceBitmap.width
        val heightOrg = sourceBitmap.height
        if (widthOrg > edgeLength && heightOrg > edgeLength) {
            val longerEdge =
                (edgeLength * Math.max(widthOrg, heightOrg) / Math.min(
                    widthOrg,
                    heightOrg
                ))
            val scaledWidth = if (widthOrg > heightOrg) longerEdge else edgeLength
            val scaledHeight = if (widthOrg > heightOrg) edgeLength else longerEdge
            val scaledBitmap: Bitmap
            try {
                scaledBitmap =
                    Bitmap.createScaledBitmap(sourceBitmap, scaledWidth, scaledHeight, true)
                val xTopLeft = (scaledWidth - edgeLength) / 2
                val yTopLeft = (scaledHeight - edgeLength) / 2
                result =
                    Bitmap.createBitmap(scaledBitmap, xTopLeft, yTopLeft, edgeLength, edgeLength)
                scaledBitmap.recycle()
            } catch (e: Exception) {
                return null
            }
        }
        return result
    }

    /**
     * Used to get a round shape image,the Effect of bigger bitmap will be more
     * obvious
     *
     * @param sourceBitmap
     * @return Bitmap
     */
    fun getRoundBitmap(sourceBitmap: Bitmap): Bitmap? {
        if (isEmty(sourceBitmap)) {
            printLog("getRoundBitmap sourceBitmap is null")
            return null
        }
        var width = sourceBitmap.width
        var height = sourceBitmap.height
        val roundPx: Float
        val left: Float
        val top: Float
        val right: Float
        val bottom: Float
        val dst_left: Float
        val dst_top: Float
        val dst_right: Float
        val dst_bottom: Float
        if (width <= height) {
            roundPx = width / 2 - 5.toFloat()
            top = 0f
            bottom = width.toFloat()
            left = 0f
            right = width.toFloat()
            height = width
            dst_left = 0f
            dst_top = 0f
            dst_right = width.toFloat()
            dst_bottom = width.toFloat()
        } else {
            roundPx = height / 2 - 5.toFloat()
            val clip = (width - height) / 2.toFloat()
            left = clip
            right = width - clip
            top = 0f
            bottom = height.toFloat()
            width = height
            dst_left = 0f
            dst_top = 0f
            dst_right = height.toFloat()
            dst_bottom = height.toFloat()
        }
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val src =
            Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        val dst = Rect(
            dst_left.toInt(),
            dst_top.toInt(),
            dst_right.toInt(),
            dst_bottom.toInt()
        )
        val rectF = RectF(dst_left + 3, dst_top + 3, dst_right - 3, dst_bottom - 3)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(sourceBitmap, src, dst, paint)
        return output
    }

    /**
     * Used to get a round bitmap,you can set range(faceRect),and border
     * color(color)
     *
     * @param sourceBitmap
     * @param faceRect
     * @param color
     * @return Bitmap
     */
    fun getRoundedCornerBitmap(
        sourceBitmap: Bitmap,
        faceRect: Rect,
        color: Int
    ): Bitmap? {
        if (isEmty(sourceBitmap)) {
            printLog("getRoundedCornerBitmap sourceBitmap is null")
            return null
        }
        val outBitmap = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(outBitmap)
        val paint = Paint()
        val rect =
            Rect(0, 0, sourceBitmap.width, sourceBitmap.height)
        val rectF = RectF(rect)
        val roundPX = (faceRect.right - faceRect.left) / 2.0f
        val roundPY = (faceRect.bottom - faceRect.top) / 2.0f
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPX, roundPY, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(sourceBitmap, rect, rect, paint)
        return outBitmap
    }

    /**
     * Used to crop a square bitmap
     *
     * @param sourceBitmap
     * @return Bitmap
     */
    fun getCropBitmap(sourceBitmap: Bitmap): Bitmap? {
        if (isEmty(sourceBitmap)) {
            printLog("getCropBitmap sourceBitmap is null")
            return null
        }
        val w = sourceBitmap.width
        val h = sourceBitmap.height
        val wh = if (w > h) h else w
        val retX = if (w > h) (w - h) / 2 else 0
        val retY = if (w > h) 0 else (h - w) / 2
        return Bitmap.createBitmap(sourceBitmap, retX, retY, wh, wh, null, false)
    }

    /**
     * Used to crop bitmap by define range(orginRect)
     *
     * @param sourceBitmap
     * @param orginRect
     * @return Bitmap
     */
    fun getCropBitmap(sourceBitmap: Bitmap?, orginRect: Rect): Bitmap? {
        if (isEmty(sourceBitmap)) {
            printLog("getCropBitmap sourceBitmap is null")
            return null
        }
        return getCropBitmap(sourceBitmap, orginRect, 2f, 2f)
    }

    fun getCropBitmap(
        sourceBitmap: Bitmap?,
        orginRect: Rect,
        scaleX: Float,
        scaleY: Float
    ): Bitmap? {
        if (sourceBitmap == null || sourceBitmap.isRecycled) {
            return null
        }
        val rect = getScaleRect(
            orginRect,
            scaleX,
            scaleY,
            sourceBitmap.width,
            sourceBitmap.height
        )
        return Bitmap.createBitmap(sourceBitmap, rect.left, rect.top, rect.width(), rect.height())
    }

    /**
     * Used to get rect which is scaled;
     *
     * @param rect
     * @param scaleX
     * @param scaleY
     * @param maxW
     * @param maxH
     * @return Rect
     */
    fun getScaleRect(
        rect: Rect,
        scaleX: Float,
        scaleY: Float,
        maxW: Int,
        maxH: Int
    ): Rect {
        val resultRect = Rect()
        val left = (rect.left - rect.width() * (scaleX - 1) / 2).toInt()
        val right = (rect.right + rect.width() * (scaleX - 1) / 2).toInt()
        val bottom = (rect.bottom + rect.height() * (scaleY - 1) / 2).toInt()
        val top = (rect.top - rect.height() * (scaleY - 1) / 2).toInt()
        resultRect.left = if (left > 0) left else 0
        resultRect.right = if (right > maxW) maxW else right
        resultRect.bottom = if (bottom > maxH) maxH else bottom
        resultRect.top = if (top > 0) top else 0
        return resultRect
    }

    /**
     * Used to scale bitmap ,you can print a scale value(float);
     *
     * @param sourceBitmap
     * @param scale
     * @return Bitmap
     */
    fun getScaleBitmap(sourceBitmap: Bitmap?, scale: Float): Bitmap? {
        if (isEmty(sourceBitmap)) {
            printLog("getScaleBitmap sourceBitmap is null")
            return null
        }
        return getScaleBitmap(sourceBitmap, scale, true)
    }

    private fun getScaleBitmap(
        bitmap: Bitmap?,
        scale: Float,
        recycle: Boolean
    ): Bitmap? {
        var bitmap = bitmap
        if (scale == 1f) {
            printLog("getScaleBitmap scale == 1f")
            return bitmap
        }
        val matrix = matrix
        matrix.setScale(scale, scale)
        val bmp =
            Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (recycle && bitmap != null && bitmap != bmp && !bitmap.isRecycled) {
            bitmap.recycle()
            bitmap = null
        }
        return bmp
    }

    /**
     * Used to scale bitmap by defineWidth and defineHeight;
     *
     * @param sourceBitmap
     * @param defineWidth
     * @param defineHeight
     * @return Bitmap
     */
    fun getScaleBitmap(sourceBitmap: Bitmap, defineWidth: Int, defineHeight: Int): Bitmap? {
        if (isEmty(sourceBitmap)) {
            printLog("getCropBitmap sourceBitmap is null")
            return null
        }
        val w = sourceBitmap.width
        val h = sourceBitmap.height
        val sw = defineWidth.toFloat() / w
        val sh = defineHeight.toFloat() / h
        val scale = if (sw < sh) sw else sh
        return getScaleBitmap(sourceBitmap, scale)
    }

    /**
     * Used to get bitmap size which you want;
     *
     * @param bitmap
     * @param size
     * @return Bitmap
     */
    fun getResizeBitmap(bitmap: Bitmap, size: Int): Bitmap? {
        if (isEmty(bitmap)) {
            printLog("getResizeBitmap bitmap is null")
            return null
        }
        val maxSize = size * 1e6
        val w = bitmap.width
        val h = bitmap.height
        val ratio = Math.sqrt(maxSize / (w * h)).toFloat()
        return if (ratio >= 1) {
            bitmap
        } else getRotateAndScaleBitmap(bitmap, 0, ratio)
    }

    /**
     * return Media image orientation
     *
     * @param context
     * @param photoUri
     * @return int
     */
    fun getOrientationFromMedia(context: Context, photoUri: Uri?): Int {
        val imgs =
            arrayOf(MediaStore.Images.Media.ORIENTATION)
        val cursor =
            context.contentResolver.query(photoUri!!, imgs, null, null, null)
        cursor!!.moveToFirst()
        val index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)
        val roate = cursor.getInt(index)
        try {
            cursor.close()
        } catch (e: Exception) {
        }
        return roate
    }

    /**
     * Used to rotate bitmap by rotateDegree which you print
     *
     * @param bitmap
     * @param rotateDegree
     * @return Bitmap
     */
    fun getRotateBitmap(bitmap: Bitmap?, rotateDegree: Float): Bitmap? {
        if (isEmty(bitmap)) {
            printLog("getRotateBitmap bitmap is null")
            return null
        }
        val matrix = Matrix()
        matrix.postRotate(rotateDegree)
        return Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, false)
    }

    /**
     * Used to rotate and scale bitmap
     *
     * @param angle
     * @param bitmap
     * @return Bitmap
     */
    fun getRotateAndScaleBitmap(bitmap: Bitmap, angle: Int, scale: Float): Bitmap? {
        if (isEmty(bitmap)) {
            printLog("getRotateAndScaleBitmap bitmap is null")
            return null
        }
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Used to get the image degree;
     *
     * @param path
     * @return degree
     */
    fun getPictureDegree(path: String?): Int {
        var degree = 0
        try {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            Log.d(TAG, "orientation=$orientation")
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> degree = 0
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> degree = 180
                ExifInterface.ORIENTATION_TRANSPOSE -> degree = 90
                ExifInterface.ORIENTATION_TRANSVERSE -> degree = 270
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return degree
    }

    fun getPictureOrientation(path: String?): Int {
        val degree = getPictureDegree(path)
        when (degree) {
            0 -> {
            }
            90 -> {
            }
            180 -> {
            }
            270 -> {
            }
            else -> {
            }
        }
        return degree
    }

    /**
     * Return a MirrorHorizontal Bitmap
     *
     * @param sourceBitmap
     *
     * @return Bitmap
     */
    fun getMirrorHorizontalBitmap(sourceBitmap: Bitmap?): Bitmap? {
        if (isEmty(sourceBitmap)) {
            printLog("getMirrorHorizontalBitmap sourceBitmap is null")
            return null
        }
        return getMirrorHorizontalBitmap(sourceBitmap, true)
    }

    private fun getMirrorHorizontalBitmap(sourceBitmap: Bitmap?, recycle: Boolean): Bitmap? {
        var sourceBitmap = sourceBitmap
        if (isEmty(sourceBitmap)) {
            printLog("getMirrorHorizontalBitmap sourceBitmap is null")
            return null
        }
        val matrix = matrix
        matrix.setScale(-1f, 1f)
        matrix.postTranslate(sourceBitmap!!.width.toFloat(), 0f)
        val bmp = Bitmap.createBitmap(
            sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix,
            true
        )
        if (recycle && sourceBitmap != null && !sourceBitmap.isRecycled) {
            sourceBitmap.recycle()
            sourceBitmap = null
        }
        return bmp
    }

    fun getRotateBitmap(
        sourceBitmap: Bitmap,
        degrees: Int,
        isFrontCamera: Boolean
    ): Bitmap? {
        var sourceBitmap = sourceBitmap
        if (isEmty(sourceBitmap)) {
            printLog("getMirrorHorizontalBitmap sourceBitmap is null")
            return null
        }
        val matrix = matrix
        matrix.postRotate(degrees.toFloat())
        if (isFrontCamera) {
            matrix.postScale(-1f, 1f)
        }
        sourceBitmap = Bitmap.createBitmap(
            sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix,
            true
        )
        return sourceBitmap
    }

    /**
     * Return a mirrorVertical Bitmap
     *
     * @param sourceBitmap
     * @return Bitmap
     */
    fun getMirrorVerticalBitmap(sourceBitmap: Bitmap?): Bitmap? {
        if (isEmty(sourceBitmap)) {
            printLog("getMirrorVerticalBitmap sourceBitmap is null")
            return null
        }
        return getMirrorVerticalBitmap(sourceBitmap, true)
    }

    private fun getMirrorVerticalBitmap(sourceBitmap: Bitmap?, recycle: Boolean): Bitmap? {
        var sourceBitmap = sourceBitmap
        if (isEmty(sourceBitmap)) {
            printLog("getMirrorVerticalBitmap sourceBitmap is null")
            return null
        }
        val matrix = matrix
        matrix.setScale(1f, -1f)
        matrix.postTranslate(0f, sourceBitmap!!.height.toFloat())
        val bmp = Bitmap.createBitmap(
            sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix,
            true
        )
        if (recycle && sourceBitmap != null && sourceBitmap != bmp && !sourceBitmap.isRecycled) {
            sourceBitmap.recycle()
            sourceBitmap = null
        }
        return bmp
    }

    /**
     * Used to draw a bitmap(size/2) which get from resource into the empty
     * bitmap
     *
     * @param sourceBitmap
     * @param resourceId
     * @return Bitmap
     */
    fun getScreenshotBitmap(
        context: Context,
        sourceBitmap: Bitmap,
        resourceId: Int
    ): Bitmap? {
        if (isEmty(sourceBitmap)) {
            printLog("getScreenshotBitmap bitmap is null")
            return null
        }
        val screenshot = Bitmap.createBitmap(
            sourceBitmap.width, sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val content = BitmapFactory.decodeResource(context.resources, resourceId)
        val canvas = Canvas(screenshot)
        canvas.drawBitmap(
            content, (sourceBitmap.width - content.width) / 2.toFloat(),
            (sourceBitmap.height - content.height) / 2.toFloat(), Paint()
        )
        canvas.drawBitmap(sourceBitmap, 0f, 0f, Paint())
        canvas.save()
        canvas.restore()
        return screenshot
    }

    /**
     * Used to save image(data) to savePath ,at the same time,you can print
     * angle to rotate image and save it;
     *
     * @param savePath
     * @param data
     * @param angle
     */
    fun saveJpegData(savePath: String?, data: ByteArray?, angle: Int) {
        var data = data
        if (data == null || data.size == 0) {
            printLog("saveJpegData data==null||data.length==0")
            return
        }
        try {
            var bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
            val matrix = Matrix()
            matrix.reset()
            matrix.postRotate(angle.toFloat())
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            val file = File(savePath)
            val bos =
                BufferedOutputStream(FileOutputStream(file))
            bmp.compress(CompressFormat.JPEG, 100, bos)
            bos.flush()
            bos.close()
            recycle(bmp)
            data = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun compressBitmapToJPGByte(bitmap: Bitmap): ByteArray? {
        if (isEmty(bitmap)) {
            printLog("saveBitmapToStorage bitmap is null")
            return null
        }
        var jpgByte: ByteArray? = null
        var outputStream: ByteArrayOutputStream? = null
        outputStream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 100, outputStream)
        try {
            outputStream.close()
            Log.d(TAG, "saveImageToStorage success")
            jpgByte = outputStream.toByteArray()
        } catch (exception4: Exception) {
            Log.e(TAG, "saveImageToStorage error")
        }
        return jpgByte
    }

    fun compressBitmapToPNGByte(bitmap: Bitmap): ByteArray? {
        if (isEmty(bitmap)) {
            printLog("saveBitmapToStorage bitmap is null")
            return null
        }
        var jpgByte: ByteArray? = null
        var outputStream: ByteArrayOutputStream? = null
        outputStream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 100, outputStream)
        try {
            outputStream.close()
            Log.d(TAG, "saveImageToStorage success")
            jpgByte = outputStream.toByteArray()
        } catch (exception4: Exception) {
            Log.e(TAG, "saveImageToStorage error")
        }
        return jpgByte
    }

    /**
     * Used to save bitmap to storage
     *
     * @param savePath
     * @param bitmap
     * @return boolean
     */
    fun saveBitmap(savePath: String?, bitmap: Bitmap): Boolean {
        if (isEmty(bitmap)) {
            printLog("saveBitmapToStorage bitmap is null")
            return false
        }
        val saveFile = File(savePath)
        if (!saveFile.parentFile.exists() || saveFile.parentFile.isFile) {
            saveFile.parentFile.mkdirs()
        }
        var fileoutputstream: FileOutputStream? = null
        try {
            fileoutputstream = FileOutputStream(savePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        bitmap.compress(CompressFormat.JPEG, 100, fileoutputstream)
        return try {
            fileoutputstream?.close()
            Log.d(TAG, "saveImageToStorage success")
            true
        } catch (exception4: Exception) {
            Log.e(TAG, "saveImageToStorage error")
            false
        }
    }

    fun saveBitmapPNG(savePath: String?, bitmap: Bitmap): Boolean {
        if (isEmty(bitmap)) {
            printLog("saveBitmapPNG bitmap is null")
            return false
        }
        val saveFile = File(savePath)
        if (!saveFile.parentFile.exists() || saveFile.parentFile.isFile) {
            saveFile.parentFile.mkdirs()
        }
        var fileoutputstream: FileOutputStream? = null
        try {
            fileoutputstream = FileOutputStream(savePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        bitmap.compress(CompressFormat.PNG, 100, fileoutputstream)
        return try {
            fileoutputstream?.close()
            Log.d(TAG, "saveBitmapPNG success")
            true
        } catch (exception4: Exception) {
            Log.e(TAG, "saveBitmapPNG error")
            false
        }
    }

    /**
     * Used to save bitmap to define directory,at the same time ,update it to
     * the mobile phone gallery;
     *
     * @param context
     * @param bmp
     * @param absPath
     */
    fun saveImageToGallery(
        context: Context,
        bmp: Bitmap,
        absPath: String
    ) {
        if (isEmty(bmp)) {
            printLog("saveImageToGallery bitmap is null")
            return
        }
        val appDir = File(absPath)
        if (!appDir.exists() && !appDir.isDirectory) {
            appDir.mkdir()
        }
        val fileName = "IMAGE_" + System.currentTimeMillis() + ".jpg"
        val file = File(appDir, fileName)
        try {
            val fos = FileOutputStream(file)
            bmp.compress(CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            MediaStore.Images.Media.insertImage(
                context.contentResolver,
                file.absolutePath,
                fileName,
                null
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        context.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(
                    File(
                        absPath
                                + fileName
                    )
                )
            )
        )
    }

    /**
     * Used to save image to MediaStore
     *
     * @param imagePath
     * @param date
     * @param orientation
     * @param size
     * @param location
     * @param contentresolver
     * @return Uri
     */
    fun savetImageToMediaStore(
        imagePath: String, date: Long, orientation: Int, size: Long,
        location: Location?, contentresolver: ContentResolver
    ): Uri? {
        val fileName =
            imagePath.substring(imagePath.lastIndexOf("/"), imagePath.lastIndexOf("."))
        val contentvalues = ContentValues(9)
        contentvalues.put("title", fileName)
        contentvalues.put(
            "_display_name",
            StringBuilder().append(fileName).append(".jpg").toString()
        )
        contentvalues.put("datetaken", java.lang.Long.valueOf(date))
        contentvalues.put("mime_type", "image/jpeg")
        contentvalues.put("orientation", Integer.valueOf(orientation))
        contentvalues.put("_data", imagePath)
        contentvalues.put("_size", java.lang.Long.valueOf(size))
        if (location != null) {
            contentvalues.put("latitude", java.lang.Double.valueOf(location.latitude))
            contentvalues.put("longitude", java.lang.Double.valueOf(location.longitude))
        }
        var uri =
            contentresolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentvalues)
        if (uri == null) {
            Log.e(TAG, "Failed to write MediaStore")
            uri = null
        }
        return uri
    }

    /**
     * Used to recycle bitmap
     *
     * @param bitmap
     */
    fun recycle(bitmap: Bitmap?) {
        var bitmap = bitmap
        if (null == bitmap || bitmap.isRecycled) {
            return
        }
        bitmap.recycle()
        bitmap = null
    }

    /**
     * whether bitmap is null or is recycled
     *
     * @param bitmap
     * @return boolean
     */
    fun isEmty(bitmap: Bitmap?): Boolean {
        return bitmap == null || bitmap.isRecycled
    }

    /**
     * print log
     *
     * @param logStr
     */
    fun printLog(logStr: String?) {
        if (DEBUG) {
            Log.d(TAG, logStr)
        }
    }

    fun getBitmapFromGrayBuffer(buffer: ByteArray, width: Int, height: Int): Bitmap {
        val rgba = ByteArray(width * height * 4)
        for (i in 0 until width * height) {
            rgba[4 * i] = buffer[i]
            rgba[4 * i + 1] = buffer[i]
            rgba[4 * i + 2] = buffer[i]
            rgba[4 * i + 3] = 0xff.toByte()
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rgba))
        return bitmap
    }
}