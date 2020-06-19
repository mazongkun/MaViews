package com.mama.views.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.renderscript.*
import android.util.Log
import java.nio.ByteBuffer

object YuvUtils {
    private val TAG = YuvUtils::class.java.simpleName
    private var rs: RenderScript? = null
    private var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB? = null
    private var yuvToRgbIntrinsicRGB: ScriptIntrinsicYuvToRGB? = null

    //    private static Type.Builder nv21Type, rgbaType;
    //    private static Allocation in, out;
    private fun getRenderScript(context: Context): RenderScript? {
        if (rs == null) {
            synchronized(YuvUtils::class.java) {
                if (rs == null) {
                    rs = RenderScript.create(context)
                }
            }
        }
        return rs
    }

    private fun getYuvToRgbIntrinsic(context: Context): ScriptIntrinsicYuvToRGB? {
        if (yuvToRgbIntrinsic == null) {
            synchronized(YuvUtils::class.java) {
                if (yuvToRgbIntrinsic == null) {
                    val renderScript = getRenderScript(context)
                    yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(
                        renderScript,
                        Element.U8_4(rs)
                    )
                }
            }
        }
        return yuvToRgbIntrinsic
    }

    private fun getYuvToRgbIntrinsicRGB(context: Context): ScriptIntrinsicYuvToRGB? {
        if (yuvToRgbIntrinsicRGB == null) {
            synchronized(YuvUtils::class.java) {
                if (yuvToRgbIntrinsicRGB == null) {
                    val renderScript = getRenderScript(context)
                    yuvToRgbIntrinsicRGB = ScriptIntrinsicYuvToRGB.create(
                        renderScript,
                        Element.U8_3(rs)
                    )
                }
            }
        }
        return yuvToRgbIntrinsicRGB
    }

    fun init(context: Context) {
        getRenderScript(context)
        getYuvToRgbIntrinsic(context)
        getYuvToRgbIntrinsicRGB(context)
    }

    fun destroy() {
        rs!!.destroy()
        rs = null
        yuvToRgbIntrinsic!!.destroy()
        yuvToRgbIntrinsic = null
        yuvToRgbIntrinsicRGB!!.destroy()
        yuvToRgbIntrinsicRGB = null
    }

    fun yuvToRGBA(
        nv21: ByteArray?,
        width: Int,
        height: Int,
        format: Int,
        rgba: ByteArray?
    ): ByteArray? {
        var format = format
        if (rs == null || yuvToRgbIntrinsic == null || nv21 == null || width < 1 || height < 1 || rgba == null || rgba.size < width * height * 4
        ) {
            Log.e(
                TAG, "nv21ToRGBA params error: rs=" + rs
                        + ", yToR=" + yuvToRgbIntrinsic
                        + ", buf=" + nv21 + ", size=" + nv21!!.size
                        + ", w=" + width + "x" + height
            )
            return null
        }
        val nv21Type: Type.Builder
        val rgbaType: Type.Builder
        val `in`: Allocation
        val out: Allocation
        if (format != ImageFormat.NV21 && format != ImageFormat.YV12) {
            format = ImageFormat.NV21
        }
        nv21Type = Type.Builder(
            rs,
            Element.U8(rs)
        ).setX(width).setY(height).setYuvFormat(format)
        `in` = Allocation.createTyped(rs, nv21Type.create(), Allocation.USAGE_SCRIPT)
        rgbaType = Type.Builder(
            rs,
            Element.RGBA_8888(rs)
        ).setX(width).setY(height)
        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        `in`.copyFrom(nv21)
        yuvToRgbIntrinsic!!.setInput(`in`)
        yuvToRgbIntrinsic!!.forEach(out)

//        byte[] rgba = new byte[out.getBytesSize()];
        out.copyTo(rgba)
        `in`.destroy()
        out.destroy()
        return rgba
    }

    fun nv21ToBitmap(nv21: ByteArray?, width: Int, height: Int): Bitmap? {
        if (rs == null || yuvToRgbIntrinsic == null || nv21 == null || width < 1 || height < 1) {
            return null
        }
        val nv21Type: Type.Builder
        val rgbaType: Type.Builder
        val `in`: Allocation
        val out: Allocation
        nv21Type = Type.Builder(
            rs,
            Element.U8(rs)
        ).setX(width).setY(height).setYuvFormat(ImageFormat.NV21)
        `in` = Allocation.createTyped(rs, nv21Type.create(), Allocation.USAGE_SCRIPT)
        rgbaType = Type.Builder(
            rs,
            Element.RGBA_8888(rs)
        ).setX(width).setY(height)
        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        `in`.copyFrom(nv21)
        yuvToRgbIntrinsic!!.setInput(`in`)
        yuvToRgbIntrinsic!!.forEach(out)
        //        Log.e(TAG, "mama= nv21ToRGBA byteSize=" + out.getBytesSize());
        val bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.copyTo(bmpout)
        return bmpout
    }

    fun YUV_420_888toGRAY(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val y = ByteArray(ySize)
        val yBuffer = image.planes[0].buffer // Y
        val rowStride = image.planes[0].rowStride
        assert(image.planes[0].pixelStride == 1)
        var pos = 0
        if (rowStride == width) { // likely
            yBuffer[y, 0, ySize]
            pos += ySize
        } else {
            var yBufferPos = width - rowStride // not an actual position
            while (pos < ySize) {
                yBufferPos += rowStride - width
                yBuffer.position(yBufferPos)
                yBuffer[y, pos, width]
                pos += width
            }
        }
        return y
    }

    fun YUV_420_888toNV21(
        yBuffer: ByteBuffer,
        uBuffer: ByteBuffer,
        vBuffer: ByteBuffer
    ): ByteArray {
        val nv: ByteArray
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        nv = ByteArray(ySize + uSize + vSize)
        yBuffer[nv, 0, ySize]
        vBuffer[nv, ySize, vSize]
        uBuffer[nv, ySize + vSize, uSize]
        return nv
    }

    fun YUV_420_888toYUV(image: Image, outFormat: IntArray): ByteArray {
        val nv: ByteArray
        val plane0 = image.planes[0]
        val plane1 = image.planes[1]
        val plane2 = image.planes[2]
        val yBuffer = plane0.buffer
        val uBuffer = plane1.buffer
        val vBuffer = plane2.buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        nv = ByteArray(ySize + uSize + vSize)
        if (plane1.pixelStride == 1 && plane2.pixelStride == 1) {
            // YV12
            if (outFormat.size > 0) outFormat[0] = ImageFormat.YV12
            yBuffer[nv, 0, ySize]
            uBuffer[nv, ySize, uSize]
            vBuffer[nv, ySize + uSize, vSize]
        } else {
            // nv21
            if (outFormat.size > 0) outFormat[0] = ImageFormat.NV21
            yBuffer[nv, 0, ySize]
            vBuffer[nv, ySize, vSize]
            uBuffer[nv, ySize + vSize, uSize]
        }

//        yBuffer.get(nv, 0, ySize);
//        vBuffer.get(nv, ySize, vSize);
//        uBuffer.get(nv, ySize + vSize, uSize);
        return nv
    }
}