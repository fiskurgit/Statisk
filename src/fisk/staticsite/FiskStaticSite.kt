package fisk.staticsite

import fisk.staticsite.image.ImageProcessor
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.awt.Color
import java.io.File
import java.util.regex.Pattern

import java.lang.StringBuilder
import java.util.zip.GZIPOutputStream
import java.io.OutputStreamWriter
import java.io.FileOutputStream

/*

    This is terrible terrible stream-of-thought code that will probably never be refactored into something sane and safe.
    It's pattern-free and debt heavy.
    You have been warned.

    It's also a static site generator in a few hundred lines of code though.

 */
fun main(args: Array<String>) {
    Generator().parseStartupArguments(args)
}

class Generator {

    data class Link(val date: String, val title: String, val link: String)
    private var links = mutableListOf<Link>()

    private var pageBytes = 0L
    private var isSingle = false
    private var webroot: File? = null

    var directoryArg: String? = null
    var fileArg: String? = null
    var templateArg: String? = null

    companion object {
        const val TEMPLATE = "_template.html"

        var config = Config()
        var configBackup: Config? = null
    }

    fun parseStartupArguments(args: Array<String>) {
        if (args.isEmpty() || args[0] == "help"){
            Out.help()
        }

        if(args.size == 1){
            val arg = args[0]
            val argFile = File(arg)
            if(arg.endsWith(".md") && argFile.exists()){
                fileArg = arg
            }else if(argFile.exists() && argFile.isDirectory){
                directoryArg = arg
            }
        }else if(args.size == 2){
            val argA = args[0]
            val argFileA = File(argA)

            val argB = args[1]
            val argFileB = File(argB)

            if(argA.endsWith(".md")){
                //Hopefully passing index.md and _template.html
                if(argFileA.exists() && argB.endsWith(".html") && argFileB.exists()){
                    fileArg = argA
                    templateArg = argB
                }else{
                    Out.die("Bad arguments, check you're passing a valid reference to a markdown file and Statisk html template")
                }
            }else if(argA.endsWith(".html")){
                //Hopefully passing _template.html and index.md
                if(argFileA.exists() && argB.endsWith(".md") && argFileB.exists()){
                    templateArg = argA
                    fileArg = argB
                }else{
                    Out.die("Bad arguments, check you're passing a valid reference to a markdown file and Statisk html template")
                }
            }
        }else {
            extractArgs(args, config)
        }

        when {
            directoryArg != null -> {
                //Iterate directories
                build(directoryArg!!, null)
            }
            fileArg != null && templateArg == null -> {
                //Build single with root template
                build(fileArg!!, null)
            }
            fileArg != null && templateArg != null -> {
                //Build single with supplied template
                build(fileArg!!, templateArg)
            }
            else -> Out.die("Bad arguments. see -help for usage instructions")
        }
    }

