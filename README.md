# Statisk

Static site generator heavily inspired by [Low←Tech Magazine](https://solar.lowtechmagazine.com/)

Work in progress/a hacky mess. DO NOT USE.

Converts Markdown files to simple Html with monochrome dithered images creating extremely low-bandwidth web pages.

![screenshot](screenshot.png)

### Links

* https://github.com/lowtechmag/solar/wiki/Solar-Web-Design

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

## Filter Override

Not all images look great with the default filter settings. To override add `filter_override` to the image name followed by the filter (See Filter.kt). To override the standard threshold value of 128 end the filename with `_255.jpeg`, you must also override the filter to trigger this, eg.

```example_filter_override_8by8Bayer_255.jpeg```


To keep the original image entirely add `no_transform`, eg:

```example_no_transform.jpeg```

To have no filter but still scale the image to a reasonable size use `no_filter`:

```example_no_filter.jpeg```

## Future Plans

* RSS feed
