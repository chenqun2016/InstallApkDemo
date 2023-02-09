package com.example.installdemo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.FileProvider
import com.yanzhenjie.permission.runtime.Permission
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class MainActivity : AppCompatActivity() {
    companion object{
      const val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.tv_1).setOnClickListener {
            doOther(0)
        }
        findViewById<View>(R.id.tv_2).setOnClickListener {
            doOther(1)
        }
        findViewById<View>(R.id.tv_3).setOnClickListener {
            doOther(3)
        }
        AndPermission.with(this)
            .runtime()
            .permission(Permission.WRITE_EXTERNAL_STORAGE,
                Permission.READ_EXTERNAL_STORAGE)
            .onGranted {

            }
            .start()

    }

    private fun doOther(type:Int) {
        val s = PathUtils.getExternalAppCachePath() + "/dddd/"
        File(s).mkdirs()
        val s1 = s + "a.apk"
        copyAssetsFile(this,"aaa.apk",s1)

        val des = File(s1)
        if(type == 0){
            Update(des,this)
        }else if(type == 1){
            AndPermission.with(this)
                .install()
                .file(des)
                .start()
        }else{
            AppUtils.installApp(des)
        }

//        openApk(Uri.parse(s1),this)
    }

    fun Update(downloaded_apk: File, context: Context) {
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                val installApplicationIntent = Intent(Intent.ACTION_VIEW)
                if (downloaded_apk.exists()) {
                    downloaded_apk.setReadable(true)
                    installApplicationIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            "$packageName.fileprovider",
                            downloaded_apk
                        ), "application/vnd.android.package-archive"
                    )
                }
                installApplicationIntent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                installApplicationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                installApplicationIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(installApplicationIntent)
            } catch (cv: java.lang.Exception) {
                cv.printStackTrace()
            }
        }
    }
    /**
     * 安装apk
     * @param uri apk存放的路径
     * @param context
     */
    fun openApk(uri: Uri?, context: Context) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        if (Build.VERSION.SDK_INT >= 24) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }


    /**
     * 复制文件到SD卡
     * @param context
     * @param fileName 复制的文件名
     * @param path  保存的目录路径
     * @return
     */
    fun copyAssetsFile(context: Context, fileName: String, path: String): Uri? {
        return try {
            val mInputStream: InputStream = context.getAssets().open(fileName)
            val mFile = File(path)
            mFile.parentFile.mkdirs()
            if (!mFile.exists()) mFile.createNewFile()
            val mFileOutputStream = FileOutputStream(mFile)
            val mbyte = ByteArray(1024)
            var i = 0
            while (mInputStream.read(mbyte).also { i = it } > 0) {
                mFileOutputStream.write(mbyte, 0, i)
            }
            mInputStream.close()
            mFileOutputStream.close()
            var uri: Uri? = null
            try {
                uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    //包名.fileprovider
                    FileProvider.getUriForFile(context, "com.example.installdemo.fileprovider", mFile)
                } else {
                    Uri.fromFile(mFile)
                }
            } catch (anfe: ActivityNotFoundException) {
                LogUtils.e(TAG, anfe.message)
            }
            MediaScannerConnection.scanFile(context, arrayOf(mFile.getAbsolutePath()), null, null)
//            LogUtils.e(TAG, "拷贝完毕：$uri")
            uri
        } catch (e: IOException) {
            e.printStackTrace()
            LogUtils.e(TAG, fileName + "not exists" + "or write err")
            null
        } catch (e: Exception) {
            null
        }
    }
}