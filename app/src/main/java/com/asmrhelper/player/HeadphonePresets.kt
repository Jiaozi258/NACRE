package com.asmrhelper.player

/**
 * 耳机适配预设：按品牌分组的耳机型号 + 对应 10 段均衡器增益曲线。
 * 曲线为各耳机常见声学取向的近似补偿值（非厂商官方数据），
 * 通过 [EqualizerController.applyPreset] 作用到 31Hz~16kHz 十段。
 */
data class HeadphonePreset(
    val brand: String,
    val model: String,
    val values: List<Float>
) {
    fun toEqPreset(): EqPreset = EqPreset(model, values)
}

object HeadphonePresets {
    val ALL: List<HeadphonePreset> = listOf(
        HeadphonePreset("AKG", "K240 MKII", listOf(2f, 2f, 1f, 0f, 0f, 0f, 1f, 1f, 2f, 3f)),

        HeadphonePreset("Apple", "AirPods", listOf(1f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 2f, 2f)),
        HeadphonePreset("Apple", "AirPods Pro", listOf(1f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 2f, 2f)),
        HeadphonePreset("Apple", "AirPods Max", listOf(2f, 2f, 2f, 1f, 0f, 0f, 0f, 1f, 1f, 2f)),

        HeadphonePreset("Beats", "Beats Pro", listOf(4f, 4f, 3f, 2f, 1f, 0f, 0f, 1f, 1f, 1f)),

        HeadphonePreset("Bose", "QuietComfort 35 II", listOf(2f, 3f, 2f, 1f, 0f, -1f, 0f, 1f, 1f, 2f)),

        HeadphonePreset("Samsung", "EO-IG955 (AKG)", listOf(3f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 3f)),
        HeadphonePreset("Samsung", "Galaxy Buds", listOf(2f, 2f, 1f, 0f, 0f, 0f, 1f, 1f, 2f, 2f)),
        HeadphonePreset("Samsung", "Galaxy Buds+", listOf(2f, 2f, 1f, 0f, 0f, 0f, 1f, 1f, 2f, 2f)),
        HeadphonePreset("Samsung", "Galaxy Buds Live", listOf(2f, 2f, 1f, 0f, 0f, 0f, 0f, 1f, 2f, 2f)),
        HeadphonePreset("Samsung", "Galaxy Buds Pro", listOf(2f, 2f, 1f, 0f, 0f, 0f, 1f, 2f, 2f, 2f)),
        HeadphonePreset("Samsung", "Galaxy Buds2", listOf(2f, 2f, 1f, 0f, 0f, 0f, 1f, 1f, 2f, 2f)),

        HeadphonePreset("Sennheiser", "IE 60", listOf(1f, 2f, 2f, 1f, 0f, 0f, 0f, 1f, 2f, 2f)),

        HeadphonePreset("Sony", "MDR-EX15LP", listOf(3f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 3f)),
        HeadphonePreset("Sony", "WF-1000XM3", listOf(3f, 4f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 2f)),
        HeadphonePreset("Sony", "WF-1000XM4", listOf(3f, 3f, 2f, 1f, 0f, 0f, 1f, 1f, 2f, 2f)),
        HeadphonePreset("Sony", "WH-1000XM3", listOf(3f, 3f, 2f, 1f, 0f, 0f, 1f, 1f, 2f, 2f)),
        HeadphonePreset("Sony", "WH-H900N", listOf(2f, 3f, 2f, 1f, 0f, 0f, 1f, 1f, 2f, 2f)),
        HeadphonePreset("Sony", "WH-H910N", listOf(2f, 3f, 2f, 1f, 0f, 0f, 1f, 1f, 2f, 2f))
    )

    val BRANDS: List<String> = ALL.map { it.brand }.distinct()

    fun byBrand(brand: String): List<HeadphonePreset> = ALL.filter { it.brand == brand }
}
