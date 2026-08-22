package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AudioWeightPreset
import com.example.model.VehicleProfile
import org.json.JSONArray
import org.json.JSONObject

class VehicleRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("dyno_lite_vehicles_store", Context.MODE_PRIVATE)

  private val KEY_VEHICLES_JSON = "key_vehicles_json"
  private val KEY_PRIMARY_VEHICLE_ID = "key_primary_vehicle_id"

  fun getVehicles(): List<VehicleProfile> {
    val jsonStr = prefs.getString(KEY_VEHICLES_JSON, null) ?: return emptyList()
    val list = mutableListOf<VehicleProfile>()
    try {
      val jsonArray = JSONArray(jsonStr)
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(deserializeVehicle(obj))
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list
  }

  fun getPrimaryVehicle(): VehicleProfile? {
    val vehicles = getVehicles()
    if (vehicles.isEmpty()) return null
    val primaryId = prefs.getString(KEY_PRIMARY_VEHICLE_ID, null)
    return vehicles.firstOrNull { it.id == primaryId } ?: vehicles.firstOrNull { it.isPrimary } ?: vehicles.firstOrNull()
  }

  fun getPrimaryVehicleId(): String? {
    return prefs.getString(KEY_PRIMARY_VEHICLE_ID, null)
  }

  fun saveVehicle(vehicle: VehicleProfile) {
    val currentVehicles = getVehicles().toMutableList()
    val existingIndex = currentVehicles.indexOfFirst { it.id == vehicle.id }

    val shouldBePrimary = if (currentVehicles.isEmpty()) true else vehicle.isPrimary

    val updatedVehicle = vehicle.copy(isPrimary = shouldBePrimary)

    if (existingIndex >= 0) {
      currentVehicles[existingIndex] = updatedVehicle
    } else {
      currentVehicles.add(updatedVehicle)
    }

    if (shouldBePrimary) {
      for (i in currentVehicles.indices) {
        if (currentVehicles[i].id != updatedVehicle.id) {
          currentVehicles[i] = currentVehicles[i].copy(isPrimary = false)
        }
      }
      prefs.edit().putString(KEY_PRIMARY_VEHICLE_ID, updatedVehicle.id).apply()
    }

    saveAll(currentVehicles)
  }

  fun setPrimaryVehicle(vehicleId: String) {
    val currentVehicles = getVehicles().toMutableList()
    for (i in currentVehicles.indices) {
      currentVehicles[i] = currentVehicles[i].copy(isPrimary = (currentVehicles[i].id == vehicleId))
    }
    prefs.edit().putString(KEY_PRIMARY_VEHICLE_ID, vehicleId).apply()
    saveAll(currentVehicles)
  }

  fun duplicateVehicle(vehicleId: String) {
    val vehicle = getVehicles().firstOrNull { it.id == vehicleId } ?: return
    val duplicate = vehicle.copy(
      id = java.util.UUID.randomUUID().toString(),
      model = "${vehicle.model} (Cópia)",
      isPrimary = false
    )
    saveVehicle(duplicate)
  }

  fun deleteVehicle(vehicleId: String) {
    val currentVehicles = getVehicles().toMutableList()
    val removed = currentVehicles.removeAll { it.id == vehicleId }
    if (removed) {
      val primaryId = prefs.getString(KEY_PRIMARY_VEHICLE_ID, null)
      if (primaryId == vehicleId) {
        if (currentVehicles.isNotEmpty()) {
          currentVehicles[0] = currentVehicles[0].copy(isPrimary = true)
          prefs.edit().putString(KEY_PRIMARY_VEHICLE_ID, currentVehicles[0].id).apply()
        } else {
          prefs.edit().remove(KEY_PRIMARY_VEHICLE_ID).apply()
        }
      }
      saveAll(currentVehicles)
    }
  }

  private fun saveAll(vehicles: List<VehicleProfile>) {
    val jsonArray = JSONArray()
    for (v in vehicles) {
      jsonArray.put(serializeVehicle(v))
    }
    prefs.edit().putString(KEY_VEHICLES_JSON, jsonArray.toString()).apply()
  }

  private fun serializeVehicle(v: VehicleProfile): JSONObject {
    val obj = JSONObject()
    obj.put("id", v.id)
    obj.put("manufacturer", v.manufacturer)
    obj.put("model", v.model)
    obj.put("year", v.year)
    obj.put("version", v.version)
    obj.put("engine", v.engine)
    obj.put("displacement", v.displacement)
    if (v.factoryPowerCv != null) obj.put("factoryPowerCv", v.factoryPowerCv.toDouble())
    if (v.factoryTorqueKgf != null) obj.put("factoryTorqueKgf", v.factoryTorqueKgf.toDouble())
    obj.put("curbWeightKg", v.curbWeightKg.toDouble())
    obj.put("drivetrain", v.drivetrain)
    if (v.transmissionId != null) obj.put("transmissionId", v.transmissionId)
    if (v.customTransmissionName != null) obj.put("customTransmissionName", v.customTransmissionName)
    obj.put("tireWidthMm", v.tireWidthMm)
    obj.put("tireAspectRatio", v.tireAspectRatio)
    obj.put("wheelDiameterInches", v.wheelDiameterInches)
    obj.put("driverWeightKg", v.driverWeightKg.toDouble())
    obj.put("passengerWeightKg", v.passengerWeightKg.toDouble())
    obj.put("cargoWeightKg", v.cargoWeightKg.toDouble())
    obj.put("audioPreset", v.audioPreset.name)
    obj.put("audioWeightKg", v.audioWeightKg.toDouble())
    obj.put("gnvWeightKg", v.gnvWeightKg.toDouble())
    obj.put("otherWeightKg", v.otherWeightKg.toDouble())
    obj.put("removedWeightKg", v.removedWeightKg.toDouble())
    if (v.measuredTotalWeightKg != null) obj.put("measuredTotalWeightKg", v.measuredTotalWeightKg.toDouble())
    obj.put("useMeasuredWeight", v.useMeasuredWeight)
    obj.put("isPrimary", v.isPrimary)
    obj.put("isCustom", v.isCustom)
    return obj
  }

  private fun deserializeVehicle(obj: JSONObject): VehicleProfile {
    return VehicleProfile(
      id = obj.optString("id"),
      manufacturer = obj.optString("manufacturer"),
      model = obj.optString("model"),
      year = obj.optInt("year", 2010),
      version = obj.optString("version"),
      engine = obj.optString("engine"),
      displacement = obj.optString("displacement"),
      factoryPowerCv = if (obj.has("factoryPowerCv")) obj.getDouble("factoryPowerCv").toFloat() else null,
      factoryTorqueKgf = if (obj.has("factoryTorqueKgf")) obj.getDouble("factoryTorqueKgf").toFloat() else null,
      curbWeightKg = obj.optDouble("curbWeightKg", 1000.0).toFloat(),
      drivetrain = obj.optString("drivetrain", "Dianteira"),
      transmissionId = if (obj.has("transmissionId")) obj.getString("transmissionId") else null,
      customTransmissionName = if (obj.has("customTransmissionName")) obj.getString("customTransmissionName") else null,
      tireWidthMm = obj.optInt("tireWidthMm", 185),
      tireAspectRatio = obj.optInt("tireAspectRatio", 70),
      wheelDiameterInches = obj.optInt("wheelDiameterInches", 14),
      driverWeightKg = obj.optDouble("driverWeightKg", 0.0).toFloat(),
      passengerWeightKg = obj.optDouble("passengerWeightKg", 0.0).toFloat(),
      cargoWeightKg = obj.optDouble("cargoWeightKg", 0.0).toFloat(),
      audioPreset = try {
        AudioWeightPreset.valueOf(obj.optString("audioPreset", AudioWeightPreset.NONE.name))
      } catch (e: Exception) {
        AudioWeightPreset.NONE
      },
      audioWeightKg = obj.optDouble("audioWeightKg", 0.0).toFloat(),
      gnvWeightKg = obj.optDouble("gnvWeightKg", 0.0).toFloat(),
      otherWeightKg = obj.optDouble("otherWeightKg", 0.0).toFloat(),
      removedWeightKg = obj.optDouble("removedWeightKg", 0.0).toFloat(),
      measuredTotalWeightKg = if (obj.has("measuredTotalWeightKg")) obj.getDouble("measuredTotalWeightKg").toFloat() else null,
      useMeasuredWeight = obj.optBoolean("useMeasuredWeight", false),
      isPrimary = obj.optBoolean("isPrimary", false),
      isCustom = obj.optBoolean("isCustom", false)
    )
  }
}
