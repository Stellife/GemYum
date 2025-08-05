package com.stel.gemmunch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar

enum class MealTimingOption {
    USE_PHOTO_TIME,
    CUSTOM_TIME
}

data class MealTiming(
    val option: MealTimingOption = MealTimingOption.USE_PHOTO_TIME,
    val customDateTime: LocalDateTime? = null,
    val photoTimestamp: Instant? = null,
    val photoTimestampError: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTimingCard(
    isFromGallery: Boolean,
    photoTimestamp: Instant?,
    mealTiming: MealTiming,
    onTimingUpdate: (MealTiming) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableStateOf(mealTiming.option) }
    var customDateTime by remember { mutableStateOf(mealTiming.customDateTime ?: LocalDateTime.now()) }
    val context = LocalContext.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Timing of Meal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column(Modifier.selectableGroup()) {
                // Use photo time option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedOption == MealTimingOption.USE_PHOTO_TIME,
                            onClick = { 
                                selectedOption = MealTimingOption.USE_PHOTO_TIME
                                onTimingUpdate(mealTiming.copy(
                                    option = selectedOption,
                                    photoTimestamp = photoTimestamp,
                                    photoTimestampError = if (isFromGallery && photoTimestamp == null) 
                                        "ERROR loading image time" else null
                                ))
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == MealTimingOption.USE_PHOTO_TIME,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Use time of photograph")
                        if (selectedOption == MealTimingOption.USE_PHOTO_TIME) {
                            Text(
                                text = when {
                                    !isFromGallery -> "Using current time"
                                    photoTimestamp != null -> {
                                        val dateTime = LocalDateTime.ofInstant(photoTimestamp, ZoneId.systemDefault())
                                        "Photo taken: ${dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))}"
                                    }
                                    else -> "ERROR loading image time"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFromGallery && photoTimestamp == null) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Custom time option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedOption == MealTimingOption.CUSTOM_TIME,
                            onClick = { 
                                selectedOption = MealTimingOption.CUSTOM_TIME
                                onTimingUpdate(mealTiming.copy(
                                    option = selectedOption,
                                    customDateTime = customDateTime
                                ))
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == MealTimingOption.CUSTOM_TIME,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter custom time/date")
                }
            }
            
            // Custom date/time pickers
            if (selectedOption == MealTimingOption.CUSTOM_TIME) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date picker
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.set(customDateTime.year, customDateTime.monthValue - 1, customDateTime.dayOfMonth)
                            
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    customDateTime = customDateTime.withYear(year)
                                        .withMonth(month + 1)
                                        .withDayOfMonth(day)
                                    onTimingUpdate(mealTiming.copy(
                                        option = selectedOption,
                                        customDateTime = customDateTime
                                    ))
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(customDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                    }
                    
                    // Time picker
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    customDateTime = customDateTime.withHour(hour).withMinute(minute)
                                    onTimingUpdate(mealTiming.copy(
                                        option = selectedOption,
                                        customDateTime = customDateTime
                                    ))
                                },
                                customDateTime.hour,
                                customDateTime.minute,
                                false
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(customDateTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                    }
                }
                
                Text(
                    "Timezone: ${ZoneId.systemDefault()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}