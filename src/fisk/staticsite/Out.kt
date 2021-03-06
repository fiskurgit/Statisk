package fisk.staticsite

object Out {

    private var loggingOn = false

    fun welcome(){
        l("")
        l("S T A T I S K Site Generator")
        l("")
    }

    fun help(){
        l("S T A T I S K Site Generator - HELP")
        l("")
        l("USAGE - TODO")

        l("Available dithering algorithms:")
        listDitherAlgorithms()

        System.exit(0)
    }

    private fun listDitherAlgorithms(){
        l("2by2Bayer")
        l("3by3Bayer")
        l("4by4Bayer")
        l("5by3Bayer")
        l("8by8Bayer")
        l("FloydSteinberg")
        l("FalseFloydSteinberg")
        l("NewspaperHalftone")
        l("JarvisJudiceNinke")
        l("Sierra")
        l("SierraLite")
        l("TwoRowSierra")
        l("Burkes")
        l("Atkinson")
        l("Stucki")
        l("ErrorDif")
        l("Threshold")
        l("Random")
    }

    fun l(message: String) {
        if(loggingOn) println(message)
    }

    fun ll(message: String) {
        println(message)
    }

    fun d(message: String) {
        if(loggingOn) println("DEBUG: $message")
    }

    fun die(message: String) {
        l(message)
        System.exit(-1)
    }
}