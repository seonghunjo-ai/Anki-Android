// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ichi2.anki.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * A compact monthly calendar showing the number of Anki reviews recorded on each local date.
 *
 * The caller supplies review counts keyed by yyyy-MM-dd and loads the requested month range.
 */
class OnigiriStudyCalendarView(
    context: Context,
) : LinearLayout(context) {
    private val month = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private var reviewCounts: Map<String, Int> = emptyMap()
    private var selectedDateKey: String? = dateKey(Calendar.getInstance())
    private var onMonthRangeRequested: ((Long, Long) -> Unit)? = null

    private val monthLabel = TextView(context)
    private val daysGrid = GridLayout(context).apply {
        columnCount = 7
        rowCount = 6
        useDefaultMargins = false
        alignmentMode = GridLayout.ALIGN_BOUNDS
    }
    private val monthSummary = TextView(context)
    private val selectedDaySummary = TextView(context)

    init {
        orientation = VERTICAL
        setPadding(18.dp(), 18.dp(), 18.dp(), 16.dp())
        setBackgroundResource(R.drawable.onigiri_calendar_sheet_background)
        buildHeader()
        buildWeekdayLabels()
        addView(
            daysGrid,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 6.dp()
            },
        )
        monthSummary.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(color(R.color.onigiri_calendar_subtle))
        }
        addView(
            monthSummary,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12.dp()
            },
        )
        selectedDaySummary.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(color(R.color.onigiri_calendar_text))
            setTypeface(typeface, Typeface.BOLD)
        }
        addView(
            selectedDaySummary,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 5.dp()
            },
        )
        renderCalendar()
    }

    fun setOnMonthRangeRequestedListener(listener: (Long, Long) -> Unit) {
        onMonthRangeRequested = listener
        requestCurrentMonth()
    }

    fun setReviewCounts(counts: Map<String, Int>) {
        reviewCounts = counts
        renderCalendar()
    }

    private fun buildHeader() {
        val title = TextView(context).apply {
            text = context.getString(R.string.onigiri_calendar_eyebrow)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(color(R.color.onigiri_calendar_accent))
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val navigation = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val previous = navigationButton(
            text = "‹",
            description = R.string.onigiri_calendar_previous_month,
        ) {
            month.add(Calendar.MONTH, -1)
            selectedDateKey = null
            renderCalendar()
            requestCurrentMonth()
        }
        navigation.addView(previous, LayoutParams(40.dp(), 40.dp()))

        monthLabel.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTextColor(color(R.color.onigiri_calendar_text))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        navigation.addView(
            monthLabel,
            LayoutParams(0, 44.dp(), 1f),
        )

        val next = navigationButton(
            text = "›",
            description = R.string.onigiri_calendar_next_month,
        ) {
            month.add(Calendar.MONTH, 1)
            selectedDateKey = null
            renderCalendar()
            requestCurrentMonth()
        }
        navigation.addView(next, LayoutParams(40.dp(), 40.dp()))
        addView(
            navigation,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 7.dp()
            },
        )
    }

    private fun buildWeekdayLabels() {
        val weekdayGrid = GridLayout(context).apply {
            columnCount = 7
            rowCount = 1
            useDefaultMargins = false
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        context.resources.getStringArray(R.array.onigiri_calendar_weekdays).forEachIndexed { index, label ->
            val weekday = TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(color(R.color.onigiri_calendar_subtle))
            }
            weekdayGrid.addView(
                weekday,
                gridCellParams(row = 0, column = index, height = 24.dp()),
            )
        }
        addView(
            weekdayGrid,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8.dp()
            },
        )
    }

    private fun navigationButton(
        text: String,
        description: Int,
        onClick: () -> Unit,
    ): TextView =
        TextView(context).apply {
            this.text = text
            contentDescription = context.getString(description)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTextColor(color(R.color.onigiri_calendar_text))
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
        }

    private fun renderCalendar() {
        monthLabel.text =
            SimpleDateFormat(
                if (Locale.getDefault().language == "ko") "yyyy년 M월" else "MMMM yyyy",
                Locale.getDefault(),
            ).format(month.time)

        val firstOfMonth = (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        // Monday-first calendar grid.
        val leadingDays = (firstOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        daysGrid.removeAllViews()

        var activeDays = 0
        var totalReviews = 0
        reviewCounts.values.forEach { count ->
            if (count > 0) activeDays += 1
            totalReviews += count
        }

        for (index in 0 until 42) {
            val dayOfMonth = index - leadingDays + 1
            val cell = TextView(context).apply {
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                minHeight = 34.dp()
                minWidth = 0
            }
            if (dayOfMonth in 1..daysInMonth) {
                val dayCalendar = (firstOfMonth.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val key = dateKey(dayCalendar)
                val count = reviewCounts[key] ?: 0
                cell.text = dayOfMonth.toString()
                cell.contentDescription =
                    context.getString(R.string.onigiri_calendar_day_accessibility, dayOfMonth, count)
                cell.setTextColor(color(R.color.onigiri_calendar_text))
                cell.background = dayBackground(count, key == selectedDateKey)
                cell.isFocusable = true
                cell.isClickable = true
                cell.setOnClickListener {
                    selectedDateKey = key
                    renderCalendar()
                }
            } else {
                cell.isEnabled = false
                cell.text = ""
            }
            val row = index / 7
            val column = index % 7
            daysGrid.addView(cell, gridCellParams(row, column, 38.dp()))
        }

        monthSummary.text =
            context.getString(
                R.string.onigiri_calendar_month_summary,
                activeDays,
                totalReviews,
            )
        selectedDaySummary.text = selectedDateSummary()
    }

    private fun selectedDateSummary(): String {
        val key = selectedDateKey ?: return context.getString(R.string.onigiri_calendar_select_day)
        val count = reviewCounts[key] ?: 0
        val parsedDate =
            runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key)
            }.getOrNull()
        val label = parsedDate?.let {
            SimpleDateFormat("M/d (EEE)", Locale.getDefault()).format(it)
        } ?: key
        return if (count > 0) {
            context.getString(R.string.onigiri_calendar_selected_summary, label, count)
        } else {
            context.getString(R.string.onigiri_calendar_no_reviews, label)
        }
    }

    private fun dayBackground(
        count: Int,
        selected: Boolean,
    ): GradientDrawable {
        val fill =
            when {
                selected -> color(R.color.onigiri_calendar_selected)
                count >= 30 -> color(R.color.onigiri_calendar_level_4)
                count >= 15 -> color(R.color.onigiri_calendar_level_3)
                count >= 5 -> color(R.color.onigiri_calendar_level_2)
                count > 0 -> color(R.color.onigiri_calendar_level_1)
                else -> Color.TRANSPARENT
            }
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            if (selected) {
                setStroke(2.dp(), color(R.color.onigiri_calendar_accent))
            }
        }
    }

    private fun requestCurrentMonth() {
        val start = (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        onMonthRangeRequested?.invoke(start.timeInMillis, end.timeInMillis)
    }

    private fun gridCellParams(
        row: Int,
        column: Int,
        height: Int,
    ): GridLayout.LayoutParams =
        GridLayout.LayoutParams(
            GridLayout.spec(row),
            GridLayout.spec(column, 1f),
        ).apply {
            width = 0
            this.height = height
            setMargins(2.dp(), 2.dp(), 2.dp(), 2.dp())
        }

    private fun dateKey(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

    private fun color(resourceId: Int): Int = context.getColor(resourceId)

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
