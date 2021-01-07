package coverosR3z.server.utility

const val successHTML = """
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
"""

const val failureHTML = """
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
"""