package coverosR3z.server.utility

class PageComponents {
    companion object {
        private const val standardHeader = """<header><a class="home-button" href="homepage">homepage</a></header>"""

        fun makeTemplate(title: String, apiFile: String, body: String, extraHeaderContent: String="") = """
<!DOCTYPE html>    
<html lang="en">
    <head>
        <link rel="stylesheet" href="general.css" />
        $extraHeaderContent
        <title>$title</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta apifile="$apiFile" >
    </head>
    <body>
        $standardHeader
        $body
    </body>
</html>
"""

    }

}