package com.ridesync.data.model

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class AlertBanner(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val message: String,
    val severity: AlertSeverity = AlertSeverity.WARNING,
    val timestamp: Long = System.currentTimeMillis()
)
