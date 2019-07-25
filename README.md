# Statisk

Static site generator heavily inspired by [Low←Tech Magazine](https://solar.lowtechmagazine.com/)

Work in progress/a hacky mess.

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
        <title>{{ title }}</title>  
    </head>
    <body>
        {{ content }}
    </body>
</html>
```

and Markdown posts kept in a `Year/Month/Day` directory structure
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