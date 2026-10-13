package com.example.ota.data

import java.util.concurrent.TimeUnit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

/**
 * OTA 平台 API 客户端（ota.aittch.xyz）。
 * 登录后 session cookie 自动保存/携带（OkHttp CookieJar）。
 */
object OtaApi {
    const val BASE = "https://ota.aittch.xyz"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            private val store = HashMap<String, List<Cookie>>()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                // 强制 domain=host（OkHttp 对 IP host 的 cookie domain
                // 归一化有 quirk，可能导致后续请求匹配失败不带 cookie）
                store[url.host] = cookies.map {
                    Cookie.Builder().name(it.name).value(it.value)
                        .domain(url.host).path("/").build()
                }
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                store[url.host] ?: emptyList()
        })
        .build()

    /** 回调统一切主线程（Compose state 只能主线程写） */
    private fun mainThread(cb: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(cb)
    }

    // ── 登录态 ──
    @Volatile var csrfToken: String = ""
    @Volatile var loggedIn: Boolean = false
    @Volatile var displayName: String = ""

    fun login(phone: String, password: String, onResult: (Boolean, String) -> Unit) {
        // CSRF 必须从 /api/auth/me 拿：SPA 模式下 GET /login 返回的是
        // static/index.html 壳，meta csrf 是空的（真实 token 由该接口下发）
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            // 同步拿 csrf（IO 协程内允许阻塞）：/api/auth/me 未登录也返回 csrf_token
            try {
                val req = okhttp3.Request.Builder().url(BASE + "/api/auth/me").build()
                val resp = client.newCall(req).execute()
                val o = org.json.JSONObject(resp.body?.string() ?: "{}")
                csrfToken = o.optString("csrf_token", "")
            } catch (e: Exception) {
                mainThread { onResult(false, "网络错误（无法访问 ota.aittch.xyz）") }
                return@launch
            }
            fetchJson(
                method = "POST",
                path = "/login",
                body = mapOf("phone" to phone, "password" to password, "login_type" to "password"),
                headers = mapOf("X-CSRF-Token" to csrfToken)
            ) { ok, body ->
                if (ok) {
                    loggedIn = true
                    // 再拉一次拿用户信息（登录后的 session）
                    meJson { success ->
                        onResult(success, if (success) "登录成功" else "登录成功（信息获取失败）")
                    }
                } else {
                    onResult(false, body.ifEmpty { "登录失败（账号或密码错误）" })
                }
            }
        }
    }

    fun meJson(onResult: (Boolean) -> Unit) {
        meJsonRetry(onResult, retries = 2)
    }

    /** me 接口带重试：登录后首个请求偶发 cookie 同步延迟 */
    private fun meJsonRetry(onResult: (Boolean) -> Unit, retries: Int) {
        val req = okhttp3.Request.Builder().url(BASE + "/api/auth/me").build()
        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                if (retries > 0) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        { meJsonRetry(onResult, retries - 1) }, 800)
                } else {
                    mainThread { onResult(false) }
                }
            }
            override fun onResponse(call: okhttp3.Call, resp: okhttp3.Response) {
                val body = resp.body?.string() ?: ""
                try {
                    val o = JSONObject(body)
                    csrfToken = o.optString("csrf_token", "")
                    val u = o.optJSONObject("user")
                    displayName = u?.optString("nickname") ?: ""
                    loggedIn = o.optBoolean("logged_in", false)
                    val ok = loggedIn
                    mainThread { onResult(ok) }
                } catch (e: Exception) {
                    if (retries > 0) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            { meJsonRetry(onResult, retries - 1) }, 800)
                    } else {
                        mainThread { onResult(false) }
                    }
                }
            }
        })
    }



    // ── 设备实体（详情页）──
    fun deviceEntities(deviceId: String, onResult: (List<com.example.ota.data.Entity>, List<com.example.ota.data.ButtonMapping>, String?) -> Unit) {
        getJson("/api/device/$deviceId/entities") { ok, body ->
            if (!ok) return@getJson onResult(emptyList(), emptyList(), body)
            try {
                val o = org.json.JSONObject(body)
                val arr = o.optJSONArray("entities") ?: org.json.JSONArray()
                val entities = mutableListOf<com.example.ota.data.Entity>()
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    val rgb = e.optJSONArray("rgb_color")?.let { ja ->
                        (0 until ja.length()).map { ja.optInt(it) }.takeIf { it.size >= 3 }
                    }
                    entities.add(com.example.ota.data.Entity(
                        entityId = e.optString("entity_id"),
                        friendlyName = e.optString("friendly_name"),
                        domain = e.optString("domain"),
                        state = e.optString("state"),
                        brightness = if (e.has("brightness") && !e.isNull("brightness")) e.optInt("brightness") else null,
                        rgbColor = rgb,
                        supportedBrightness = e.optBoolean("supported_brightness"),
                        supportedColor = e.optBoolean("supported_color"),
                        supportedColorTemp = e.optBoolean("supported_color_temp"),
                        colorTemp = if (e.has("color_temp") && !e.isNull("color_temp")) e.optInt("color_temp") else null,
                        minMireds = if (e.has("min_mireds") && !e.isNull("min_mireds")) e.optInt("min_mireds") else null,
                        maxMireds = if (e.has("max_mireds") && !e.isNull("max_mireds")) e.optInt("max_mireds") else null
                    ))
                }
                val maps = mutableListOf<com.example.ota.data.ButtonMapping>()
                val marr = o.optJSONArray("button_mappings") ?: org.json.JSONArray()
                for (i in 0 until marr.length()) {
                    val m = marr.getJSONObject(i)
                    maps.add(com.example.ota.data.ButtonMapping(
                        m.optInt("btn_index"), m.optInt("gpio"), m.optString("devices")))
                }
                onResult(entities, maps, null)
            } catch (e: Exception) {
                onResult(emptyList(), emptyList(), e.message)
            }
        }
    }

    // ── 实体快捷控制 ──
    fun controlEntity(entityId: String, action: String, value: String? = null,
                      onResult: (Boolean, String) -> Unit) {
        val body = mutableMapOf("entity_id" to entityId, "action" to action)
        if (value != null) body["value"] = value
        postJson("/api/entity/control", body) { ok, respBody ->
            try {
                val o = org.json.JSONObject(respBody)
                onResult(o.optBoolean("success", false),
                    o.optString("error").ifEmpty { if (ok) "已执行" else "失败" })
            } catch (e: Exception) {
                onResult(ok, if (ok) "已执行" else respBody.take(60))
            }
        }
    }


    // ── 注册 ──
    fun register(phone: String, password: String, nickname: String,
                 onResult: (Boolean, String) -> Unit) {
        // 先拿 csrf（未登录也可）
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val req = okhttp3.Request.Builder().url(BASE + "/api/auth/me").build()
                val resp = client.newCall(req).execute()
                val o = org.json.JSONObject(resp.body?.string() ?: "{}")
                csrfToken = o.optString("csrf_token", "")
            } catch (e: Exception) { /* 继续，无 csrf 时由服务端 403 提示 */ }
            fetchJson(
                method = "POST",
                path = "/register",
                body = mapOf("phone" to phone, "password" to password,
                             "nickname" to nickname),
                headers = mapOf("X-CSRF-Token" to csrfToken)
            ) { ok, body ->
                var success = ok
                var msg = body.take(80)
                try {
                    val o = org.json.JSONObject(body)
                    success = ok && o.optBoolean("success", false)
                    msg = o.optString("message").ifEmpty { o.optString("error", body.take(60)) }
                } catch (e: Exception) { /* HTML 响应 */ }
                mainThread { onResult(success, msg) }
            }
        }
    }


    // ── 短信验证码 ──
    fun sendSms(phone: String, scene: String = "register",
                onResult: (Boolean, String) -> Unit) {
        postJson("/api/sms/send", mapOf("phone" to phone, "scene" to scene)) { ok, body ->
            var success = ok
            var msg = body.take(60)
            try {
                val o = org.json.JSONObject(body)
                success = ok && o.optBoolean("success", false)
                msg = o.optString("message").ifEmpty { o.optString("error", body.take(60)) }
            } catch (e: Exception) { /* HTML */ }
            mainThread { onResult(success, msg) }
        }
    }

    /** 短信验证码登录（未注册自动注册，需审核） */
    fun loginSms(phone: String, smsCode: String, onResult: (Boolean, String) -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val req = okhttp3.Request.Builder().url(BASE + "/api/auth/me").build()
                val resp = client.newCall(req).execute()
                val o = org.json.JSONObject(resp.body?.string() ?: "{}")
                csrfToken = o.optString("csrf_token", "")
            } catch (e: Exception) {
                mainThread { onResult(false, "网络错误（无法访问 ota.aittch.xyz）") }
                return@launch
            }
            fetchJson(
                method = "POST",
                path = "/login",
                body = mapOf("phone" to phone, "sms_code" to smsCode,
                             "login_type" to "sms"),
                headers = mapOf("X-CSRF-Token" to csrfToken)
            ) { ok, body ->
                if (ok) {
                    loggedIn = true
                    meJson { success ->
                        onResult(success, if (success) "登录成功" else "登录成功（信息获取失败）")
                    }
                } else {
                    var msg = "登录失败"
                    try {
                        val o = org.json.JSONObject(body)
                        msg = o.optString("error", msg)
                    } catch (e: Exception) {}
                    mainThread { onResult(false, msg) }
                }
            }
        }
    }

    /** 短信注册（新手机号 + 验证码 + 密码） */
    fun registerSms(phone: String, smsCode: String, password: String,
                    nickname: String, onResult: (Boolean, String) -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val req = okhttp3.Request.Builder().url(BASE + "/api/auth/me").build()
                val resp = client.newCall(req).execute()
                val o = org.json.JSONObject(resp.body?.string() ?: "{}")
                csrfToken = o.optString("csrf_token", "")
            } catch (e: Exception) {
                mainThread { onResult(false, "网络错误") }
                return@launch
            }
            fetchJson(
                method = "POST",
                path = "/register",
                body = mapOf("phone" to phone, "password" to password,
                             "nickname" to nickname, "sms_code" to smsCode),
                headers = mapOf("X-CSRF-Token" to csrfToken)
            ) { ok, body ->
                var success = ok
                var msg = body.take(80)
                try {
                    val o = org.json.JSONObject(body)
                    success = ok && o.optBoolean("success", false)
                    msg = o.optString("message").ifEmpty { o.optString("error", msg) }
                } catch (e: Exception) {}
                mainThread { onResult(success, msg) }
            }
        }
    }

    // ── 操作日志 ──
    fun operationLogs(onResult: (List<Map<String, String>>, String?) -> Unit) {
        getJson("/api/operation-logs?limit=50") { ok, body ->
            if (!ok) return@getJson onResult(emptyList(), body)
            try {
                val o = org.json.JSONObject(body)
                val arr = o.optJSONArray("logs") ?: org.json.JSONArray()
                val list = mutableListOf<Map<String, String>>()
                for (i in 0 until arr.length()) {
                    val l = arr.getJSONObject(i)
                    list.add(mapOf(
                        "device" to (l.optString("device_remark").ifEmpty { l.optString("device_id") }),
                        "action" to l.optString("action"),
                        "result" to l.optString("result"),
                        "time" to (l.optString("created_at").take(19))
                    ))
                }
                onResult(list, null)
            } catch (e: Exception) {
                onResult(emptyList(), e.message)
            }
        }
    }

    // ── 退出登录 ──
    fun logout(onDone: () -> Unit) {
        postJson("/logout", mapOf()) { _, _ ->
            loggedIn = false
            csrfToken = ""
            displayName = ""
            onDone()
        }
    }

    // ── 设备列表 ──
    fun devices(onResult: (List<Device>, String?) -> Unit) {
        getJson("/api/dashboard") { ok, body ->
            if (!ok) return@getJson onResult(emptyList(), body)
            try {
                val arr = JSONObject(body).optJSONArray("devices") ?: JSONArray()
                val list = mutableListOf<Device>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(Device(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        status = o.optString("status"),
                        firmware = o.optString("firmware_version"),
                        ip = o.optString("ip"),
                        rssi = o.optInt("rssi", 0),
                        otaStatus = o.optString("ota_status"),
                        otaProgress = o.optInt("ota_progress", 0)
                    ))
                }
                onResult(list, null)
            } catch (e: Exception) {
                onResult(emptyList(), e.message)
            }
        }
    }

    // ── 固件列表 ──
    fun firmwares(onResult: (List<Firmware>, String?) -> Unit) {
        getJson("/api/firmware") { ok, body ->
            if (!ok) return@getJson onResult(emptyList(), body)
            try {
                val arr = JSONArray(body)
                val list = mutableListOf<Firmware>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(Firmware(
                        id = o.optInt("id"),
                        name = o.optString("original_name"),
                        version = o.optString("version"),
                        size = o.optLong("size", 0),
                        target = o.optString("target_device")
                    ))
                }
                onResult(list, null)
            } catch (e: Exception) {
                onResult(emptyList(), e.message)
            }
        }
    }

    // ── 发起 OTA ──
    fun startOta(deviceId: String, firmwareId: Int, onResult: (Boolean, String) -> Unit) {
        postJson("/api/ota/upgrade",
            mapOf("device_id" to deviceId, "firmware_id" to firmwareId.toString())) { ok, body ->
            try {
                val o = JSONObject(body)
                onResult(ok && o.optBoolean("success", false),
                    o.optString("message").ifEmpty { o.optString("error", if (ok) "已下发" else "失败") })
            } catch (e: Exception) {
                onResult(ok, if (ok) "已下发" else body)
            }
        }
    }

    fun deviceStatus(deviceId: String, onResult: (JSONObject?) -> Unit) {
        getJson("/api/device/$deviceId/status") { ok, body ->
            onResult(if (ok) try { JSONObject(body) } catch (e: Exception) { null } else null)
        }
    }

    // ── HTTP 基础封装 ──
    private fun get(path: String): String? = try {
        val req = okhttp3.Request.Builder().url(BASE + path).build()
        client.newCall(req).execute().use { it.body?.string() }
    } catch (e: Exception) { null }

    private fun getJson(path: String, cb: (Boolean, String) -> Unit) {
        val builder = okhttp3.Request.Builder().url(BASE + path)
        if (csrfToken.isNotEmpty()) builder.header("X-CSRF-Token", csrfToken)
        client.newCall(builder.build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                mainThread { cb(false, e.message ?: "网络错误") }
            }
            override fun onResponse(call: okhttp3.Call, resp: okhttp3.Response) {
                val body = resp.body?.string() ?: ""
                val ok = resp.isSuccessful
                mainThread { cb(ok, body) }
            }
        })
    }

    private fun postJson(path: String, body: Map<String, String>, cb: (Boolean, String) -> Unit) {
        val json = JSONObject(body).toString()
        val builder = okhttp3.Request.Builder().url(BASE + path)
            .post(okhttp3.RequestBody.create(
                "application/json; charset=utf-8".toMediaType(), json))
        if (csrfToken.isNotEmpty()) builder.header("X-CSRF-Token", csrfToken)
        client.newCall(builder.build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                mainThread { cb(false, e.message ?: "网络错误") }
            }
            override fun onResponse(call: okhttp3.Call, resp: okhttp3.Response) {
                val body = resp.body?.string() ?: ""
                val ok = resp.isSuccessful
                mainThread { cb(ok, body) }
            }
        })
    }

    private fun fetchJson(method: String, path: String, body: Map<String, Any?>?,
                          headers: Map<String, String> = emptyMap(), cb: (Boolean, String) -> Unit) {
        val builder = okhttp3.Request.Builder().url(BASE + path)
        when (method) {
            "POST" -> builder.post(okhttp3.RequestBody.create(
                "application/x-www-form-urlencoded".toMediaType(),
                body?.entries?.joinToString("&") {
                    java.net.URLEncoder.encode(it.key, "UTF-8") + "=" +
                    java.net.URLEncoder.encode(it.value?.toString() ?: "", "UTF-8")
                } ?: ""))
        }
        headers.forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                mainThread { cb(false, e.message ?: "网络错误") }
            }
            override fun onResponse(call: okhttp3.Call, resp: okhttp3.Response) {
                val body = resp.body?.string() ?: resp.message
                val ok = resp.isSuccessful
                mainThread { cb(ok, body) }
            }
        })
    }
}

data class Device(
    val id: String, val name: String, val status: String,
    val firmware: String, val ip: String, val rssi: Int,
    val otaStatus: String, val otaProgress: Int
) {
    val online: Boolean get() = status == "online"
}

data class Firmware(
    val id: Int, val name: String, val version: String,
    val size: Long, val target: String
)
