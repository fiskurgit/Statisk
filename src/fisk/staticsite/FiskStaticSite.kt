package fisk.staticsite

import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.awt.image.BufferedImage
import java.io.File
import java.util.regex.Pattern
import javax.imageio.ImageIO
import java.awt.RenderingHints
import java.awt.Transparency
import javax.imageio.ImageWriteParam
import javax.imageio.IIOImage
import java.text.DecimalFormat
import java.io.FileOutputStream
import java.lang.StringBuilder

fun main(args: Array<String>) {
    if(args.isEmpty() || args[0] == "help"){
        Generator().help()
    }else if(args.size == 1){
        val aRef = args[0]
        Generator().build(aRef)
    }else if(args.size == 2){
        val aRef = args[0]
        val bRef = args[1]
        Generator().build(aRef, bRef)
    }else{
        Generator().die("Error: Too many arguments")
    }
}



class Generator {

    data class Link(val date: String, val title: String, val link: String)
    var links = mutableListOf<Link>()

    private var pageBytes = 0L

    private var webroot: File? = null

    companion object {
        fun l(message: String) {
            println(message)
        }

        fun d(message: String) {
            println("DEBUG: $message")
        }

        const val TEMPLATE = "_template.html"
    }

    fun help(){
        l("S T A T I S K Site Generator")
        l("")
        l("HELP - TODO")
    }

    fun die(message: String){
        l(message)
        help()
        System.exit(-1)
    }

    fun build(fileARef: String){
        build(fileARef, null)
    }

    fun build(fileARef: String, fileBRef: String?) {
        l("")
        l("S T A T I S K Site Generator")
        l("")

        val fileA = File(fileARef)
        val fileB = when {
            !fileBRef.isNullOrEmpty() -> File(fileBRef)
            else -> File("")
        }

        when {
            fileA.exists() && fileARef.toLowerCase().endsWith(".md") && fileB.exists() && fileBRef != null && fileBRef.endsWith(TEMPLATE) -> {
                //We're converting a single file with a supplied template
                d("MODE: Single index.md with supplied template")
                parseMarkdown(fileA, fileB, false)
            }
            fileA.exists() && fileARef.toLowerCase().endsWith(".md") -> {
                //Single post but look for _template.html three directories up
                d("MODE: Single index.md, no supplied template")

                val rootDir = fileA.dir().parentFile.parentFile.parentFile

                d("Looking for $TEMPLATE in $rootDir")

                val template = File(rootDir, TEMPLATE)

                when {
                    template.exists() -> parseMarkdown(fileA, template, false)
                    else -> die("Could not find _template.html, check it exists in root and post directory structure is correct")
                }

            }
            fileA.exists() && fileA.isDirectory -> {
                //The full iterative flow
                d("MODE: Full directory flow")

                val template = File(fileA, TEMPLATE)

                when {
                    template.exists() -> {
                        webroot = fileA
                        fullBuild(fileA, template)

                        l("Post conversion complete, building menu...")

                        val index = File(webroot, "index.md")

                        if(index.exists()){
                            parseMarkdown(index, template, true)

                            val linkBuilder = StringBuilder()

                            links.forEach {link ->
                                d("Add link: ${link.date} title: ${link.title} href: ${link.link}")

                                linkBuilder.append("<a href=\"")
                                linkBuilder.append("${link.link}\">")
                                linkBuilder.append("${link.date} ${link.title}</href><br>")
                            }

                            val htmlIndex = File(index.path.replace(".md", ".html"))

                            var content = htmlIndex.readText()

                            if(content.contains("{{ posts }}")){
                                content = content.replace("{{ posts }}", linkBuilder.toString())

                                htmlIndex.writeText(content)

                                l("All posts converted and index updated - DONE")
                                System.exit(0)
                            }else{
                                l("Could not find '{{ posts }}', skipping post index - DONE")
                                System.exit(0)
                            }
                        }else{
                            l("No root index.md, skipping menu  - DONE")
                            System.exit(0)
                        }
                    }
                    else -> die("Could not find _template.html, check it exists in the website root and you're passing the correct path")
                }
            }
            else -> die("Bad arguments, check your file references")
        }
    }

