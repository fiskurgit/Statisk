package fisk.staticsite

import fisk.staticsite.image.ImageProcessor

object Config {
    var imageConversion = ImageProcessor.ImageConversion.COLOR_SCALE
    var imageFormat = ImageProcessor.ImageSaveFormat.PNG
    var ditherFilter: Filter = Filter.FilterAtkinson()
    var threshold = 128
    var maxImageWidth  = 960
    var gzip: Boolean = false
}