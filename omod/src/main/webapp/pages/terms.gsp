<%
    ui.includeFragment("appui", "standardEmrIncludes")
    ui.includeJavascript("aihdconfigs", "bootstrap.min.js")
    ui.includeCss("referenceapplication", "login.css")
    ui.includeCss("aihdconfigs", "bootstrap.min.css")

    def now = new Date()
    def year = now.getAt(Calendar.YEAR);
%>

<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Terms & Conditions</title>
    <link rel="shortcut icon" type="image/ico" href="/${ ui.contextPath() }/images/openmrs-favicon.ico"/>
    <link rel="icon" type="image/png\" href="/${ ui.contextPath() }/images/openmrs-favicon.png"/>
    ${ ui.resourceLinks() }

    <style media="screen" type="text/css">
        html {
            height: 100%;
        }
        body {
            font-family: "OpenSans", Arial, sans-serif;
            -webkit-font-smoothing: subpixel-antialiased;
            margin: 0;
            padding: 0;
            background-color: white;
            background-repeat: no-repeat;
            height: 100%;
            min-width: 100%;
        }
    </style>
</head>
<body>
<script type="text/javascript">
    var OPENMRS_CONTEXT_PATH = '${ ui.contextPath() }';
</script>


${ ui.includeFragment("referenceapplication", "infoAndErrorMessages") }

<div class="container" style="height: 100%; width: 100%; padding: 0">
    <section style="height: 100%">
        <iframe src="${ui.resourceLink("aihdconfigs", "documents/privacy_policy.pdf")}" style= "height:100%; width:100%"></iframe>
    </section>
 </div>
</body>
</html>
