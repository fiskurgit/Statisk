package fisk.staticsite

import java.awt.Color

abstract class FilterImage {
    abstract var width: Int
    abstract var height: Int

    abstract fun getPixel(x: Int, y: Int): Int
    abstract fun setPixel(x: Int, y: Int, colour: Color)

}

abstract class Filter {

    companion object {
        fun find(fileRef: String): Filter? {
            val fileRefLower = fileRef.toLowerCase()
            return when {
                fileRefLower.contains("2by2bayer") -> Filter2By2Bayer()
                fileRefLower.contains("3by3bayer") -> Filter3By3Bayer()
                fileRefLower.contains("4by4bayer") -> Filter4By4Bayer()
                fileRefLower.contains("5by3bayer") -> Filter5By3Bayer()
                fileRefLower.contains("8by8bayer") -> Filter8By48Bayer()
                fileRefLower.contains("floydsteinberg") -> FilterFloydSteinberg()
                fileRefLower.contains("falsefloydsteinberg") -> FilterFalseFloydSteinberg()
                fileRefLower.contains("newspaperhalftone") -> FilterNewspaperHalftone()
                fileRefLower.contains("jarvisjudiceninke") -> FilterJarvisJudiceNinke()
                fileRefLower.contains("sierra") -> FilterSierra()
                fileRefLower.contains("sierralite") -> FilterSierraLite()
                fileRefLower.contains("tworowsierra") -> FilterTwoRowSierra()
                fileRefLower.contains("burkes") -> FilterBurkes()
                fileRefLower.contains("atkinson") -> FilterAtkinson()
                fileRefLower.contains("stucki") -> FilterStucki()
                fileRefLower.contains("errordif") -> FilterLeftToRightErrorDiffusion()
                fileRefLower.contains("threshold") -> FilterThreshold()
                fileRefLower.contains("random") -> FilterRandom()
                else -> null
            }
        }

        var threshold = 128

        var backgroundColor = Color(255, 255, 255, 0)
    }

    abstract fun process(source: FilterImage, destination: FilterImage)

    fun threshold(value: Int): Filter {
        threshold = value
        return this
    }


