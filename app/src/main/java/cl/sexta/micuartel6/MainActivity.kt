package cl.sexta.micuartel6

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("mc6", MODE_PRIVATE) }
    // API REST de WordPress. Se puede cambiar aquí cuando definamos la URL definitiva.
    private val baseUrl = "https://sexta.cl/wp-json/mc6/v1"

    private var requestInProgress = false

    private lateinit var loginBox: LinearLayout
    private lateinit var homeBox: LinearLayout
    private lateinit var user: EditText
    private lateinit var password: EditText
    private lateinit var status: TextView
    private lateinit var welcome: TextView
    private lateinit var modules: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loginBox = findViewById(R.id.loginBox)
        homeBox = findViewById(R.id.homeBox)
        user = findViewById(R.id.etUser)
        password = findViewById(R.id.etPassword)
        status = findViewById(R.id.tvStatus)
        welcome = findViewById(R.id.tvWelcome)
        modules = findViewById(R.id.modulesContainer)
        findViewById<Button>(R.id.btnLogin).setOnClickListener { login() }
        findViewById<Button>(R.id.btnLogout).setOnClickListener { logout() }
        val token = prefs.getString("token", null)
        if (!token.isNullOrBlank()) loadMe(token)
    }

    private fun login() {
        val u = user.text.toString().trim()
        val p = password.text.toString()
        if (u.isEmpty() || p.isEmpty()) { status.text = "Ingresa usuario y contraseña."; return }
        setBusy(true, "Ingresando…")
        executor.execute {
            try {
                val body = JSONObject().put("username", u).put("password", p).toString()
                val response = request("/login", "POST", body, null)
                if (response.code in 200..299) {
                    val json = JSONObject(response.body)
                    val token = json.getString("token")
                    prefs.edit().putString("token", token).apply()
                    runOnUiThread { showHome(json.optJSONObject("user")); loadModules(token) }
                } else {
                    val msg = JSONObject(response.body).optString("message", "No fue posible iniciar sesión.")
                    runOnUiThread { setBusy(false, msg) }
                }
            } catch (e: Exception) { runOnUiThread { setBusy(false, "Error de conexión: ${e.message ?: "sin detalle"}") } }
        }
    }

    private fun loadMe(token: String) {
        setBusy(true, "Comprobando sesión…")
        executor.execute {
            try {
                val r = request("/me", "GET", null, token)
                if (r.code in 200..299) {
                    val u = JSONObject(r.body).optJSONObject("user")
                    runOnUiThread { showHome(u); loadModules(token) }
                } else {
                    prefs.edit().remove("token").apply(); runOnUiThread { showLogin("La sesión ya no es válida.") }
                }
            } catch (e: Exception) { runOnUiThread { showLogin("No se pudo conectar con Mi Cuartel 6.") } }
        }
    }

    private fun loadModules(token: String) {
        executor.execute {
            try {
                val r = request("/modules", "GET", null, token)
                if (r.code !in 200..299) throw Exception(JSONObject(r.body).optString("message", "No se pudieron cargar los módulos."))
                val arr = JSONObject(r.body).optJSONArray("modules")
                runOnUiThread {
                    modules.removeAllViews()
                    if (arr == null || arr.length() == 0) {
                        val t = TextView(this); t.text = "No hay módulos asignados."; modules.addView(t)
                    } else for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        val b = Button(this)
                        b.text = m.optString("title", m.optString("key"))
                        b.isAllCaps = false
                        b.setOnClickListener { Toast.makeText(this, "Módulo preparado: ${m.optString("title")}", Toast.LENGTH_SHORT).show() }
                        modules.addView(b)
                    }
                    setBusy(false, "Conectado correctamente.")
                }
            } catch (e: Exception) { runOnUiThread { setBusy(false, "No se pudieron cargar los módulos: ${e.message ?: "error"}") } }
        }
    }

    private fun logout() {
        val token = prefs.getString("token", null)
        prefs.edit().remove("token").apply()
        if (token != null) executor.execute { try { request("/logout", "POST", "{}", token) } catch (_: Exception) {} }
        showLogin("Sesión cerrada.")
    }

    private fun showHome(u: JSONObject?) {
        loginBox.visibility = View.GONE; homeBox.visibility = View.VISIBLE
        val name = u?.optString("display_name").orEmpty()
        welcome.text = if (name.isBlank()) "Bienvenido a Mi Cuartel 6" else "Bienvenido, $name"
        status.text = ""
    }

    private fun showLogin(message: String) {
        loginBox.visibility = View.VISIBLE; homeBox.visibility = View.GONE; setBusy(false, message)
    }

    private fun setBusy(busy: Boolean, message: String) {
        requestInProgress = busy
        status.text = message
        findViewById<Button>(R.id.btnLogin).isEnabled = !busy
        findViewById<Button>(R.id.btnLogout).isEnabled = !busy
    }

    private data class Response(val code: Int, val body: String)

    private fun request(path: String, method: String, body: String?, token: String?): Response {
        val c = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 15000; readTimeout = 20000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) doOutput = true
        }
        if (body != null) c.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        val stream = if (c.responseCode >= 400) c.errorStream else c.inputStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
        return Response(c.responseCode, text)
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
