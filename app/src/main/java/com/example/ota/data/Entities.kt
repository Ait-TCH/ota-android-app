package com.example.ota.data

import org.json.JSONObject

/** 实体（App 用） */
data class Entity(
    val entityId: String,
    val friendlyName: String,
    val domain: String,
    val state: String,
    val brightness: Int?,
    val rgbColor: List<Int>?,
    val supportedBrightness: Boolean,
    val supportedColor: Boolean,
    val supportedColorTemp: Boolean,
    val colorTemp: Int?,
    val minMireds: Int?,
    val maxMireds: Int?
)

data class ButtonMapping(
    val btnIndex: Int,
    val gpio: Int,
    val devices: String
)
