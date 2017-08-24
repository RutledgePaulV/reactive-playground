<html lang="en">
<head>
    <meta charset="UTF-8">
    <link href="/static/css/semantic.min.css" type="text/css" rel="stylesheet"/>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.2.13/semantic.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/rxjs/5.4.3/Rx.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/vue/2.4.2/vue.min.js"></script>
    <script src="https://unpkg.com/vue-router@2.7.0/dist/vue-router.js"></script>
    <script src="/static/js/vue-rx.js"></script>
    <script>
        window.csrf = "{[csrf]}";
    </script>
    <script src="/static/js/main.js"></script>
</head>
<body>
<div id="app">
    <router-view></router-view>
</div>
</body>
</html>