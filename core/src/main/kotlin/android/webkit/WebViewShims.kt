package android.webkit

import android.content.Context
import android.net.Uri

open class WebView(context: Context? = null) {
    var settings: WebSettings = WebSettings()
    var webViewClient: WebViewClient? = null
    var webChromeClient: WebChromeClient? = null

    fun loadUrl(url: String) {}
    fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {}
    fun evaluateJavascript(script: String, resultCallback: ((String) -> Unit)? = null) {}
    fun addJavascriptInterface(obj: Any, name: String) {}
    fun stopLoading() {}
    fun destroy() {}
}

open class WebSettings {
    var javaScriptEnabled: Boolean = true
    var domStorageEnabled: Boolean = true
    var userAgentString: String = ""
}

open class WebViewClient {
    open fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
    open fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false
    open fun onPageFinished(view: WebView?, url: String?) {}
    open fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? = null
}

open class WebChromeClient {
    open fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = false
}

class ConsoleMessage(private val msg: String) {
    fun message(): String = msg
}

class WebResourceRequest(val uri: Uri = Uri.parse("")) {
    val url: Uri get() = uri
}

class WebResourceResponse(val mimeType: String = "", val encoding: String = "", val data: java.io.InputStream? = null)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class JavascriptInterface
