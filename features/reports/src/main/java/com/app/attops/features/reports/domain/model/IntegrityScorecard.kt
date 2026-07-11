package com.app.attops.features.reports.domain.model

data class IntegrityScorecard(
    val employeeId: String,
    val fullName: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val averageIntegrityDistance: Double, // in meters
    val flagCount: Int, // Number of times deviation exceeded 200m
    val attendanceRate: Float // percentage
)
