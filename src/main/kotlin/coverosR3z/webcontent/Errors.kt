package coverosR3z.webcontent

val badRequestHTML = """
<html>
    <head>
    </head>
        <title>400 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p>400 error - BAD REQUEST</p>
    </body>
</html>
"""

val notFoundHTML = """
<html>
    <head>
    </head>
        <title>404 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p>404 error - NOT FOUND</p>
    </body>
</html>
"""

val unauthorizedHTML = """
<html>
    <head>
    </head>
        <title>401 error</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p>401 error - UNAUTHORIZED</p>
    </body>
</html>        
"""

fun generalMessageHTML(msg : String) : String {
    return """
<html>
    <head>
    </head>
        <title>general error page</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <body>
       <p><a href="homepage">Homepage</a></p>
       <p>Error message: $msg</p>
       <p></p>
    </body>
</html>        
"""
}