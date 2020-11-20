package coverosR3z.authentication

import coverosR3z.domainobjects.Employee

fun registerHTML(employees: List<Employee>) : String {
    return """
<!DOCTYPE html>        
<html>
  <head>
      <link rel="stylesheet" href="register.css">
      <link href="https://fonts.googleapis.com/css?family=Ubuntu" rel="stylesheet">
      <title>register</title>
      <link rel="stylesheet" href="register.css" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
  </head>
  <header><a href="homepage">r3z</a></header>
  <body>
    <br>
    <h2>Register a User</h2>
    
    <form method="post" action="register">
      <div class="container"> 
        <label for="username">Username</label>
        <input class="input" type="text" name="username" id="username">
        <label for="password">Password</label>
        <input class="input" type="password" name="password" id="password">
        <label for="employee">Employee</label>
        <select class="input" id="employee" name="employee">
"""+employees.joinToString("") {
        "<option value =\"${it.id.value}\">${it.name.value}</option>\n"
    } +
            """
          <option selected disabled hidden>Choose here</option>
        </select>
        <button id="register_button" class="submit">Register</button>
      </div>
    </form>
  </body>
</html>
"""
}