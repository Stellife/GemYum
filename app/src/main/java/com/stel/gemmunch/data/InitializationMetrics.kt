package com.stel.gemmunch.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * Tracks detailed timing metrics for app initialization phases
 */
data class InitializationMetrics(
    val startTime: Instant = Instant.now(),
    val phases: MutableMap<String, PhaseMetrics> = mutableMapOf(),
    private val _liveUpdates: MutableStateFlow<List<MetricUpdate>> = MutableStateFlow(emptyList())
) {
    val liveUpdates: StateFlow<List<MetricUpdate>> = _liveUpdates.asStateFlow()
    
    data class MetricUpdate(
        val timestamp: Instant,
        val type: UpdateType,
        val phaseName: String,
        val subPhaseName: String? = null,
        val durationMs: Long? = null,
        val details: String? = null
    )
    
    enum class UpdateType {
        PHASE_START,
        PHASE_END,
        SUBPHASE_START,
        SUBPHASE_END
    }
    data class PhaseMetrics(
        val name: String,
        val startTime: Instant,
        var endTime: Instant? = null,
        val subPhases: MutableMap<String, SubPhaseMetrics> = mutableMapOf()
    ) {
        val durationMs: Long
            get() = endTime?.let { 
                it.toEpochMilli() - startTime.toEpochMilli() 
            } ?: (Instant.now().toEpochMilli() - startTime.toEpochMilli())
    }
    
    data class SubPhaseMetrics(
        val name: String,
        val startTime: Instant,
        var endTime: Instant? = null,
        var details: String? = null
    ) {
        val durationMs: Long
            get() = endTime?.let { 
                it.toEpochMilli() - startTime.toEpochMilli() 
            } ?: (Instant.now().toEpochMilli() - startTime.toEpochMilli())
    }
    
    fun startPhase(phaseName: String) {
        phases[phaseName] = PhaseMetrics(phaseName, Instant.now())
        addUpdate(MetricUpdate(
            timestamp = Instant.now(),
            type = UpdateType.PHASE_START,
            phaseName = phaseName
        ))
    }
    
    fun endPhase(phaseName: String) {
        val phase = phases[phaseName]
        phase?.endTime = Instant.now()
        phase?.let {
            addUpdate(MetricUpdate(
                timestamp = Instant.now(),
                type = UpdateType.PHASE_END,
                phaseName = phaseName,
                durationMs = it.durationMs
            ))
        }
    }
    
    fun startSubPhase(phaseName: String, subPhaseName: String) {
        phases[phaseName]?.subPhases?.put(
            subPhaseName, 
            SubPhaseMetrics(subPhaseName, Instant.now())
        )
        addUpdate(MetricUpdate(
            timestamp = Instant.now(),
            type = UpdateType.SUBPHASE_START,
            phaseName = phaseName,
            subPhaseName = subPhaseName
        ))
    }
    
    fun endSubPhase(phaseName: String, subPhaseName: String, details: String? = null) {
        val subPhase = phases[phaseName]?.subPhases?.get(subPhaseName)
        subPhase?.apply {
            endTime = Instant.now()
            this.details = details
        }
        subPhase?.let {
            addUpdate(MetricUpdate(
                timestamp = Instant.now(),
                type = UpdateType.SUBPHASE_END,
                phaseName = phaseName,
                subPhaseName = subPhaseName,
                durationMs = it.durationMs,
                details = details
            ))
        }
    }
    
    private fun addUpdate(update: MetricUpdate) {
        _liveUpdates.value = _liveUpdates.value + update
    }
    
    fun clearMetrics() {
        phases.clear()
        _liveUpdates.value = emptyList()
    }
    
    fun getTotalDurationMs(): Long {
        // Calculate the actual initialization duration based on completed phases
        // Use the latest end time of all phases, not "now"
        val latestEndTime = phases.values
            .mapNotNull { it.endTime }
            .maxByOrNull { it.toEpochMilli() }
        
        return if (latestEndTime != null) {
            // If we have completed phases, use the latest end time
            latestEndTime.toEpochMilli() - startTime.toEpochMilli()
        } else {
            // If no phases are complete yet, show time since start
            Instant.now().toEpochMilli() - startTime.toEpochMilli()
        }
    }
    
    companion object {
        fun formatDuration(durationMs: Long): String {
            return when {
                durationMs >= 10000 -> String.format("%.1fs", durationMs / 1000.0)
                durationMs >= 1000 -> String.format("%.2fs", durationMs / 1000.0)
                else -> "${durationMs}ms"
            }
        }
    }
    
    fun generateReport(): String {
        val sb = StringBuilder()
        sb.appendLine("=== INITIALIZATION METRICS ===")
        
        // Show actual initialization time from start to completion
        val totalDuration = getTotalDurationMs()
        val isComplete = phases.values.any { it.endTime != null }
        
        if (isComplete) {
            sb.appendLine("Total Initialization Time: ${formatDuration(totalDuration)}")
        } else {
            sb.appendLine("Elapsed Time (in progress): ${formatDuration(totalDuration)}")
        }
        sb.appendLine("")
        
        phases.values.sortedBy { it.startTime }.forEach { phase ->
            sb.appendLine("📊 ${phase.name}: ${formatDuration(phase.durationMs)}")
            
            // Calculate overhead (time not accounted for by subphases)
            val subPhasesTotal = phase.subPhases.values.sumOf { it.durationMs }
            val overhead = phase.durationMs - subPhasesTotal
            
            phase.subPhases.values.sortedBy { it.startTime }.forEach { subPhase ->
                sb.append("   └─ ${subPhase.name}: ${formatDuration(subPhase.durationMs)}")
                subPhase.details?.let { sb.append(" ($it)") }
                sb.appendLine()
            }
            
            if (overhead > 10) { // Only show overhead if it's significant (>10ms)
                sb.appendLine("   └─ [Overhead/Other]: ${formatDuration(overhead)}")
            }
            
            sb.appendLine()
        }
        
        return sb.toString()
    }
}