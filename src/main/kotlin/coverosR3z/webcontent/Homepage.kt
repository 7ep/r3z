package coverosR3z.webcontent

import coverosR3z.misc.toBytes

val homepageHTML = toBytes("""
<!DOCTYPE html>    
<html>
    <head>
      <link rel="stylesheet" href="general.css" />
    </head>
        <title>Homepage</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    <header><a class="home-button" href="homepage">r3z</a></header>
    <body>
        <div class="container">
            <h2>You are on the homepage</h2>
            <p><a href="login">Login</a></p>
            <p><a href="register">Register</a></p>
        </div>
    </body>
</html>
""")