# Statisk

Static site generator heavily inspired by [Low←Tech Magazine](https://solar.lowtechmagazine.com/)

Work in progress/a hacky mess. DO NOT USE.

Converts Markdown files to simple Html with support for extreme image size reduction for low-bandwidth web pages.

![screenshot](screenshot.png)

### Format

`_template.html` should be placed in root directory of the website, simple example:

```html
<!DOCTYPE html>
<html>
    <head>
        <!-- Optional -->
        <title>{{ title }}</title>  
    </head>
    <body>
        {{ content }}
        <footer>
            <!-- Optional -->
            {{ page_size }}
        </footer>
    </body>
</html>
```

An `index.md` in the root should include `{{ posts }}` where a list of posts will be rendered.

Markdown posts need to be in a Year/Month/Day (`YYYY/MM/DD`) directory structure:
<pre style="font-family: monospace;">
|-_layout.html  
|- 2020/  
    |- 01/ 
        |-20/ 
           |- index.md   
           |- picture.png  
        |-15/ 
           |- index.md
|- 2019/  
    |- 12/    
        |-24/ 
           |- index.md
           |- pictureA.png 
           |- pictureB.png 
</pre> 

## Usage

* `statisk path/to/websiteroot/` - Statisk will then iterate over the directories and convert the markdown and images
* `statisk index.md` - Convert single post and images, Statisk will look for `_template.html` thee directories up in the hierarchy
* `statisk index.md path/to/_a_new_template.html` - Convert single post and images using the supplied template.

## Image Conversion

The default behaviour is to resize any images larger than 960px to 960px. You can override this with various options:

* `statisk -dir path/to/websiteroot/ -convert_color` - this is the default behaviour, images are resized to max image width (default is 960px)
* `statisk -dir path/to/websiteroot/ -convert_none` - leave all images as they are
* `statisk -dir path/to/websiteroot/ -convert_greyscale` - reduce image filesizes by converting to greyscale and resizing to max image width
* `statisk -dir path/to/websiteroot/ -convert_dither` - use the default monochrome dither algorithm (Atkinson) to drastically reduce image sizes
* `statisk -dir path/to/websiteroot/ -algorithm atkinson` - specify dither algorithm to drastically reduce image sizes (see available options below)
* `statisk -dir path/to/websiteroot/ -dither -threshold 255` - set the the threshold value for dithering, default is 128

The default image file format is .png, you can specify jpeg:

* `statisk -dir path/to/websiteroot/ -image_format jpeg-high` - options are `png`, `jpeg` (medium quality: 0.65), `jpeg_low` (0.5), `jpeg_medium`, or `jpeg_high` (0.85) 

### Other Flags

* `-gzip` - saves a compressed file alongside any html so the server can save some processing ([if supported](http://nginx.org/en/docs/http/ngx_http_gzip_static_module.html)), eg. index.html, index.gz

## Dithering

_TODO_

## Future Plans

* RSS feed

### Links

* https://github.com/lowtechmag/solar/wiki/Solar-Web-Design
* http://nginx.org/en/docs/http/ngx_http_gzip_static_module.html

## Issues/To do

* ...
