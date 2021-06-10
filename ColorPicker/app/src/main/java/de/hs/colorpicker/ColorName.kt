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
        colorList.add(ColorName("White", 210, 210, 210))
        colorList.add(ColorName("Orange", 220, 0x88, 48))
        colorList.add(ColorName("Brown", 0x96, 0x4B, 48))
        colorList.add(ColorName("Violet", 0x88, 48, 220))
        colorList.add(ColorName("Dark Yellow", 110, 110, 48))
        colorList.add(ColorName("Yellow", 200, 170, 48))
        colorList.add(ColorName("Dark Magenta", 110, 48, 110))
        colorList.add(ColorName("Magenta", 220, 48, 220))
        colorList.add(ColorName("Dark Cyan", 48, 110, 110))
        colorList.add(ColorName("Cyan", 48, 220, 220))
        colorList.add(ColorName("Dark Blue", 48, 48, 110))
        colorList.add(ColorName("Blue", 48, 48, 220))
        colorList.add(ColorName("Dark Green", 48, 110, 48))
        colorList.add(ColorName("Green", 48, 220, 48))
        colorList.add(ColorName("Dark Red", 110, 48, 48))
        colorList.add(ColorName("Red", 220, 48, 48))
        //colorList.add(ColorName("Grey", 0x88, 0x88, 0x88))
        colorList.add(ColorName("Black", 48, 48, 48))
        // Add more colors here
        return colorList
    }
    // get closes color name
     fun getColorNameFromRgb(r: Int, g: Int, b: Int): String {

        if (r > 50 && r < 160 && Math.abs(r-g) < 10 &&  Math.abs(r-b) < 10 && Math.abs(g-b)  < 10 ) {
            return "Grey"
        }

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