    private fun fullBuild(root: File, template: File){
        l("Scanning project")
        root.listFiles().forEach { fileInRoot ->
            if(fileInRoot.isDirectory){
                d("Found directory: $fileInRoot")

                if(fileInRoot.name.isYear()){
                    scanMonths(fileInRoot, template)
                }
            }

        }
    }

    private fun scanMonths(root: File, template: File){
        l("Processing year: ${root.name}")
        root.listFiles().forEach { fileInRoot ->
            when {
                fileInRoot.isDirectory && fileInRoot.name.isMonthOrDay() -> scanDays(fileInRoot, template)
            }
        }
    }

    private fun scanDays(root: File, template: File){
        l("Processing month: ${root.name}")
        root.listFiles().forEach { fileInRoot ->
            if(fileInRoot.isDirectory && fileInRoot.name.isMonthOrDay()){
                l("Processing day: ${fileInRoot.name}")
                fileInRoot.listFiles().forEach {dayFile ->
                    l("Inspecting file: " + dayFile.name)
                    if(dayFile.name.endsWith(".md")){
                        l("Found markdown: ${dayFile.path}")
                        parseMarkdown(dayFile, template, false)
                    }
                }
            }
        }
    }

    private fun parseMarkdown(mdFile: File, template: File, isIndex: Boolean){
        l("parsing Markdown...")

        val sourceMd = mdFile.readText()

        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(sourceMd)
        var convertedHtml = HtmlGenerator(sourceMd, parsedTree, flavour).generateHtml()

        convertedHtml = convertedHtml.replace("<body>", "")
        convertedHtml = convertedHtml.replace("</body>", "")
        convertedHtml = convertImages(mdFile.dir(), convertedHtml)

        val templateHtml = template.readText()

        var output = templateHtml.replace("{{ content }}", convertedHtml)

        //Update images for css
        output = output.replace("<p><img src=\"", "<p class=\"img\"><img src=\"")

        //Extract title from Markdown
        val firstTitleNode = parsedTree.children.firstOrNull { it.type.name =="ATX_1" }
        val title = when {
            firstTitleNode != null -> sourceMd.substring(firstTitleNode!!.startOffset + 1, firstTitleNode.endOffset).trim()
            else -> ""
        }
        output = output.replace("{{ title }}", title)


        pageBytes += output.toByteArray().size
        output = output.replace("{{ page_size }}", "Page size including images: ${readableFileSize(pageBytes)}")

        val outputFile = File(mdFile.dir(), mdFile.nameWithoutExtension + ".html")
        outputFile.writeText(output, Charsets.UTF_8)

        l("${mdFile.name} converted to ${outputFile.name}")
        d(outputFile.absolutePath)

        if(!isIndex) {
            val hrefLink = "." + outputFile.absolutePath.replace(webroot!!.absolutePath, "")
            d("hrefLink: $hrefLink")


            val dateLabel = outputFile.dateLabel()
            val link = Link(dateLabel, title, hrefLink)
            links.add(link)

            l("")
        }

        //reset byte counter
        pageBytes = 0L

        //ends
    }