    class FilterError : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {
            System.out.println("Filter name not recognised")


        }
    }

    class Filter2By2Bayer : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val matrix = arrayOf(
                intArrayOf(1, 3),
                intArrayOf(4, 2)
            )

            for (y in 0 until height) {
                for (x in 0 until width) {

                    var gray = source.getPixel(x, y) shr 16 and 0xFF

                    gray += gray * matrix[x % 2][y % 2] / 5

                    when {
                        gray < threshold -> destination.setPixel(x, y, Generator.config.foregroundColor)
                        else -> if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                    }
                }
            }

            

        }
    }

    class Filter3By3Bayer : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val matrix = arrayOf(
                intArrayOf(3, 7, 4),
                intArrayOf(6, 1, 9),
                intArrayOf(2, 8, 5)
            )

            for (y in 0 until height) {
                for (x in 0 until width) {

                    var gray = source.getPixel(x, y) shr 16 and 0xFF
                    gray += gray * matrix[x % 3][y % 3] / 10

                    when {
                        gray < threshold -> destination.setPixel(x, y, Generator.config.foregroundColor)
                        else -> if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                    }
                }
            }
        }
    }

    class Filter4By4Bayer : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val matrix = arrayOf(
                intArrayOf(1, 9, 3, 11),
                intArrayOf(13, 5, 15, 7),
                intArrayOf(4, 12, 2, 10),
                intArrayOf(16, 8, 14, 6)
            )


            for (y in 0 until height) {
                for (x in 0 until width) {

                    var gray = source.getPixel(x, y) shr 16 and 0xFF
                    gray += gray * matrix[x % 4][y % 4] / 17

                    when {
                        gray < threshold -> destination.setPixel(x, y, Generator.config.foregroundColor)
                        else -> if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                    }
                }
            }
        }
    }

    class Filter8By48Bayer : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val matrix = arrayOf(
                intArrayOf(1, 49, 13, 61, 4, 52, 16, 64),
                intArrayOf(33, 17, 45, 29, 36, 20, 48, 32),
                intArrayOf(9, 57, 5, 53, 12, 60, 8, 56),
                intArrayOf(41, 25, 37, 21, 44, 28, 40, 24),
                intArrayOf(3, 51, 15, 63, 2, 50, 14, 62),
                intArrayOf(35, 19, 47, 31, 34, 18, 46, 30),
                intArrayOf(11, 59, 7, 55, 10, 58, 6, 54),
                intArrayOf(43, 27, 39, 23, 42, 26, 38, 22)
            )

            for (y in 0 until height) {
                for (x in 0 until width) {

                    var gray = source.getPixel(x, y) shr 16 and 0xFF
                    gray += gray * matrix[x % 8][y % 8] / 65

                    when {
                        gray < threshold -> destination.setPixel(x, y, Generator.config.foregroundColor)
                        else -> if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                    }
                }
            }
        }
    }

    class Filter5By3Bayer : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val matrix = arrayOf(
                intArrayOf(9, 3, 0, 6, 12),
                intArrayOf(10, 4, 1, 7, 13),
                intArrayOf(11, 5, 2, 8, 14)
            )

            for (y in 0 until height) {
                for (x in 0 until width) {

                    var gray = source.getPixel(x, y) shr 16 and 0xFF

                    //Horizontal:
                    gray += gray * matrix[x % 3][y % 5] / 16

                    //Vertical:
                    //gray += gray * matrix[y % 3][x % 5] / 16

                    when {
                        gray < threshold -> destination.setPixel(x, y, Generator.config.foregroundColor)
                        else -> if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                    }
                }
            }
        }
    }

    class FilterNewspaperHalftone : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val matrix = arrayOf(
                intArrayOf(24, 10, 12, 26, 35, 47, 49, 37),
                intArrayOf(8, 0, 2, 14, 45, 59, 61, 51),
                intArrayOf(22, 6, 4, 16, 43, 57, 63, 53),
                intArrayOf(30, 20, 18, 28, 33, 41, 55, 39),
                intArrayOf(34, 46, 48, 36, 25, 11, 13, 27),
                intArrayOf(44, 58, 60, 50, 9, 1, 3, 15),
                intArrayOf(42, 56, 62, 52, 23, 7, 5, 17),
                intArrayOf(32, 40, 54, 38, 31, 21, 19, 29)
            )

            for (y in 0 until height) {
                for (x in 0 until width) {

                    var gray = source.getPixel(x, y) shr 16 and 0xFF

                    gray += gray * matrix[x % 8][y % 8] / 65

                    when {
                        gray < threshold -> destination.setPixel(x, y, Generator.config.foregroundColor)
                        else -> if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                    }
                }
            }
        }
    }

    class FilterFloydSteinberg : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val errors = Array(width) { IntArray(height) }

            for (y in 0 until height - 1) {
                for (x in 1 until width - 1) {

                    val gray = source.getPixel(x, y) shr 16 and 0xFF
                    var error: Int

                    when {
                        gray + errors[x][y] < threshold -> {
                            error = gray + errors[x][y]
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            error = gray + errors[x][y] - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }

                    errors[x + 1][y] += 7 * error / 16
                    errors[x - 1][y + 1] += 3 * error / 16
                    errors[x][y + 1] += 5 * error / 16
                    errors[x + 1][y + 1] += 1 * error / 16
                }
            }
        }
    }

    class FilterJarvisJudiceNinke : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {


            val width = source.width
            val height = source.height

            val errors = Array(width) { IntArray(height) }

            for (y in 0 until height - 2) {
                for (x in 2 until width - 2) {

                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    var error: Int

                    when {
                        gray + errors[x][y] < threshold -> {
                            error = gray + errors[x][y]
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            error = gray + errors[x][y] - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }

                    errors[x + 1][y] += 7 * error / 48
                    errors[x + 2][y] += 5 * error / 48

                    errors[x - 2][y + 1] += 3 * error / 48
                    errors[x - 1][y + 1] += 5 * error / 48
                    errors[x][y + 1] += 7 * error / 48
                    errors[x + 1][y + 1] += 5 * error / 48
                    errors[x + 2][y + 1] += 3 * error / 48

                    errors[x - 2][y + 2] += 1 * error / 48
                    errors[x - 1][y + 2] += 3 * error / 48
                    errors[x][y + 2] += 5 * error / 48
                    errors[x + 1][y + 2] += 3 * error / 48
                    errors[x + 2][y + 2] += 1 * error / 48
                }
            }
        }
    }

    class FilterSierra : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val errors = Array(width) { IntArray(height) }
            for (y in 0 until height - 2) {
                for (x in 2 until width - 2) {

                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    var error: Int

                    when {
                        gray + errors[x][y] < threshold -> {
                            error = gray + errors[x][y]
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            error = gray + errors[x][y] - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }

                    errors[x + 1][y] += 5 * error / 32
                    errors[x + 2][y] += 3 * error / 32

                    errors[x - 2][y + 1] += 2 * error / 32
                    errors[x - 1][y + 1] += 4 * error / 32
                    errors[x][y + 1] += 5 * error / 32
                    errors[x + 1][y + 1] += 4 * error / 32
                    errors[x + 2][y + 1] += 2 * error / 32

                    errors[x - 1][y + 2] += 2 * error / 32
                    errors[x][y + 2] += 3 * error / 32
                    errors[x + 1][y + 2] += 2 * error / 32
                }
            }
        }
    }

    class FilterTwoRowSierra : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val errors = Array(width) { IntArray(height) }

            for (y in 0 until height - 1) {
                for (x in 2 until width - 2) {
                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    var error: Int

                    when {
                        gray + errors[x][y] < threshold -> {
                            error = gray + errors[x][y]
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            error = gray + errors[x][y] - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }

                    errors[x + 1][y] += 4 * error / 16
                    errors[x + 2][y] += 3 * error / 16

                    errors[x - 2][y + 1] += 1 * error / 16
                    errors[x - 1][y + 1] += 2 * error / 16
                    errors[x][y + 1] += 3 * error / 16
                    errors[x + 1][y + 1] += 2 * error / 16
                    errors[x + 2][y + 1] += 1 * error / 16
                }
            }
        }
    }

    //Incremented by 2X2 each iteration
    class FilterSierraLite : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val errors = Array(width) { IntArray(height) }

            var y = 0
            while (y < height - 1) {
                var x = 1
                while (x < width - 1) {
                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    var error: Int

                    when {
                        gray + errors[x][y] < threshold -> {
                            error = gray + errors[x][y]
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            error = gray + errors[x][y] - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }


                    errors[x + 1][y] += 2 * error / 4

                    errors[x - 1][y + 1] += 1 * error / 4
                    errors[x][y + 1] += 1 * error / 4

                    x += 2
                }
                y += 2
            }
        }
    }

    class FilterAtkinson : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val errors = Array(width) { IntArray(height) }

            for (y in 0 until height - 2) {
                for (x in 1 until width - 2) {
                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    var error: Int

                    when {
                        gray + errors[x][y] < threshold -> {
                            error = gray + errors[x][y]
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            error = gray + errors[x][y] - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }

                    errors[x + 1][y] += error / 8
                    errors[x + 2][y] += error / 8

                    errors[x - 1][y + 1] += error / 8
                    errors[x][y + 1] += error / 8
                    errors[x + 1][y + 1] += error / 8

                    errors[x][y + 2] += error / 8
                }
            }
        }
    }

    class FilterStucki : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val errors = Array(width) { IntArray(height) }
            for (y in 0 until height - 2) {
                for (x in 2 until width - 2) {
                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    var error: Int

                    when {
                        gray + errors[x][y] < threshold -> {
                            error = gray + errors[x][y]
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            error = gray + errors[x][y] - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }

                    errors[x + 1][y] += 8 * error / 42
                    errors[x + 2][y] += 4 * error / 42

                    errors[x - 2][y + 1] += 2 * error / 42
                    errors[x - 1][y + 1] += 4 * error / 42
                    errors[x][y + 1] += 8 * error / 42
                    errors[x + 1][y + 1] += 4 * error / 42
                    errors[x + 2][y + 1] += 2 * error / 42

                    errors[x - 2][y + 2] += 1 * error / 42
                    errors[x - 1][y + 2] += 2 * error / 42
                    errors[x][y + 2] += 4 * error / 42
                    errors[x + 1][y + 2] += 2 * error / 42
                    errors[x + 2][y + 2] += 1 * error / 42
                }
            }
        }
    }

    class FilterBurkes : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val errors = Array(width) { IntArray(height) }
            for (y in 0 until height - 1) {
                for (x in 2 until width - 2) {

                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    var error: Int

                    when {
                        gray + errors[x][y] < threshold -> {
                            error = gray + errors[x][y]
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            error = gray + errors[x][y] - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }

                    errors[x + 1][y] += 8 * error / 32
                    errors[x + 2][y] += 4 * error / 32

                    errors[x - 2][y + 1] += 2 * error / 32
                    errors[x - 1][y + 1] += 4 * error / 32
                    errors[x][y + 1] += 8 * error / 32
                    errors[x + 1][y + 1] += 4 * error / 32
                    errors[x + 2][y + 1] += 2 * error / 32
                }
            }
        }
    }

    class FilterFalseFloydSteinberg : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            val errors = Array(width) { IntArray(height) }
            for (y in 0 until height - 1) {
                for (x in 1 until width - 1) {

                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    var error: Int

                    when {
                        gray + errors[x][y] < threshold -> {
                            error = gray + errors[x][y]
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            error = gray + errors[x][y] - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }

                    errors[x + 1][y] += 3 * error / 8
                    errors[x][y + 1] += 3 * error / 8
                    errors[x + 1][y + 1] += 2 * error / 8
                }
            }
        }
    }

    class FilterLeftToRightErrorDiffusion : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            for (y in 0 until height) {
                var error = 0

                for (x in 0 until width) {

                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    var delta: Int

                    when {
                        gray + error < threshold -> {
                            delta = gray
                            destination.setPixel(x, y, Generator.config.foregroundColor)
                        }
                        else -> {
                            delta = gray - 255
                            if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                        }
                    }

                    if (Math.abs(delta) < 10) delta = 0

                    error += delta
                }
            }
        }
    }

    class FilterRandom : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            for (y in 0 until height) {

                for (x in 0 until width) {

                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    val threshold = (Math.random() * 1000).toInt() % 256

                    when {
                        gray < threshold -> destination.setPixel(x, y, Generator.config.foregroundColor)
                        else -> if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                    }
                }
            }
        }
    }

    class FilterThreshold : Filter() {
        override fun process(source: FilterImage, destination: FilterImage) {

            val width = source.width
            val height = source.height

            for (y in 0 until height) {
                for (x in 0 until width) {

                    val gray = source.getPixel(x, y) shr 16 and 0xFF

                    when {
                        gray < threshold -> destination.setPixel(x, y, Generator.config.foregroundColor)
                        else -> if(!Generator.config.transparentBackground) destination.setPixel(x, y, backgroundColor)
                    }
                }
            }
        }
    }
}