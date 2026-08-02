package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigaaNativeMarcarAlmocoScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("sigaa_prefs", Context.MODE_PRIVATE)
    val savedUsername = sharedPrefs.getString("username", "") ?: ""
    val savedPassword = sharedPrefs.getString("password", "") ?: ""

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var step by remember { mutableStateOf(0) } // 0: init, 1: clicking menu, 2: parsing, 3: ready, 4: confirming, 5: done
    var mealOptions by remember { mutableStateOf<List<MealOption>>(emptyList()) }
    var confirmMessage by remember { mutableStateOf("") }
    var loadError by remember { mutableStateOf("") }
    var loadingStatus by remember { mutableStateOf("Acessando o SIGAA...") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Marcar Almoço", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onMealsParsed(jsonStr: String) {
                                (context as? android.app.Activity)?.runOnUiThread {
                                    if (step == 4) {
                                        step = 5
                                        confirmMessage = "Refeições salvas com sucesso no SIGAA!"
                                        isLoading = false
                                        return@runOnUiThread
                                    }
                                    try {
                                        var cleanStr = jsonStr.trim()
                                        if (cleanStr.startsWith("\"") && cleanStr.endsWith("\"")) {
                                            cleanStr = cleanStr.substring(1, cleanStr.length - 1).replace("\\\"", "\"")
                                        }
                                        val jsonArray = if (cleanStr == "[]" || cleanStr.isEmpty()) JSONArray() else JSONArray(cleanStr)
                                        val options = mutableListOf<MealOption>()
                                        for (i in 0 until jsonArray.length()) {
                                            val obj = jsonArray.getJSONObject(i)
                                            options.add(
                                                MealOption(
                                                    id = obj.getString("id"),
                                                    name = obj.getString("name"),
                                                    date = obj.getString("date"),
                                                    checked = obj.getBoolean("checked"),
                                                    disabled = obj.getBoolean("disabled")
                                                )
                                            )
                                        }
                                        mealOptions = options
                                        step = 3
                                        isLoading = false
                                    } catch (e: Exception) {
                                        loadError = "Erro ao ler as refeições: " + e.message
                                        isLoading = false
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onStepChanged(newStep: Int, msg: String) {
                                (context as? android.app.Activity)?.runOnUiThread {
                                    step = newStep
                                    loadingStatus = msg
                                    isLoading = true
                                }
                            }

                            @JavascriptInterface
                            fun onNotLoggedIn() {
                                (context as? android.app.Activity)?.runOnUiThread {
                                    loadError = "Não foi possível logar. Verifique suas credenciais."
                                    isLoading = false
                                }
                            }

                            @JavascriptInterface
                            fun onActionNotFound() {
                                (context as? android.app.Activity)?.runOnUiThread {
                                    loadError = "Opção de marcar almoço não encontrada no menu do SIGAA."
                                    isLoading = false
                                }
                            }
                        }, "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url == null) return

                                if (url.contains("logar.do") || url.contains("verTelaLogin.do")) {
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        loadingStatus = "Realizando login automático..."
                                    }
                                    val js = """
                                        javascript:(function() {
                                            var userField = document.querySelector('input[name="user.login"]');
                                            var passField = document.querySelector('input[name="user.senha"]');
                                            if (userField && passField) {
                                                userField.value = '${savedUsername.replace("\\", "\\\\").replace("'", "\\'")}';
                                                passField.value = '${savedPassword.replace("\\", "\\\\").replace("'", "\\'")}';
                                                var form = document.querySelector('form');
                                                if (form) {
                                                    form.submit();
                                                    return;
                                                }
                                            }
                                            AndroidBridge.onNotLoggedIn();
                                        })()
                                    """.trimIndent()
                                    view?.evaluateJavascript(js, null)
                                } else {
                                    val js = """
                                        javascript:(function() {
                                            var step = $step;
                                            var hasMenu = (document.forms['menu:form_menu_discente'] || document.getElementById('menu:form_menu_discente')) !== null;
                                            var hasConfirmButton = document.querySelector('input[type="submit"][value="Confirmar"]') !== null;
                                            var isMealsPage = document.querySelector('table.formulario') !== null && hasConfirmButton;
                                            var isMealsEmpty = document.body.innerHTML.indexOf('Nenhum') !== -1 || document.body.innerHTML.indexOf('Nenhuma') !== -1;
                                            var isTitleCorrect = document.body.innerHTML.indexOf('Informar Dias de Comparecimento') !== -1;
                                            
                                            if (hasMenu && step === 0) {
                                                var form = document.getElementById('menu:form_menu_discente') || document.forms['menu:form_menu_discente'];
                                                if (form) {
                                                    var action = null;
                                                    var scripts = document.getElementsByTagName('script');
                                                    for (var i = 0; i < scripts.length; i++) {
                                                        var match = scripts[i].innerHTML.match(/'([^']+#\{\s*comparecimentoRefeitorioMBean\.iniciar\s*\})/);
                                                        if (match) { action = match[1]; break; }
                                                    }
                                                    if (action) {
                                                        AndroidBridge.onStepChanged(1, "Acessando menu de refeitório...");
                                                        form.elements['jscook_action'].value = action;
                                                        form.submit();
                                                    } else {
                                                        AndroidBridge.onActionNotFound();
                                                    }
                                                }
                                            } else if (isMealsPage) {
                                                var result = [];
                                                var rows = document.querySelectorAll('table.formulario tbody tr');
                                                for(var i=0; i<rows.length; i++) {
                                                    var cols = rows[i].querySelectorAll('td');
                                                    if(cols.length >= 2) {
                                                        var date = cols[0].innerText;
                                                        if (!date) date = cols[0].textContent;
                                                        if (date) date = date.trim();
                                                        var checkbox = cols[1].querySelector('input[type="checkbox"]');
                                                        if(checkbox) {
                                                            result.push({
                                                                id: checkbox.id,
                                                                name: checkbox.name,
                                                                date: date,
                                                                checked: checkbox.checked,
                                                                disabled: checkbox.disabled
                                                            });
                                                        }
                                                    }
                                                }
                                                AndroidBridge.onMealsParsed(JSON.stringify(result));
                                            } else if (isTitleCorrect && isMealsEmpty && step === 1) {
                                                AndroidBridge.onMealsParsed('[]');
                                            } else if (step === 4) {
                                                AndroidBridge.onMealsParsed('[]');
                                            }
                                        })()
                                    """.trimIndent()
                                    view?.evaluateJavascript(js, null)
                                }
                            }
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        if (savedUsername.isBlank() || savedPassword.isBlank()) {
                            loadError = "Credenciais não encontradas. Salve-as na tela de Login primeiro."
                            isLoading = false
                        } else {
                            // Start flow
                            loadUrl("https://sigaa.ifal.edu.br/sigaa/verTelaLogin.do")
                        }
                        webView = this
                    }
                },
                modifier = Modifier.size(1.dp) // Hide webview
            )

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(loadingStatus)
                }
            } else if (loadError.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(loadError, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) { Text("Voltar") }
                }
            } else if (step == 5) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(confirmMessage, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) { Text("Voltar") }
                }
            } else if (step == 3) {
                if (mealOptions.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Nenhum dia disponível para marcação.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) { Text("Voltar") }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(mealOptions) { option ->
                                val (dayOfWeek, formattedDate) = getDayOfWeekAndDate(option.date)
                                val isChecked = option.checked
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .then(
                                            if (!option.disabled) {
                                                Modifier.clickable {
                                                    val index = mealOptions.indexOf(option)
                                                    if (index != -1) {
                                                        val newList = mealOptions.toMutableList()
                                                        newList[index] = option.copy(checked = !isChecked)
                                                        mealOptions = newList
                                                    }
                                                }
                                            } else Modifier
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                         else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (dayOfWeek.isNotEmpty()) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                ) {
                                                    Text(
                                                        text = dayOfWeek.uppercase(),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (formattedDate.isNotEmpty()) "Data: $formattedDate" else "Data: ${option.date}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (option.disabled) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Indisponível para alteração",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }

                                        Checkbox(
                                            checked = option.checked,
                                            onCheckedChange = { checked ->
                                                val index = mealOptions.indexOf(option)
                                                if (index != -1) {
                                                    val newList = mealOptions.toMutableList()
                                                    newList[index] = option.copy(checked = checked)
                                                    mealOptions = newList
                                                }
                                            },
                                            enabled = !option.disabled
                                        )
                                    }
                                }
                            }
                        }
                        
                        Button(
                            onClick = {
                                step = 4
                                loadingStatus = "Confirmando no SIGAA..."
                                isLoading = true
                                val updatesArray = JSONArray()
                                mealOptions.forEach {
                                    val obj = JSONObject()
                                    obj.put("id", it.id)
                                    obj.put("checked", it.checked)
                                    updatesArray.put(obj)
                                }
                                val jsonStr = updatesArray.toString()
                                val js = """
                                    javascript:(function() {
                                        var updates = $jsonStr;
                                        for (var i=0; i<updates.length; i++) {
                                            var cb = document.getElementById(updates[i].id);
                                            if (cb && !cb.disabled) {
                                                cb.checked = updates[i].checked;
                                            }
                                        }
                                        var btn = document.querySelector('input[type="submit"][value="Confirmar"]');
                                        if (btn) {
                                            btn.click();
                                        } else {
                                            AndroidBridge.onMealsParsed('[]');
                                        }
                                    })()
                                """.trimIndent()
                                webView?.evaluateJavascript(js, null)
                            },
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)
                        ) {
                            Text("Confirmar no SIGAA", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                        }
                    }
                }
            }
        }
    }
}

