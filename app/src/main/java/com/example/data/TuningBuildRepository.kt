package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TuningBuildRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("dyno_tuning_garage_store", Context.MODE_PRIVATE)

  private val KEY_TUNING_BUILDS_JSON = "key_tuning_builds_json"
  private val KEY_ACTIVE_BUILD_ID = "key_active_build_id"

  fun getSavedBuilds(): List<TuningBuild> {
    val jsonStr = prefs.getString(KEY_TUNING_BUILDS_JSON, null)
    if (jsonStr.isNullOrEmpty()) {
      val defaultBuild = GarageTuningEngine.createDefaultVectraBuild()
      saveBuild(defaultBuild)
      return listOf(defaultBuild)
    }

    val list = mutableListOf<TuningBuild>()
    try {
      val jsonArray = JSONArray(jsonStr)
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(deserializeBuild(obj))
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    if (list.isEmpty()) {
      val defaultBuild = GarageTuningEngine.createDefaultVectraBuild()
      list.add(defaultBuild)
      saveBuild(defaultBuild)
    }

    return list
  }

  fun getBuildById(id: String): TuningBuild? {
    return getSavedBuilds().firstOrNull { it.id == id }
  }

  fun getActiveBuild(): TuningBuild {
    val activeId = prefs.getString(KEY_ACTIVE_BUILD_ID, null)
    val builds = getSavedBuilds()
    return builds.firstOrNull { it.id == activeId } ?: builds.first()
  }

  fun setActiveBuildId(id: String) {
    prefs.edit().putString(KEY_ACTIVE_BUILD_ID, id).apply()
  }

  fun saveBuild(build: TuningBuild) {
    val current = getSavedBuilds().toMutableList()
    val idx = current.indexOfFirst { it.id == build.id }
    if (idx >= 0) {
      current[idx] = build
    } else {
      current.add(0, build)
    }
    saveAll(current)
    setActiveBuildId(build.id)
  }

  fun duplicateBuild(id: String): TuningBuild? {
    val existing = getBuildById(id) ?: return null
    val copy = existing.copy(
      id = UUID.randomUUID().toString(),
      projectName = "${existing.projectName} (Cópia)"
    )
    saveBuild(copy)
    return copy
  }

  fun deleteBuild(id: String) {
    val current = getSavedBuilds().toMutableList()
    val removed = current.removeAll { it.id == id }
    if (removed && current.isNotEmpty()) {
      saveAll(current)
      setActiveBuildId(current.first().id)
    }
  }

  private fun saveAll(builds: List<TuningBuild>) {
    val jsonArray = JSONArray()
    for (b in builds) {
      jsonArray.put(serializeBuild(b))
    }
    prefs.edit().putString(KEY_TUNING_BUILDS_JSON, jsonArray.toString()).apply()
  }

  private fun serializeBuild(b: TuningBuild): JSONObject {
    val obj = JSONObject()
    obj.put("id", b.id)
    obj.put("projectName", b.projectName)
    obj.put("vehicleName", b.vehicleName)
    obj.put("baseRunId", b.baseRunId ?: "")
    obj.put("isDemonstrativeVehicle", b.isDemonstrativeVehicle)
    obj.put("displacementCc", b.displacementCc)
    obj.put("cylindersCount", b.cylindersCount)
    obj.put("factoryEnginePowerCv", b.factoryEnginePowerCv.toDouble())
    obj.put("factoryEngineTorqueKgfm", b.factoryEngineTorqueKgfm.toDouble())
    obj.put("factoryPeakPowerRpm", b.factoryPeakPowerRpm)
    obj.put("factoryPeakTorqueRpm", b.factoryPeakTorqueRpm)
    obj.put("factoryRedlineRpm", b.factoryRedlineRpm)
    obj.put("baseVehicleCurbWeightKg", b.baseVehicleCurbWeightKg.toDouble())
    obj.put("driverWeightKg", b.driverWeightKg.toDouble())
    obj.put("baseDrivetrain", b.baseDrivetrain.name)
    obj.put("baseCompressionRatio", b.baseCompressionRatio.toDouble())

    val gearArray = JSONArray()
    b.gearRatios.forEach { gearArray.put(it.toDouble()) }
    obj.put("gearRatios", gearArray)
    obj.put("finalDriveRatio", b.finalDriveRatio.toDouble())
    obj.put("drivetrainLossPercent", b.drivetrainLossPercent.toDouble())
    obj.put("shiftSpeed", b.shiftSpeed.name)

    obj.put("tireWidthMm", b.tireWidthMm)
    obj.put("tireAspectRatio", b.tireAspectRatio)
    obj.put("rimInches", b.rimInches)
    obj.put("baseCd", b.baseCd.toDouble())
    obj.put("baseFrontalAreaM2", b.baseFrontalAreaM2.toDouble())

    // Peças
    obj.put("pistons", b.pistons.name)
    obj.put("rods", b.rods.name)
    obj.put("studs", b.studs.name)
    obj.put("headGasket", b.headGasket.name)
    obj.put("crankshaft", b.crankshaft.name)
    obj.put("extraCompressionRatio", b.extraCompressionRatio.toDouble())

    obj.put("injectorFlowLbHr", b.injectorFlowLbHr.toDouble())
    obj.put("injectorBasePressureBar", b.injectorBasePressureBar.toDouble())
    obj.put("injectorOperatingPressureBar", b.injectorOperatingPressureBar.toDouble())
    obj.put("injectorCount", b.injectorCount)
    obj.put("maxInjectorDutyCyclePercent", b.maxInjectorDutyCyclePercent.toDouble())
    obj.put("fuelPumpFlowLph", b.fuelPumpFlowLph.toDouble())
    obj.put("fuelPumpCount", b.fuelPumpCount)

    obj.put("aspiration", b.aspiration.name)
    obj.put("turboBoostBar", b.turboBoostBar.toDouble())
    obj.put("turboSpoolStartRpm", b.turboSpoolStartRpm)
    obj.put("turboFullBoostRpm", b.turboFullBoostRpm)
    obj.put("turboMaxFlowHp", b.turboMaxFlowHp.toDouble())
    obj.put("turboEfficiency", b.turboEfficiency.toDouble())
    obj.put("intercooler", b.intercooler.name)

    obj.put("fuelType", b.fuelType.name)
    obj.put("ecu", b.ecu.name)
    obj.put("tuneMap", b.tuneMap.name)
    obj.put("ignitionCoil", b.ignitionCoil.name)
    obj.put("sparkPlugs", b.sparkPlugs.name)
    obj.put("timingAdvanceDegrees", b.timingAdvanceDegrees.toDouble())

    obj.put("intake", b.intake.name)
    obj.put("throttleBody", b.throttleBody.name)
    obj.put("exhaustHeader", b.exhaustHeader.name)
    obj.put("exhaustSystem", b.exhaustSystem.name)

    obj.put("cylinderHead", b.cylinderHead.name)
    obj.put("camshaft", b.camshaft.name)
    obj.put("customRedlineRpm", b.customRedlineRpm ?: -1)

    obj.put("clutch", b.clutch.name)
    obj.put("tireCompound", b.tireCompound.name)
    obj.put("weightReduction", b.weightReduction.name)
    obj.put("aero", b.aero.name)
    obj.put("laborCostBrl", b.laborCostBrl)

    return obj
  }

  private fun deserializeBuild(obj: JSONObject): TuningBuild {
    val gearList = mutableListOf<Float>()
    val gearArray = obj.optJSONArray("gearRatios")
    if (gearArray != null) {
      for (i in 0 until gearArray.length()) {
        gearList.add(gearArray.getDouble(i).toFloat())
      }
    }
    if (gearList.isEmpty()) {
      gearList.addAll(listOf(3.73f, 1.96f, 1.32f, 0.95f, 0.76f))
    }

    val customRedline = obj.optInt("customRedlineRpm", -1)

    return TuningBuild(
      id = obj.optString("id", UUID.randomUUID().toString()),
      projectName = obj.optString("projectName", "Meu Projeto"),
      vehicleName = obj.optString("vehicleName", "Chevrolet Vectra 2.2 8V 1999"),
      baseRunId = obj.optString("baseRunId", "").ifEmpty { null },
      isDemonstrativeVehicle = obj.optBoolean("isDemonstrativeVehicle", false),
      displacementCc = obj.optInt("displacementCc", 2198),
      cylindersCount = obj.optInt("cylindersCount", 4),
      factoryEnginePowerCv = obj.optDouble("factoryEnginePowerCv", 123.0).toFloat(),
      factoryEngineTorqueKgfm = obj.optDouble("factoryEngineTorqueKgfm", 19.4).toFloat(),
      factoryPeakPowerRpm = obj.optInt("factoryPeakPowerRpm", 5200),
      factoryPeakTorqueRpm = obj.optInt("factoryPeakTorqueRpm", 2800),
      factoryRedlineRpm = obj.optInt("factoryRedlineRpm", 6200),
      baseVehicleCurbWeightKg = obj.optDouble("baseVehicleCurbWeightKg", 1260.0).toFloat(),
      driverWeightKg = obj.optDouble("driverWeightKg", 80.0).toFloat(),
      baseDrivetrain = DrivetrainType.fromString(obj.optString("baseDrivetrain", "FWD")),
      baseCompressionRatio = obj.optDouble("baseCompressionRatio", 9.2).toFloat(),
      gearRatios = gearList,
      finalDriveRatio = obj.optDouble("finalDriveRatio", 3.94).toFloat(),
      drivetrainLossPercent = obj.optDouble("drivetrainLossPercent", 12.0).toFloat(),
      shiftSpeed = try { ShiftSpeedType.valueOf(obj.optString("shiftSpeed", "MANUAL_FAST")) } catch (e: Exception) { ShiftSpeedType.MANUAL_FAST },
      tireWidthMm = obj.optInt("tireWidthMm", 195),
      tireAspectRatio = obj.optInt("tireAspectRatio", 60),
      rimInches = obj.optInt("rimInches", 15),
      baseCd = obj.optDouble("baseCd", 0.31).toFloat(),
      baseFrontalAreaM2 = obj.optDouble("baseFrontalAreaM2", 2.05).toFloat(),

      pistons = try { PistonType.valueOf(obj.optString("pistons", "ORIGINAL")) } catch (e: Exception) { PistonType.ORIGINAL },
      rods = try { RodsType.valueOf(obj.optString("rods", "ORIGINAL")) } catch (e: Exception) { RodsType.ORIGINAL },
      studs = try { StudsType.valueOf(obj.optString("studs", "ORIGINAL")) } catch (e: Exception) { StudsType.ORIGINAL },
      headGasket = try { HeadGasketType.valueOf(obj.optString("headGasket", "ORIGINAL")) } catch (e: Exception) { HeadGasketType.ORIGINAL },
      crankshaft = try { CrankshaftType.valueOf(obj.optString("crankshaft", "ORIGINAL")) } catch (e: Exception) { CrankshaftType.ORIGINAL },
      extraCompressionRatio = obj.optDouble("extraCompressionRatio", 0.0).toFloat(),

      injectorFlowLbHr = obj.optDouble("injectorFlowLbHr", 28.0).toFloat(),
      injectorBasePressureBar = obj.optDouble("injectorBasePressureBar", 3.0).toFloat(),
      injectorOperatingPressureBar = obj.optDouble("injectorOperatingPressureBar", 3.0).toFloat(),
      injectorCount = obj.optInt("injectorCount", 4),
      maxInjectorDutyCyclePercent = obj.optDouble("maxInjectorDutyCyclePercent", 85.0).toFloat(),
      fuelPumpFlowLph = obj.optDouble("fuelPumpFlowLph", 100.0).toFloat(),
      fuelPumpCount = obj.optInt("fuelPumpCount", 1),

      aspiration = try { AspirationType.valueOf(obj.optString("aspiration", "ASPIRADO")) } catch (e: Exception) { AspirationType.ASPIRADO },
      turboBoostBar = obj.optDouble("turboBoostBar", 0.0).toFloat(),
      turboSpoolStartRpm = obj.optInt("turboSpoolStartRpm", 2200),
      turboFullBoostRpm = obj.optInt("turboFullBoostRpm", 3200),
      turboMaxFlowHp = obj.optDouble("turboMaxFlowHp", 260.0).toFloat(),
      turboEfficiency = obj.optDouble("turboEfficiency", 0.85).toFloat(),
      intercooler = try { IntercoolerType.valueOf(obj.optString("intercooler", "SEM_INTERCOOLER")) } catch (e: Exception) { IntercoolerType.SEM_INTERCOOLER },

      fuelType = try { FuelTypeOption.valueOf(obj.optString("fuelType", "ETANOL")) } catch (e: Exception) { FuelTypeOption.ETANOL },
      ecu = try { EcuType.valueOf(obj.optString("ecu", "ORIGINAL")) } catch (e: Exception) { EcuType.ORIGINAL },
      tuneMap = try { TuneMapType.valueOf(obj.optString("tuneMap", "RUA_EQUILIBRADO")) } catch (e: Exception) { TuneMapType.RUA_EQUILIBRADO },
      ignitionCoil = try { IgnitionCoilType.valueOf(obj.optString("ignitionCoil", "ORIGINAL")) } catch (e: Exception) { IgnitionCoilType.ORIGINAL },
      sparkPlugs = try { SparkPlugType.valueOf(obj.optString("sparkPlugs", "ORIGINAL")) } catch (e: Exception) { SparkPlugType.ORIGINAL },
      timingAdvanceDegrees = obj.optDouble("timingAdvanceDegrees", 0.0).toFloat(),

      intake = try { IntakeType.valueOf(obj.optString("intake", "FILTRO_ORIGINAL")) } catch (e: Exception) { IntakeType.FILTRO_ORIGINAL },
      throttleBody = try { ThrottleBodyType.valueOf(obj.optString("throttleBody", "ORIGINAL")) } catch (e: Exception) { ThrottleBodyType.ORIGINAL },
      exhaustHeader = try { ExhaustHeaderType.valueOf(obj.optString("exhaustHeader", "ORIGINAL")) } catch (e: Exception) { ExhaustHeaderType.ORIGINAL },
      exhaustSystem = try { ExhaustSystemType.valueOf(obj.optString("exhaustSystem", "ORIGINAL")) } catch (e: Exception) { ExhaustSystemType.ORIGINAL },

      cylinderHead = try { CylinderHeadType.valueOf(obj.optString("cylinderHead", "ORIGINAL")) } catch (e: Exception) { CylinderHeadType.ORIGINAL },
      camshaft = try { CamshaftProfile.valueOf(obj.optString("camshaft", "ORIGINAL")) } catch (e: Exception) { CamshaftProfile.ORIGINAL },
      customRedlineRpm = if (customRedline > 0) customRedline else null,

      clutch = try { ClutchType.valueOf(obj.optString("clutch", "ORIGINAL")) } catch (e: Exception) { ClutchType.ORIGINAL },
      tireCompound = try { TireCompound.valueOf(obj.optString("tireCompound", "RUA_CONVENCIONAL")) } catch (e: Exception) { TireCompound.RUA_CONVENCIONAL },
      weightReduction = try { WeightReductionStage.valueOf(obj.optString("weightReduction", "ORIGINAL")) } catch (e: Exception) { WeightReductionStage.ORIGINAL },
      aero = try { AeroPackage.valueOf(obj.optString("aero", "ORIGINAL")) } catch (e: Exception) { AeroPackage.ORIGINAL },
      laborCostBrl = obj.optDouble("laborCostBrl", 0.0)
    )
  }
}
