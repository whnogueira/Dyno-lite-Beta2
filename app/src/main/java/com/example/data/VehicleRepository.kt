package com.example.data

import com.example.data.db.VehicleDao
import com.example.data.db.VehicleEntity
import com.example.model.AspirationType
import com.example.model.FuelType
import com.example.model.TireSpec
import com.example.model.Vehicle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VehicleRepository(private val vehicleDao: VehicleDao) {
    private val gson = Gson()

    val allVehicles: Flow<List<Vehicle>> = vehicleDao.getAllVehicles().map { entities ->
        entities.map { it.toDomain(gson) }
    }

    suspend fun getVehicleById(id: String): Vehicle? {
        return vehicleDao.getVehicleById(id)?.toDomain(gson)
    }

    suspend fun getPrimaryVehicle(): Vehicle? {
        return vehicleDao.getPrimaryVehicle()?.toDomain(gson)
    }

    suspend fun insertOrUpdateVehicle(vehicle: Vehicle) {
        vehicleDao.insertVehicle(vehicle.toEntity(gson))
    }

    suspend fun setPrimaryVehicle(id: String) {
        vehicleDao.clearPrimaryFlags()
        vehicleDao.setPrimaryVehicle(id)
    }

    suspend fun deleteVehicle(id: String) {
        vehicleDao.deleteVehicleById(id)
    }
}

fun VehicleEntity.toDomain(gson: Gson): Vehicle {
    val gearRatios: List<Float> = try {
        val type = object : TypeToken<List<Float>>() {}.type
        gson.fromJson<List<Float>>(gearRatiosJson, type) ?: listOf(3.78f, 2.12f, 1.46f, 1.03f, 0.86f, 0.73f)
    } catch (_: Exception) {
        listOf(3.78f, 2.12f, 1.46f, 1.03f, 0.86f, 0.73f)
    }

    val aspirationEnum = AspirationType.values().firstOrNull { it.name == aspiration } ?: AspirationType.TURBOCHARGED
    val fuelTypeEnum = FuelType.values().firstOrNull { it.name == fuelType } ?: FuelType.GASOLINE

    return Vehicle(
        id = id,
        name = name,
        brand = brand,
        model = model,
        year = year,
        curbWeightKg = curbWeightKg,
        driverWeightKg = driverWeightKg,
        additionalWeightKg = additionalWeightKg,
        frontalAreaM2 = frontalAreaM2,
        dragCoefficientCd = dragCoefficientCd,
        drivetrainLossPercent = drivetrainLossPercent,
        tireSpec = TireSpec(tireWidthMm, tireProfilePercent, tireRimInches),
        finalDriveRatio = finalDriveRatio,
        gearRatios = gearRatios,
        testGearIndex = testGearIndex,
        engineDisplacementCc = engineDisplacementCc,
        aspiration = aspirationEnum,
        fuelType = fuelTypeEnum,
        revLimitRpm = revLimitRpm,
        isPrimary = isPrimary
    )
}

fun Vehicle.toEntity(gson: Gson): VehicleEntity {
    return VehicleEntity(
        id = id,
        name = name,
        brand = brand,
        model = model,
        year = year,
        curbWeightKg = curbWeightKg,
        driverWeightKg = driverWeightKg,
        additionalWeightKg = additionalWeightKg,
        frontalAreaM2 = frontalAreaM2,
        dragCoefficientCd = dragCoefficientCd,
        drivetrainLossPercent = drivetrainLossPercent,
        tireWidthMm = tireSpec.widthMm,
        tireProfilePercent = tireSpec.profilePercent,
        tireRimInches = tireSpec.rimInches,
        finalDriveRatio = finalDriveRatio,
        gearRatiosJson = gson.toJson(gearRatios),
        testGearIndex = testGearIndex,
        engineDisplacementCc = engineDisplacementCc,
        aspiration = aspiration.name,
        fuelType = fuelType.name,
        revLimitRpm = revLimitRpm,
        isPrimary = isPrimary
    )
}
