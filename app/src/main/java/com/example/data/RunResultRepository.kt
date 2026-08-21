package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.RunResult
import com.example.model.RunSample
import org.json.JSONArray
import org.json.JSONObject

class RunResultRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("dyno_lite_runs_store", Context.MODE_PRIVATE)

  private val KEY_RUNS_JSON = "key_runs_json"
  private val MAX_STORED_RUNS = 100
  private val MAX_RUNS_WITH_DETAILED_SAMPLES = 30
  private val MAX_SAMPLES_PER_RUN = 500

  fun getResults(): List<RunResult> {
    val jsonStr = prefs.getString(KEY_RUNS_JSON, null) ?: return emptyList()
    val list = mutableListOf<RunResult>()
    try {
      val jsonArray = JSONArray(jsonStr)
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(deserializeRunResult(obj))
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list.sortedByDescending { it.timestamp }
  }

  fun getResultById(id: String): RunResult? {
    return getResults().firstOrNull { it.id == id }
  }

  fun getOrderedRunSamples(resultId: String): List<RunSample> {
    val run = getResultById(resultId) ?: return emptyList()
    val rawSamples = run.samples
    if (rawSamples.isEmpty()) return emptyList()

    val cleanedSamples = mutableListOf<RunSample>()
    var lastTimeMs = -1L

    // Sort by elapsedTimeMs ascending
    val sorted = rawSamples.sortedBy { it.elapsedTimeMs }

    for (sample in sorted) {
      // Validate NaN, Infinity and strictly increasing time
      val timeMs = sample.elapsedTimeMs
      if (timeMs <= lastTimeMs) continue // Skip duplicate or non-increasing timestamps

      val filtAccZ = if (sample.filteredAccelerationZ.isNaN() || sample.filteredAccelerationZ.isInfinite()) 0f else sample.filteredAccelerationZ
      val corrAccZ = if (sample.correctedAccelerationZ.isNaN() || sample.correctedAccelerationZ.isInfinite()) 0f else sample.correctedAccelerationZ
      val gpsSpd = if (sample.gpsSpeedKmh.isNaN() || sample.gpsSpeedKmh.isInfinite()) 0f else sample.gpsSpeedKmh.coerceAtLeast(0f)
      val calcSpd = if (sample.calculatedSpeedKmh.isNaN() || sample.calculatedSpeedKmh.isInfinite()) 0f else sample.calculatedSpeedKmh.coerceAtLeast(0f)
      val spdDiff = if (sample.speedDifferenceKmh.isNaN() || sample.speedDifferenceKmh.isInfinite()) 0f else sample.speedDifferenceKmh.coerceAtLeast(0f)
      val gpsAcc = if (sample.gpsAccuracyMeters.isNaN() || sample.gpsAccuracyMeters.isInfinite()) 0f else sample.gpsAccuracyMeters.coerceAtLeast(0f)
      val gyroMag = if (sample.gyroMagnitude.isNaN() || sample.gyroMagnitude.isInfinite()) 0f else sample.gyroMagnitude.coerceAtLeast(0f)

      cleanedSamples.add(
        sample.copy(
          elapsedTimeMs = timeMs,
          filteredAccelerationZ = filtAccZ,
          correctedAccelerationZ = corrAccZ,
          gpsSpeedKmh = gpsSpd,
          calculatedSpeedKmh = calcSpd,
          speedDifferenceKmh = spdDiff,
          gpsAccuracyMeters = gpsAcc,
          gyroMagnitude = gyroMag
        )
      )
      lastTimeMs = timeMs
    }

    return cleanedSamples
  }

  fun saveResult(run: RunResult) {
    val currentRuns = getResults().toMutableList()
    val existingIndex = currentRuns.indexOfFirst { it.id == run.id }

    // Enforce max 500 samples per run
    val trimmedRun = if (run.samples.size > MAX_SAMPLES_PER_RUN) {
      run.copy(samples = run.samples.subList(0, MAX_SAMPLES_PER_RUN))
    } else {
      run
    }

    if (existingIndex >= 0) {
      currentRuns[existingIndex] = trimmedRun
    } else {
      currentRuns.add(0, trimmedRun)
    }

    val trimmedRuns = if (currentRuns.size > MAX_STORED_RUNS) {
      currentRuns.subList(0, MAX_STORED_RUNS)
    } else {
      currentRuns
    }

    saveAll(trimmedRuns)
  }

  fun deleteResult(id: String) {
    val currentRuns = getResults().toMutableList()
    val removed = currentRuns.removeAll { it.id == id }
    if (removed) {
      saveAll(currentRuns)
    }
  }

  fun clearAllResults() {
    prefs.edit().remove(KEY_RUNS_JSON).apply()
  }

  private fun saveAll(runs: List<RunResult>) {
    val jsonArray = JSONArray()
    for (i in runs.indices) {
      val r = runs[i]
      // Preserve detailed samples only for the 30 most recent runs
      val includeSamples = i < MAX_RUNS_WITH_DETAILED_SAMPLES
      jsonArray.put(serializeRunResult(r, includeSamples))
    }
    prefs.edit().putString(KEY_RUNS_JSON, jsonArray.toString()).apply()
  }

  private fun serializeRunResult(r: RunResult, includeSamples: Boolean): JSONObject {
    val obj = JSONObject()
    obj.put("id", r.id)
    obj.put("timestamp", r.timestamp)
    if (r.vehicleId != null) obj.put("vehicleId", r.vehicleId)
    obj.put("vehicleName", r.vehicleName)
    obj.put("runStartCalculatedSpeedKmh", r.runStartCalculatedSpeedKmh.toDouble())
    obj.put("runStartGpsSpeedKmh", r.runStartGpsSpeedKmh.toDouble())
    obj.put("maximumGpsSpeedKmh", r.maximumGpsSpeedKmh.toDouble())
    obj.put("maximumCalculatedSpeedKmh", r.maximumCalculatedSpeedKmh.toDouble())
    obj.put("finalGpsSpeedKmh", r.finalGpsSpeedKmh.toDouble())
    obj.put("finalCalculatedSpeedKmh", r.finalCalculatedSpeedKmh.toDouble())
    obj.put("elapsedSeconds", r.elapsedSeconds.toDouble())
    obj.put("gpsAccuracyMeters", r.gpsAccuracyMeters.toDouble())
    obj.put("totalSamples", r.totalSamples)
    obj.put("rejectedSamples", r.rejectedSamples)
    obj.put("validSamplesCount", r.validSamplesCount)
    obj.put("averageSamplingRateHz", r.averageSamplingRateHz.toDouble())
    obj.put("quality", r.quality)
    obj.put("finishReason", r.finishReason)
    obj.put("averageSpeedDifferenceKmh", r.averageSpeedDifferenceKmh.toDouble())
    obj.put("maximumSpeedDifferenceKmh", r.maximumSpeedDifferenceKmh.toDouble())
    obj.put("appVersion", r.appVersion)

    if (includeSamples && r.samples.isNotEmpty()) {
      val samplesArray = JSONArray()
      val sampleLimit = minOf(r.samples.size, MAX_SAMPLES_PER_RUN)
      for (sIdx in 0 until sampleLimit) {
        val s = r.samples[sIdx]
        val sObj = JSONObject()
        sObj.put("t", s.elapsedTimeMs)
        sObj.put("az", s.filteredAccelerationZ.toDouble())
        sObj.put("cz", s.correctedAccelerationZ.toDouble())
        sObj.put("gps", s.gpsSpeedKmh.toDouble())
        sObj.put("calc", s.calculatedSpeedKmh.toDouble())
        sObj.put("diff", s.speedDifferenceKmh.toDouble())
        sObj.put("acc", s.gpsAccuracyMeters.toDouble())
        sObj.put("gyro", s.gyroMagnitude.toDouble())
        sObj.put("val", s.isValid)
        if (s.rejectionReason != null) sObj.put("rej", s.rejectionReason)
        samplesArray.put(sObj)
      }
      obj.put("samples", samplesArray)
    }

    return obj
  }

  private fun deserializeRunResult(obj: JSONObject): RunResult {
    val samplesList = mutableListOf<RunSample>()
    if (obj.has("samples")) {
      val samplesArray = obj.getJSONArray("samples")
      for (j in 0 until samplesArray.length()) {
        val sObj = samplesArray.getJSONObject(j)
        samplesList.add(
          RunSample(
            elapsedTimeMs = sObj.optLong("t", 0L),
            filteredAccelerationZ = sObj.optDouble("az", 0.0).toFloat(),
            correctedAccelerationZ = sObj.optDouble("cz", 0.0).toFloat(),
            gpsSpeedKmh = sObj.optDouble("gps", 0.0).toFloat(),
            calculatedSpeedKmh = sObj.optDouble("calc", 0.0).toFloat(),
            speedDifferenceKmh = sObj.optDouble("diff", 0.0).toFloat(),
            gpsAccuracyMeters = sObj.optDouble("acc", 0.0).toFloat(),
            gyroMagnitude = sObj.optDouble("gyro", 0.0).toFloat(),
            isValid = sObj.optBoolean("val", true),
            rejectionReason = if (sObj.has("rej")) sObj.getString("rej") else null
          )
        )
      }
    }

    val total = obj.optInt("totalSamples", samplesList.size)
    val rej = obj.optInt("rejectedSamples", samplesList.count { !it.isValid })
    val valid = obj.optInt("validSamplesCount", if (total >= rej) total - rej else 0)

    return RunResult(
      id = obj.optString("id"),
      timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
      vehicleId = if (obj.has("vehicleId")) obj.getString("vehicleId") else null,
      vehicleName = obj.optString("vehicleName", ""),
      runStartCalculatedSpeedKmh = obj.optDouble("runStartCalculatedSpeedKmh", 40.0).toFloat(),
      runStartGpsSpeedKmh = obj.optDouble("runStartGpsSpeedKmh", 0.0).toFloat(),
      maximumGpsSpeedKmh = obj.optDouble("maximumGpsSpeedKmh", 0.0).toFloat(),
      maximumCalculatedSpeedKmh = obj.optDouble("maximumCalculatedSpeedKmh", 40.0).toFloat(),
      finalGpsSpeedKmh = obj.optDouble("finalGpsSpeedKmh", 0.0).toFloat(),
      finalCalculatedSpeedKmh = obj.optDouble("finalCalculatedSpeedKmh", 0.0).toFloat(),
      elapsedSeconds = obj.optDouble("elapsedSeconds", 0.0).toFloat(),
      gpsAccuracyMeters = obj.optDouble("gpsAccuracyMeters", 0.0).toFloat(),
      totalSamples = total,
      rejectedSamples = rej,
      validSamplesCount = valid,
      averageSamplingRateHz = obj.optDouble("averageSamplingRateHz", 0.0).toFloat(),
      quality = obj.optString("quality", "BOA"),
      finishReason = obj.optString("finishReason", "SENSOR_DECELERATION"),
      averageSpeedDifferenceKmh = obj.optDouble("averageSpeedDifferenceKmh", 0.0).toFloat(),
      maximumSpeedDifferenceKmh = obj.optDouble("maximumSpeedDifferenceKmh", 0.0).toFloat(),
      appVersion = obj.optString("appVersion", "0.17.0"),
      samples = samplesList
    )
  }
}
