package com.soumit.technovision.ui.screens.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soumit.technovision.data.model.StudentModel
import com.soumit.technovision.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.soumit.technovision.util.BillingUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class StudentBirthdayItem(
    val student: StudentModel,
    val birthDay: Int,
    val birthMonth: Int,
    val birthYear: Int?,
    val turningAge: Int?,
    val nextBirthdayDate: LocalDate,
    val daysUntilNextBirthday: Long,
    val isToday: Boolean,
    val isThisMonth: Boolean
)

object BirthdayUtils {
    data class ParsedDate(val day: Int, val month: Int, val year: Int?)

    fun parseDob(dobStr: String): ParsedDate? {
        if (dobStr.isBlank()) return null
        val clean = BillingUtils.normalizeDigits(dobStr.trim())

        // 1. dd/MM/yyyy or dd-MM-yyyy or dd.MM.yyyy
        val dmyRegex = Regex("""^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})$""")
        val m1 = dmyRegex.matchEntire(clean)
        if (m1 != null) {
            val (dStr, mStr, yStr) = m1.destructured
            val day = dStr.toIntOrNull() ?: return null
            val month = mStr.toIntOrNull() ?: return null
            var year = yStr.toIntOrNull() ?: return null
            if (year < 100) year += 2000
            if (month in 1..12 && day in 1..31) {
                return ParsedDate(day, month, year)
            }
        }

        // 2. yyyy/MM/dd or yyyy-MM-dd
        val ymdRegex = Regex("""^(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})$""")
        val m2 = ymdRegex.matchEntire(clean)
        if (m2 != null) {
            val (yStr, mStr, dStr) = m2.destructured
            val year = yStr.toIntOrNull() ?: return null
            val month = mStr.toIntOrNull() ?: return null
            val day = dStr.toIntOrNull() ?: return null
            if (month in 1..12 && day in 1..31) {
                return ParsedDate(day, month, year)
            }
        }

        return null
    }

    fun calculateBirthdayItem(student: StudentModel, today: LocalDate = LocalDate.now()): StudentBirthdayItem? {
        val parsed = parseDob(student.dob) ?: return null
        val day = parsed.day
        val month = parsed.month
        val year = parsed.year

        val daysInMonthThisYear = YearMonth.of(today.year, month).lengthOfMonth()
        val safeDayThisYear = day.coerceAtMost(daysInMonthThisYear)
        val birthdayThisYear = LocalDate.of(today.year, month, safeDayThisYear)

        val nextBirthdayDate = if (birthdayThisYear.isBefore(today)) {
            val daysInMonthNextYear = YearMonth.of(today.year + 1, month).lengthOfMonth()
            val safeDayNextYear = day.coerceAtMost(daysInMonthNextYear)
            LocalDate.of(today.year + 1, month, safeDayNextYear)
        } else {
            birthdayThisYear
        }

        val daysUntil = ChronoUnit.DAYS.between(today, nextBirthdayDate)
        val isToday = today.monthValue == month && today.dayOfMonth == safeDayThisYear
        val turningAge = if (year != null) nextBirthdayDate.year - year else null

        return StudentBirthdayItem(
            student = student,
            birthDay = day,
            birthMonth = month,
            birthYear = year,
            turningAge = turningAge,
            nextBirthdayDate = nextBirthdayDate,
            daysUntilNextBirthday = daysUntil,
            isToday = isToday,
            isThisMonth = today.monthValue == month
        )
    }
}

data class BirthdayCalendarUiState(
    val allItems: List<StudentBirthdayItem> = emptyList(),
    val currentYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = LocalDate.now(),
    val searchQuery: String = "",
    val selectedClassFilter: String = "All",
    val availableClasses: List<String> = emptyList(),
    val selectedTab: Int = 0, // 0: Month Calendar (Option A), 1: Upcoming Timeline (Option B)
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val todaysBirthdays: List<StudentBirthdayItem>
        get() = allItems.filter { it.isToday }

    val nextUpcomingCelebration: StudentBirthdayItem?
        get() = allItems.filter { !it.isToday }.minByOrNull { it.daysUntilNextBirthday }

    val filteredItems: List<StudentBirthdayItem>
        get() = allItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.student.fullName.contains(searchQuery, ignoreCase = true) ||
                    item.student.studentClass.contains(searchQuery, ignoreCase = true) ||
                    item.student.school.contains(searchQuery, ignoreCase = true) ||
                    item.student.phone.contains(searchQuery, ignoreCase = true)
            val matchesClass = selectedClassFilter == "All" ||
                    item.student.studentClass.equals(selectedClassFilter, ignoreCase = true)
            matchesQuery && matchesClass
        }

    val birthdaysInCurrentMonth: List<StudentBirthdayItem>
        get() = filteredItems.filter { it.birthMonth == currentYearMonth.monthValue }
            .sortedBy { it.birthDay }

    val birthdaysByDayInCurrentMonth: Map<Int, List<StudentBirthdayItem>>
        get() = birthdaysInCurrentMonth.groupBy { it.birthDay }

    val birthdaysOnSelectedDate: List<StudentBirthdayItem>
        get() {
            val date = selectedDate ?: return emptyList()
            if (date.monthValue != currentYearMonth.monthValue || date.year != currentYearMonth.year) {
                return emptyList()
            }
            return birthdaysByDayInCurrentMonth[date.dayOfMonth] ?: emptyList()
        }

    val timelineUpcomingItems: List<StudentBirthdayItem>
        get() = filteredItems.sortedBy { it.daysUntilNextBirthday }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class BirthdayCalendarViewModel @javax.inject.Inject constructor(
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BirthdayCalendarUiState())
    val uiState: StateFlow<BirthdayCalendarUiState> = _uiState.asStateFlow()

    init {
        observeStudents()
    }

    private fun observeStudents() {
        viewModelScope.launch {
            studentRepository.getStudentsStream()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { students ->
                    val today = LocalDate.now()
                    val items = students.mapNotNull { BirthdayUtils.calculateBirthdayItem(it, today) }
                    val classes = listOf("All") + students.map { it.studentClass.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()

                    _uiState.update {
                        it.copy(
                            allItems = items,
                            availableClasses = classes,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onPreviousMonth() {
        _uiState.update {
            val prev = it.currentYearMonth.minusMonths(1)
            it.copy(currentYearMonth = prev, selectedDate = null)
        }
    }

    fun onNextMonth() {
        _uiState.update {
            val next = it.currentYearMonth.plusMonths(1)
            it.copy(currentYearMonth = next, selectedDate = null)
        }
    }

    fun onJumpToToday() {
        val today = LocalDate.now()
        _uiState.update {
            it.copy(
                currentYearMonth = YearMonth.of(today.year, today.month),
                selectedDate = today
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update {
            if (it.selectedDate == date) {
                it.copy(selectedDate = null)
            } else {
                it.copy(selectedDate = date)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onClassFilterSelected(className: String) {
        _uiState.update { it.copy(selectedClassFilter = className) }
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}