    private fun extractArgs(args: Array<String>, cfg: Config){
        var argIndex = 0
        args.forEach { arg ->
            when (arg) {
                //Flags:
                "-convert_none" -> cfg.imageConversion = ImageProcessor.ImageConversion.NONE
                "-convert_greyscale" -> cfg.imageConversion = ImageProcessor.ImageConversion.GREYSCALE_SCALE
                "-convert_color" -> cfg.imageConversion = ImageProcessor.ImageConversion.COLOR_SCALE
                "-convert_dither" -> cfg.imageConversion = ImageProcessor.ImageConversion.DITHER
                "-gzip" -> cfg.gzip = true
                //Settings requiring arguments:
                "-image_format" -> {
                    if (argIndex + 1 < args.size) {
                        val imageFormat = args[argIndex + 1]
                        when (imageFormat) {
                            "png" -> cfg.imageFormat = ImageProcessor.ImageSaveFormat.PNG
                            "jpeg" -> cfg.imageFormat = ImageProcessor.ImageSaveFormat.JPEG_MED
                            "jpeg_high" -> cfg.imageFormat = ImageProcessor.ImageSaveFormat.JPEG_HI
                            "jpeg_medium" -> cfg.imageFormat = ImageProcessor.ImageSaveFormat.JPEG_MED
                            "jpeg_low" -> cfg.imageFormat = ImageProcessor.ImageSaveFormat.JPEG_LO
                        }
                    } else {
                        Out.die("-image_format requires png, jpeg, jpeg_high, jpeg_medium, or jpeg_low")
                    }
                }
                "-maxwidth" -> {
                    if (argIndex + 1 < args.size) {
                        val maxWidthArg = args[argIndex + 1].toIntOrNull()
                        if (maxWidthArg == null) {
                            Out.die("Bad argument: -maxwidth requires a number")
                        } else {
                            cfg.maxImageWidth = maxWidthArg
                        }
                    } else {
                        Out.die("-maxwidth requires number")
                    }
                }
                "-algorithm" -> {
                    if (argIndex + 1 < args.size) {
                        val requestedAlgorithm = args[argIndex + 1]
                        val filter = Filter.find(requestedAlgorithm)
                        if (filter != null) {
                            cfg.imageConversion = ImageProcessor.ImageConversion.DITHER
                            cfg.ditherFilter = filter
                        } else {
                            Out.die("-algorithm filter $requestedAlgorithm not recognised, see -help for available options")
                        }
                    } else {
                        Out.die("-algorithm requires a dithering algorithm, see -help for available options")
                    }

                }
                "-threshold" -> {
                    if (argIndex + 1 < args.size) {
                        val thresholdArg = args[argIndex + 1].toIntOrNull()
                        if (thresholdArg == null) {
                            Out.die("Bad argument: -threshold requires a value in range 0 to 255")
                        } else {
                            cfg.threshold = thresholdArg
                        }
                    } else {
                        Out.die("-threshold requires a value in range 0 to 255")
                    }
                }
                "-foreground" -> {
                    if (argIndex + 1 < args.size) {
                        val foregroundArg = args[argIndex + 1]
                        val foreground = Color.decode(foregroundArg)
                        config.foregroundColor = foreground
                    } else {
                        Out.die("-threshold requires a value in range 0 to 255")
                    }
                }
                "-dir" -> {
                    if (argIndex + 1 < args.size) {
                        directoryArg = args[argIndex + 1]
                    } else {
                        Out.die("-dir requires a directory")
                    }
                }
                "-single" -> {
                    if (argIndex + 1 < args.size) {
                        fileArg = args[argIndex + 1]
                    } else {
                        Out.die("-dir requires a directory")
                    }
                }
                "-template" -> {
                    if (argIndex + 1 < args.size) {
                        templateArg = args[argIndex + 1]
                    } else {
                        Out.die("-template requires a Statisk template to use")
                    }
                }
            }

            argIndex++
        }
    }

    private fun build(fileARef: String, fileBRef: String?) {
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
                    else -> Out.die("Could not find _template.html, check it exists in root and post directory structure is correct")
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
                    else -> Out.die("Could not find _template.html, check it exists in the website root and you're passing the correct path")
                }
            }
            else -> Out.die("Bad arguments, check your file references")
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

        checkConfigOverride(mdFile)


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
        output = output.replace("{{ page_size }}", "Page size including images: ${pageBytes.bytesToLabel()}")

        val outputFile = File(mdFile.dir(), mdFile.nameWithoutExtension + ".html")
        outputFile.writeText(output, Charsets.UTF_8)

        if(config.gzip){
            val gzipFile = File(mdFile.dir(), mdFile.nameWithoutExtension + ".gz")
            val gzipFOS = FileOutputStream(gzipFile)
            gzipFOS.use { fos ->
                val writer = OutputStreamWriter(GZIPOutputStream(fos), "UTF-8")
                writer.use { osw ->
                    osw.write(output)
                }
            }
        }

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

        if(configBackup != null) {
            config = configBackup!!.copy()
            configBackup = null
        }
        //ends
    }

    private fun checkConfigOverride(file: File){
        var firstLine = file.bufferedReader().use { it.readLine() }

        Out.l("checkConfigOverride: $firstLine")

        if(firstLine.startsWith("<!---") && firstLine.trim().endsWith("-->")){
            configBackup = config.copy()

            firstLine = firstLine.removePrefix("<!---")
            firstLine = firstLine.removeSuffix("-->")
            firstLine = firstLine.trim()

            val argsOverride = firstLine.split(",").toTypedArray()
            extractArgs(argsOverride, config)
        }
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
            val converted = ImageProcessor.convertImage(saveDir, image)

            when {
                converted != null -> {
                    pageBytes += File(saveDir, converted).path.fileSize()
                    html = html.replace(image, converted)
                }
                else -> pageBytes += File(saveDir, image).path.fileSize()
            }
        }

        return html
    }
}