    private fun readableFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("pageBytes", "kb", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / Math.pow(
                1024.0,
                digitGroups.toDouble()
            )
        ) + " " + units[digitGroups]
    }

    private fun convertImages(saveDir: File, _html: String): String{
        var html = _html
        l("Looking for image tags")
        val imagesPattern= Pattern.compile("(?:<img[^>]*src=\")([^\"]*)", Pattern.CASE_INSENSITIVE)
        val imagesMatcher = imagesPattern.matcher(html)

        val images = mutableListOf<String>()

        while (imagesMatcher.find()) {
            val image = imagesMatcher.group(1)
            l("Image found: $image")
            images.add(image)
        }

        for(image in images){
            val converted = convertImage(saveDir, image)
            pageBytes += fileSize(converted)
            html = html.replace(image, converted)
        }

        return html
    }

    private fun fileSize(fileRef: String): Long{
        val file = File(fileRef)
        return file.length()
    }

    private fun convertImage(saveDir: File, source: String): String{
        val sourceImage = ImageIO.read(File(source))

        val resized = if(sourceImage.width > 800){resize(sourceImage, 800)}else{sourceImage}
        val destination = BufferedImage(resized.width, resized.height, BufferedImage.TYPE_BYTE_GRAY)
        val destinationImpl = FilterImageImpl(destination)

        Filter.Filter8By48ayer().threshold(255).process(FilterImageImpl(resized), destinationImpl)

        val outputFile = File(saveDir, source.substring(0, source.lastIndexOf(".")) + "_dithered.png")

        //ImageIO.write(destinationImpl.image, "png", outputFile)

        val writer = ImageIO.getImageWritersByFormatName("png").next()
        val param = writer?.defaultWriteParam

        if (param!= null && param.canWriteCompressed()) {
            d("canWriteCompressed: true")
            param.compressionMode = ImageWriteParam.MODE_EXPLICIT
            param.compressionQuality = 0.0f//favour filesize over quality
        }else{
            d("canWriteCompressed: false")
        }

        val os = FileOutputStream(outputFile)
        val ios = ImageIO.createImageOutputStream(os)
        writer?.output = ios
        writer?.write(null, IIOImage(destinationImpl.image, null, null), param)

        return outputFile.name
    }

    private fun resize(src: BufferedImage, targetSize: Int): BufferedImage {
        var targetWidth = targetSize
        var targetHeight = targetSize
        val ratio = src.height.toFloat() / src.width.toFloat()
        if (ratio <= 1) { //square or landscape-oriented image
            targetHeight = Math.ceil((targetWidth.toFloat() * ratio).toDouble()).toInt()
        } else { //portrait image
            targetWidth = Math.round(targetHeight.toFloat() / ratio)
        }
        val bi = BufferedImage(targetWidth, targetHeight,
            if (src.transparency == Transparency.OPAQUE) BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
        )
        val g2d = bi.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(src, 0, 0, targetWidth, targetHeight, null)
        g2d.dispose()
        return bi
    }

    //Extensions
    private fun File.dir(): File {
        val dirPath = this.absolutePath.substring(0, this.absolutePath.lastIndexOf("/"))
        return File(dirPath)
    }

    //Expect path in form: /Users/670m/pi/fisk_solar_website/2019/07/25/
    private fun File.dateLabel(): String {
        if(name.endsWith(".html")){
            var path = this.path.substring(0, this.path.lastIndexOf("/"))

            val day = path.takeLast(2)

            path = path.substring(0, path.lastIndexOf("/"))

            val month = path.takeLast(2)

            path = path.substring(0, path.lastIndexOf("/"))

            val year = path.takeLast(4)

            val dateLabel = "$day/$month/$year"

            d("date label: $dateLabel")

            return dateLabel
        }else{
            return "ERROR"
        }
    }

    private fun String.isYear(): Boolean {
        return when {
            this.length == 4 -> {
                val i = this.toIntOrNull()
                when(i) {
                    null -> false
                    else -> true
                }
            }
            else -> false
        }
    }

    private fun String.isMonthOrDay(): Boolean {
        return when {
            this.length == 2 -> {
                val i = this.toIntOrNull()
                when(i) {
                    null -> false
                    else -> true
                }
            }
            else -> false
        }
    }

    class FilterImageImpl(val image: BufferedImage): FilterImage() {

        override var width: Int
            get() = image.width

            @Suppress("UNUSED_PARAMETER")
            set(value) {
                //unused'
            }

        override var height: Int
            get() = image.height

            @Suppress("UNUSED_PARAMETER")
            set(value) {
                //unused
            }

        override fun getPixel(x: Int, y: Int): Int {
            return image.getRGB(x, y)
        }

        override fun setPixel(x: Int, y: Int, colour: Int) {
            image.setRGB(x, y, colour)
        }
    }
}


