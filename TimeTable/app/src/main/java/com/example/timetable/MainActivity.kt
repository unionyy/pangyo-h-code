package com.example.timetable

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeTableApp()
        }
    }
}

@Composable
fun TimeTableApp() {
    val grades = (1..3).map { it.toString() }
    val classes = (1..8).map { it.toString() }
    val dates = (0..6).map { LocalDate.now().plusDays(it.toLong()) }

    var selectedGrade by remember { mutableStateOf(grades.first()) }
    var selectedClass by remember { mutableStateOf(classes.first()) }
    var selectedDate by remember { mutableStateOf(dates.first()) }
    var timeTable by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val client = remember { OkHttpClient() }

    Column(modifier = Modifier.padding(16.dp)) {
        DropdownSelector("학년", grades, selectedGrade) { selectedGrade = it }
        DropdownSelector("반", classes, selectedClass) { selectedClass = it }
        DropdownSelector("날짜", dates.map { it.format(DateTimeFormatter.ISO_DATE) }, selectedDate.format(DateTimeFormatter.ISO_DATE)) {
            selectedDate = LocalDate.parse(it)
        }

        Button(onClick = {
            scope.launch(Dispatchers.IO) {
                try {
                    val url = "https://open.neis.go.kr/hub/hisTimetable" +
                            "?KEY=YOUR_API_KEY" +
                            "&Type=json" +
                            "&ATPT_OFCDC_SC_CODE=J10" +
                            "&SD_SCHUL_CODE=7531255" +
                            "&GRADE=$selectedGrade" +
                            "&CLASS_NM=$selectedClass" +
                            "&ALL_TI_YMD=${selectedDate.format(DateTimeFormatter.BASIC_ISO_DATE)}"

                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val body = response.body()?.string()
                    Log.d("NEIS", "Response: $body")

                    val json = JSONObject(body ?: "{}")
                    val rows = json.getJSONArray("hisTimetable").getJSONObject(1).getJSONArray("row")

                    val list = mutableListOf<String>()
                    for (i in 0 until rows.length()) {
                        val item = rows.getJSONObject(i)
                        list.add(item.getString("ITRT_CNTNT"))
                    }
                    timeTable = list
                } catch (e: Exception) {
                    Log.e("NEIS", "Error: ${e.message}", e)
                    timeTable = listOf("시간표를 불러오지 못했습니다.")
                }
            }
        }, modifier = Modifier.padding(vertical = 16.dp)) {
            Text("확인")
        }

        Divider()

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(timeTable) { item ->
                Text(item, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, items: List<String>, selectedItem: String, onItemSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}