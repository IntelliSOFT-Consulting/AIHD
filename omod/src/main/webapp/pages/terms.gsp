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
        body {
            font-family: "OpenSans", Arial, sans-serif;
            -webkit-font-smoothing: subpixel-antialiased;
            margin: 5px auto;
            background-color: white;
            background-repeat: no-repeat;
            height: 100%;
        }
        form {border: 3px solid #f1f1f1;}
        input[type=text], input[type=password] {
            width: 100%;
            padding: 12px 20px;
            margin: 8px 0;
            display: inline-block;
            border: 1px solid #ccc;
            box-sizing: border-box;
        }

        button {
            background-color: #4CAF50;
            color: white;
            padding: 14px 20px;
            margin: 8px 0;
            border: none;
            cursor: pointer;
            width: 100%;
        }

        button:hover {
            opacity: 0.8;
        }
        .imgcontainer {
            text-align: center;
            margin: 24px 0 12px 0;
        }

        .container {
            padding: 16px;
        }
        .logo {
            margin: 0px;
            text-align: center;
        }
        #error-message {
            color: #B53D3D;
            text-align: center;
        }
        .footer{
            float: left;
            margin: 0px 15px;
            width: 80%;
            display: inline-block;
            font-size: 0.7em;
            color: #808080;
        }
        .footer .left_al {
            float: left;
        }
        .footer .right_al{
            float: right;
        }
        .footer .center_al{
            float: center;
         }
        .footer a{
            color: #404040;
            font-size: 1em;
            padding: 5px;
            text-decoration: none;
        }
        .footer a:hover{
            color: #404040;
            font-size: 1em;
            padding: 5px;
            text-decoration: underline;
        }
        .footer a:active{
            color: #404040;
            font-size: 1em;
            padding: 5px;
            text-decoration: none;
        }
        .footer a:after{
            color: #404040;
            font-size: 1em;
            padding: 5px;
            text-decoration: none;
        }
        header {
            line-height: 1em;
            -moz-border-radius: 5px;
            -webkit-border-radius: 5px;
            -o-border-radius: 5px;
            -ms-border-radius: 5px;
            -khtml-border-radius: 5px;
            border-radius: 5px;
            position: relative;
            background-color: white;
            color: #CCC;
            align: left;
        }
        header .logo img {
            width: 200px;
        }
        header .logo {
            float: none;
            margin: 4px;
        }
        #login-form ul.select {
            padding: 10px;
            background: beige;
        }
        ul.select li.selected {
            background-color: #94979A;
            color: white;
            border-color: transparent;
            -moz-border-radius: 5px;
            -webkit-border-radius: 5px;
            -o-border-radius: 5px;
            -ms-border-radius: 5px;
            -khtml-border-radius: 5px;
            border-radius: 5px;
            padding: 5px;
            text-align: center;
        }
        ul.select li:hover {
            background-color: #AB3A15;
            color: white;
            cursor: pointer;
        }
        ul.select li {
            margin: 0 10px;
            text-align: left;
            display: inline-block;
            width: 20%;
            padding: 5px;
            color: #3B6692;
            background-color: #FFF;
            /* border-bottom: 1px solid #efefef; */
            border: dashed 1px #CEC6C6;
            text-align: center;
        }
        form fieldset, .form fieldset {
            border: solid 1px #CECECE;
            -moz-border-radius: 5px;
            -webkit-border-radius: 5px;
            -o-border-radius: 5px;
            -ms-border-radius: 5px;
            -khtml-border-radius: 5px;
            border-radius: 5px;
            background: #FFFFFB;
            margin-left: 8%;
        }
        #county-logo{
            margin-left: 0;
            padding-top: 0;
            height: 170px;
            width: auto;
            align:left;
        }

        #bmz-logo img{
            width: 100%;
            height:auto;
        }
        #bmz-logo{
                width: auto;
                float: left;
                padding: 5px;
                align:left;
        }

        #malteser-logo img{
            width: auto;
            float: left;
            border-right-style: solid;
            border-right-width: 0;
            border-right-color: #e5e5e5;
            padding: 0;
            align:left;
        }
        #aihd-logo img{
            width: 100%;
            float: left;
            padding: 0;
            align:left;
        }
    </style>
</head>
<body>
<script type="text/javascript">
    var OPENMRS_CONTEXT_PATH = '${ ui.contextPath() }';
</script>


${ ui.includeFragment("referenceapplication", "infoAndErrorMessages") }

<div class="container">
    <section>
        <div class="page-header">
            <a href="${ui.pageLink("referenceapplication", "home")}" class="pull-left">
                <img src="${ui.resourceLink("aihdconfigs", "images/banners/city_county_logo.jpg")}" id="county-logo"/>
            </a>
            <div class="clearfix"></div>
        </div>
    </section>
 </div>
</body>
</html>
