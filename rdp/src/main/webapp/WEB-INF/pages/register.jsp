<!DOCTYPE html>
<html lang="en" class="no-js">
<head>
<meta charset="UTF-8" />
<title>Rare Disease Project Registration</title>

<!-- Bootstrap core CSS -->
<link href="styles/bootstrap.min.css" rel="stylesheet">

<!-- Optional Bootstrap Theme -->
<link href="data:text/css;charset=utf-8,"
    data-href="styles/bootstrap-theme.min.css" rel="stylesheet"
    id="bs-theme-stylesheet">

</head>

<body id="register">

    <div id="content">

        <header>Rare Disease Project Registration</header>

        <!-- TODO FIXME -->
        <bold>TODO FIXME</bold>

        <form></form>

        <!-- User signup -->
        <%@ include file="editUser.jsp"%>

        <!-- Change Password -->
        <button class="btn btn-primary" data-toggle="modal"
            data-target="#changePasswordModal">Change Password</button>

        <!-- Logout -->
        <form>
            <div>
                <input type="submit" id="btnLogout" value="Logout">
            </div>
        </form>

    </div>

    <!-- include jQuery, and our script file -->
    <script src="scripts/lib/jquery-1.11.1.min.js"></script>
    <script src="scripts/lib/bootstrap.min.js"></script>
    <script src="scripts/lib/jquery.validate.min.js"></script>

    <!-- Our scripts -->
    <script src="scripts/api/register.js"></script>

</body>
</html>