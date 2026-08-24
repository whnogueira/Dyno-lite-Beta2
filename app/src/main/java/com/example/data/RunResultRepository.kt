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
    obj.put("speedGainKmh", r.speedGainKmh.toDouble())
    obj.put("estimatedPowerCv", r.estimatedPowerCv.toDouble())
    obj.put("estimatedTorqueKgfm", r.estimatedTorqueKgfm.toDouble())
    obj.put("wheelPowerCv", r.wheelPowerCv.toDouble())
    obj.put("enginePowerCv", r.enginePowerCv.toDouble())
    obj.put("wheelTorqueKgfm", r.wheelTorqueKgfm.toDouble())
    obj.put("engineTorqueKgfm", r.engineTorqueKgfm.toDouble())
    obj.put("peakLongitudinalG", r.peakLongitudinalG.toDouble())
    obj.put("averageLongitudinalG", r.averageLongitudinalG.toDouble())
    if (r.peakPowerRpm != null) obj.put("peakPowerRpm", r.peakPowerRpm)
    if (r.peakTorqueRpm != null) obj.put("peakTorqueRpm", r.peakTorqueRpm)
    obj.put("peakPowerSpeedKmh", r.peakPowerSpeedKmh.toDouble())
    obj.put("peakTorqueSpeedKmh", r.peakTorqueSpeedKmh.toDouble())
    obj.put("totalVehicleMassKg", r.totalVehicleMassKg.toDouble())
    obj.put("drivetrainLossPercent", r.drivetrainLossPercent.toDouble())
    obj.put("estimatedMarginPercent", r.estimatedMarginPercent.toDouble())
    obj.put("gearUsed", r.gearUsed)
    obj.put("isAerodynamicsEstimated", r.isAerodynamicsEstimated)
    obj.put("elapsedSeconds", r.elapsedSeconds.toDouble())
    obj.put("gpsAccuracyMeters", r.gpsAccuracyMeters.toDouble())
    obj.put("totalSamples", r.totalSamples)
    obj.put("rejectedSamples", r.rejectedSamples)
    obj.put("validSamplesCount", r.validSamplesCount)
    obj.put("validGpsLocationsCount", r.validGpsLocationsCount)
    obj.put("averageSamplingRateHz", r.averageSamplingRateHz.toDouble())
    obj.put("averageGpsFrequencyHz", r.averageGpsFrequencyHz.toDouble())
    obj.put("quality", r.quality)
    obj.put("finishReason", r.finishReason)
    obj.put("averageSpeedDifferenceKmh", r.averageSpeedDifferenceKmh.toDouble())
    obj.put("maximumSpeedDifferenceKmh", r.maximumSpeedDifferenceKmh.toDouble())
    if (r.invalidationReason != null) obj.put("invalidationReason", r.invalidationReason)
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
        sObj.put("g", s.longitudinalG.toDouble())
        sObj.put("gps", s.gpsSpeedKmh.toDouble())
        sObj.put("calc", s.calculatedSpeedKmh.toDouble())
        sObj.put("diff", s.speedDifferenceKmh.toDouble())
        sObj.put("acc", s.gpsAccuracyMeters.toDouble())
        sObj.put("gyro", s.gyroMagnitude.toDouble())
        sObj.put("wp", s.wheelPowerCv.toDouble())
        sObj.put("ep", s.enginePowerCv.toDouble())
        sObj.put("wt", s.wheelTorqueKgfm.toDouble())
        sObj.put("et", s.engineTorqueKgfm.toDouble())
        if (s.engineRpm != null) sObj.put("rpm", s.engineRpm)
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
        val filtAz = sObj.optDouble("az", 0.0).toFloat()
        samplesList.add(
          RunSample(
            elapsedTimeMs = sObj.optLong("t", 0L),
            filteredAccelerationZ = filtAz,
            correctedAccelerationZ = sObj.optDouble("cz", 0.0).toFloat(),
            longitudinalG = sObj.optDouble("g", (filtAz / 9.80665).toDouble()).toFloat(),
            gpsSpeedKmh = sObj.optDouble("gps", 0.0).toFloat(),
            calculatedSpeedKmh = sObj.optDouble("calc", 0.0).toFloat(),
            speedDifferenceKmh = sObj.optDouble("diff", 0.0).toFloat(),
            gpsAccuracyMeters = sObj.optDouble("acc", 0.0).toFloat(),
            gyroMagnitude = sObj.optDouble("gyro", 0.0).toFloat(),
            wheelPowerCv = sObj.optDouble("wp", 0.0).toFloat(),
            enginePowerCv = sObj.optDouble("ep", 0.0).toFloat(),
            wheelTorqueKgfm = sObj.optDouble("wt", 0.0).toFloat(),
            engineTorqueKgfm = sObj.optDouble("et", 0.0).toFloat(),
            engineRpm = if (sObj.has("rpm")) sObj.getInt("rpm") else null,
            isValid = sObj.optBoolean("val", true),
            rejectionReason = if (sObj.has("rej")) sObj.getString("rej") else null
          )
        )
      }
    }

    val total = obj.optInt("totalSamples", samplesList.size)
    val rej = obj.optInt("rejectedSamples", samplesList.count { !it.isValid })
    val valid = obj.optInt("validSamplesCount", if (total >= rej) total - rej else 0)
    val maxGps = obj.optDouble("maximumGpsSpeedKmh", 0.0).toFloat()
    val startGps = obj.optDouble("runStartGpsSpeedKmh", 0.0).toFloat()
    val estPower = obj.optDouble("estimatedPowerCv", 0.0).toFloat()
    val estTorque = obj.optDouble("estimatedTorqueKgfm", 0.0).toFloat()

    return RunResult(
      id = obj.optString("id"),
      timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
      vehicleId = if (obj.has("vehicleId")) obj.getString("vehicleId") else null,
      vehicleName = obj.optString("vehicleName", ""),
      runStartCalculatedSpeedKmh = obj.optDouble("runStartCalculatedSpeedKmh", 40.0).toFloat(),
      runStartGpsSpeedKmh = startGps,
      maximumGpsSpeedKmh = maxGps,
      maximumCalculatedSpeedKmh = obj.optDouble("maximumCalculatedSpeedKmh", 40.0).toFloat(),
      finalGpsSpeedKmh = obj.optDouble("finalGpsSpeedKmh", 0.0).toFloat(),
      finalCalculatedSpeedKmh = obj.optDouble("finalCalculatedSpeedKmh", 0.0).toFloat(),
      speedGainKmh = obj.optDouble("speedGainKmh", (maxGps - startGps).coerceAtLeast(0f).toDouble()).toFloat(),
      estimatedPowerCv = estPower,
      estimatedTorqueKgfm = estTorque,
      wheelPowerCv = obj.optDouble("wheelPowerCv", (estPower * 0.85).toDouble()).toFloat(),
      enginePowerCv = obj.optDouble("enginePowerCv", estPower.toDouble()).toFloat(),
      wheelTorqueKgfm = obj.optDouble("wheelTorqueKgfm", (estTorque * 0.85).toDouble()).toFloat(),
      engineTorqueKgfm = obj.optDouble("engineTorqueKgfm", estTorque.toDouble()).toFloat(),
      peakLongitudinalG = obj.optDouble("peakLongitudinalG", 0.0).toFloat(),
      averageLongitudinalG = obj.optDouble("averageLongitudinalG", 0.0).toFloat(),
      peakPowerRpm = if (obj.has("peakPowerRpm")) obj.getInt("peakPowerRpm") else null,
      peakTorqueRpm = if (obj.has("peakTorqueRpm")) obj.getInt("peakTorqueRpm") else null,
      peakPowerSpeedKmh = obj.optDouble("peakPowerSpeedKmh", maxGps.toDouble()).toFloat(),
      peakTorqueSpeedKmh = obj.optDouble("peakTorqueSpeedKmh", (startGps + (maxGps - startGps) * 0.45).toDouble()).toFloat(),
      totalVehicleMassKg = obj.optDouble("totalVehicleMassKg", 0.0).toFloat(),
      drivetrainLossPercent = obj.optDouble("drivetrainLossPercent", 15.0).toFloat(),
      estimatedMarginPercent = obj.optDouble("estimatedMarginPercent", 10.0).toFloat(),
      gearUsed = obj.optString("gearUsed", "2ª"),
      isAerodynamicsEstimated = obj.optBoolean("isAerodynamicsEstimated", true),
      elapsedSeconds = obj.optDouble("elapsedSeconds", 0.0).toFloat(),
      gpsAccuracyMeters = obj.optDouble("gpsAccuracyMeters", 0.0).toFloat(),
      totalSamples = total,
      rejectedSamples = rej,
      validSamplesCount = valid,
      validGpsLocationsCount = obj.optInt("validGpsLocationsCount", 4),
      averageSamplingRateHz = obj.optDouble("averageSamplingRateHz", 0.0).toFloat(),
      averageGpsFrequencyHz = obj.optDouble("averageGpsFrequencyHz", 0.0).toFloat(),
      quality = obj.optString("quality", "BOA"),
      finishReason = obj.optString("finishReason", "GPS_DECELERATION"),
      averageSpeedDifferenceKmh = obj.optDouble("averageSpeedDifferenceKmh", 0.0).toFloat(),
      maximumSpeedDifferenceKmh = obj.optDouble("maximumSpeedDifferenceKmh", 0.0).toFloat(),
      invalidationReason = if (obj.has("invalidationReason")) obj.getString("invalidationReason") else null,
      appVersion = obj.optString("appVersion", "0.20.0"),
      samples = samplesList
    )
  }
}
