package com.abhinav.otapulse.ota.engine

import com.abhinav.otapulse.ota.network.OtaApi
import com.abhinav.otapulse.core.model.OtaRequest
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.ota.engine.OtaRepository
import com.abhinav.otapulse.core.network.Request
import com.abhinav.otapulse.core.common.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtaRepositoryImpl @Inject constructor(
    private val otaApi: OtaApi
) : OtaRepository {

    override suspend fun fetchOtaUpdate(request: OtaRequest): Result<List<OtaUpdate>> =
        withContext(Dispatchers.IO) {

            val requestHelper = Request(
                reqVersion = request.version,
                model = request.model,
                firmwareVersion = request.firmwareVersion,
                region = request.region,
                ruiVersion = request.ruiVersion,
                imei0 = request.imei0,
                beta = request.beta,
                deviceId = request.deviceId,
                nvIdentifier = request.nvIdentifier,
                imei1 = request.imei1,
                language = request.language
            )

            requestHelper.prepare()

            otaApi.executeOtaRequest(requestHelper)
                .map { components -> components.map { it.toDomain() } }
        }
}
