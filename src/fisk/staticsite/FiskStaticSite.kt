package fisk.staticsite

import org.intellij.markdown.ast.ASTNode
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
import javax.imageio.plugins.jpeg.JPEGImageWriteParam
import javax.imageio.IIOImage
import javax.imageio.stream.FileImageOutputStream
import java.text.DecimalFormat



//Build issues resolved here: https://github.com/valich/intellij-markdown/issues/18

fun main(args: Array<String>) {
    if(args.isEmpty() || args[0] == "help"){
        Generator().help()
    }else{
        val inMarkdown = args[0]
        val template = args[1]
        Generator().build(inMarkdown, template)
    }
}

class Generator {

    private var bytes = 0L

    companion object {
        fun out(message: String) {
            System.out.println(message)
        }
    }

    fun help(){
        out("F I S K Static Site Generator")
        out("")
        out("HELP")
    }

    fun build(markdownFileRef: String, templateFileRef: String) {
        out("")
        out("F I S K Static Site Generator")
        out("")

        val mdFile = File(markdownFileRef)
        val templateFile = File(templateFileRef)

        if(mdFile.exists() && markdownFileRef.toLowerCase().endsWith(".md") && templateFile.exists()){
            parseMarkdown(mdFile, templateFile)
        }else{
            if(!mdFile.exists()) {
                out("Error: $markdownFileRef does not exist or is not a Markdown file")
            }
            if(!templateFile.exists()) {
                out("Error: $templateFileRef does not exist")
            }

            System.exit(-1)
        }
    }

    private fun parseMarkdown(mdFile: File, template: File){
        out("parsing Markdown...")

        val sourceMd = mdFile.readText()


        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(sourceMd)
        var convertedHtml = HtmlGenerator(sourceMd, parsedTree, flavour).generateHtml()

        convertedHtml = convertedHtml.replace("<body>", "")
        convertedHtml = convertedHtml.replace("</body>", "")
        convertedHtml = convertImages(mdFile.dir(), convertedHtml)

        val templateHtml = template.readText()


        var output = templateHtml.replace("{{ content }}", convertedHtml)

        //Extract title from Markdown
        val firstTitleNode = parsedTree.children.firstOrNull { it.type.name =="ATX_1" }
        val title = when {
            firstTitleNode != null -> sourceMd.substring(firstTitleNode!!.startOffset + 1, firstTitleNode.endOffset).trim()
            else -> ""
        }
        output = output.replace("{{ title }}", title)


        bytes += output.toByteArray().size
        output = output.replace("{{ page_size }}", "Page size including images: ${readableFileSize(bytes)}")

        val outputFile = File(mdFile.dir(), mdFile.nameWithoutExtension + ".html")
        outputFile.writeText(output, Charsets.UTF_8)

        out("${mdFile.name} converted to ${outputFile.name}")
    }

    fun readableFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("bytes", "kb", "MB", "GB", "TB")
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
        out("Find images in: \n$html")
        out("")
        out("")
        val imagesPattern= Pattern.compile("(?:<img[^>]*src=\")([^\"]*)",Pattern.CASE_INSENSITIVE)
        val imagesMatcher = imagesPattern.matcher(html)

        val images = mutableListOf<String>()

        while (imagesMatcher.find()) {
            val image = imagesMatcher.group(1)
            out("Image found: $image")
            images.add(image)
        }

        for(image in images){
            val converted = convertImage(saveDir, image)
            bytes += fileSize(converted)
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

        val resized = if(sourceImage.width > 960){resize(sourceImage, 960)}else{sourceImage}
        val destination = BufferedImage(resized.width, resized.height, BufferedImage.TYPE_INT_RGB)
        val destinationImpl = FilterImageImpl(destination)

        Filter.FilterAtkinson().process(FilterImageImpl(resized), destinationImpl)

        val outputFile = File(saveDir, source.substring(0, source.lastIndexOf(".")) + "_dithered.jpg")
        val dithered = destinationImpl.image
        //ImageIO.write(dithered, "png", outputFile)


        val jpegParams = JPEGImageWriteParam(null)
        jpegParams.compressionMode = ImageWriteParam.MODE_EXPLICIT
        jpegParams.compressionQuality = 0.1f

        val writer = ImageIO.getImageWritersByFormatName("jpg").next()
        writer.output = FileImageOutputStream(outputFile)
        writer.write(null, IIOImage(dithered, null, null), jpegParams)


        //ImageIO.write(dithered, "jpeg", outputFile)

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

    fun File.dir(): File {
        val dirPath = this.absolutePath.substring(0, this.absolutePath.lastIndexOf("/"))
        return File(dirPath)
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