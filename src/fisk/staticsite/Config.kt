package fisk.staticsite

import fisk.staticsite.image.ImageProcessor
import java.awt.Color

data class Config(
    var imageConversion: ImageProcessor.ImageConversion = ImageProcessor.ImageConversion.COLOR_SCALE,
    var imageFormat: ImageProcessor.ImageSaveFormat = ImageProcessor.ImageSaveFormat.PNG,

    //Dither items
    var ditherFilter: Filter = Filter.FilterAtkinson(),
    var foregroundColor: Color = Color(0, 0, 0),
    var threshold: Int = 128,
    var transparentBackground: Boolean = true,


    var maxImageWidth: Int = 960,
    var gzip: Boolean = false
)