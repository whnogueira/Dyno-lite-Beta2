package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DrivetrainType
import com.example.model.SavedSimulationProject
import com.example.model.SimulationConfidence
import com.example.model.SimulationConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SimulationRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("dyno_lite_simulations_store", Context.MODE_PRIVATE)

  private val KEY_SIMULATIONS_JSON = "key_simulations_json"

  fun getSavedProjects(): List<SavedSimulationProject> {
    val jsonStr = prefs.getString(KEY_SIMULATIONS_JSON, null) ?: return emptyList()
    val list = mutableListOf<SavedSimulationProject>()
    try {
      val jsonArray = JSONArray(jsonStr)
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(deserializeProject(obj))
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list.sortedByDescending { it.createdAt }
  }

  fun getProjectById(id: String): SavedSimulationProject? {
    return getSavedProjects().firstOrNull { it.id == id }
  }

  fun saveProject(project: SavedSimulationProject) {
    val current = getSavedProjects().toMutableList()
    val idx = current.indexOfFirst { it.id == project.id }
    if (idx >= 0) {
      current[idx] = project
    } else {
      current.add(0, project)
    }
    saveAll(current)
  }

  fun duplicateProject(id: String): SavedSimulationProject? {
    val existing = getProjectById(id) ?: return null
    val duplicated = existing.copy(
      id = UUID.randomUUID().toString(),
      name = "${existing.name} (Cópia)",
      createdAt = System.currentTimeMillis()
    )
    saveProject(duplicated)
    return duplicated
  }

  fun deleteProject(id: String) {
    val current = getSavedProjects().toMutableList()
    val removed = current.removeAll { it.id == id }
    if (removed) {
      saveAll(current)
    }
  }

  private fun saveAll(projects: List<SavedSimulationProject>) {
    val jsonArray = JSONArray()
    for (p in projects) {
      jsonArray.put(serializeProject(p))
    }
    prefs.edit().putString(KEY_SIMULATIONS_JSON, jsonArray.toString()).apply()
  }

  private fun serializeConfig(c: SimulationConfig): JSONObject {
    val obj = JSONObject()
    obj.put("label", c.label)
    obj.put("vehicleName", c.vehicleName)
    obj.put("vehicleCurbWeightKg", c.vehicleCurbWeightKg.toDouble())
    obj.put("driverWeightKg", c.driverWeightKg.toDouble())
    obj.put("additionalWeightKg", c.additionalWeightKg.toDouble())
    obj.put("enginePowerCv", c.enginePowerCv.toDouble())
    obj.put("engineTorqueKgfm", c.engineTorqueKgfm.toDouble())
    obj.put("peakPowerRpm", c.peakPowerRpm)
    obj.put("peakTorqueRpm", c.peakTorqueRpm)
    obj.put("maxRpm", c.maxRpm)
    val gearArray = JSONArray()
    c.gearRatios.forEach { gearArray.put(it.toDouble()) }
    obj.put("gearRatios", gearArray)
    obj.put("finalDriveRatio", c.finalDriveRatio.toDouble())
    obj.put("drivetrainLossPercent", c.drivetrainLossPercent.toDouble())
    obj.put("drivetrainType", c.drivetrainType.name)
    obj.put("shiftTimeSeconds", c.shiftTimeSeconds.toDouble())
    obj.put("tireWidthMm", c.tireWidthMm)
    obj.put("tireAspectRatio", c.tireAspectRatio)
    obj.put("rimDiameterInches", c.rimDiameterInches)
    obj.put("cd", c.cd.toDouble())
    obj.put("frontalAreaM2", c.frontalAreaM2.toDouble())
    obj.put("crr", c.crr.toDouble())
    obj.put("airDensityKgM3", c.airDensityKgM3.toDouble())
    obj.put("tireGripMu", c.tireGripMu.toDouble())
    obj.put("trackSlopePercent", c.trackSlopePercent.toDouble())
    obj.put("headwindSpeedKmh", c.headwindSpeedKmh.toDouble())
    obj.put("isTurboSimulated", c.isTurboSimulated)
    obj.put("turboBoostBar", c.turboBoostBar.toDouble())
    obj.put("turboEfficiency", c.turboEfficiency.toDouble())
    obj.put("isUsingRealRunCurve", c.isUsingRealRunCurve)
    if (c.baseRunId != null) obj.put("baseRunId", c.baseRunId)
    return obj
  }

  private fun deserializeConfig(obj: JSONObject): SimulationConfig {
    val gearRatios = mutableListOf<Float>()
    if (obj.has("gearRatios")) {
      val gArr = obj.getJSONArray("gearRatios")
      for (i in 0 until gArr.length()) {
        gearRatios.add(gArr.getDouble(i).toFloat())
      }
    }
    if (gearRatios.isEmpty()) {
      gearRatios.addAll(listOf(3.73f, 2.05f, 1.36f, 1.03f, 0.82f))
    }

    return SimulationConfig(
      label = obj.optString("label", "Configuração"),
      vehicleName = obj.optString("vehicleName", "Veículo"),
      vehicleCurbWeightKg = obj.optDouble("vehicleCurbWeightKg", 1200.0).toFloat(),
      driverWeightKg = obj.optDouble("driverWeightKg", 80.0).toFloat(),
      additionalWeightKg = obj.optDouble("additionalWeightKg", 0.0).toFloat(),
      enginePowerCv = obj.optDouble("enginePowerCv", 130.0).toFloat(),
      engineTorqueKgfm = obj.optDouble("engineTorqueKgfm", 18.0).toFloat(),
      peakPowerRpm = obj.optInt("peakPowerRpm", 5800),
      peakTorqueRpm = obj.optInt("peakTorqueRpm", 3800),
      maxRpm = obj.optInt("maxRpm", 6500),
      gearRatios = gearRatios,
      finalDriveRatio = obj.optDouble("finalDriveRatio", 4.19).toFloat(),
      drivetrainLossPercent = obj.optDouble("drivetrainLossPercent", 12.0).toFloat(),
      drivetrainType = DrivetrainType.fromString(obj.optString("drivetrainType", "FWD")),
      shiftTimeSeconds = obj.optDouble("shiftTimeSeconds", 0.50).toFloat(),
      tireWidthMm = obj.optInt("tireWidthMm", 195),
      tireAspectRatio = obj.optInt("tireAspectRatio", 55),
      rimDiameterInches = obj.optInt("rimDiameterInches", 15),
      cd = obj.optDouble("cd", 0.33).toFloat(),
      frontalAreaM2 = obj.optDouble("frontalAreaM2", 2.10).toFloat(),
      crr = obj.optDouble("crr", 0.015).toFloat(),
      airDensityKgM3 = obj.optDouble("airDensityKgM3", 1.225).toFloat(),
      tireGripMu = obj.optDouble("tireGripMu", 0.80).toFloat(),
      trackSlopePercent = obj.optDouble("trackSlopePercent", 0.0).toFloat(),
      headwindSpeedKmh = obj.optDouble("headwindSpeedKmh", 0.0).toFloat(),
      isTurboSimulated = obj.optBoolean("isTurboSimulated", false),
      turboBoostBar = obj.optDouble("turboBoostBar", 0.0).toFloat(),
      turboEfficiency = obj.optDouble("turboEfficiency", 0.85).toFloat(),
      isUsingRealRunCurve = obj.optBoolean("isUsingRealRunCurve", false),
      baseRunId = if (obj.has("baseRunId")) obj.getString("baseRunId") else null
    )
  }

  private fun serializeProject(p: SavedSimulationProject): JSONObject {
    val obj = JSONObject()
    obj.put("id", p.id)
    obj.put("name", p.name)
    obj.put("createdAt", p.createdAt)
    obj.put("vehicleName", p.vehicleName)
    if (p.baseRunId != null) obj.put("baseRunId", p.baseRunId)
    if (p.baseRunDateFormatted != null) obj.put("baseRunDateFormatted", p.baseRunDateFormatted)
    obj.put("notes", p.notes)
    obj.put("confidence", p.confidence.name)
    obj.put("configA", serializeConfig(p.configA))
    obj.put("configB", serializeConfig(p.configB))
    return obj
  }

  private fun deserializeProject(obj: JSONObject): SavedSimulationProject {
    val confName = obj.optString("confidence", "MEDIUM")
    val conf = try {
      SimulationConfidence.valueOf(confName)
    } catch (e: Exception) {
      SimulationConfidence.MEDIUM
    }

    return SavedSimulationProject(
      id = obj.optString("id", UUID.randomUUID().toString()),
      name = obj.optString("name", "Projeto de Simulação"),
      createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
      vehicleName = obj.optString("vehicleName", "Veículo"),
      baseRunId = if (obj.has("baseRunId")) obj.getString("baseRunId") else null,
      baseRunDateFormatted = if (obj.has("baseRunDateFormatted")) obj.getString("baseRunDateFormatted") else null,
      notes = obj.optString("notes", ""),
      confidence = conf,
      configA = deserializeConfig(obj.getJSONObject("configA")),
      configB = deserializeConfig(obj.getJSONObject("configB"))
    )
  }
}
