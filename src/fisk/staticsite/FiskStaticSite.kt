package fisk.staticsite

import fisk.staticsite.image.ImageProcessor
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.io.File
import java.util.regex.Pattern

import java.text.DecimalFormat
import java.lang.StringBuilder

/*

    This is terrible terrible stream-of-thought code that will probably never be refactored into something sane and safe.
    It's pattern-free and debt heavy.
    You have been warned.

    It's also a static site generator in a few hundred lines of code though.

 */
fun main(args: Array<String>) {
    if(args.isEmpty() || args[0] == "help"){
        Out.help()
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
    private var links = mutableListOf<Link>()

    private var pageBytes = 0L
    private var isSingle = false
    private var webroot: File? = null

    //Dither
    private val defaultFilter = "Atkinson"
    private var threshold = 128

    enum class ImageConversion{
        NONE,
        GREYSCALE_SCALE,
        COLOR_SCALE,
        DITHER
    }

    private var defaultConversion = ImageConversion.DITHER

    companion object {
        const val TEMPLATE = "_template.html"
        const val MAX_IMG_WIDTH = 960
    }

    fun die(message: String){
        Out.l(message)
        Out.help()
        System.exit(-1)
    }

    fun build(fileARef: String){
        build(fileARef, null)
    }

    fun build(fileARef: String, fileBRef: String?) {
        Out.welcome()

        val fileA = File(fileARef)
        val fileB = when {
            !fileBRef.isNullOrEmpty() -> File(fileBRef)
            else -> File("")
        }

        when {
            fileA.exists() && fileARef.toLowerCase().endsWith(".md") && fileB.exists() && fileBRef != null && fileBRef.endsWith(TEMPLATE) -> {
                //We're converting a single file with a supplied template
                Out.d("MODE: Single index.md with supplied template")
                isSingle = true
                parseMarkdown(fileA, fileB, false)
            }
            fileA.exists() && fileARef.toLowerCase().endsWith(".md") -> {
                //Single post but look for _template.html three directories up
                Out.d("MODE: Single index.md, no supplied template")

                val rootDir = fileA.dir().parentFile.parentFile.parentFile

                Out.d("Looking for $TEMPLATE in $rootDir")

                val template = File(rootDir, TEMPLATE)
                isSingle = true
                when {
                    template.exists() -> parseMarkdown(fileA, template, false)
                    else -> die("Could not find _template.html, check it exists in root and post directory structure is correct")
                }

            }
            fileA.exists() && fileA.isDirectory -> {
                //The full iterative flow
                Out.d("MODE: Full directory flow")
                isSingle = false
                val template = File(fileA, TEMPLATE)

                when {
                    template.exists() -> {
                        webroot = fileA
                        fullBuild(fileA, template)

                        Out.l("Post conversion complete, building menu...")

                        val index = File(webroot, "index.md")

                        if(index.exists()){
                            parseMarkdown(index, template, true)

                            val linkBuilder = StringBuilder()

                            links.asReversed().forEach {link ->
                                Out.d("Add link: ${link.date} title: ${link.title} href: ${link.link}")

                                linkBuilder.append("<a href=\"")
                                linkBuilder.append("${link.link}\">")
                                linkBuilder.append("${link.date} ${link.title}</a><br>")
                            }

                            val htmlIndex = File(index.path.replace(".md", ".html"))

                            var content = htmlIndex.readText()

                            if(content.contains("{{ posts }}")){
                                content = content.replace("{{ posts }}", linkBuilder.toString())

                                htmlIndex.writeText(content)

                                Out.l("All posts converted and index updated - DONE")
                                System.exit(0)
                            }else{
                                Out.l("Could not find '{{ posts }}', skipping post index - DONE")
                                System.exit(0)
                            }
                        }else{
                            Out.l("No root index.md, skipping menu  - DONE")
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
        Out.l("Scanning project")
        root.listFiles().forEach { fileInRoot ->
            if(fileInRoot.isDirectory){
                Out.d("Found directory: $fileInRoot")

                if(fileInRoot.name.isYear()){
                    scanMonths(fileInRoot, template)
                }
            }
        }
    }

    private fun scanMonths(root: File, template: File){
        Out.l("Processing year: ${root.name}")
        root.listFiles().forEach { fileInRoot ->
            when {
                fileInRoot.isDirectory && fileInRoot.name.isMonthOrDay() -> scanDays(fileInRoot, template)
            }
        }
    }

    private fun scanDays(root: File, template: File){
        Out.l("Processing month: ${root.name}")
        root.listFiles().forEach { fileInRoot ->
            if(fileInRoot.isDirectory && fileInRoot.name.isMonthOrDay()){
                Out.l("Processing day: ${fileInRoot.name}")
                fileInRoot.listFiles().forEach {dayFile ->
                    Out.l("Inspecting file: " + dayFile.name)
                    if(dayFile.name.endsWith(".md")){
                        Out.l("Found markdown: ${dayFile.path}")
                        parseMarkdown(dayFile, template, false)
                    }
                }
            }
        }
    }

    private fun parseMarkdown(mdFile: File, template: File, isIndex: Boolean){
        Out.l("parsing Markdown...")

        val sourceMd = mdFile.readText()

        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(sourceMd)
        var convertedHtml = HtmlGenerator(sourceMd, parsedTree, flavour).generateHtml()

        convertedHtml = convertedHtml.replace("<body>", "")
        convertedHtml = convertedHtml.replace("</body>", "")
        convertedHtml = convertImages(mdFile.dir(), convertedHtml)

        val templateHtml = template.readText()

        var output = templateHtml.replace("{{ content }}", convertedHtml)

        //Add class type for images so they can override css width values - UPDATE: rethinking this
        //output = output.replaceFirst("<p><img src=\"", "<p class=\"poster\"><img src=\"")

        //Extract title from Markdown
        val firstTitleNode = parsedTree.children.firstOrNull { it.type.name =="ATX_1" }
        val title = when {
            firstTitleNode != null -> sourceMd.substring(firstTitleNode.startOffset + 1, firstTitleNode.endOffset).trim()
            else -> "FISK"
        }
        output = output.replace("{{ title }}", title)


        pageBytes += output.toByteArray().size
        output = output.replace("{{ page_size }}", "Page size including images: ${readableFileSize(pageBytes)}")

        val outputFile = File(mdFile.dir(), mdFile.nameWithoutExtension + ".html")
        outputFile.writeText(output, Charsets.UTF_8)

        Out.l("${mdFile.name} converted to ${outputFile.name}")
        Out.d(outputFile.absolutePath)

        if(!isSingle && !isIndex && outputFile.name.endsWith("index.html")) {
            val hrefLink = "." + outputFile.absolutePath.replace(webroot!!.absolutePath, "")
            Out.d("hrefLink: $hrefLink")

            val dateLabel = outputFile.dateLabel()
            val link = Link(dateLabel, title, hrefLink)
            links.add(link)

            Out.l("")
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
        ) + units[digitGroups]
    }

    private fun convertImages(saveDir: File, _html: String): String{
        var html = _html
        Out.l("Looking for image tags")
        val imagesPattern= Pattern.compile("(?:<img[^>]*src=\")([^\"]*)", Pattern.CASE_INSENSITIVE)
        val imagesMatcher = imagesPattern.matcher(html)

        val images = mutableListOf<String>()

        while (imagesMatcher.find()) {
            val image = imagesMatcher.group(1)
            Out.l("--- Image found: $image")
            images.add(image)
        }

        for(image in images){
            val converted = convertImage(saveDir, image, defaultConversion)

            when {
                converted != null -> {
                    pageBytes += fileSize(File(saveDir, converted).path)
                    html = html.replace(image, converted)
                }
                else -> pageBytes += fileSize(File(saveDir, image).path)
            }
        }

        return html
    }

    private fun fileSize(fileRef: String): Long{
        val file = File(fileRef)
        return file.length()
    }

    private fun convertImage(saveDir: File, source: String, conversion: ImageConversion): String?{
        return when(conversion){
            ImageConversion.NONE -> null
            ImageConversion.COLOR_SCALE -> ImageProcessor.colorResize(saveDir, source)
            ImageConversion.GREYSCALE_SCALE -> ImageProcessor.greyscaleResize(saveDir, source)
            ImageConversion.DITHER -> ImageProcessor.ditherResize(saveDir, source, defaultFilter, threshold)
        }
    }
}


