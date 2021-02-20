package com.dashboard.kotlin

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import java.io.File



class MainActivity : AppCompatActivity() {
    private lateinit var cmdHelper:CommandHelper
    private lateinit var webView: WebView
    private val handler: Handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCenter.start(application, "541c81e0-57ce-4ea1-a6df-503900749e46",
                Analytics::class.java, Crashes::class.java)

        cmdHelper = CommandHelper(applicationContext)

        if (!File(dataDir,"started").exists()){
            val dirArray: Array<String> = arrayOf("/data/adb/clash","/sdcard/Documents/clash")
            for (trydir in dirArray){
                val suCmdR: String = cmdHelper.suCmd("if [ -d \"${trydir}\" ];then echo \"${trydir}\"; fi")
                if (suCmdR != "") {
                    cmdHelper.suCmd("mv $suCmdR "+getString(R.string.ConfigDir))
                    Toast.makeText(applicationContext,"配置文件已迁移到新目录",Toast.LENGTH_LONG).show()
                    break
                }
            }
            File(dataDir,"started").createNewFile()
        }




        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val toolBar:androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        val navigationview: NavigationView = findViewById(R.id.navigation_view)
        val drawer:DrawerLayout = findViewById(R.id.drawer_layout)
        setSupportActionBar(toolBar)
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolBar, 0, 0
        )
        drawer.setDrawerListener(toggle)
        toggle.syncState()

        val headerView = navigationview.getHeaderView(0)


        //<editor-fold desc="按键监听事件区">
        //<editor-fold desc="切换版本 btn">
        val switchClashVersionBtn: Button = navigationview.findViewById(R.id.footer_item_switchClashVersionBtn)
        switchClashVersionBtn.setOnClickListener{

            Toast.makeText(applicationContext,"已废弃",Toast.LENGTH_SHORT).show()

        }
        //</editor-fold>


        val selfReporBtn: Button = navigationview.findViewById(R.id.footer_item_selfReportBtn)
        selfReporBtn.setOnClickListener{
//            Toast.makeText(applicationContext,"已将你的 操作记录/节点信息/IP信息 提交到公安部在线举报中心",Toast.LENGTH_LONG).show()
//            Toast.makeText(applicationContext,"望改邪归正",Toast.LENGTH_LONG).show()
//            Toast.makeText(applicationContext,"FAKE MESSAGE",Toast.LENGTH_LONG).show()
        }

        //<editor-fold desc="头像点击事件">
        val imageView: ImageView = headerView.findViewById(R.id.iv_head)
        imageView.setOnClickListener {
            Toast.makeText(applicationContext, "新年快乐", Toast.LENGTH_SHORT).show()
        }
        //</editor-fold>

        //设置条目点击监听
        //<editor-fold desc="侧栏点击事件">
        navigationview.setNavigationItemSelectedListener { menuItem ->
            Log.i("setNavigationItemSelectedListener",menuItem.itemId.toString())
            when(menuItem.itemId){
                R.id.subscriptionManagementItem -> {
                    val editText = EditText(this)
                    val inputDialog = AlertDialog.Builder(this)
                    inputDialog.setTitle("还没写")
                        .setView(editText)
                        .setCancelable(false)
                        .setPositiveButton("OK",
                        DialogInterface.OnClickListener { dialog, which ->

                        }).show()

                }
                R.id.checkIPItem -> webView.loadUrl("https://ip.skk.moe/")
                R.id.dashBoardItem -> {
                    webView.loadUrl(cmdHelper.getDashBoardURL())
                }
                R.id.updateGeoIPDatabaseItem -> {
                    Toast.makeText(applicationContext,"GeoIP 开始下载",Toast.LENGTH_SHORT).show()
                    Thread{

                        val isSuccess: Boolean = cmdHelper.downloadGithub(getString(R.string.GeoIPURL),
                                externalCacheDir.toString(),
                                getString(R.string.GeoIPDatabaseFile),
                                true)
                        if (isSuccess){
                            val oldDir: String =externalCacheDir.toString()+
                                    getString(R.string.GeoIPDatabaseFile)
                            val newDir: String =getString(R.string.ConfigDir) +
                                    getString(R.string.GeoIPDatabaseFile)
                            cmdHelper.suCmd("mv $oldDir $newDir")

                            handler.post(Runnable {
                                Toast.makeText(applicationContext,
                                        "更新完成", Toast.LENGTH_LONG).show()
                            })
                        }


                    }.start()

                }
                R.id.downLoadYACDItem ->{
                    Toast.makeText(applicationContext,"YACD 面板 开始下载",Toast.LENGTH_SHORT).show()

                    Thread{

                        val isSuccess: Boolean = cmdHelper.downloadGithub(
                                getString(R.string.yacdURL),
                                externalCacheDir.toString(),
                                "yacd.zip",
                                true
                        )
                        if (isSuccess) {
                            cmdHelper.suCmd("unzip ${externalCacheDir}/yacd.zip -d " +
                                    getString(R.string.ConfigDir))

                            cmdHelper.modifyConfiguration(getString(R.string.ConfigDir)+
                                    getString(R.string.ConfigFile))


                        }

                        handler.post(Runnable {
                            Toast.makeText(applicationContext,
                            "安装完成", Toast.LENGTH_LONG).show()
                        })
                    }.start()


                }
                R.id.modifyConfigurationItem ->{
                    Toast.makeText(applicationContext,"正在修改",Toast.LENGTH_SHORT).show()

                    Thread{
                        cmdHelper.modifyConfiguration(getString(R.string.ConfigDir)+
                                getString(R.string.ConfigFile))

                        handler.post(Runnable {
                            Toast.makeText(applicationContext,
                                    "修改完成", Toast.LENGTH_LONG).show()
                        })
                    }.start()

                }
                else -> Toast.makeText(applicationContext,
                        menuItem.title.toString() + "-- 还没做",
                        Toast.LENGTH_SHORT).show()
            }

            //设置哪个按钮被选中
//                menuItem.setChecked(true);
            //关闭侧边栏
            drawer.closeDrawers();
            false
        }
        //</editor-fold>
        //</editor-fold>

        //<editor-fold desc="WebView">
        // WebView
        Log.i("URL",cmdHelper.getDashBoardURL())
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.databaseEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.saveFormData = true
        webView.webViewClient = MyWebViewClient()
        webView.loadUrl(cmdHelper.getDashBoardURL())
        //</editor-fold>




    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()){
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    class MyWebViewClient: WebViewClient(){
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)
            return true
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.control_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Toast.makeText(applicationContext,"已执行命令",Toast.LENGTH_SHORT).show()
        when (item.itemId) {
            R.id.startClash -> cmdHelper.suCmd(getString(R.string.startClashCmd))
            R.id.stopClash -> cmdHelper.suCmd(getString(R.string.stopClashCmd))
            R.id.restartClash -> cmdHelper.suCmd(getString(R.string.restartClashCmd))
            else ->return super.onOptionsItemSelected(item)
        }
        return true
    }
    private fun showInputPasswordDialog() {

    }

}


//<editor-fold desc="ContentProvider - Disable">
//class InitProvider : ContentProvider() {
//
//    override fun onCreate(): Boolean {
//        // 这里可以获得 context 属性
//        return false
//    }
//
//    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
//
//    override fun query(
//            uri: Uri,
//            projection: Array<out String>?,
//            selection: String?,
//            selectionArgs: Array<out String>?,
//            sortOrder: String?
//    ): Cursor? = null
//
//    override fun update(
//            uri: Uri,
//            values: ContentValues?,
//            selection: String?,
//            selectionArgs: Array<out String>?
//    ): Int = 0
//
//    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
//
//    override fun getType(uri: Uri): String? = null
//}
//</editor-fold>
