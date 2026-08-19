package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.RunResult
import org.json.JSONArray
import org.json.JSONObject

class RunResultRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("dyno_lite_runs_store", Context.MODE_PRIVATE)

  private val KEY_RUNS_JSON = "key_runs_json"
  private val MAX_STORED_RUNS = 100

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

  fun saveResult(run: RunResult) {
    val currentRuns = getResults().toMutableList()
    val existingIndex = currentRuns.indexOfFirst { it.id == run.id }

    if (existingIndex >= 0) {
      currentRuns[existingIndex] = run
    } else {
      currentRuns.add(0, run)
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
    for (r in runs) {
      jsonArray.put(serializeRunResult(r))
    }
    prefs.edit().putString(KEY_RUNS_JSON, jsonArray.toString()).apply()
  }

  private fun serializeRunResult(r: RunResult): JSONObject {
    val obj = JSONObject()
    obj.put("id", r.id)
    obj.put("timestamp", r.timestamp)
    if (r.vehicleId != null) obj.put("vehicleId", r.vehicleId)
    obj.put("vehicleName", r.vehicleName)
    obj.put("runStartGpsSpeedKmh", r.runStartGpsSpeedKmh.toDouble())
    obj.put("maximumGpsSpeedKmh", r.maximumGpsSpeedKmh.toDouble())
    obj.put("maximumCalculatedSpeedKmh", r.maximumCalculatedSpeedKmh.toDouble())
    obj.put("finalGpsSpeedKmh", r.finalGpsSpeedKmh.toDouble())
    obj.put("finalCalculatedSpeedKmh", r.finalCalculatedSpeedKmh.toDouble())
    obj.put("elapsedSeconds", r.elapsedSeconds.toDouble())
    obj.put("gpsAccuracyMeters", r.gpsAccuracyMeters.toDouble())
    obj.put("totalSamples", r.totalSamples)
    obj.put("rejectedSamples", r.rejectedSamples)
    obj.put("quality", r.quality)
    obj.put("finishReason", r.finishReason)
    obj.put("averageSpeedDifferenceKmh", r.averageSpeedDifferenceKmh.toDouble())
    obj.put("maximumSpeedDifferenceKmh", r.maximumSpeedDifferenceKmh.toDouble())
    obj.put("appVersion", r.appVersion)
    return obj
  }

  private fun deserializeRunResult(obj: JSONObject): RunResult {
    return RunResult(
      id = obj.optString("id"),
      timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
      vehicleId = if (obj.has("vehicleId")) obj.getString("vehicleId") else null,
      vehicleName = obj.optString("vehicleName", ""),
      runStartGpsSpeedKmh = obj.optDouble("runStartGpsSpeedKmh", 0.0).toFloat(),
      maximumGpsSpeedKmh = obj.optDouble("maximumGpsSpeedKmh", 0.0).toFloat(),
      maximumCalculatedSpeedKmh = obj.optDouble("maximumCalculatedSpeedKmh", 0.0).toFloat(),
      finalGpsSpeedKmh = obj.optDouble("finalGpsSpeedKmh", 0.0).toFloat(),
      finalCalculatedSpeedKmh = obj.optDouble("finalCalculatedSpeedKmh", 0.0).toFloat(),
      elapsedSeconds = obj.optDouble("elapsedSeconds", 0.0).toFloat(),
      gpsAccuracyMeters = obj.optDouble("gpsAccuracyMeters", 0.0).toFloat(),
      totalSamples = obj.optInt("totalSamples", 0),
      rejectedSamples = obj.optInt("rejectedSamples", 0),
      quality = obj.optString("quality", "BOA"),
      finishReason = obj.optString("finishReason", "SENSOR_DECELERATION"),
      averageSpeedDifferenceKmh = obj.optDouble("averageSpeedDifferenceKmh", 0.0).toFloat(),
      maximumSpeedDifferenceKmh = obj.optDouble("maximumSpeedDifferenceKmh", 0.0).toFloat(),
      appVersion = obj.optString("appVersion", "0.14.0")
    )
  }
}
