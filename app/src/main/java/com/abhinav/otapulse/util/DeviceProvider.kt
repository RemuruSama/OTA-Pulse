package com.abhinav.otapulse.util

/**
 * Defines a contract for classes that provide a list of predefined devices.
 *
 * This interface is a key part of the app's modular architecture. By having each
 * device-specific file implement this interface, the [DeviceCatalog] can easily
 * aggregate all devices from different sources without being tightly coupled to them.
 * This makes the device list highly scalable and easy to maintain.
 */
interface DeviceProvider {
    /**
     * Returns a list of [PredefinedDevice] objects.
     *
     * @return A list of devices provided by the implementing class.
     */
    fun getDevices(): List<PredefinedDevice>
}
