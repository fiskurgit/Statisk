package fisk.staticsite

import fisk.staticsite.image.ImageProcessor

object Config {
    var imageConversion = ImageProcessor.ImageConversion.COLOR_SCALE
    var ditherAlgorithm = "Atkinson"
    var threshold = 128
    var maxImageWidth  = 960
}