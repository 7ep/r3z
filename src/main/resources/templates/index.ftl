<#-- @ftlvariable name="data" type="com.coveros.IndexData" -->
<html>
    <head>
        <title>Library</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="static/main.css">

    </head>
    <body>
        <p>Hi everybody!</p>
        <ul>
        <#list data.items as item>
            <li>${item}</li>
        </#list>
        </ul>

        <form method="post" action="login" autocomplete="off" class="regular-form">

            <h2>Librarian login</h2>
            <label for="login_username">Name:</label>
            <p><input type="text" id="login_username" name="username" placeholder="name"/></p>

            <label for="login_password">Password:</label>
            <p><input type="password" id="login_password" name="password" placeholder="password123"/></p>

            <p><input type="submit" id="login_submit" value="login"/></p>
        </form>

        <form method="post" action="register" autocomplete="off" class="regular-form">

            <h2>Register librarian</h2>
            <label for="register_username">Name:</label>
            <p><input type="text" id="register_username" name="username" placeholder="name"/></p>

            <label for="register_password">Password:</label>
            <p><input type="password" id="register_password" name="password" placeholder="password123"/></p>
            <p><input type="submit" id="register_submit" value="register"/></p>
        </form>

        <form method="post" action="lend" autocomplete="off" class="regular-form">

            <h2>Borrow a book</h2>

            <label for="lend_book">Book:</label>
            <p><input type="text" id="lend_book" name="book" placeholder="book"/></p>

            <label for="lend_borrower">Borrower:</label>
            <p><input type="text" id="lend_borrower" name="borrower" placeholder="borrower"/></p>

            <p><input type="submit" id="lend_book_submit" value="lend"/></p>
        </form>


        <form method="post" action="registerbook" autocomplete="off" class="regular-form">

            <h2>Register a book</h2>

            <label for="register_book">Book:</label>
            <p><input type="text" id="register_book" name="book" placeholder="book"/></p>

            <p><input type="submit" id="register_book_submit" value="register"/></p>
        </form>


        <form method="post" action="registerborrower" autocomplete="off" class="regular-form">

            <h2>Register a borrower</h2>

            <label for="book">Borrower:</label>
            <p><input type="text" id="register_borrower" name="borrower" placeholder="borrower"/></p>

            <p><input type="submit" id="register_borrower_submit" value="register"/></p>
        </form>

        <div class="buttons">
          <h2>Database versioning</h2>
          <form action="flyway" class="button-form">
              <input type="hidden" name="action" value="clean"/>
              <input type="submit" value="Clean"/>
          </form>
          <form action="flyway" class="button-form">
              <input type="hidden" name="action" value="migrate"/>
              <input type="submit" value="Migrate"/>
          </form>
          <form action="flyway" class="button-form">
              <input type="submit" value="Clean / Migrate"/>
          </form>
            <p><a href="console">Database console</a></p>
            <p><a href="dbhelp.html">Console help</a></p>
        </div>
    </body>
</html>
