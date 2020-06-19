package com.mama.views.utils

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    private const val TAG = "FileUtils"
    private val sFileComparator = FileComparator()
    private const val IMAGE_JPG = ".jpg"
    val SYSTEM_PHOTO_PATH =
        Environment.getExternalStorageDirectory().path + "/DCIM/Camera/"

    //	public static final String SMART_PATH = Environment.getExternalStorageDirectory().getPath() + "/smartmore";
    private var SMART_DIR: String? = null
    private var SMART_MODEL_DIR: String? = null
    fun init(context: Context) {
        Log.d(
            TAG,
            "mama= SYSTEM_PHOTO_PATH=$SYSTEM_PHOTO_PATH"
        )
        Log.d(
            TAG,
            "mama= context.getExternalFilesDir=" + context.getExternalFilesDir(null)
        )
        Log.d(
            TAG,
            "mama= context.getFilesDir=" + context.filesDir
        )
        Log.d(
            TAG,
            "mama= picture=" + context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        val smartDir = context.getExternalFilesDir(null)
        SMART_DIR = if (smartDir == null) context.filesDir
            .absolutePath else smartDir.absolutePath
        val smartModelDir = context.getExternalFilesDir("MODEL")
        SMART_MODEL_DIR = if (smartModelDir == null) File(
            SMART_DIR,
            "MODEL"
        ).absolutePath else smartModelDir.absolutePath
        checkDirPath(SMART_DIR)
        checkDirPath(SMART_MODEL_DIR)
    }

    fun SMART_DIR(): String? {
        return SMART_DIR
    }

    fun SMART_MODEL_DIR(): String? {
        return SMART_MODEL_DIR
    }

    fun getAssetData(context: Context, path: String?): ByteArray? {
        var stream: InputStream? = null
        var data: ByteArray? = null
        try {
            stream = context.assets.open(path!!)
            val length = stream.available()
            data = ByteArray(length)
            stream.read(data)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return data
    }

    fun getAssetDataStr(context: Context, path: String?): String? {
        var stream: InputStream? = null
        try {
            stream = context.assets.open(path!!)
            val length = stream.available()
            val data = ByteArray(length)
            stream.read(data)
            return String(data)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * file is exist and the file is not Directory
     *
     * @param filePath
     * @return boolean
     */
    fun isFileExist(filePath: String?): Boolean {
        val file = File(filePath)
        return file.exists() && !file.isDirectory
    }

    /**
     * file is exist and the file is Directory
     *
     * @param dirPath
     * @return boolean
     */
    fun isDirExist(dirPath: String?): Boolean {
        val file = File(dirPath)
        return file.exists() && file.isDirectory
    }

    fun isPathExist(path: String?): Boolean {
        if (TextUtils.isEmpty(path)) {
            printLog("isPathExist path is null")
            return false
        }
        return File(path).exists()
    }

    /**
     * Create Directory For Path
     *
     * @param path
     * @return boolean
     */
    fun createDirectory(path: String?): Boolean {
        if (TextUtils.isEmpty(path)) {
            printLog("createDirectory path is empty")
            return false
        }
        val dirFile = File(path)
        return if (!dirFile.exists() || !dirFile.isDirectory) {
            dirFile.mkdirs()
        } else true
    }

    /**
     * assert_path resources copy into Local SD card
     *
     * @param assertPathDir
     * @param dirPath
     */
    fun copyFilesToLocalIfNeed(
        context: Context,
        assertPathDir: String,
        dirPath: String
    ) {
        val pictureDir = File(dirPath)
        if (!pictureDir.exists() || !pictureDir.isDirectory) {
            pictureDir.mkdirs()
        }
        try {
            val fileNames = context.assets.list(assertPathDir)
            if (fileNames!!.size == 0) return
            for (i in fileNames.indices) {
                val file =
                    File(dirPath + File.separator + fileNames[i])
                if (file.exists() && file.isFile) {
                    if (compareFile(
                            context, dirPath + File.separator + fileNames[i],
                            assertPathDir + File.separator + fileNames[i]
                        )
                    ) {
                        printLog("-->copyAssertDirToLocalIfNeed " + file.name + " exists")
                        continue
                    }
                }
                val `is` = context.assets
                    .open(assertPathDir + File.separator + fileNames[i])
                val size = `is`.available()
                val buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
                val mypath = dirPath + File.separator + fileNames[i]
                val fop = FileOutputStream(mypath)
                fop.write(buffer)
                fop.flush()
                fop.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Used to copy assert file into Local SD card
     *
     * @param context
     * @param assetsPath
     * @param strOutFileName
     * @param isCover
     * @throws IOException
     */
    @Throws(IOException::class)
    fun copyAssetsFileToLocalIfNeed(
        context: Context,
        assetsPath: String?,
        strOutFileName: String?,
        isCover: Boolean
    ) {
        val file = File(strOutFileName)
        if (file.exists() && file.isFile && !isCover) {
            printLog("copyAssertFileToLocalIfNeed " + file.name + " exists")
            return
        }
        val myInput: InputStream
        val myOutput: OutputStream = FileOutputStream(strOutFileName)
        myInput = context.assets.open(assetsPath!!)
        val buffer = ByteArray(1024)
        var length = myInput.read(buffer)
        while (length > 0) {
            myOutput.write(buffer, 0, length)
            length = myInput.read(buffer)
        }
        myOutput.flush()
        myInput.close()
        myOutput.close()
    }

    fun setPermissions(
        filePath: String?,
        read: Boolean,
        write: Boolean,
        exe: Boolean,
        userOnly: Boolean
    ) {
        val file = File(filePath)
        if (!file.exists() || file.isDirectory) {
            return
        }
        file.setReadable(read, userOnly)
        file.setWritable(write, userOnly)
        file.setExecutable(exe, userOnly)
    }

    /**
     * 获取指定文件大小
     *
     * @param path
     * @return
     * @throws Exception
     */
    fun getFileSize(path: String?): Long {
        val f = File(path)
        return getFileSize(f)
    }

    /**
     * 获取指定文件大小
     *
     * @param file
     * @return
     * @throws Exception
     */
    fun getFileSize(file: File): Long {
        var size: Long = 0
        if (!file.exists() || file.isDirectory) {
            printLog("getFileSize file is not exists or isDirectory !")
            return 0
        }
        if (file.exists()) {
            var fis: FileInputStream? = null
            try {
                fis = FileInputStream(file)
                size = fis.available().toLong()
                fis.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return size
    }

    /**
     * 获取Asset目录下某个文件的大小，非目录
     *
     * @param context
     * @param path
     * @return
     */
    fun getAssertFileSize(context: Context?, path: String?): Long {
        if (context == null || path == null || "" == path) {
            printLog("getAssertFileSize context is null or path is null !")
            return 0
        }
        printLog("getAssertFileSize path:$path")
        val assetManager = context.assets
        var size = 0
        try {
            val inStream = assetManager.open(path)
            size = inStream.available()
            inStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return size.toLong()
    }

    /**
     * 比较sd卡文件与asset目录下的文件大小是否一致
     *
     * @param context
     * @param filePath
     * @param assetPath
     * @return
     */
    fun compareFile(
        context: Context?,
        filePath: String?,
        assetPath: String?
    ): Boolean {
        var isSameFile = false
        val file = File(filePath)
        if (!file.exists() || file.isDirectory) {
            isSameFile = false
        }
        if (getFileSize(file) == getAssertFileSize(
                context,
                assetPath
            )
        ) {
            isSameFile = true
        }
        return isSameFile
    }

    @SuppressLint("SimpleDateFormat")
    fun genSystemCameraPhotoPath(): String {
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        checkDirPath(SYSTEM_PHOTO_PATH)
        return SYSTEM_PHOTO_PATH + "IMAGE_" + timeStamp + ".jpg"
    }

    fun checkDirPath(path: String?) {
        if (!isDirExist(path)) {
            createDirectory(path)
        }
    }

    /**
     * Delete file by File
     *
     * @param filePath
     */
    fun deleteFile(filePath: String?) {
        if (TextUtils.isEmpty(filePath)) {
            printLog("deleteFile path is null")
            return
        }
        val file = File(filePath)
        deleteFile(file)
    }

    fun deleteDirFile(dir: File?) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        for (file in dir.listFiles()) {
            if (file.isFile) file.delete() else if (file.isDirectory) deleteDirFile(
                file
            )
        }
        dir.delete()
    }

    fun deleteAndMakeDir(path: String?) {
        val outDir = File(path)
        deleteDirFile(outDir)
        if (!outDir.exists() || !outDir.isDirectory) {
            outDir.mkdirs()
        }
    }

    /**
     * Delete file by File
     *
     * @param file
     */
    fun deleteFile(file: File?) {
        if (null == file) {
            printLog("deleteFile file is null")
            return
        }
        if (!file.exists() || !file.isFile) {
            printLog("deleteFile file is not exists or file is dir!")
            return
        }
        file.delete()
    }

    /**
     * Used to clear file of directory
     *
     * @param path
     * @param isDeleteThisDir
     */
    fun clearDir(path: String?, isDeleteThisDir: Boolean) {
        if (TextUtils.isEmpty(path)) {
            printLog("clearDir path is null")
            return
        }
        val file = File(path)
        clearDir(file, isDeleteThisDir)
    }

    fun checkSDCardAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Delete the dir in SD card
     *
     * @param dirFile
     * @param isDeleteThisDir
     */
    fun clearDir(dirFile: File?, isDeleteThisDir: Boolean) {
        if (!checkSDCardAvailable() || dirFile == null) {
            printLog("clearDir dirFile is null")
            return
        }
        try {
            if (dirFile.isDirectory) {
                val files = dirFile.listFiles()
                for (i in files.indices) {
                    clearDir(files[i], true)
                }
            }
            if (isDeleteThisDir) {
                if (!dirFile.isDirectory) {
                    dirFile.delete()
                } else {
                    if (dirFile.listFiles().size == 0) {
                        dirFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Used to get System Camera Photo Path
     *
     * @return String
     */
    @get:SuppressLint("SimpleDateFormat")
    val systemCameraPhotoPath: String
        get() {
            printLog("genSystemCameraPhotoPath SYSTEM_PHOTO_PATH ---> $SYSTEM_PHOTO_PATH")
            val timeStamp =
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            createDirectory(SYSTEM_PHOTO_PATH)
            return SYSTEM_PHOTO_PATH + "IMAGE_" + timeStamp + IMAGE_JPG
        }

    /**
     * Used to get System Camera Photo Path
     *
     * @return String
     */
    @get:SuppressLint("SimpleDateFormat")
    val bitmapSavePath: String
        get() {
            printLog("genSystemCameraPhotoPath SYSTEM_PHOTO_PATH ---> $SYSTEM_PHOTO_PATH")
            val timeStamp =
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            return timeStamp + IMAGE_JPG
        }

    /**
     * Used to get the file name if file is exist
     *
     * @param filePath
     * @return String
     */
    fun getFileName(filePath: String?): String? {
        val file = File(filePath)
        return if (file.exists()) {
            file.name
        } else null
    }

    /**
     * Used to get the files of direction and put into list<String>
     *
     * @param dirPath
     * @return List<String>
    </String></String> */
    fun loadChildFiles(dirPath: String?): List<String>? {
        if (TextUtils.isEmpty(dirPath)) {
            printLog("loadImages---> path is null")
            return null
        }
        val fileList: MutableList<String> =
            ArrayList()
        val file = File(dirPath)
        if (!file.exists()) {
            return fileList
        }
        val files = file.listFiles()
        val allFiles: MutableList<File> =
            ArrayList()
        for (img in files) {
            allFiles.add(img)
        }
        Collections.sort(
            allFiles,
            sFileComparator
        )
        for (img in allFiles) {
            fileList.add(img.absolutePath)
        }
        return fileList
    }

    /**
     * Put the file into byte[]
     *
     * @param filePath
     * @return byte[]
     */
    fun readFile(filePath: String?): ByteArray? {
        if (TextUtils.isEmpty(filePath) && isPathExist(filePath)) {
            printLog("readFile path is null")
            return null
        }
        try {
            val fis = FileInputStream(filePath)
            val length = fis.available()
            val buffer = ByteArray(length)
            fis.read(buffer)
            fis.close()
            return buffer
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(IOException::class)
    fun readRawFile(context: Context?, resourceId: Int): ByteArray? {
        if (null == context || resourceId < 0) {
            return null
        }
        val result: String? = null
        var buffer: ByteArray? = null
        val inputStream = context.resources.openRawResource(resourceId)
        // 获取文件的字节数
        val length = inputStream.available()
        if (length <= 0) {
            return null
        }

        // 创建byte数组
        buffer = ByteArray(length)
        // 将文件中的数据读到byte数组中
        val len = inputStream.read(buffer)
        return buffer
    }

    /**
     * Save the file to savePath if file is exist
     *
     * @param savePath
     * @param data
     */
    fun saveFile(savePath: String?, data: ByteArray?) {
        if (data == null || data.size == 0) {
            printLog("saveFile data is null")
            return
        }
        try {
            val file = File(savePath)
            val fos = FileOutputStream(file)
            fos.write(data)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Print log
     *
     * @param logStr
     */
    fun printLog(logStr: String?) {
        Log.d(TAG, logStr)
    }

    fun getFromAssets(
        context: Context,
        fileName: String?
    ): ByteArray? {
        var buffer: ByteArray? = null
        try {
            val `in` = context.resources.assets.open(fileName!!)
            // 获取文件的字节数
            val lenght = `in`.available()
            // 创建byte数组
            buffer = ByteArray(lenght)
            // 将文件中的数据读到byte数组中
            `in`.read(buffer)
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return buffer
    }

    fun getFromSdcard(context: Context?, path: String?): ByteArray? {
        var buffer: ByteArray? = null
        try {
            val `in` = FileInputStream(path)
            // 获取文件的字节数
            val lenght = `in`.available()
            // 创建byte数组
            buffer = ByteArray(lenght)
            // 将文件中的数据读到byte数组中
            `in`.read(buffer)
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return buffer
    }

    fun getPath(context: Context, uri: Uri): String {
        val pojo = arrayOf(MediaStore.Images.Media.DATA)
        val cr = context.contentResolver
        var c: Cursor? = null
        c = if (uri.scheme == "content") { // 判断uri地址是以什么开头的
            cr.query(uri, pojo, null, null, null)
        } else {
            cr.query(
                getFileUri(context, uri),
                null,
                null,
                null,
                null
            ) // 红色字体判断地址如果以file开头
        }
        c!!.moveToFirst()
        // 这是获取的图片保存在sdcard中的位置
        val colunm_index = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        return c.getString(colunm_index)
    }

    fun getFileUri(context: Context, uri: Uri): Uri {
        var uri = uri
        if (uri.scheme == "file") {
            var path = uri.encodedPath
            Log.d(TAG, "path1 is $path")
            if (path != null) {
                path = Uri.decode(path)
                Log.d(TAG, "path2 is $path")
                val cr = context.contentResolver
                val buff = StringBuffer()
                buff.append("(").append(MediaStore.Images.ImageColumns.DATA).append("=")
                    .append("'$path'")
                    .append(")")
                val cur = cr.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.ImageColumns._ID),
                    buff.toString(),
                    null,
                    null
                )
                var index = 0
                cur!!.moveToFirst()
                while (!cur.isAfterLast) {
                    index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID)
                    index = cur.getInt(index)
                    cur.moveToNext()
                }
                if (index == 0) {
                    // do nothing
                } else {
                    val uri_temp =
                        Uri.parse("content://media/external/images/media/$index")
                    Log.d(
                        TAG,
                        "uri_temp is $uri_temp"
                    )
                    if (uri_temp != null) {
                        uri = uri_temp
                    }
                }
            }
        }
        return uri
    }

    fun getLatestPhotoPath(context: Context): String? {
        val sdcardPath =
            Environment.getExternalStorageDirectory().toString()
        val mContentResolver = context.contentResolver
        val mCursor = mContentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA),
            MediaStore.Images.Media.MIME_TYPE + "=? OR " + MediaStore.Images.Media.MIME_TYPE + "=?",
            arrayOf("image/jpeg", "image/png"),
            MediaStore.Images.Media._ID + " DESC"
        )
        var photoPath: String? = null
        while (mCursor!!.moveToNext()) {
            val path =
                mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA))
            if (path.startsWith("$sdcardPath/DCIM/100MEDIA") || path.startsWith("$sdcardPath/DCIM/Camera/")
                || path.startsWith(sdcardPath + "DCIM/100Andro")
            ) {
                photoPath = path
                break
            }
        }
        mCursor.close()
        return photoPath
    }

    fun getLatestPhotoPathFromDir(dir: String?): String? {
        val dirFile = File(dir)
        if (dirFile.isDirectory) {
            val files =
                dirFile.listFiles { file, s -> s.endsWith(".jpg") }
            if (files != null && files.size > 0) {
                return files[files.size - 1].absolutePath
            }
        }
        return null
    }

    fun getFilesPathFromDir(dir: String?): List<String> {
        val list: MutableList<String> =
            ArrayList()
        val dirFile = File(dir)
        if (dirFile.isDirectory) {
            val files =
                dirFile.listFiles { file, s -> s.endsWith(".jpg") }
            for (i in files.indices.reversed()) {
                list.add(files[i].absolutePath)
            }
        }
        return list
    }

    private class FileComparator : Comparator<File> {
        override fun compare(file1: File, file2: File): Int {
            return if (file1.lastModified() < file2.lastModified()) {
                -1
            } else {
                1
            }
        }
    }
}