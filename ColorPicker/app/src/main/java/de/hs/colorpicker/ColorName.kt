package de.hs.colorpicker

class ColorName {

    var name = ""
    var r = 0
    var g = 0
    var b = 0

    constructor() {}

    constructor(name: String, r: Int, g: Int, b: Int) {
        this.name = name
        this.r = r
        this.g = g
        this.b = b
    }

    private fun computeMSE(pixR: Int, pixG: Int, pixB: Int): Int {
        return (((pixR - r) * (pixR - r) + (pixG - g) * (pixG - g) + ((pixB - b)
                * (pixB - b))) / 3)
    }
    //color list
    private fun initColorList(): ArrayList<ColorName>? {
        val colorList: ArrayList<ColorName> = ArrayList<ColorName>()
        colorList.add(ColorName("Orange", 0xFF, 0x88, 0x00))
        colorList.add(ColorName("Violet", 0x88, 0x00, 0xFF))
        colorList.add(ColorName("Dark Yellow", 0x88, 0x88, 0x00))
        colorList.add(ColorName("Yellow", 0xFF, 0xFF, 0x00))
        colorList.add(ColorName("Dark Magenta", 0x88, 0x00, 0x88))
        colorList.add(ColorName("Magenta", 0xFF, 0x00, 0xFF))
        colorList.add(ColorName("Dark Cyan", 0xF0, 0x88, 0x88))
        colorList.add(ColorName("Cyan", 0xF0, 0xFF, 0xFF))
        colorList.add(ColorName("Dark Blue", 0xF0, 0xF8, 0x88))
        colorList.add(ColorName("Blue", 0x00, 0x00, 0xFF))
        colorList.add(ColorName("Dark Green", 0x00, 0x88, 0x00))
        colorList.add(ColorName("Green", 0x00, 0xFF, 0x00))
        colorList.add(ColorName("Dark Red", 0x88, 0x00, 0x00))
        colorList.add(ColorName("Red", 0xFF, 0x00, 0x00))
        colorList.add(ColorName("Grey", 0x88, 0x88, 0x88))
        colorList.add(ColorName("Black", 0x00, 0x00, 0x00))
        // Add more colors here
        return colorList
    }
    // get closes color name
    fun getColorNameFromRgb(r: Int, g: Int, b: Int): String {
        val colorList: ArrayList<ColorName>? = initColorList()
        var closestMatch: ColorName? = null
        var minMSE = Int.MAX_VALUE
        var mse: Int
        for (c in colorList!!) {
            mse = c.computeMSE(r, g, b)
            if (mse < minMSE) {
                minMSE = mse
                closestMatch = c
            }
        }
        return closestMatch?.name ?: "No matched color name."
    }

}