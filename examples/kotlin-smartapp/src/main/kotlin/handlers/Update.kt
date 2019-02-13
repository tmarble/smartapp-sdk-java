package app.handlers

import com.smartthings.sdk.client.ApiClient
import com.smartthings.sdk.client.methods.SubscriptionsApi
import com.smartthings.sdk.client.models.DeviceSubscriptionDetail
import com.smartthings.sdk.client.models.SubscriptionRequest
import com.smartthings.sdk.client.models.SubscriptionSource
import com.smartthings.sdk.smartapp.core.Response
import com.smartthings.sdk.smartapp.core.extensions.UpdateHandler
import com.smartthings.sdk.smartapp.core.models.ConfigEntry
import com.smartthings.sdk.smartapp.core.models.ExecutionRequest
import com.smartthings.sdk.smartapp.core.models.ExecutionResponse
import com.smartthings.sdk.smartapp.core.models.UpdateResponseData

class Update(val api: ApiClient) : UpdateHandler {

    override fun handle(request: ExecutionRequest?): ExecutionResponse {
        // If not null, execute and return ExecutionResponse()
        request?.let {
            it.updateData.run {
                // Build API client
                val subscriptionsApi = api.buildClient(SubscriptionsApi::class.java)
                val auth = "Bearer ${this.authToken}"

                // Clear subscriptions to re-add them
                subscriptionsApi.deleteAllSubscriptions(this.installedApp.installedAppId, auth, emptyMap())

                // Iterate all devices returned in the InstalledApp's config for key "selectedSwitches"
                // That key is defined in Configuration.kt
                val devices = this.installedApp.config["selectedSwitches"]
                devices?.forEach { switchesConfig ->
                    if (switchesConfig.valueType == ConfigEntry.ValueTypeEnum.DEVICE) {
                        val deviceId = switchesConfig.deviceConfig.deviceId
                        val componentId = switchesConfig.deviceConfig.componentId
                        val subscriptionRequest = SubscriptionRequest().apply {
                            sourceType = SubscriptionSource.DEVICE
                            device = DeviceSubscriptionDetail().apply {
                                this.deviceId = deviceId
                                this.componentId = componentId
                                this.capability = "switch"
                                this.attribute = "switch"
                                this.isStateChangeOnly = true
                                this.value = "*"
                            }
                        }

                        // Create subscription for the device
                        val subscription = subscriptionsApi.saveSubscription(this.installedApp.installedAppId, auth, subscriptionRequest)
                        println("Creating subscription for ${subscription.id}")
                    }
                }
            }
            // Server expects empty Update response data
            println("Responding to the server")
            return Response.ok(UpdateResponseData())
        } ?: return Response.notFound()
        // If null, return ExecutionResponse of "not found"
    }
}
