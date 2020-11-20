package coverosR3z.webcontent

import coverosR3z.misc.toBytes

val successHTML = toBytes("""
    <!DOCTYPE html>
    <html>
    <head>
    </head>
        <title>SUCCESS</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p>SUCCESS</p>
        <p><a href="homepage">Homepage</a></p>
    </body>
</html>
""")

val failureHTML = toBytes("""
<!DOCTYPE html>    
<html>
    <head>
    </head>
        <title>FAILURE</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p>FAILURE</p>
    </body>
</html>        
""")