package coverosR3z.webcontent

val loginHTML = """
<html>
    <head>
        <title>login</title>
    </head>
    <body>
            <h2>Login</h2>
            <form action="login" method="post">

                <label for="username">Username</label>
                <input type="text" name="username" id="username" />

                <label for="password">Password</label>
                <input type="password" name="password" id="password" />

                <button id="login_button">Login</button>
            </form>
    </body>
</html>        
"""