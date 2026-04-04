package com.abhinav.otapulse.core.network

/**
 * Singleton object holding constant data for making API requests.
 */
object Data {
    val defaultHeaders = mapOf(
        "language" to "en-EN", "romVersion" to "unknown", "otaVersion" to "unknown",
        "androidVersion" to "unknown", "colorOSVersion" to "unknown", "model" to "unknown",
        "infVersion" to "1", "operator" to "unknown", "nvCarrier" to "unknown",
        "uRegion" to "unknown", "trackRegion" to "unknown", "imei" to "000000000000000",
        "imei1" to "000000000000000", "deviceId" to "0", "mode" to "client_auto",
        "channel" to "pc", "version" to "1", "Accept" to "application/json",
        "Content-Type" to "application/json", "User-Agent" to "NULL"
    )

    val defaultBody = mapOf(
        "language" to "en-EN", "romVersion" to "unknown", "otaVersion" to "unknown",
        "androidVersion" to "unknown", "colorOSVersion" to "unknown", "model" to "unknown",
        "productName" to "unknown", "operator" to "unknown", "uRegion" to "unknown",
        "trackRegion" to "unknown", "imei" to "000000000000000", "imei1" to "000000000000000",
        "mode" to "0", "registrationId" to "unknown", "deviceId" to "0", "version" to "3",
        "type" to "1", "otaPrefix" to "unknown", "isRealme" to "unknown", "time" to "0",
        "canCheckSelf" to "0"
    )

    val urls = mapOf(
        1 to mapOf(
            0 to "https://ifota.realmemobile.com/post/Query_Update",    // GL
            1 to "https://iota.coloros.com/post/Query_Update",          // CN
            2 to "https://ifota-in.realmemobile.com/post/Query_Update", // IN
            3 to "https://ifota-eu.realmemobile.com/post/Query_Update"  // EU
        ),
        2 to mapOf(
            0 to "https://component-ota-f.coloros.com/update/v3",       // GL
            1 to "https://component-ota.coloros.com/update/v3",         // CN
            2 to "https://component-ota-in.coloros.com/update/v3",      // IN
            3 to "https://component-ota-eu.coloros.com/update/v3"       // EU
        )
    )

    data class ServerConfig(val serverURL: String, val pubKey: String, val negotiationVersion: String)

    val serverParams: Map<Int, ServerConfig> = mapOf(
        0 to ServerConfig( // GL
            serverURL = "https://component-otapc-sg.allawnos.com/update/v3",
            pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkA980wxi+eTGcFDiw2I6RrUeO4jL/Aj3Yw4dNuW7tYt+O1sRTHgrzxPD9SrOqzz7G0KgoSfdFHe3JVLPN+U1waK+T0HfLusVJshDaMrMiQFDUiKajb+QKr+bXQhVofH74fjat+oRJ8vjXARSpFk4/41x5j1Bt/2bHoqtdGPcUizZ4whMwzap+hzVlZgs7BNfepo24PWPRujsN3uopl+8u4HFpQDlQl7GdqDYDj2zNOHdFQI2UpSf0aIeKCKOpSKF72KDEESpJVQsqO4nxMwEi2jMujQeCHyTCjBZ+W35RzwT9+0pyZv8FB3c7FYY9FdF/+lvfax5mvFEBd9jO+dpMQIDAQAB",
            negotiationVersion = "1615895993238"
        ),
        1 to ServerConfig( // CN
            serverURL = "https://component-otapc-cn.allawntech.com/update/v3",
            pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApXYGXQpNL7gmMzzvajHaoZIHQQvBc2cOEhJc7/tsaO4sT0unoQnwQKfNQCuv7qC1Nu32eCLuewe9LSYhDXr9KSBWjOcCFXVXteLO9WCaAh5hwnUoP/5/Wz0jJwBA+yqs3AaGLA9wJ0+B2lB1vLE4FZNE7exUfwUc03fJxHG9nCLKjIZlrnAAHjRCd8mpnADwfkCEIPIGhnwq7pdkbamZcoZfZud1+fPsELviB9u447C6bKnTU4AaMcR9Y2/uI6TJUTcgyCp+ilgU0JxemrSIPFk3jbCbzamQ6Shkw/jDRzYoXpBRg/2QDkbq+j3ljInu0RHDfOeXf3VBfHSnQ66HCwIDAQAB",
            negotiationVersion = "1615879139745"
        ),
        2 to ServerConfig( // IN
            serverURL = "https://component-otapc-in.allawnos.com/update/v3",
            pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwYtghkzeStC9YvAwOQmWylbp74Tj8hhi3f9IlK7A/CWrGbLgzz/BeKxNb45zBN8pgaaEOwAJ1qZQV5G4nProWCPOP1ro1PkemFJvw/vzOOT5uN0ADnHDzZkZXCU/knxqUSfLcwQlHXsYhNsAm7uOKjY9YXF4zWzYN0eFPkML3Pj/zg7hl/ov9clB2VeyI1/blMHFfcNA/fvqDTENXcNBIhgJvXiCpLcZqp+aLZPC5AwY/sCb3j5jTWer0Rk0ZjQBZE1AncwYvUx4mA65U59cWpTyl4c47J29MsQ66hqWv6eBHlDNZSEsQpHePUqgsf7lmO5Wd7teB8ugQki2oz1Y5QIDAQAB",
            negotiationVersion = "1615896309308"
        ),
        3 to ServerConfig( // EU
            serverURL = "https://component-otapc-eu.allawnos.com/update/v3",
            pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh8/EThsK3f0WyyPgrtXb/D0Xni6UZNppaQHUqHWo976cybl92VxmehE0ISObnxERaOtrlYmTPIxkVC9MMueDvTwZ1l0KxevZVKU0sJRxNR9AFcw6D7k9fPzzpNJmhSlhpNbt3BEepdgibdRZbacF3NWy3ejOYWHgxC+I/Vj1v7QU5gD+1OhgWeRDcwuV4nGY1ln2lvkRj8EiJYXfkSq/wUI5AvPdNXdEqwou4FBcf6mD84G8pKDyNTQwwuk9lvFlcq4mRqgYaFg9DAgpDgqVK4NTJWM7tQS1GZuRA6PhupfDqnQExyBFhzCefHkEhcFywNyxlPe953NWLFWwbGvFKwIDAQAB",
            negotiationVersion = "1615897067573"
        )
    )

    fun getServerId(serverCode: String?): Int {
        return when (serverCode?.uppercase()) {
            "GL" -> 0
            "CN" -> 1
            "IN" -> 2
            "EU" -> 3
            else -> 0
        }
    }
}
