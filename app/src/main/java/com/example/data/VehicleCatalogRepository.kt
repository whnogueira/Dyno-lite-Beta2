package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.CatalogManifest
import com.example.model.DynoCompatibility
import com.example.model.ManufacturerManifestEntry
import com.example.model.TransmissionCatalogEntry
import com.example.model.VehicleCatalogEntry
import com.example.model.VerificationStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class VehicleCatalogRepository(private val context: Context) {

  companion object {
    @Volatile
    private var INSTANCE: VehicleCatalogRepository? = null

    fun getInstance(context: Context): VehicleCatalogRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: VehicleCatalogRepository(context.applicationContext).also { INSTANCE = it }
      }
    }
  }

  private val TAG = "VehicleCatalogRepo"
  private val ASSET_BASE_DIR = "vehicle_catalog"

  private var manifest: CatalogManifest? = null
  private val brandVehiclesCache = mutableMapOf<String, List<VehicleCatalogEntry>>()
  private var transmissionsCache: List<TransmissionCatalogEntry>? = null

  init {
    loadManifest()
  }

  fun getManifest(): CatalogManifest? {
    if (manifest == null) {
      loadManifest()
    }
    return manifest
  }

  fun getManufacturers(): List<ManufacturerManifestEntry> {
    return getManifest()?.manufacturers ?: emptyList()
  }

  fun getManufacturerById(id: String): ManufacturerManifestEntry? {
    return getManufacturers().firstOrNull { it.id.equals(id, ignoreCase = true) }
  }

  /**
   * Lazily loads and returns the catalog entries for a given manufacturer.
   */
  fun getVehiclesForManufacturer(manufacturerId: String): List<VehicleCatalogEntry> {
    val mfgId = manufacturerId.lowercase().trim()
    brandVehiclesCache[mfgId]?.let { return it }

    val mfgEntry = getManufacturers().firstOrNull { it.id.equals(mfgId, ignoreCase = true) }
    if (mfgEntry == null) {
      Log.w(TAG, "Fabricante não encontrado no manifesto: $manufacturerId")
      return emptyList()
    }

    val vehicles = loadVehicleFile(mfgEntry.file, mfgEntry.id, mfgEntry.name)
    brandVehiclesCache[mfgId] = vehicles
    return vehicles
  }

  /**
   * Retrieves unique model names for a manufacturer.
   */
  fun getModelsByManufacturer(manufacturerId: String): List<String> {
    return getVehiclesForManufacturer(manufacturerId)
      .map { it.model }
      .distinct()
      .sorted()
  }

  /**
   * Retrieves unique generations for a manufacturer and model.
   */
  fun getGenerations(manufacturerId: String, model: String): List<String> {
    return getVehiclesForManufacturer(manufacturerId)
      .filter { it.model.equals(model, ignoreCase = true) }
      .mapNotNull { it.generation }
      .distinct()
      .sorted()
  }

  /**
   * Retrieves unique trims/versions for a manufacturer, model, and optional generation.
   */
  fun getTrims(manufacturerId: String, model: String, generation: String? = null): List<String> {
    return getVehiclesForManufacturer(manufacturerId)
      .filter {
        it.model.equals(model, ignoreCase = true) &&
          (generation == null || it.generation.equals(generation, ignoreCase = true))
      }
      .mapNotNull { it.trim }
      .distinct()
      .sorted()
  }

  /**
   * Retrieves vehicles matching filters.
   */
  fun getVehicles(
    manufacturerId: String,
    model: String? = null,
    generation: String? = null,
    year: Int? = null
  ): List<VehicleCatalogEntry> {
    return getVehiclesForManufacturer(manufacturerId).filter { v ->
      (model == null || v.model.equals(model, ignoreCase = true)) &&
        (generation == null || v.generation.equals(generation, ignoreCase = true)) &&
        (year == null || v.matchesYear(year))
    }
  }

  /**
   * Finds a vehicle entry by ID across all loaded or all available manufacturers.
   */
  fun getVehicleById(id: String): VehicleCatalogEntry? {
    // Check cached brands first
    for (list in brandVehiclesCache.values) {
      list.firstOrNull { it.id == id }?.let { return it }
    }
    // If not found in cache, load remaining manufacturers
    for (mfg in getManufacturers()) {
      if (!brandVehiclesCache.containsKey(mfg.id)) {
        val list = getVehiclesForManufacturer(mfg.id)
        list.firstOrNull { it.id == id }?.let { return it }
      }
    }
    return null
  }

  /**
   * Loads all transmissions from asset files listed in manifest.
   */
  fun getAllTransmissions(): List<TransmissionCatalogEntry> {
    transmissionsCache?.let { return it }

    val transFiles = getManifest()?.transmissionFiles ?: emptyList()
    val allTransmissions = mutableListOf<TransmissionCatalogEntry>()
    val seenIds = mutableSetOf<String>()

    for (file in transFiles) {
      val loaded = loadTransmissionFile(file)
      for (tx in loaded) {
        if (!seenIds.contains(tx.id)) {
          seenIds.add(tx.id)
          allTransmissions.add(tx)
        } else {
          Log.w(TAG, "Ignorando transmissão duplicada com ID: ${tx.id}")
        }
      }
    }

    transmissionsCache = allTransmissions
    return allTransmissions
  }

  fun getTransmissionById(id: String?): TransmissionCatalogEntry? {
    if (id == null) return null
    return getAllTransmissions().firstOrNull { it.id == id }
  }

  fun getTransmissionsForVehicle(vehicle: VehicleCatalogEntry): List<TransmissionCatalogEntry> {
    val all = getAllTransmissions()
    val specific = all.filter { tx ->
      vehicle.originalTransmissionIds.contains(tx.id) ||
        tx.compatibleVehicleIds.contains(vehicle.id) ||
        (vehicle.engineFamily != null && tx.compatibleEngineFamilies.contains(vehicle.engineFamily))
    }
    if (specific.isNotEmpty()) return specific
    // Fallback: transmissions by same manufacturer
    return all.filter {
      it.manufacturer.contains(vehicle.manufacturerName, ignoreCase = true) ||
        vehicle.manufacturerName.contains(it.manufacturer, ignoreCase = true)
    }
  }

  fun getTransmissionsForManufacturer(manufacturerIdOrName: String): List<TransmissionCatalogEntry> {
    val all = getAllTransmissions()
    return all.filter {
      it.manufacturer.contains(manufacturerIdOrName, ignoreCase = true) ||
        manufacturerIdOrName.contains(it.manufacturer, ignoreCase = true) ||
        (manufacturerIdOrName.equals("vw", ignoreCase = true) && it.manufacturer.contains("Volkswagen", ignoreCase = true)) ||
        (manufacturerIdOrName.equals("gm", ignoreCase = true) && it.manufacturer.contains("Chevrolet", ignoreCase = true))
    }
  }

  /**
   * Search across all brands in the catalog.
   */
  fun searchCatalog(query: String): List<VehicleCatalogEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()

    // Ensure all brands are loaded for a search
    for (mfg in getManufacturers()) {
      getVehiclesForManufacturer(mfg.id)
    }

    val results = mutableListOf<VehicleCatalogEntry>()
    for (list in brandVehiclesCache.values) {
      for (item in list) {
        if (item.model.lowercase().contains(q) ||
          item.manufacturerName.lowercase().contains(q) ||
          (item.generation?.lowercase()?.contains(q) == true) ||
          (item.trim?.lowercase()?.contains(q) == true) ||
          item.engineDescription.lowercase().contains(q) ||
          (item.engineFamily?.lowercase()?.contains(q) == true) ||
          (item.engineCode?.lowercase()?.contains(q) == true) ||
          q in item.startYear.toString()..item.endYear.toString()
        ) {
          results.add(item)
        }
      }
    }
    return results
  }

  // --- Private Loaders & JSON Parsing ---

  private fun loadManifest() {
    try {
      val jsonStr = readAssetFile("$ASSET_BASE_DIR/catalog_manifest.json")
      if (jsonStr == null) {
        Log.e(TAG, "Não foi possível carregar o arquivo catalog_manifest.json")
        return
      }
      val json = JSONObject(jsonStr)
      val version = json.optInt("catalogVersion", 1)
      val country = json.optString("country", "BR")
      val updatedAt = json.optString("updatedAt", "")

      val mfgArray = json.optJSONArray("manufacturers") ?: JSONArray()
      val mfgs = mutableListOf<ManufacturerManifestEntry>()
      for (i in 0 until mfgArray.length()) {
        val mObj = mfgArray.optJSONObject(i) ?: continue
        val id = mObj.optString("id", "")
        val name = mObj.optString("name", "")
        val file = mObj.optString("file", "")
        if (id.isNotBlank() && file.isNotBlank()) {
          mfgs.add(ManufacturerManifestEntry(id = id, name = name, file = file))
        }
      }

      val txArray = json.optJSONArray("transmissionFiles") ?: JSONArray()
      val txFiles = mutableListOf<String>()
      for (i in 0 until txArray.length()) {
        val f = txArray.optString(i)
        if (!f.isNullOrBlank()) {
          txFiles.add(f)
        }
      }

      manifest = CatalogManifest(
        catalogVersion = version,
        country = country,
        updatedAt = updatedAt,
        manufacturers = mfgs,
        transmissionFiles = txFiles
      )
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao processar catalog_manifest.json", e)
    }
  }

  private fun loadVehicleFile(
    filename: String,
    defaultMfgId: String,
    defaultMfgName: String
  ): List<VehicleCatalogEntry> {
    val list = mutableListOf<VehicleCatalogEntry>()
    val seenIds = mutableSetOf<String>()

    try {
      val jsonStr = readAssetFile("$ASSET_BASE_DIR/$filename") ?: return emptyList()
      val array = JSONArray(jsonStr)

      for (i in 0 until array.length()) {
        try {
          val obj = array.getJSONObject(i)
          val id = obj.optString("id", "").trim()
          if (id.isEmpty()) {
            Log.w(TAG, "Registro sem ID ignorado no arquivo $filename no índice $i")
            continue
          }

          if (seenIds.contains(id)) {
            Log.w(TAG, "ID de veículo duplicado ignorado: $id")
            continue
          }

          val startYear = obj.optInt("startYear", 0)
          val endYear = obj.optInt("endYear", 0)
          if (startYear <= 0 || endYear <= 0 || startYear > endYear) {
            Log.w(TAG, "Registro com anos inválidos ignorado ($startYear-$endYear): $id")
            continue
          }

          val model = obj.optString("model", "").trim()
          if (model.isEmpty()) {
            Log.w(TAG, "Registro sem modelo ignorado: $id")
            continue
          }

          val engineDesc = obj.optString("engineDescription", "").trim()
          if (engineDesc.isEmpty()) {
            Log.w(TAG, "Registro sem engineDescription ignorado: $id")
            continue
          }

          val mfgId = obj.optString("manufacturerId", defaultMfgId)
          val mfgName = obj.optString("manufacturerName", defaultMfgName)

          val curbWeight = if (!obj.isNull("curbWeightKg")) obj.optDouble("curbWeightKg") else null
          if (curbWeight != null && (curbWeight < 300 || curbWeight > 6000)) {
            Log.w(TAG, "Peso fora dos limites razoáveis ignorado para $id: $curbWeight")
            continue
          }

          val pwrCv = if (!obj.isNull("factoryPowerCv")) obj.optDouble("factoryPowerCv") else null
          val trqKgf = if (!obj.isNull("factoryTorqueKgf")) obj.optDouble("factoryTorqueKgf") else null

          val txIds = mutableListOf<String>()
          val txArray = obj.optJSONArray("originalTransmissionIds")
          if (txArray != null) {
            for (t in 0 until txArray.length()) {
              val txId = txArray.optString(t)
              if (!txId.isNullOrBlank()) {
                txIds.add(txId)
              }
            }
          }

          val entry = VehicleCatalogEntry(
            id = id,
            manufacturerId = mfgId,
            manufacturerName = mfgName,
            model = model,
            generation = obj.optStringOrNull("generation"),
            bodyType = obj.optStringOrNull("bodyType"),
            startYear = startYear,
            endYear = endYear,
            trim = obj.optStringOrNull("trim"),
            engineFamily = obj.optStringOrNull("engineFamily"),
            engineCode = obj.optStringOrNull("engineCode"),
            engineDescription = engineDesc,
            displacementCc = if (!obj.isNull("displacementCc")) obj.optInt("displacementCc") else null,
            valves = if (!obj.isNull("valves")) obj.optInt("valves") else null,
            aspiration = obj.optStringOrNull("aspiration"),
            fuelType = obj.optStringOrNull("fuelType"),
            factoryPowerCv = pwrCv,
            factoryPowerRpm = if (!obj.isNull("factoryPowerRpm")) obj.optInt("factoryPowerRpm") else null,
            factoryTorqueKgf = trqKgf,
            factoryTorqueRpm = if (!obj.isNull("factoryTorqueRpm")) obj.optInt("factoryTorqueRpm") else null,
            curbWeightKg = curbWeight,
            drivetrain = obj.optStringOrNull("drivetrain") ?: "FWD",
            originalTransmissionIds = txIds,
            verificationStatus = VerificationStatus.fromString(obj.optStringOrNull("verificationStatus")),
            sourceName = obj.optStringOrNull("sourceName"),
            sourceReference = obj.optStringOrNull("sourceReference"),
            notes = obj.optStringOrNull("notes")
          )

          seenIds.add(id)
          list.add(entry)
        } catch (e: Exception) {
          Log.w(TAG, "Erro ao analisar registro $i no arquivo $filename", e)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao carregar arquivo de veículos $filename", e)
    }

    return list
  }

  private fun loadTransmissionFile(filename: String): List<TransmissionCatalogEntry> {
    val list = mutableListOf<TransmissionCatalogEntry>()
    val seenIds = mutableSetOf<String>()

    try {
      val jsonStr = readAssetFile("$ASSET_BASE_DIR/$filename") ?: return emptyList()
      val array = JSONArray(jsonStr)

      for (i in 0 until array.length()) {
        try {
          val obj = array.getJSONObject(i)
          val id = obj.optString("id", "").trim()
          if (id.isEmpty()) continue

          if (seenIds.contains(id)) {
            Log.w(TAG, "ID de transmissão duplicado ignorado: $id")
            continue
          }

          val mfg = obj.optString("manufacturer", "")
          val family = obj.optString("family", "")
          val displayName = obj.optString("displayName", "$mfg $family")
          val numGears = obj.optInt("numberOfGears", 5)

          val gearRatios = mutableListOf<Double?>()
          val ratiosArray = obj.optJSONArray("gearRatios")
          if (ratiosArray != null) {
            for (g in 0 until ratiosArray.length()) {
              if (ratiosArray.isNull(g)) {
                gearRatios.add(null)
              } else {
                gearRatios.add(ratiosArray.optDouble(g))
              }
            }
          }

          val finalDrive = if (!obj.isNull("finalDrive")) obj.optDouble("finalDrive") else null

          val engineFamilies = mutableListOf<String>()
          val efArray = obj.optJSONArray("compatibleEngineFamilies")
          if (efArray != null) {
            for (e in 0 until efArray.length()) {
              val ef = efArray.optString(e)
              if (!ef.isNullOrBlank()) engineFamilies.add(ef)
            }
          }

          val vehicleIds = mutableListOf<String>()
          val viArray = obj.optJSONArray("compatibleVehicleIds")
          if (viArray != null) {
            for (v in 0 until viArray.length()) {
              val vi = viArray.optString(v)
              if (!vi.isNullOrBlank()) vehicleIds.add(vi)
            }
          }

          val entry = TransmissionCatalogEntry(
            id = id,
            manufacturer = mfg,
            family = family,
            code = obj.optStringOrNull("code"),
            displayName = displayName,
            type = obj.optString("type", "MANUAL"),
            numberOfGears = numGears,
            gearRatios = gearRatios,
            finalDrive = finalDrive,
            compatibleEngineFamilies = engineFamilies,
            compatibleVehicleIds = vehicleIds,
            dynoCompatibility = DynoCompatibility.fromString(obj.optStringOrNull("dynoCompatibility")),
            verificationStatus = VerificationStatus.fromString(obj.optStringOrNull("verificationStatus")),
            sourceName = obj.optStringOrNull("sourceName"),
            sourceReference = obj.optStringOrNull("sourceReference"),
            notes = obj.optStringOrNull("notes")
          )

          seenIds.add(id)
          list.add(entry)
        } catch (e: Exception) {
          Log.w(TAG, "Erro ao analisar transmissão $i no arquivo $filename", e)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao ler arquivo de transmissões $filename", e)
    }

    return list
  }

  private fun readAssetFile(path: String): String? {
    return try {
      context.assets.open(path).use { inputStream ->
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
          reader.readText()
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao ler asset: $path", e)
      null
    }
  }

  private fun JSONObject.optStringOrNull(name: String): String? {
    if (isNull(name)) return null
    val v = optString(name, "").trim()
    return if (v.isEmpty()) null else v
  }
}
