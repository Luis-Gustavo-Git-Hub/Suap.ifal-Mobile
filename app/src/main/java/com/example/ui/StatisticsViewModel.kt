package com.example.ui

import com.example.data.Subject
import com.example.domain.StatisticsCalculator
import com.example.domain.StatisticsSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Projects offline academic sources into a screen-ready statistics state. */
class StatisticsViewModel(
    subjects: Flow<List<Subject>>,
    globalFrequency: Flow<String>,
    globalAbsences: Flow<String>,
    totalClasses: Flow<String>,
    passedClasses: Flow<String>,
    scope: CoroutineScope
) {
    val snapshot: StateFlow<StatisticsSnapshot> = combine(
        subjects, globalFrequency, globalAbsences, totalClasses, passedClasses
    ) { currentSubjects, frequency, absences, total, passed ->
        StatisticsCalculator.calculate(currentSubjects, frequency, absences, total, passed)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), StatisticsSnapshot.Empty)
}
