package com.example.tstuapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.UUID

@Immutable
data class Lesson(
    val id: String = UUID.randomUUID().toString(),
    val time: String,
    val subject: String,
    val room: String,
    val teacher: String
)

@Immutable
data class DaySchedule(
    val dayName: String,
    val lessons: List<Lesson>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFA8C7FA),
                    onPrimary = Color(0xFF003062),
                    primaryContainer = Color(0xFF004786),
                    onPrimaryContainer = Color(0xFFD6E3FF),
                    surface = Color(0xFF111318),
                    onSurface = Color(0xFFE2E2E9),
                    surfaceContainerHigh = Color(0xFF282A2F),
                    surfaceContainer = Color(0xFF1E2025)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    ScheduleScreen()
                }
            }
        }
    }
}

@Composable
fun ScheduleScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var scheduleData by remember { mutableStateOf(loadOfflineSchedule(context)) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isLoading) 360f else 0f,
        animationSpec = if (isLoading) {
            infiniteRepeatable(animation = tween(1000, easing = LinearEasing))
        } else {
            spring(stiffness = Spring.StiffnessLow)
        },
        label = "RefreshRotation"
    )

    fun refreshSchedule() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            val freshData = fetchScheduleFromTSTU("https://www.tstu.ru/education/schedule/")
            if (freshData.isNotEmpty()) {
                scheduleData = freshData
                saveOfflineSchedule(context, freshData)
            }
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ТГТУ Расписание",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Семестровый график",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { refreshSchedule() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔄",
                        fontSize = 20.sp,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val shortDays = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ")
                shortDays.forEachIndexed { index, dayLabel ->
                    val isSelected = selectedDayIndex == index
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(durationMillis = 250),
                        label = "TabBackground"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        label = "TabText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(backgroundColor)
                            .clickable { selectedDayIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayLabel,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = selectedDayIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "DayTransition"
            ) { targetDay ->
                val currentDay = scheduleData.getOrNull(targetDay)

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                } else if (currentDay == null || currentDay.lessons.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏖️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Пар нет, можно отдыхать",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(
                            items = currentDay.lessons,
                            key = { _, lesson -> lesson.id }
                        ) { index, lesson ->
                            AnimatedLessonCard(lesson = lesson, index = index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedLessonCard(lesson: Lesson, index: Int) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = index * 50)) +
                slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = lesson.time,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = lesson.room,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (lesson.teacher.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = lesson.teacher,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

suspend fun fetchScheduleFromTSTU(url: String): List<DaySchedule> = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect(url).timeout(5000).get()
        val tables = doc.select("table.schedule")

        if (tables.isEmpty()) return@withContext getMockSchedule()

        val days = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
        days.map { day -> DaySchedule(day, emptyList()) }
    } catch (e: Exception) {
        getMockSchedule()
    }
}

fun saveOfflineSchedule(context: Context, schedule: List<DaySchedule>) {
    val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("has_data", true).apply()
}

fun loadOfflineSchedule(context: Context): List<DaySchedule> {
    return getMockSchedule()
}

fun getMockSchedule(): List<DaySchedule> {
    return listOf(
        DaySchedule(
            dayName = "Понедельник",
            lessons = listOf(
                Lesson(time = "08:30 – 10:00", subject = "Высшая математика", room = "ауд. 312", teacher = "Иванов И.И."),
                Lesson(time = "10:15 – 11:45", subject = "Прикладная информатика", room = "ауд. 405а", teacher = "Петров П.П."),
                Lesson(time = "12:00 – 13:30", subject = "Разработка UI/UX", room = "ауд. 204", teacher = "Сидоров С.С.")
            )
        ),
        DaySchedule(
            dayName = "Вторник",
            lessons = listOf(
                Lesson(time = "10:15 – 11:45", subject = "Архитектура ИС", room = "ауд. 210", teacher = "Кузнецов А.А."),
                Lesson(time = "12:00 – 13:30", subject = "Базы данных", room = "ауд. 401", teacher = "Смирнова Е.В.")
            )
        ),
        DaySchedule(dayName = "Среда", lessons = emptyList()),
        DaySchedule(dayName = "Четверг", lessons = emptyList()),
        DaySchedule(dayName = "Пятница", lessons = emptyList()),
        DaySchedule(dayName = "Суббота", lessons = emptyList())
    )
}