private fun getDayOfWeekAndDate(rawDate: String): Pair<String, String> {
    val clean = rawDate.trim()
    if (clean.isBlank()) return Pair("", "")

    val lower = clean.lowercase()
    var extractedDay: String? = when {
        lower.contains("segunda") -> "Segunda-feira"
        lower.contains("terça") || lower.contains("terca") -> "Terça-feira"
        lower.contains("quarta") -> "Quarta-feira"
        lower.contains("quinta") -> "Quinta-feira"
        lower.contains("sexta") -> "Sexta-feira"
        lower.contains("sábado") || lower.contains("sabado") -> "Sábado"
        lower.contains("domingo") -> "Domingo"
        else -> null
    }

    val dateRegex = Regex("""(\d{2}/\d{2}/\d{4})|(\d{4}-\d{2}-\d{2})|(\d{2}-\d{2}-\d{4})""")
    val match = dateRegex.find(clean)
    val dateStr = match?.value ?: clean

    if (extractedDay == null && match != null) {
        val str = match.value
        try {
            val localDate = if (str.contains("/")) {
                val parts = str.split("/")
                if (parts.size == 3) {
                    java.time.LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                } else null
            } else if (str.contains("-")) {
                val parts = str.split("-")
                if (parts[0].length == 4) {
                    java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                } else {
                    java.time.LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                }
            } else null

            localDate?.let {
                extractedDay = when (it.dayOfWeek) {
                    java.time.DayOfWeek.MONDAY -> "Segunda-feira"
                    java.time.DayOfWeek.TUESDAY -> "Terça-feira"
                    java.time.DayOfWeek.WEDNESDAY -> "Quarta-feira"
                    java.time.DayOfWeek.THURSDAY -> "Quinta-feira"
                    java.time.DayOfWeek.FRIDAY -> "Sexta-feira"
                    java.time.DayOfWeek.SATURDAY -> "Sábado"
                    java.time.DayOfWeek.SUNDAY -> "Domingo"
                }
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    val day = extractedDay ?: ""
    return Pair(day, dateStr)
}
