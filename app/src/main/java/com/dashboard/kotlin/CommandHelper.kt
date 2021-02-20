package com.dashboard.kotlin

import android.R.attr.port
import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL


class CommandHelper(context: Context) {
    private val context = context
    private val handler: Handler = Handler()
    //                handler.post(Runnable {
//                    Toast.makeText(context.applicationContext,
//                            "下载完成", Toast.LENGTH_LONG).show()
//                })


    fun saveFile(path: String, filename: String, text: String): Unit {
        try {
            val f = File(path, filename)
            if (!f.exists()){
                if (!f.getParentFile().exists()){
                    f.getParentFile().mkdirs()
                }
            }
            f.writeText(text)
        } catch (e: Exception){Log.e("saveFile", "Exception: $e")}

    }

    fun download(link: String, path: String, filename: String): Boolean {
        var isSuccess = false
        val downloadThread: Thread = Thread{
            Log.i("download", "下载开始")
            try {
                    URL(link).openStream().use { input ->
                        FileOutputStream({
                            val f = File(path, filename)
                            try {
                                if (!f.exists()) {
                                    if (!f.getParentFile().exists()) {
                                        f.getParentFile().mkdirs()
                                    }
                                }

                            } catch (e: Exception) {
                                Log.e("download", "Exception: $e")
                            }
                            f
                        }()).use { output ->
                            input.copyTo(output)
                        }
                    }

            isSuccess = true

            }catch (e: Exception) {
                Log.e("download", "Exception: $e")
            }
        }

        downloadThread.start()
        downloadThread.join()




        handler.post(Runnable {
                    Toast.makeText(context, {
                        if (isSuccess) {
                            context.getString(R.string.downloadSuccess)
                        } else {
                            context.getString(R.string.downloadFailure)
                        }
                    }(), Toast.LENGTH_SHORT).show()
                })

        Log.i("download", "下载线程退出")

        return isSuccess



    }

    fun downloadGithub(link: String, path: String, filename: String, useCDN: Boolean): Boolean{
        return download({
            if (useCDN) {
                context.getString(R.string.CDN) + link
            } else {
                link
            }
        }(), path, filename)
    }

    fun suCmd(cmd: String): String {
        var process: Process? = null
        var os: DataOutputStream? = null
        var ls: DataInputStream? = null
        var result: String = ""
        Log.d("suCmd", "Command: $cmd")
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            ls = DataInputStream(process.inputStream)
            os.writeBytes(cmd + "\n")
            os.writeBytes("exit\n")
            os.flush()
            var line = ls.readLine()
            while (line != null) {
                result += line
                line = ls.readLine()
            }
            process.waitFor()
        } catch (e: Exception) {
            Log.e("suCmd", "Exception: $e")
        } finally {
            try {
                os?.close()
                ls?.close()
                process?.destroy()
            } catch (e: Exception) {
                Log.e("suCmd", "close stream exception: $e")
            }
        }
        Log.d("suCmd", "result: $result")
        return result
    }

    fun modifyConfiguration(confFilePath: String){
        var grepR: String = ""
        var awkR: String =""

        val replaceMap: Map<String,String> = mapOf(
                "Proxy" to "proxies",
                "Proxy Group" to "proxy-groups",
                "Rule" to "rules",
                "proxy-provider" to "proxy-providers",
                "rule-provider" to "rule-providers",
                "Final" to "Match",
                "SOURCE-IP-CIDR"  to "SRC-IP-CIDR")

        replaceMap.map { (before, after) ->
            suCmd("sed -i 's/^${before}/${after}/g' $confFilePath")

        }


        grepR = suCmd("cat $confFilePath | grep \"external-controller\"")
        if (grepR == ""){
            suCmd("sed -i '/^proxies/i external-controller: 127.0.0.1:9090' $confFilePath")
        }
        else{
            awkR = suCmd(" echo \"${grepR}\" | awk -F ' ' '{print \$2}' ")
            if ({
                    val rePattren: String = context.getString(R.string.pattern)
                    val isMacthed: Boolean = Regex(rePattren).matches(awkR)
                    Log.i("re",rePattren)
                    Log.i("re",isMacthed.toString())
                    !isMacthed
                }()
            ){
                suCmd("sed -i '/^external-controller/c external-controller: 127.0.0.1:9090' $confFilePath")
            }

        }


        grepR = suCmd("cat $confFilePath | grep \"external-ui\"")
        if (grepR == ""){
            suCmd("sed -i '/^external-controller/a external-ui: yacd-gh-pages' $confFilePath")
        }
        else{
            suCmd("sed -i '/^external-ui/c external-ui: yacd-gh-pages' $confFilePath")
        }


        grepR = suCmd("cat $confFilePath | grep \"tproxy-port\"")
        if (grepR == ""){
            suCmd("sed -i '/^proxies/i tproxy-port: 63002' $confFilePath")
        }
        else{
            awkR = suCmd(" echo \"${grepR}\" | awk -F ':' '{print \$2}' ")
            if (awkR == ""){
                suCmd("sed -i '/^tproxy-port/c tproxy-port: 127.0.0.1:63002' $confFilePath")
            }
        }

        awkR = suCmd("grep \"listen\" $confFilePath | awk -F ':' '{print \$3}'  ")
        if (awkR == ""){
            suCmd("sed -i '/^proxies/i " +
                    "dns:\\n  enable: true\\n  ipv6: false\\n  listen: 0.0.0.0:1053\\n  enhanced-mode: redir-host \\n  default-nameserver: \\n    - 223.5.5.5 \\n  nameserver:\\n    - tls://dns.pub\\n    - tls://doh.pub\\n    # - tls://223.5.5.5\\n    # - tls://185.222.222.222\\n    - https://doh.pub/dns-query\\n    - https://dns.pub/dns-query\\n    - https://dns.alidns.com/dns-query\\n    - https://rubyfish.cn/dns-query\\n  fallback:\\n    # - tls://1.1.1.1\\n    # - tls://8.8.8.8\\n    - https://cloudflare-dns.com/dns-query\\n    - https://dns.google/dns-query\\n    - https://doh.dns.sb/dns-query\\n    - https://doh.opendns.com/dns-query\\n    - https://doh.pub/dns-query\\n    - https://dns.alidns.com/dns-query' " +
                    "$confFilePath")
        }

    }


}