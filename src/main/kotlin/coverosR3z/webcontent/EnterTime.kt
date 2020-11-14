package coverosR3z.webcontent

import coverosR3z.domainobjects.Project

fun entertimeHTML(username: String, projects : List<Project>) : String {
    return """
    <html>
    <head>
        <title>enter time</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="entertime.css" />
        <script src="entertime.js"></script>
    </head>
    <body>
        <form action="entertime" method="post">

            <p>
                Hello there, <span id="username">$username</span>!
            </p>

            <p>
                <label for="project_entry">Project:</label>
                <select name="project_entry" id="project_entry"/>
""" +

            projects.joinToString("") { "<option value =\"${it.id}\">${it.name}</option>\n" } +

            """             <option selected disabled hidden>Choose here</option>
            </select>
            </p>

            <p>
                <label for="time_entry">Time:</label>
                <input name="time_entry" id="time_entry" type="text" />
            </p>

            <p>
                <label for="detail_entry">Details:</label>
                <input name="detail_entry" id="detail_entry" type="text" />
            </p>

            <p>
                <button id="enter_time_button">Enter time</button>
            </p>

        </form>
    </body>
</html>
"""
}

val enterTimeCSS = """
    img {
  height: 200px;
}

.date {
    text-align: right;
}

.entry {
  padding-left: 6px;
  padding-right: 3px;
}

hr {
    margin-top: 50px;
    margin-bottom: 50px;
    margin-left: 10%;
    margin-right: 10%;
}

body {
    font-family: "serif";
    font-size: larger;
    background-color: #f7f7f8;
}

.content-interior {
    text-align: center;
}

.content-header {
    font-family: sans-serif;
    font-weight: 700;
}

.content-subheader {
    font-style: italic;
    padding-left: 20px;
}

pre {
  border: 4px dashed gray;
  padding: 10px 5px 10px 10px;
	font-size: 10pt;
  overflow-x: auto;
  overflow-y: auto;
  max-height: 400px;
}

.content-body {
    line-height: 1.2em;
}

.quote {
    padding-left: 5%;
    padding-right: 5%;
    margin-bottom: 0.5em;
    font-size: 12px;
}

.prime {
    margin-left: auto;
    margin-right: auto;
    margin-top: 20px;
    border:  double gray;
    max-width: 690px;
    min-width: 300px;
    background-color: white;
}

li.menu_item {
    list-style-type:none;
    margin:0;
    padding-left:10px;
    padding-right:10px;
    display: inline;
    font: 16px monospace;
    text-align: center;
}

ul.navmenu {
    display: block;
    text-align: center;
    padding: 0px;
}

li.menu_item a{
    text-decoration: none;
}

li.menu_item a:hover{
    text-decoration: underline;
}

.secondary {
    margin: 10px;
}

.content_interior {
    margin-left: 3%;
    margin-right: 3%;
}

.content {
    margin: 0.5%;
    border: 1px solid grey;
}

a {
    color: inherit;
}

h2 {
  margin-bottom: 0;
}

a:visited {
    color: grey;
}

div.banner h2.titleheader a {
    color: inherit;
    text-decoration: none;
}

div.banner h2.titleheader a:visited {
    color: inherit;
    text-decoration: none;
}

 @media all and (max-width: 500px) {

   body {
     margin: 0;
   }

    .prime {
        margin-top: 0;
        border:  none;
    }

    .secondary {
        margin: 0;
    }
 }

 @media print {

   body {
     margin: 0;
     font-family: "serif";
     font-size: normal;
     background-color: initial;
   }

   pre {
     border: none;
     padding: 0;
     font-size: inherit;
     overflow-x: inherit;
     overflow-y: inherit;
     max-height: initial;
   }

    .prime {
      margin-left: 0;
      margin-right:0;
      margin-top: 0;
      border:  none;
      max-width: initial;
      min-width: initial;
      background-color: initial;
    }

    .secondary {
        margin: 0;
    }

    .content.banner, .content.nav {
       display: none;
    }

    .content-body {
        line-height: initial;
    }
 }
"""

val enterTimeJS = """
    console.log("Hello from JavaScript land")
"""